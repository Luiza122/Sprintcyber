package com.ford.fordretain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

// DTO de entrada para PUT /clientes/{id}
// Email não é editável (identidade do recurso) — para trocar email, cadastre um novo cliente.
@Data
@Schema(description = "Dados do cliente para atualização completa (PUT)")
public class ClienteUpdateRequestDTO {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s'-]+$", message = "Nome contém caracteres inválidos")
    @Schema(description = "Nome completo do cliente", example = "Joao da Silva")
    private String nome;

    @Pattern(regexp = "^\\d{10,11}$", message = "Telefone deve conter 10 ou 11 dígitos")
    @Schema(description = "Telefone do cliente", example = "11999990001")
    private String telefone;

    @NotBlank(message = "Região é obrigatória")
    @Pattern(regexp = "^[A-Z]{2}$", message = "Região deve ser uma UF válida (ex: SP, RJ)")
    @Schema(description = "Estado (UF) do cliente", example = "SP")
    private String regiao;

    @NotNull(message = "Idade é obrigatória")
    @Min(value = 18, message = "Idade mínima é 18")
    @Max(value = 100, message = "Idade máxima é 100")
    @Schema(description = "Idade do cliente", example = "34")
    private Integer idade;

    @NotBlank(message = "Canal de compra é obrigatório")
    @Pattern(regexp = "CONCESSIONARIA|ONLINE|REVENDEDOR", message = "Canal de compra inválido")
    @Schema(description = "Canal pelo qual o cliente comprou", example = "CONCESSIONARIA",
            allowableValues = {"CONCESSIONARIA", "ONLINE", "REVENDEDOR"})
    private String canalCompra;

    @NotBlank(message = "Forma de pagamento é obrigatória")
    @Pattern(regexp = "FINANCIAMENTO|VISTA|CONSORCIO", message = "Forma de pagamento inválida")
    @Schema(description = "Forma de pagamento utilizada", example = "FINANCIAMENTO",
            allowableValues = {"FINANCIAMENTO", "VISTA", "CONSORCIO"})
    private String formaPagamento;

    @NotBlank(message = "Modelo do veículo é obrigatório")
    @Size(max = 50, message = "Modelo deve ter no máximo 50 caracteres")
    @Pattern(regexp = "^[A-Z0-9_\\-\\s]+$", message = "Modelo do veículo contém caracteres inválidos")
    @Schema(description = "Modelo do veículo comprado", example = "RANGER")
    private String modeloVeiculo;

    @NotNull(message = "Data de compra é obrigatória")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "Data da compra", example = "2024-03-15")
    private LocalDate dataCompra;

    @NotBlank(message = "Histórico com a marca é obrigatório")
    @Pattern(regexp = "PRIMEIRA_COMPRA|RECOMPRA", message = "Histórico da marca inválido")
    @Schema(description = "Histórico do cliente com a Ford", example = "PRIMEIRA_COMPRA",
            allowableValues = {"PRIMEIRA_COMPRA", "RECOMPRA"})
    private String historicoMarca;
}