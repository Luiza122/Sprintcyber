package com.ford.fordretain.exception;

public class ClienteJaCadastradoException extends RuntimeException {
    public ClienteJaCadastradoException(String email) {
        super("Cliente já cadastrado com o e-mail: " + email);
    }
}
