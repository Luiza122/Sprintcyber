package com.ford.fordretain.exception;

// Exceção: cliente não encontrado
public class ClienteNaoEncontradoException extends RuntimeException {
    public ClienteNaoEncontradoException(Long id) {
        super("Cliente não encontrado com ID: " + id);
    }
}
