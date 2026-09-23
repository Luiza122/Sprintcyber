package com.ford.fordretain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

// DTO de resposta do GET /dashboard
@Data
@Builder
@Schema(description = "Métricas agregadas de VIN Share e retenção")
public class DashboardDTO {

    @Schema(description = "Total de clientes na base", example = "1250")
    private Long totalClientes;

    @Schema(description = "VIN Share geral — % de clientes com predição FIEL ou ECONOMICO", example = "0.58")
    private Double vinShareGeral;

    @Schema(description = "Clientes em risco alto de evasão (score >= 70)", example = "312")
    private Long clientesRiscoAlto;

    @Schema(description = "Distribuição de perfis: FIEL, ABANDONO, ESQUECIDO, ECONOMICO")
    private Map<String, Long> distribuicaoPerfis;

    @Schema(description = "VIN Share por região (UF)")
    private Map<String, Double> vinSharePorRegiao;

    @Schema(description = "VIN Share por modelo de veículo")
    private Map<String, Double> vinSharePorModelo;

    @Schema(description = "Data de geração do relatório")
    private LocalDateTime geradoEm;
}
