package com.ford.fordretain.dao.impl;

import com.ford.fordretain.config.OracleConnectionFactory;
import com.ford.fordretain.dao.PredicaoDAO;
import com.ford.fordretain.exception.DatabaseException;
import com.ford.fordretain.model.Cliente;
import com.ford.fordretain.model.Predicao;
import com.ford.fordretain.security.CryptoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.util.*;

@Repository
@RequiredArgsConstructor
public class PredicaoDAOImpl implements PredicaoDAO {

    private final OracleConnectionFactory connectionFactory;
    private final CryptoUtils cryptoUtils;

    @Override
    public Predicao save(Predicao predicao) {
        String sql = """
            INSERT INTO predicoes (
                cliente_id, perfil_previsto, prob_fiel, prob_abandono,
                prob_esquecido, prob_economico, acao_sugerida,
                score_risco, data_predicao
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"id"})) {

            Timestamp agora = new Timestamp(System.currentTimeMillis());

            ps.setLong(1, predicao.getCliente().getId());
            ps.setString(2, predicao.getPerfilPrevisto());
            ps.setBigDecimal(3, predicao.getProbFiel());
            ps.setBigDecimal(4, predicao.getProbAbandono());
            ps.setBigDecimal(5, predicao.getProbEsquecido());
            ps.setBigDecimal(6, predicao.getProbEconomico());
            ps.setString(7, predicao.getAcaoSugerida());
            ps.setInt(8, predicao.getScoreRisco());
            ps.setTimestamp(9, agora);

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    predicao.setId(rs.getLong(1));
                }
            }

            predicao.setDataPredicao(agora.toLocalDateTime());
            return predicao;

        } catch (SQLException e) {
            throw new DatabaseException("Erro ao salvar predição", e);
        }
    }

    @Override
    public Optional<Predicao> findTopByClienteIdOrderByDataPredicaoDesc(Long clienteId) {
        String sql = """
            SELECT p.id, p.cliente_id, p.perfil_previsto, p.prob_fiel, p.prob_abandono,
                   p.prob_esquecido, p.prob_economico, p.acao_sugerida,
                   p.score_risco, p.data_predicao
            FROM predicoes p
            WHERE p.cliente_id = ?
            ORDER BY p.data_predicao DESC
            FETCH FIRST 1 ROWS ONLY
            """;

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, clienteId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Cliente cliente = Cliente.builder().id(rs.getLong("cliente_id")).build();

                    Predicao predicao = Predicao.builder()
                            .id(rs.getLong("id"))
                            .cliente(cliente)
                            .perfilPrevisto(rs.getString("perfil_previsto"))
                            .probFiel(rs.getBigDecimal("prob_fiel"))
                            .probAbandono(rs.getBigDecimal("prob_abandono"))
                            .probEsquecido(rs.getBigDecimal("prob_esquecido"))
                            .probEconomico(rs.getBigDecimal("prob_economico"))
                            .acaoSugerida(rs.getString("acao_sugerida"))
                            .scoreRisco(rs.getInt("score_risco"))
                            .dataPredicao(rs.getTimestamp("data_predicao").toLocalDateTime())
                            .build();

                    return Optional.of(predicao);
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar última predição", e);
        }
    }

    @Override
    public List<Predicao> findLeadsEmRisco(int scoreMinimo) {
        String sql = """
            SELECT p.id, p.cliente_id, p.perfil_previsto, p.prob_fiel, p.prob_abandono,
                   p.prob_esquecido, p.prob_economico, p.acao_sugerida,
                   p.score_risco, p.data_predicao,
                   c.nome, c.email, c.telefone, c.regiao, c.modelo_veiculo
            FROM predicoes p
            JOIN clientes c ON c.id = p.cliente_id
            WHERE p.score_risco >= ?
            ORDER BY p.score_risco DESC
            """;

        List<Predicao> lista = new ArrayList<>();

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, scoreMinimo);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Cliente cliente = Cliente.builder()
                            .id(rs.getLong("cliente_id"))
                            .nome(rs.getString("nome"))
                            .email(rs.getString("email"))
                            .telefone(cryptoUtils.decrypt(rs.getString("telefone")))
                            .regiao(rs.getString("regiao"))
                            .modeloVeiculo(rs.getString("modelo_veiculo"))
                            .build();

                    Predicao predicao = Predicao.builder()
                            .id(rs.getLong("id"))
                            .cliente(cliente)
                            .perfilPrevisto(rs.getString("perfil_previsto"))
                            .probFiel(rs.getBigDecimal("prob_fiel"))
                            .probAbandono(rs.getBigDecimal("prob_abandono"))
                            .probEsquecido(rs.getBigDecimal("prob_esquecido"))
                            .probEconomico(rs.getBigDecimal("prob_economico"))
                            .acaoSugerida(rs.getString("acao_sugerida"))
                            .scoreRisco(rs.getInt("score_risco"))
                            .dataPredicao(rs.getTimestamp("data_predicao").toLocalDateTime())
                            .build();

                    lista.add(predicao);
                }
            }

            return lista;

        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar leads em risco", e);
        }
    }

    @Override
    public Map<String, Long> countByPerfil() {
        String sql = """
            SELECT perfil_previsto, COUNT(*) total
            FROM predicoes
            GROUP BY perfil_previsto
            """;

        Map<String, Long> resultado = new LinkedHashMap<>();

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                resultado.put(rs.getString("perfil_previsto"), rs.getLong("total"));
            }

            return resultado;

        } catch (SQLException e) {
            throw new DatabaseException("Erro ao contar perfis", e);
        }
    }

    @Override
    public Map<String, Double> vinSharePorRegiao() {
        String sql = """
            SELECT c.regiao,
                   SUM(CASE WHEN p.perfil_previsto IN ('FIEL', 'ECONOMICO') THEN 1 ELSE 0 END) / COUNT(*) AS vin_share
            FROM predicoes p
            JOIN clientes c ON c.id = p.cliente_id
            GROUP BY c.regiao
            """;

        return executarConsultaAgrupada(sql, "regiao");
    }

    @Override
    public Map<String, Double> vinSharePorModelo() {
        String sql = """
            SELECT c.modelo_veiculo,
                   SUM(CASE WHEN p.perfil_previsto IN ('FIEL', 'ECONOMICO') THEN 1 ELSE 0 END) / COUNT(*) AS vin_share
            FROM predicoes p
            JOIN clientes c ON c.id = p.cliente_id
            GROUP BY c.modelo_veiculo
            """;

        return executarConsultaAgrupada(sql, "modelo_veiculo");
    }

    private Map<String, Double> executarConsultaAgrupada(String sql, String coluna) {
        Map<String, Double> resultado = new LinkedHashMap<>();

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                resultado.put(rs.getString(coluna), rs.getDouble("vin_share"));
            }

            return resultado;

        } catch (SQLException e) {
            throw new DatabaseException("Erro ao consultar dashboard agregado", e);
        }
    }
}
