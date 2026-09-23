package com.ford.fordretain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// DTO de resposta para cada lead no GET /leads
@Data
@Builder
@Schema(description = "Cliente em risco de evasão — lead para ação da concessionária")
public class LeadDTO {

    @Schema(description = "ID do cliente", example = "7")
    private Long clienteId;

    @Schema(description = "Nome do cliente", example = "Carlos Mendes")
    private String nome;

    @Schema(description = "Email do cliente", example = "carlos.m@email.com")
    private String email;

    @Schema(description = "Telefone do cliente", example = "21999990002")
    private String telefone;

    @Schema(description = "Região do cliente", example = "RJ")
    private String regiao;

    @Schema(description = "Modelo do veículo", example = "MAVERICK")
    private String modeloVeiculo;

    @Schema(description = "Perfil previsto", example = "ABANDONO")
    private String perfilPrevisto;

    @Schema(description = "Score de risco (0-100)", example = "85")
    private Integer scoreRisco;

    @Schema(description = "Probabilidade do perfil previsto", example = "0.7200")
    private BigDecimal probabilidadePrincipal;

    @Schema(description = "Ação sugerida para este cliente")
    private String acaoSugerida;

    @Schema(description = "Data da última predição")
    private LocalDateTime dataPredicao;
}
