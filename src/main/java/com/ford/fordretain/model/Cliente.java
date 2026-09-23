package com.ford.fordretain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cliente {

    private Long id;
    private String nome;
    private String email;
    private String telefone;
    private String regiao;
    private Integer idade;
    private String canalCompra;
    private String formaPagamento;
    private String modeloVeiculo;
    private LocalDate dataCompra;
    private String historicoMarca;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;
}