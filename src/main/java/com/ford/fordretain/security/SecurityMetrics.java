package com.ford.fordretain.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class SecurityMetrics {
    private final Counter loginSuccess;
    private final Counter loginFailure;
    private final Counter accessDenied;
    private final Counter rateLimited;

    public SecurityMetrics(MeterRegistry registry) {
        loginSuccess = Counter.builder("fordretain.security.login").description("Tentativas de login por resultado").tag("result", "success").register(registry);
        loginFailure = Counter.builder("fordretain.security.login").description("Tentativas de login por resultado").tag("result", "failure").register(registry);
        accessDenied = Counter.builder("fordretain.security.access.denied").description("Respostas HTTP 401/403").register(registry);
        rateLimited = Counter.builder("fordretain.security.rate.limited").description("Requisições bloqueadas por rate limit").register(registry);
    }
    public void loginSuccess() { loginSuccess.increment(); }
    public void loginFailure() { loginFailure.increment(); }
    public void accessDenied() { accessDenied.increment(); }
    public void rateLimited() { rateLimited.increment(); }
}
