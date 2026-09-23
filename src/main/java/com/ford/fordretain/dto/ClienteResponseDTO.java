package com.ford.fordretain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

// DTO de resposta para GET/PUT /clientes/{id}
@Data
@Builder
@Schema(description = "Dados completos do cliente cadastrado")
public class ClienteResponseDTO {

    @Schema(description = "ID do cliente", example = "7")
    private Long id;

    @Schema(description = "Nome completo do cliente", example = "Joao da Silva")
    private String nome;

    @Schema(description = "Email do cliente", example = "joao.silva@email.com")
    private String email;

    @Schema(description = "Telefone do cliente", example = "11999990001")
    private String telefone;

    @Schema(description = "Estado (UF) do cliente", example = "SP")
    private String regiao;

    @Schema(description = "Idade do cliente", example = "34")
    private Integer idade;

    @Schema(description = "Canal pelo qual o cliente comprou", example = "CONCESSIONARIA")
    private String canalCompra;

    @Schema(description = "Forma de pagamento utilizada", example = "FINANCIAMENTO")
    private String formaPagamento;

    @Schema(description = "Modelo do veículo comprado", example = "RANGER")
    private String modeloVeiculo;

    @Schema(description = "Data da compra")
    private LocalDate dataCompra;

    @Schema(description = "Histórico do cliente com a Ford", example = "PRIMEIRA_COMPRA")
    private String historicoMarca;

    @Schema(description = "Data de cadastro do cliente")
    private LocalDateTime criadoEm;

    @Schema(description = "Data da última atualização do cliente")
    private LocalDateTime atualizadoEm;
}