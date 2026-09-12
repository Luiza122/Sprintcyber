package com.ford.fordretain.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RateLimitFilterTest {

    @Test
    void terceiraTentativaDeLoginDeveRetornar429() throws Exception {
        @SuppressWarnings("unchecked")
        ObjectProvider<SecurityMetrics> metricsProvider = mock(ObjectProvider.class);
        RateLimitFilter filter = new RateLimitFilter(metricsProvider);
        ReflectionTestUtils.setField(filter, "requestsPerMinute", 100L);
        ReflectionTestUtils.setField(filter, "loginRequestsPerMinute", 2L);
        FilterChain chain = mock(FilterChain.class);

        MockHttpServletRequest first = loginRequest();
        MockHttpServletRequest second = loginRequest();
        MockHttpServletRequest third = loginRequest();
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        MockHttpServletResponse secondResponse = new MockHttpServletResponse();
        MockHttpServletResponse thirdResponse = new MockHttpServletResponse();

        filter.doFilter(first, firstResponse, chain);
        filter.doFilter(second, secondResponse, chain);
        filter.doFilter(third, thirdResponse, chain);

        verify(chain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(thirdResponse.getStatus()).isEqualTo(429);
        assertThat(thirdResponse.getHeader("Retry-After")).isEqualTo("60");
        assertThat(thirdResponse.getContentAsString()).contains("Muitas requisições");
    }

    private MockHttpServletRequest loginRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.0.2.10");
        return request;
    }
}
