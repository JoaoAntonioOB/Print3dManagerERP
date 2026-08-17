package com.print3dmanager.erp.security.ratelimit;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contador de requisições por chave (grupo de rota + IP) em janela deslizante
 * ("sliding window counter"), em memória — suficiente para o monólito de
 * instância única; num cluster o estado precisaria ir para um armazenamento
 * compartilhado (ex.: Redis).
 *
 * <p>As janelas são fixas e alinhadas à época (blocos de {@code janelaSegundos}
 * desde o epoch), mas a contagem usada para decidir o limite pondera a
 * contagem da janela anterior pela fração dela ainda "dentro" da janela
 * deslizante de {@code janelaSegundos} terminando agora: {@code estimativa =
 * contagemAnterior * (tempoRestanteDaJanelaAnterior / duracaoDaJanela) +
 * contagemAtual}. Isso evita a rajada de até 2× o limite que uma janela fixa
 * pura permite na fronteira entre duas janelas (N no fim de uma + N no
 * início da seguinte).</p>
 */
@Service
public class RateLimitService {

    /** Acima disso, entradas de janelas passadas são varridas do mapa. */
    private static final int TAMANHO_LIMPEZA = 10_000;

    private final ConcurrentHashMap<String, Janela> janelas = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimitService() {
        this(Clock.systemUTC());
    }

    RateLimitService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Registra uma tentativa e informa se a estimativa ponderada (janela
     * anterior + janela corrente) ainda cabe no limite. A primeira chamada
     * de uma chave nova sempre é permitida.
     */
    public boolean permitir(String chave, int limite, int janelaSegundos) {
        long agora = clock.millis();
        long janelaMs = janelaSegundos * 1_000L;
        long inicioJanelaAtual = agora - Math.floorMod(agora, janelaMs);

        limparSeNecessario(agora, janelaMs);
        Janela janela = janelas.compute(chave, (ignorada, atual) ->
                proximaJanela(atual, inicioJanelaAtual, janelaMs));

        int contagemAtual = janela.contagemAtual.incrementAndGet();
        long decorridoMs = agora - janela.inicioMs;
        double pesoJanelaAnterior = Math.max(0d, (janelaMs - decorridoMs) / (double) janelaMs);
        double estimativa = janela.contagemAnterior * pesoJanelaAnterior + contagemAtual;
        return estimativa <= limite;
    }

    /**
     * Reaproveita a janela corrente se a chave já está nela; caso contrário
     * inicia uma nova janela, herdando a contagem da anterior só quando ela
     * é imediatamente adjacente (janelas puladas por inatividade não têm
     * nenhuma sobreposição com a atual, então não pesam).
     */
    private static Janela proximaJanela(Janela atual, long inicioJanelaAtual, long janelaMs) {
        if (atual != null && atual.inicioMs == inicioJanelaAtual) {
            return atual;
        }
        long contagemAnterior = atual != null && inicioJanelaAtual - atual.inicioMs == janelaMs
                ? atual.contagemAtual.get()
                : 0;
        return new Janela(inicioJanelaAtual, contagemAnterior);
    }

    /** Segundos até a janela da chave expirar — valor do header Retry-After. */
    public long segundosParaLiberar(String chave, int janelaSegundos) {
        Janela janela = janelas.get(chave);
        if (janela == null) {
            return janelaSegundos;
        }
        long decorridoMs = clock.millis() - janela.inicioMs;
        return Math.max(1, janelaSegundos - decorridoMs / 1_000);
    }

    /** Evita crescimento sem fim do mapa removendo janelas já expiradas. */
    private void limparSeNecessario(long agora, long janelaMs) {
        if (janelas.size() > TAMANHO_LIMPEZA) {
            janelas.values().removeIf(janela -> agora - janela.inicioMs >= janelaMs);
        }
    }

    private static final class Janela {
        private final long inicioMs;
        /** Contagem final (congelada) da janela imediatamente anterior, para ponderação. */
        private final long contagemAnterior;
        private final AtomicInteger contagemAtual = new AtomicInteger();

        private Janela(long inicioMs, long contagemAnterior) {
            this.inicioMs = inicioMs;
            this.contagemAnterior = contagemAnterior;
        }
    }
}
