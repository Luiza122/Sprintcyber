package com.ford.fordretain.dao;

import com.ford.fordretain.model.Predicao;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PredicaoDAO {
    Predicao save(Predicao predicao);
    Optional<Predicao> findTopByClienteIdOrderByDataPredicaoDesc(Long clienteId);
    List<Predicao> findLeadsEmRisco(int scoreMinimo);
    Map<String, Long> countByPerfil();
    Map<String, Double> vinSharePorRegiao();
    Map<String, Double> vinSharePorModelo();
}