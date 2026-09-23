package com.ford.fordretain.config;

import com.ford.fordretain.exception.DatabaseException;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class OracleConnectionFactory {

    private final DataSource dataSource;

    public OracleConnectionFactory(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new DatabaseException("Erro ao conectar no banco de dados", e);
        }
    }
}
