package com.ford.fordretain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Predicao {

    private Long id;
    private Cliente cliente;
    private String perfilPrevisto;
    private BigDecimal probFiel;
    private BigDecimal probAbandono;
    private BigDecimal probEsquecido;
    private BigDecimal probEconomico;
    private String acaoSugerida;
    private Integer scoreRisco;
    private LocalDateTime dataPredicao;
}