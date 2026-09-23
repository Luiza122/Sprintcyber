package com.ford.fordretain.service;

import com.ford.fordretain.dao.ClienteDAO;
import com.ford.fordretain.dao.PredicaoDAO;
import com.ford.fordretain.dto.ClienteRequestDTO;
import com.ford.fordretain.dto.ClienteResponseDTO;
import com.ford.fordretain.dto.ClienteUpdateRequestDTO;
import com.ford.fordretain.dto.DashboardDTO;
import com.ford.fordretain.dto.LeadDTO;
import com.ford.fordretain.dto.PredicaoResponseDTO;
import com.ford.fordretain.exception.ClienteJaCadastradoException;
import com.ford.fordretain.exception.ClienteNaoEncontradoException;
import com.ford.fordretain.model.Cliente;
import com.ford.fordretain.model.Predicao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {

    private final ClienteDAO clienteDAO;
    private final PredicaoDAO predicaoDAO;

    public PredicaoResponseDTO predict(ClienteRequestDTO request) {
        log.info("Iniciando predição para novo cliente");

        if (clienteDAO.existsByEmail(request.getEmail())) {
            throw new ClienteJaCadastradoException(request.getEmail());
        }

        Cliente cliente = clienteDAO.save(Cliente.builder()
                .nome(request.getNome())
                .email(request.getEmail())
                .telefone(request.getTelefone())
                .regiao(request.getRegiao())
                .idade(request.getIdade())
                .canalCompra(request.getCanalCompra())
                .formaPagamento(request.getFormaPagamento())
                .modeloVeiculo(request.getModeloVeiculo())
                .dataCompra(request.getDataCompra())
                .historicoMarca(request.getHistoricoMarca())
                .build());

        Map<String, BigDecimal> probs = mockPredict(request);
        String perfil = perfilMaisProvavel(probs);
        int scoreRisco = calcularScoreRisco(perfil, probs);
        String acao = sugerirAcao(perfil);

        Predicao predicao = predicaoDAO.save(Predicao.builder()
                .cliente(cliente)
                .perfilPrevisto(perfil)
                .probFiel(probs.get("FIEL"))
                .probAbandono(probs.get("ABANDONO"))
                .probEsquecido(probs.get("ESQUECIDO"))
                .probEconomico(probs.get("ECONOMICO"))
                .acaoSugerida(acao)
                .scoreRisco(scoreRisco)
                .build());

        return PredicaoResponseDTO.builder()
                .predicaoId(predicao.getId())
                .clienteId(cliente.getId())
                .nomeCliente(cliente.getNome())
                .perfilPrevisto(perfil)
                .probabilidades(probs)
                .scoreRisco(scoreRisco)
                .acaoSugerida(acao)
                .dataPredicao(predicao.getDataPredicao())
                .build();
    }

    // ============================================================
    // GET /clientes/{id}
    // ============================================================
    public ClienteResponseDTO getClienteById(Long id) {
        Cliente cliente = clienteDAO.findById(id)
                .orElseThrow(() -> new ClienteNaoEncontradoException(id));
        return toResponseDTO(cliente);
    }

    // ============================================================
    // PUT /clientes/{id}
    // ============================================================
    public ClienteResponseDTO updateCliente(Long id, ClienteUpdateRequestDTO request) {
        Cliente existente = clienteDAO.findById(id)
                .orElseThrow(() -> new ClienteNaoEncontradoException(id));

        existente.setNome(request.getNome());
        existente.setTelefone(request.getTelefone());
        existente.setRegiao(request.getRegiao());
        existente.setIdade(request.getIdade());
        existente.setCanalCompra(request.getCanalCompra());
        existente.setFormaPagamento(request.getFormaPagamento());
        existente.setModeloVeiculo(request.getModeloVeiculo());
        existente.setDataCompra(request.getDataCompra());
        existente.setHistoricoMarca(request.getHistoricoMarca());

        Cliente atualizado = clienteDAO.update(existente);
        log.info("Cliente atualizado: id={}", id);
        return toResponseDTO(atualizado);
    }

    // ============================================================
    // DELETE /clientes/{id}
    // ============================================================
    public void deleteCliente(Long id) {
        if (clienteDAO.findById(id).isEmpty()) {
            throw new ClienteNaoEncontradoException(id);
        }
        clienteDAO.deleteById(id);
        log.info("Cliente removido: id={}", id);
    }

    private ClienteResponseDTO toResponseDTO(Cliente cliente) {
        return ClienteResponseDTO.builder()
                .id(cliente.getId())
                .nome(cliente.getNome())
                .email(cliente.getEmail())
                .telefone(cliente.getTelefone())
                .regiao(cliente.getRegiao())
                .idade(cliente.getIdade())
                .canalCompra(cliente.getCanalCompra())
                .formaPagamento(cliente.getFormaPagamento())
                .modeloVeiculo(cliente.getModeloVeiculo())
                .dataCompra(cliente.getDataCompra())
                .historicoMarca(cliente.getHistoricoMarca())
                .criadoEm(cliente.getCriadoEm())
                .atualizadoEm(cliente.getAtualizadoEm())
                .build();
    }

    public DashboardDTO getDashboard() {
        long total = clienteDAO.count();

        Map<String, Long> distribuicao = predicaoDAO.countByPerfil();

        long clientesFiel = distribuicao.getOrDefault("FIEL", 0L);
        long clientesEco = distribuicao.getOrDefault("ECONOMICO", 0L);
        double vinShare = total > 0 ? (double) (clientesFiel + clientesEco) / total : 0.0;

        long riscoAlto = predicaoDAO.findLeadsEmRisco(70).size();

        Map<String, Double> vinRegiao = predicaoDAO.vinSharePorRegiao();
        Map<String, Double> vinModelo = predicaoDAO.vinSharePorModelo();

        return DashboardDTO.builder()
                .totalClientes(total)
                .vinShareGeral(Math.round(vinShare * 10000.0) / 10000.0)
                .clientesRiscoAlto(riscoAlto)
                .distribuicaoPerfis(distribuicao)
                .vinSharePorRegiao(vinRegiao)
                .vinSharePorModelo(vinModelo)
                .geradoEm(LocalDateTime.now())
                .build();
    }

    public List<LeadDTO> getLeads(int scoreMinimo) {
        return predicaoDAO.findLeadsEmRisco(scoreMinimo)
                .stream()
                .map(p -> {
                    Cliente c = p.getCliente();
                    BigDecimal probPrincipal = switch (p.getPerfilPrevisto()) {
                        case "FIEL" -> p.getProbFiel();
                        case "ABANDONO" -> p.getProbAbandono();
                        case "ESQUECIDO" -> p.getProbEsquecido();
                        default -> p.getProbEconomico();
                    };

                    return LeadDTO.builder()
                            .clienteId(c.getId())
                            .nome(c.getNome())
                            .email(c.getEmail())
                            .telefone(c.getTelefone())
                            .regiao(c.getRegiao())
                            .modeloVeiculo(c.getModeloVeiculo())
                            .perfilPrevisto(p.getPerfilPrevisto())
                            .scoreRisco(p.getScoreRisco())
                            .probabilidadePrincipal(probPrincipal)
                            .acaoSugerida(p.getAcaoSugerida())
                            .dataPredicao(p.getDataPredicao())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private Map<String, BigDecimal> mockPredict(ClienteRequestDTO req) {
        Map<String, BigDecimal> probs = new LinkedHashMap<>();

        if ("PRIMEIRA_COMPRA".equals(req.getHistoricoMarca()) && "ONLINE".equals(req.getCanalCompra())) {
            probs.put("FIEL", new BigDecimal("0.0800"));
            probs.put("ABANDONO", new BigDecimal("0.6800"));
            probs.put("ESQUECIDO", new BigDecimal("0.1500"));
            probs.put("ECONOMICO", new BigDecimal("0.0900"));
        } else if ("RECOMPRA".equals(req.getHistoricoMarca())) {
            probs.put("FIEL", new BigDecimal("0.6500"));
            probs.put("ABANDONO", new BigDecimal("0.0800"));
            probs.put("ESQUECIDO", new BigDecimal("0.1200"));
            probs.put("ECONOMICO", new BigDecimal("0.1500"));
        } else if ("CONSORCIO".equals(req.getFormaPagamento())) {
            probs.put("FIEL", new BigDecimal("0.1500"));
            probs.put("ABANDONO", new BigDecimal("0.1000"));
            probs.put("ESQUECIDO", new BigDecimal("0.6000"));
            probs.put("ECONOMICO", new BigDecimal("0.1500"));
        } else {
            probs.put("FIEL", new BigDecimal("0.2000"));
            probs.put("ABANDONO", new BigDecimal("0.1500"));
            probs.put("ESQUECIDO", new BigDecimal("0.1500"));
            probs.put("ECONOMICO", new BigDecimal("0.5000"));
        }

        return probs;
    }

    private String perfilMaisProvavel(Map<String, BigDecimal> probs) {
        return probs.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("ECONOMICO");
    }

    private int calcularScoreRisco(String perfil, Map<String, BigDecimal> probs) {
        return switch (perfil) {
            case "ABANDONO" -> (int) (probs.get("ABANDONO").doubleValue() * 100);
            case "ESQUECIDO" -> (int) (probs.get("ESQUECIDO").doubleValue() * 70);
            case "ECONOMICO" -> (int) (probs.get("ECONOMICO").doubleValue() * 50);
            default -> (int) ((1 - probs.get("FIEL").doubleValue()) * 20);
        };
    }

    private String sugerirAcao(String perfil) {
        return switch (perfil) {
            case "FIEL" -> "Programa de fidelidade premium — oferecer revisão com desconto exclusivo.";
            case "ABANDONO" -> "Contato imediato — pacote de 3 revisões com desconto progressivo.";
            case "ESQUECIDO" -> "Enviar lembrete com agendamento fácil e link direto para a concessionária.";
            case "ECONOMICO" -> "Enviar cupom de desconto de 20% na próxima revisão agendada.";
            default -> "Monitorar comportamento nas próximas semanas.";
        };
    }
}
