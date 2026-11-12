package io.github.johnsampa.creatorpay.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PedidoResumoTest {

    @Test
    void mascaraOEmailMantendoODominio() {
        assertThat(PedidoResumo.mascararEmail("ana.souza@email.test")).isEqualTo("an***@email.test");
    }

    @Test
    void mascaraEmailComUsuarioCurto() {
        assertThat(PedidoResumo.mascararEmail("a@email.test")).isEqualTo("a***@email.test");
    }

    @Test
    void emailInvalidoViraSoAsteriscos() {
        assertThat(PedidoResumo.mascararEmail("sem-arroba")).isEqualTo("***");
    }
}
