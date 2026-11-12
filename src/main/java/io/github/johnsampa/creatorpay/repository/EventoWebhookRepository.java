package io.github.johnsampa.creatorpay.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.johnsampa.creatorpay.domain.EventoWebhook;

public interface EventoWebhookRepository extends JpaRepository<EventoWebhook, String> {
}
