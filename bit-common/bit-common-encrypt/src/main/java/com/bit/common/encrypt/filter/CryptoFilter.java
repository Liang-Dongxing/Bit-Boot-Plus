package com.bit.common.encrypt.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import com.bit.common.core.utils.StringUtils;
import com.bit.common.encrypt.properties.ApiDecryptProperties;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Objects;


/**
 * Crypto 过滤器
 *
 * @author wdhcr
 */
public class CryptoFilter implements Filter {
    private final ApiDecryptProperties properties;

    public CryptoFilter(ApiDecryptProperties properties) {
        this.properties = properties;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        ServletRequest requestWrapper = null;
        HttpServletRequest servletRequest = (HttpServletRequest) request;
        // 是否为 json 请求
        if (StringUtils.startsWithIgnoreCase(request.getContentType(), MediaType.APPLICATION_JSON_VALUE)) {
            // 是否为 put 或者 post 请求
            if (HttpMethod.PUT.matches(servletRequest.getMethod()) || HttpMethod.POST.matches(servletRequest.getMethod())) {
                // 是否存在加密标头
                String headerValue = servletRequest.getHeader(properties.getHeaderFlag());
                if (StringUtils.isNotBlank(headerValue)) {
                    requestWrapper = new DecryptRequestBodyWrapper(servletRequest, properties.getPublicKey(), properties.getPrivateKey(), properties.getHeaderFlag());
                }
            }
        }
        chain.doFilter(Objects.requireNonNullElse(requestWrapper, request), response);
    }

    @Override
    public void destroy() {

    }
}
