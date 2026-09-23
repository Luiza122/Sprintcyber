package com.ford.fordretain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

// DTO de resposta do endpoint POST /predict
@Data
@Builder
@Schema(description = "Resultado da predição de perfil do cliente")
public class PredicaoResponseDTO {

    @Schema(description = "ID da predição gerada", example = "42")
    private Long predicaoId;

    @Schema(description = "ID do cliente", example = "7")
    private Long clienteId;

    @Schema(description = "Nome do cliente", example = "João da Silva")
    private String nomeCliente;

    @Schema(description = "Perfil comportamental previsto", example = "ABANDONO")
    private String perfilPrevisto;

    @Schema(description = "Probabilidades para cada perfil")
    private Map<String, BigDecimal> probabilidades;

    @Schema(description = "Score de risco de evasão (0-100, maior = mais risco)", example = "85")
    private Integer scoreRisco;

    @Schema(description = "Ação sugerida para retenção", example = "Contato imediato — pacote de 3 revisões com desconto.")
    private String acaoSugerida;

    @Schema(description = "Data e hora da predição")
    private LocalDateTime dataPredicao;
}
