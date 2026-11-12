package io.github.johnsampa.creatorpay.webhook;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.johnsampa.creatorpay.service.RecursoNaoEncontradoException;
import tools.jackson.core.JacksonException;

/** Tratamento de erros só do webhook: não interfere nas respostas do endpoint MCP. */
@RestControllerAdvice(assignableTypes = WebhookController.class)
public class WebhookExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<Map<String, String>> naoEncontrado(RecursoNaoEncontradoException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> requisicaoInvalida(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", e.getMessage()));
    }

    @ExceptionHandler(JacksonException.class)
    public ResponseEntity<Map<String, String>> jsonInvalido(JacksonException e) {
        return ResponseEntity.badRequest().body(Map.of("erro", "JSON inválido."));
    }
}
