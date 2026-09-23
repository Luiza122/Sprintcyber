package com.ford.fordretain.exception;

import lombok.extern.slf4j.Slf4j;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleParameterValidation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .status(400).erro("Dados inválidos")
                .mensagem("Parâmetro fora do intervalo permitido")
                .timestamp(LocalDateTime.now()).build());
    }

    // Erros de validação (@Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> campos = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            campos.put(field, error.getDefaultMessage());
        });

        ErrorResponse response = ErrorResponse.builder()
                .status(400)
                .erro("Dados inválidos")
                .mensagem("Verifique os campos informados")
                .campos(campos)
                .timestamp(LocalDateTime.now())
                .build();

        log.warn("Validação falhou: {}", campos);
        return ResponseEntity.badRequest().body(response);
    }

    // Cliente já cadastrado
    @ExceptionHandler(ClienteJaCadastradoException.class)
    public ResponseEntity<ErrorResponse> handleClienteJaCadastrado(ClienteJaCadastradoException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(409)
                .erro("Conflito")
                .mensagem(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        log.warn("Tentativa de cadastro duplicado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // Cliente não encontrado
    @ExceptionHandler(ClienteNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleClienteNaoEncontrado(ClienteNaoEncontradoException ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(404)
                .erro("Não encontrado")
                .mensagem(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        log.warn("Cliente não encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    // Erros genéricos
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        ErrorResponse response = ErrorResponse.builder()
                .status(500)
                .erro("Erro interno")
                .mensagem("Ocorreu um erro inesperado. Tente novamente mais tarde.")
                .timestamp(LocalDateTime.now())
                .build();

        log.error("Erro inesperado: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
    @ExceptionHandler(DatabaseException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseException(DatabaseException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .status(500)
                .erro("Erro de banco de dados")
                .mensagem(ex.getMessage())
                .build();

        return ResponseEntity.status(500).body(error);
    }
}
