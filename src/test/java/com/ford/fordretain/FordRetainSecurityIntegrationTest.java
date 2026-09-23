package com.ford.fordretain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FordRetainSecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    private String token(String email) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"ford2026\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + mapper.readTree(response).get("token").asText();
    }

    @Test
    void clienteRealPassaPorRbacBancoCriptografiaDashboardEExclusao() throws Exception {
        String admin = token("admin@ford.com");
        String gerente = token("gerente@ford.com");
        String analista = token("analista@ford.com");
        String email = "teste-" + UUID.randomUUID() + "@example.test";
        String phone = "11999998888";
        String request = "{\"nome\":\"Cliente Teste\",\"email\":\"" + email + "\",\"telefone\":\"" + phone + "\",\"regiao\":\"SP\",\"idade\":34,\"canalCompra\":\"ONLINE\",\"formaPagamento\":\"FINANCIAMENTO\",\"modeloVeiculo\":\"RANGER\",\"dataCompra\":\"2026-01-15\",\"historicoMarca\":\"PRIMEIRA_COMPRA\"}";

        mvc.perform(post("/api/v1/predict").header("Authorization", analista)
                .contentType(MediaType.APPLICATION_JSON).content(request)).andExpect(status().isForbidden());
        JsonNode created = mapper.readTree(mvc.perform(post("/api/v1/predict")
                .header("Authorization", gerente).contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.perfilPrevisto").value("ABANDONO"))
                .andReturn().getResponse().getContentAsString());
        long id = created.get("clienteId").asLong();

        String stored = jdbc.queryForObject("SELECT telefone FROM clientes WHERE id = ?", String.class, id);
        assertThat(stored).startsWith("v1:").doesNotContain(phone);
        mvc.perform(get("/api/v1/clientes/{id}", id).header("Authorization", analista))
                .andExpect(status().isOk()).andExpect(jsonPath("$.telefone").value(phone));
        mvc.perform(get("/api/v1/leads?scoreMinimo=50").header("Authorization", analista))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].clienteId").value((int) id))
                .andExpect(jsonPath("$[0].telefone").value(phone));
        mvc.perform(get("/api/v1/dashboard").header("Authorization", gerente))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalClientes").isNumber());

        String update = request.replace("\"email\":\"" + email + "\",", "")
                .replace(phone, "21988887777");
        mvc.perform(put("/api/v1/clientes/{id}", id).header("Authorization", analista)
                .contentType(MediaType.APPLICATION_JSON).content(update)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/clientes/{id}", id).header("Authorization", gerente)
                .contentType(MediaType.APPLICATION_JSON).content(update))
                .andExpect(status().isOk()).andExpect(jsonPath("$.telefone").value("21988887777"));
        mvc.perform(delete("/api/v1/clientes/{id}", id).header("Authorization", gerente))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/clientes/{id}", id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM clientes WHERE id = ?", Integer.class, id)).isZero();
    }
}
