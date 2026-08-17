package com.print3dmanager.erp.security.ratelimit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitServiceTest {

    /** Relógio controlável pelos testes. */
    private static final class RelogioTeste extends Clock {
        private Instant agora = Instant.parse("2026-07-18T12:00:00Z");

        void avancarSegundos(long segundos) {
            agora = agora.plusSeconds(segundos);
        }

        @Override
        public Instant instant() {
            return agora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final RelogioTeste relogio = new RelogioTeste();
    private final RateLimitService service = new RateLimitService(relogio);

    @Test
    @DisplayName("permite até o limite e bloqueia a partir da requisição seguinte")
    void bloqueiaAcimaDoLimite() {
        for (int i = 0; i < 3; i++) {
            assertThat(service.permitir("auth:1.1.1.1", 3, 60)).isTrue();
        }
        assertThat(service.permitir("auth:1.1.1.1", 3, 60)).isFalse();
    }

    @Test
    @DisplayName("depois de mais de duas janelas sem atividade a contagem anterior deixa de pesar")
    void janelaMuitoAntigaNaoPesa() {
        for (int i = 0; i < 4; i++) {
            service.permitir("auth:1.1.1.1", 3, 60);
        }
        relogio.avancarSegundos(181);

        assertThat(service.permitir("auth:1.1.1.1", 3, 60)).isTrue();
    }

    @Test
    @DisplayName("rajada na fronteira entre janelas é bloqueada pela ponderação da janela anterior")
    void rajadaNaFronteiraEBloqueada() {
        // 3 requisições no fim de uma janela de 60s (limite cheio).
        for (int i = 0; i < 3; i++) {
            assertThat(service.permitir("auth:1.1.1.1", 3, 60)).isTrue();
        }
        // Avança 61s: já é a janela seguinte (que começou 1s atrás). Numa
        // janela fixa "pura" isso liberaria mais 3 requisições imediatamente
        // (rajada de até 2× o limite numa janela deslizante real de 60s
        // centrada na fronteira); com a ponderação da janela anterior, a
        // estimativa ainda reflete quase toda a contagem anterior e bloqueia.
        relogio.avancarSegundos(61);

        assertThat(service.permitir("auth:1.1.1.1", 3, 60)).isFalse();
    }

    @Test
    @DisplayName("no meio da nova janela o peso da anterior já caiu e libera requisições parciais")
    void meioDaJanelaLiberaParcialmente() {
        for (int i = 0; i < 3; i++) {
            service.permitir("auth:1.1.1.1", 3, 60);
        }
        // Metade da janela seguinte já passou: peso da anterior cai para ~0.5
        // (3 * 0.5 = 1.5), então uma única requisição nova (estimativa ~2.5)
        // ainda cabe no limite de 3, mas uma segunda (~3.5) não.
        relogio.avancarSegundos(90);

        assertThat(service.permitir("auth:1.1.1.1", 3, 60)).isTrue();
        assertThat(service.permitir("auth:1.1.1.1", 3, 60)).isFalse();
    }

    @Test
    @DisplayName("chaves diferentes (outro IP ou grupo) têm contadores independentes")
    void chavesIndependentes() {
        for (int i = 0; i < 4; i++) {
            service.permitir("auth:1.1.1.1", 3, 60);
        }

        assertThat(service.permitir("auth:2.2.2.2", 3, 60)).isTrue();
        assertThat(service.permitir("public:1.1.1.1", 3, 60)).isTrue();
    }

    @Test
    @DisplayName("Retry-After decresce com o tempo e nunca é menor que 1")
    void segundosParaLiberarDecresce() {
        service.permitir("auth:1.1.1.1", 3, 60);

        assertThat(service.segundosParaLiberar("auth:1.1.1.1", 60)).isEqualTo(60);
        relogio.avancarSegundos(45);
        assertThat(service.segundosParaLiberar("auth:1.1.1.1", 60)).isEqualTo(15);
        relogio.avancarSegundos(20);
        assertThat(service.segundosParaLiberar("auth:1.1.1.1", 60)).isEqualTo(1);
    }
}
