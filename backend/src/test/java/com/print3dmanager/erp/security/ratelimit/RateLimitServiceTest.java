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
    @DisplayName("janela expirada zera a contagem")
    void janelaExpiradaReseta() {
        for (int i = 0; i < 4; i++) {
            service.permitir("auth:1.1.1.1", 3, 60);
        }
        relogio.avancarSegundos(61);

        assertThat(service.permitir("auth:1.1.1.1", 3, 60)).isTrue();
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
