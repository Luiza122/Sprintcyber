package com.ford.fordretain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponseDTO {
    private String token;
    private String tipo;
    private String email;
    private String role;
    private long expiresIn;
}
