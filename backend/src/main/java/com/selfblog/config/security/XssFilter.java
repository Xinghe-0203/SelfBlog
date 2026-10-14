package com.selfblog.config.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * XSS 过滤器 - 对请求参数进行基础XSS检测和过滤
 * 作为 Service 层 HtmlSanitizer 净化的补充防御
 */
@Component
@Order(1)
public class XssFilter implements Filter {

    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "<script[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern EVENT_PATTERN = Pattern.compile(
            "\\bon\\w+\\s*=", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAVASCRIPT_PATTERN = Pattern.compile(
            "javascript:", Pattern.CASE_INSENSITIVE);
    private static final Pattern DATA_PATTERN = Pattern.compile(
            "data:text/html", Pattern.CASE_INSENSITIVE);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        chain.doFilter(new XssRequestWrapper((HttpServletRequest) request), response);
    }

    /**
     * 请求包装器 - 对请求参数进行XSS过滤
     */
    static class XssRequestWrapper extends HttpServletRequestWrapper {

        public XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String[] getParameterValues(String parameter) {
            String[] values = super.getParameterValues(parameter);
            if (values == null) {
                return null;
            }
            String[] cleanValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                cleanValues[i] = stripXss(values[i]);
            }
            return cleanValues;
        }

        @Override
        public String getParameter(String parameter) {
            String value = super.getParameter(parameter);
            return stripXss(value);
        }

        private String stripXss(String value) {
            if (value == null) {
                return null;
            }
            String clean = value;
            clean = SCRIPT_PATTERN.matcher(clean).replaceAll("");
            clean = EVENT_PATTERN.matcher(clean).replaceAll("");
            clean = JAVASCRIPT_PATTERN.matcher(clean).replaceAll("");
            clean = DATA_PATTERN.matcher(clean).replaceAll("");
            return clean;
        }
    }
}