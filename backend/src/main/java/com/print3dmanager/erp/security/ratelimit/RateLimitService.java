package com.print3dmanager.erp.security.ratelimit;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Contador de requisições por chave (grupo de rota + IP) em janela fixa,
 * em memória — suficiente para o monólito de instância única; num cluster
 * o estado precisaria ir para um armazenamento compartilhado (ex.: Redis).
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
     * Registra uma tentativa e informa se ela ainda cabe no limite da
     * janela corrente. A primeira chamada da janela sempre é permitida.
     */
    public boolean permitir(String chave, int limite, int janelaSegundos) {
        long agora = clock.millis();
        long janelaMs = janelaSegundos * 1_000L;

        limparSeNecessario(agora, janelaMs);
        Janela janela = janelas.compute(chave, (ignorada, atual) ->
                atual == null || agora - atual.inicioMs >= janelaMs
                        ? new Janela(agora)
                        : atual);
        return janela.contagem.incrementAndGet() <= limite;
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
        private final AtomicInteger contagem = new AtomicInteger();

        private Janela(long inicioMs) {
            this.inicioMs = inicioMs;
        }
    }
}
