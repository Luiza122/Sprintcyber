package com.ford.fordretain.dao.impl;

import com.ford.fordretain.config.OracleConnectionFactory;
import com.ford.fordretain.dao.ClienteDAO;
import com.ford.fordretain.exception.DatabaseException;
import com.ford.fordretain.model.Cliente;
import com.ford.fordretain.security.CryptoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ClienteDAOImpl implements ClienteDAO {

    private final OracleConnectionFactory connectionFactory;
    private final CryptoUtils cryptoUtils;

    @Override
    public Cliente save(Cliente cliente) {
        String sql = """
            INSERT INTO clientes (
                nome, email, telefone, regiao, idade,
                canal_compra, forma_pagamento, modelo_veiculo,
                data_compra, historico_marca, criado_em, atualizado_em
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"id"})) {

            LocalDateTime agora = LocalDateTime.now();

            ps.setString(1, cliente.getNome());
            ps.setString(2, cliente.getEmail());
            ps.setString(3, cryptoUtils.encrypt(cliente.getTelefone()));
            ps.setString(4, cliente.getRegiao());
            ps.setInt(5, cliente.getIdade());
            ps.setString(6, cliente.getCanalCompra());
            ps.setString(7, cliente.getFormaPagamento());
            ps.setString(8, cliente.getModeloVeiculo());
            ps.setDate(9, Date.valueOf(cliente.getDataCompra()));
            ps.setString(10, cliente.getHistoricoMarca());
            ps.setTimestamp(11, Timestamp.valueOf(agora));
            ps.setTimestamp(12, Timestamp.valueOf(agora));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    cliente.setId(rs.getLong(1));
                }
            }

            cliente.setCriadoEm(agora);
            cliente.setAtualizadoEm(agora);
            return cliente;

        } catch (SQLException e) {
            throw new DatabaseException("Erro ao salvar cliente", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(1) FROM clientes WHERE email = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
                return false;
            }

        } catch (SQLException e) {
            throw new DatabaseException("Erro ao verificar e-mail do cliente", e);
        }
    }

    @Override
    public Optional<Cliente> findByEmail(String email) {
        String sql = """
            SELECT id, nome, email, telefone, regiao, idade,
                   canal_compra, forma_pagamento, modelo_veiculo,
                   data_compra, historico_marca, criado_em, atualizado_em
            FROM clientes
            WHERE email = ?
            """;

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar cliente por e-mail", e);
        }
    }

    @Override
    public Optional<Cliente> findById(Long id) {
        String sql = """
            SELECT id, nome, email, telefone, regiao, idade,
                   canal_compra, forma_pagamento, modelo_veiculo,
                   data_compra, historico_marca, criado_em, atualizado_em
            FROM clientes
            WHERE id = ?
            """;

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new DatabaseException("Erro ao buscar cliente por ID", e);
        }
    }

    @Override
    public Cliente update(Cliente cliente) {
        String sql = """
            UPDATE clientes SET
                nome = ?, telefone = ?, regiao = ?, idade = ?,
                canal_compra = ?, forma_pagamento = ?, modelo_veiculo = ?,
                data_compra = ?, historico_marca = ?, atualizado_em = ?
            WHERE id = ?
            """;

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            LocalDateTime agora = LocalDateTime.now();

            ps.setString(1, cliente.getNome());
            ps.setString(2, cryptoUtils.encrypt(cliente.getTelefone()));
            ps.setString(3, cliente.getRegiao());
            ps.setInt(4, cliente.getIdade());
            ps.setString(5, cliente.getCanalCompra());
            ps.setString(6, cliente.getFormaPagamento());
            ps.setString(7, cliente.getModeloVeiculo());
            ps.setDate(8, Date.valueOf(cliente.getDataCompra()));
            ps.setString(9, cliente.getHistoricoMarca());
            ps.setTimestamp(10, Timestamp.valueOf(agora));
            ps.setLong(11, cliente.getId());

            ps.executeUpdate();

            cliente.setAtualizadoEm(agora);
            return cliente;

        } catch (SQLException e) {
            throw new DatabaseException("Erro ao atualizar cliente", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM clientes WHERE id = ?";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            int linhas = ps.executeUpdate();
            return linhas > 0;

        } catch (SQLException e) {
            throw new DatabaseException("Erro ao remover cliente", e);
        }
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(1) FROM clientes";

        try (Connection conn = connectionFactory.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }
            return 0L;

        } catch (SQLException e) {
            throw new DatabaseException("Erro ao contar clientes", e);
        }
    }

    private Cliente mapRow(ResultSet rs) throws SQLException {
        return Cliente.builder()
                .id(rs.getLong("id"))
                .nome(rs.getString("nome"))
                .email(rs.getString("email"))
                .telefone(cryptoUtils.decrypt(rs.getString("telefone")))
                .regiao(rs.getString("regiao"))
                .idade(rs.getInt("idade"))
                .canalCompra(rs.getString("canal_compra"))
                .formaPagamento(rs.getString("forma_pagamento"))
                .modeloVeiculo(rs.getString("modelo_veiculo"))
                .dataCompra(rs.getDate("data_compra").toLocalDate())
                .historicoMarca(rs.getString("historico_marca"))
                .criadoEm(rs.getTimestamp("criado_em").toLocalDateTime())
                .atualizadoEm(rs.getTimestamp("atualizado_em").toLocalDateTime())
                .build();
    }
}