package io.github.johnsampa.creatorpay.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Relógio injetável: as regras que dependem de "agora" (ex.: há quantas horas o carrinho foi
 * abandonado) ficam testáveis com um relógio fixo.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
