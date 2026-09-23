package com.ford.fordretain.controller;

import com.ford.fordretain.dto.ClienteRequestDTO;
import com.ford.fordretain.dto.ClienteResponseDTO;
import com.ford.fordretain.dto.ClienteUpdateRequestDTO;
import com.ford.fordretain.dto.DashboardDTO;
import com.ford.fordretain.dto.LeadDTO;
import com.ford.fordretain.dto.PredicaoResponseDTO;
import com.ford.fordretain.service.PredictionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "FordRetain API", description = "Endpoints de retenção preditiva de clientes Ford")
public class FordRetainController {

    private final PredictionService predictionService;

    // ============================================================
    // POST /predict
    // Recebe dados de um novo cliente e retorna o perfil + ação
    // ============================================================
    @PostMapping("/predict")
    @Operation(
            summary = "Prever perfil de um novo cliente",
            description = """
            Recebe os dados do cliente disponíveis no momento da compra e retorna:
            - O perfil comportamental previsto (FIEL, ABANDONO, ESQUECIDO, ECONOMICO)
            - As probabilidades de cada perfil
            - O score de risco de evasão (0-100)
            - A ação de retenção sugerida para a concessionária
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Predição realizada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "409", description = "Cliente já cadastrado com este e-mail")
    })
    public ResponseEntity<PredicaoResponseDTO> predict(
            @Valid @RequestBody ClienteRequestDTO request) {

        log.info("POST /predict");
        PredicaoResponseDTO response = predictionService.predict(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ============================================================
    // GET /clientes/{id}
    // Recurso individual — REST maturidade nível 2
    // ============================================================
    @GetMapping("/clientes/{id}")
    @Operation(
            summary = "Buscar cliente por ID",
            description = "Retorna os dados completos de um cliente cadastrado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    public ResponseEntity<ClienteResponseDTO> getCliente(
            @Parameter(description = "ID do cliente", example = "1")
            @PathVariable Long id) {

        log.info("GET /clientes/{}", id);
        return ResponseEntity.ok(predictionService.getClienteById(id));
    }

    // ============================================================
    // PUT /clientes/{id}
    // Atualização completa do recurso — REST maturidade nível 2
    // ============================================================
    @PutMapping("/clientes/{id}")
    @Operation(
            summary = "Atualizar cliente",
            description = "Atualiza integralmente os dados de um cliente já cadastrado. O e-mail não é alterável por este endpoint."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    public ResponseEntity<ClienteResponseDTO> updateCliente(
            @Parameter(description = "ID do cliente", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody ClienteUpdateRequestDTO request) {

        log.info("PUT /clientes/{}", id);
        return ResponseEntity.ok(predictionService.updateCliente(id, request));
    }

    // ============================================================
    // DELETE /clientes/{id}
    // Remoção do recurso — REST maturidade nível 2
    // ============================================================
    @DeleteMapping("/clientes/{id}")
    @Operation(
            summary = "Remover cliente",
            description = "Remove definitivamente um cliente cadastrado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    public ResponseEntity<Void> deleteCliente(
            @Parameter(description = "ID do cliente", example = "1")
            @PathVariable Long id) {

        log.info("DELETE /clientes/{}", id);
        predictionService.deleteCliente(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================================
    // GET /dashboard
    // Retorna métricas agregadas de VIN Share
    // ============================================================
    @GetMapping("/dashboard")
    @Operation(
            summary = "Métricas de VIN Share",
            description = """
            Retorna um resumo executivo com:
            - VIN Share geral e por região/modelo
            - Distribuição de perfis de clientes
            - Número de clientes em risco alto de evasão
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard gerado com sucesso")
    })
    public ResponseEntity<DashboardDTO> getDashboard() {
        log.info("GET /dashboard");
        return ResponseEntity.ok(predictionService.getDashboard());
    }

    // ============================================================
    // GET /leads
    // Lista clientes em risco, ordenados por prioridade
    // ============================================================
    @GetMapping("/leads")
    @Operation(
            summary = "Listar clientes em risco de evasão",
            description = """
            Retorna a lista de clientes com score de risco acima do mínimo informado,
            ordenados por prioridade (maior risco primeiro).
            Use esta lista para direcionar ações proativas da concessionária.
            """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista gerada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Parâmetro scoreMinimo inválido")
    })
    public ResponseEntity<List<LeadDTO>> getLeads(
            @Parameter(description = "Score mínimo de risco para incluir na lista (0-100)", example = "50")
            @RequestParam(defaultValue = "50") @Min(0) @Max(100) int scoreMinimo) {

        log.info("GET /leads — scoreMinimo: {}", scoreMinimo);
        return ResponseEntity.ok(predictionService.getLeads(scoreMinimo));
    }
}
