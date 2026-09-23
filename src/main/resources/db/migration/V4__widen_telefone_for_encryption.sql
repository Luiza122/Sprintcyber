-- O telefone passa a ser armazenado criptografado (AES/GCM + Base64), que ocupa
-- bem mais espaço que o número original (VARCHAR2(20) não é suficiente).
ALTER TABLE clientes MODIFY (telefone VARCHAR2(255));