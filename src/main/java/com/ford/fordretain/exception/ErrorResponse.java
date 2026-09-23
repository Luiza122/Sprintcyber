package com.ford.fordretain.exception;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class ErrorResponse {
    private int status;
    private String erro;
    private String mensagem;
    private Map<String, String> campos;
    private LocalDateTime timestamp;
}
