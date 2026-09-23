-- Sem isso, DELETE /clientes/{id} falha com ORA-02292 (child record found)
-- para qualquer cliente que já tenha uma predição associada.
ALTER TABLE predicoes DROP CONSTRAINT fk_predicao_cliente;

ALTER TABLE predicoes
    ADD CONSTRAINT fk_predicao_cliente
    FOREIGN KEY (cliente_id) REFERENCES clientes(id)
    ON DELETE CASCADE;