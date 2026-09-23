package com.ford.fordretain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OracleConnectionSmokeTest {

    @Test
    @EnabledIfEnvironmentVariable(named = "ORACLE_SMOKE_TEST", matches = "true")
    void conectaAoOracleSemAlterarDados() throws Exception {
        String url = requiredEnvironment("ORACLE_URL");
        String user = requiredEnvironment("ORACLE_USER");
        String password = requiredEnvironment("ORACLE_PASSWORD");

        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            assertTrue(connection.isValid(10), "A conexão Oracle deve estar válida");
            assertTrue(
                    connection.getMetaData().getDatabaseProductName().toLowerCase().contains("oracle"),
                    "O destino deve ser um banco Oracle"
            );

            try (Statement statement = connection.createStatement();
                 ResultSet result = statement.executeQuery("SELECT 1 FROM DUAL")) {
                assertTrue(result.next(), "A consulta de diagnóstico deve retornar uma linha");
                assertEquals(1, result.getInt(1));
            }
        }
    }

    private String requiredEnvironment(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Variável obrigatória ausente: " + name);
        }
        return value;
    }
}
