package com.selfblog.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.mockito.Mockito.*;

@DisplayName("XssFilter 单元测试")
class XssFilterTest {

    private XssFilter xssFilter;
    private FilterChain filterChain;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        xssFilter = new XssFilter();
        filterChain = mock(FilterChain.class);
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("doFilter - 始终调用FilterChain（不阻断请求）")
    void doFilter_shouldAlwaysCallFilterChain() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        xssFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(any(), eq(response));
    }

    @Test
    @DisplayName("doFilter - Script标签不阻断请求，仅记录日志")
    void doFilter_shouldNotBlockScriptTags() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("content", "<script>alert('xss')</script>");
        xssFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(any(), eq(response));
    }

    @Test
    @DisplayName("doFilter - 正常文本不阻断")
    void doFilter_shouldNotBlockNormalContent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("content", "Hello World! This is normal text.");
        xssFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(any(), eq(response));
    }

    @Test
    @DisplayName("doFilter - 无参数时不阻断")
    void doFilter_shouldHandleNullParameters() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        xssFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(any(), eq(response));
    }

    @Test
    @DisplayName("doFilter - 多参数混合内容不阻断")
    void doFilter_shouldHandleMultipleParameters() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("title", "Normal Title");
        request.setParameter("content", "<script>alert('xss')</script>");
        xssFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(any(), eq(response));
    }

    @Test
    @DisplayName("doFilter - 事件处理器检测不阻断")
    void doFilter_shouldDetectEventHandlers() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("content", "<img onerror='alert(1)' src='x'>");
        xssFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(any(), eq(response));
    }

    @Test
    @DisplayName("doFilter - JavaScript URI检测不阻断")
    void doFilter_shouldDetectJavaScriptUri() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("url", "javascript:alert(1)");
        xssFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(any(), eq(response));
    }

    @Test
    @DisplayName("doFilter - Data URI检测不阻断")
    void doFilter_shouldDetectDataUri() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("url", "data:text/html,<script>alert(1)</script>");
        xssFilter.doFilter(request, response, filterChain);
        verify(filterChain, times(1)).doFilter(any(), eq(response));
    }

    @Test
    @DisplayName("doFilter - 返回的请求为XssRequestWrapper类型")
    void doFilter_shouldWrapRequestInXssRequestWrapper() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("name", "test");
        xssFilter.doFilter(request, response, filterChain);
        verify(filterChain).doFilter(argThat(req -> req instanceof XssFilter.XssRequestWrapper), eq(response));
    }

    @Test
    @DisplayName("XssRequestWrapper - getParameterValues返回清洗后的值")
    void xssRequestWrapper_shouldReturnCleanedParameterValues() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("content", "Normal text");
        request.setParameter("xss", "<script>alert(1)</script>");

        xssFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(argThat(req -> {
            if (!(req instanceof XssFilter.XssRequestWrapper)) return false;
            XssFilter.XssRequestWrapper wrapper = (XssFilter.XssRequestWrapper) req;
            String[] contentValues = wrapper.getParameterValues("content");
            String[] xssValues = wrapper.getParameterValues("xss");
            return contentValues != null && contentValues[0].equals("Normal text")
                    && xssValues != null && !xssValues[0].contains("<script>");
        }), eq(response));
    }

    @Test
    @DisplayName("XssRequestWrapper - getParameter不存在时返回null")
    void xssRequestWrapper_shouldReturnNullForMissingParameter() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();

        xssFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(argThat(req -> {
            if (!(req instanceof XssFilter.XssRequestWrapper)) return false;
            XssFilter.XssRequestWrapper wrapper = (XssFilter.XssRequestWrapper) req;
            return wrapper.getParameter("nonexistent") == null;
        }), eq(response));
    }

    @Test
    @DisplayName("XssRequestWrapper - getParameterValues不存在时返回null")
    void xssRequestWrapper_shouldReturnNullArrayForMissingParameter() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();

        xssFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(argThat(req -> {
            if (!(req instanceof XssFilter.XssRequestWrapper)) return false;
            XssFilter.XssRequestWrapper wrapper = (XssFilter.XssRequestWrapper) req;
            return wrapper.getParameterValues("nonexistent") == null;
        }), eq(response));
    }
}
