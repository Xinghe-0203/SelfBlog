package com.selfblog.controller;

import com.selfblog.controller.circle.CircleController;
import com.selfblog.service.circle.CircleInteractionService;
import com.selfblog.service.circle.CircleService;
import com.selfblog.utils.JwtUtils;
import com.selfblog.vo.circle.CirclePostVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CircleController 详情接口的切片测试。
 *
 * <p>锁定两件承重的事：①浏览量自增发生在详情读取**之前**，且走独立的交互服务方法
 * （不能在只读读取路径里自增，否则 MySQL 上会因行锁自死锁等满 50s）；②自增失败
 * 不得让详情请求失败。</p>
 */
@WebMvcTest(CircleController.class)
class CircleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CircleService circleService;

    @MockBean
    private CircleInteractionService circleInteractionService;

    @MockBean
    private JwtUtils jwtUtils;

    private CirclePostVO detail;

    @BeforeEach
    void setUp() {
        detail = new CirclePostVO();
        detail.setId(7L);
        detail.setContent("公开动态");
        detail.setViewCount(1L);
        when(circleService.getPostDetail(eq(7L), any())).thenReturn(detail);
    }

    @Test
    @DisplayName("详情接口先自增浏览量再读取详情")
    void getPostDetail_IncrementsViewCountBeforeReading() throws Exception {
        mockMvc.perform(get("/circle/post/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(7));

        InOrder order = inOrder(circleInteractionService, circleService);
        order.verify(circleInteractionService).incrementViewCount(7L);
        order.verify(circleService).getPostDetail(eq(7L), any());
    }

    @Test
    @DisplayName("浏览量自增失败不影响详情返回")
    void getPostDetail_StillReturnsDetailWhenIncrementFails() throws Exception {
        doThrow(new IllegalStateException("counter unavailable"))
                .when(circleInteractionService).incrementViewCount(7L);

        mockMvc.perform(get("/circle/post/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("公开动态"));

        verify(circleInteractionService).incrementViewCount(7L);
    }
}
