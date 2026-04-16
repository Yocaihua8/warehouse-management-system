package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.CurrentUserContext;
import com.yocaihua.wms.common.UserRoleConstant;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.vo.StockVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LowStockAlertServiceImplTest {

    @Mock
    private StockMapper stockMapper;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private JavaMailSender mailSender;

    private LowStockAlertServiceImpl service;

    @BeforeEach
    void setUp() {
        CurrentUserContext.clear();
        service = createService(mailSender);
        setField("enabled", true);
        setField("cooldownMinutes", 60L);
        setField("maxItemsInMessage", 20);
        setField("mailEnabled", false);
        setField("mailTo", "ops@example.com");
        setField("mailSubjectPrefix", "[WMS低库存预警]");
        setField("webhookEnabled", false);
        setField("webhookUrl", "http://127.0.0.1:9001/low-stock");
        setField("lastDigest", "");
        setField("lastSentAtMillis", 0L);
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void triggerNow_shouldThrow_whenCurrentUserNotAdmin() {
        BusinessException exception = assertThrows(BusinessException.class, () -> service.triggerNow());

        assertEquals("仅管理员可执行：手动触发低库存预警", exception.getMessage());
        verify(stockMapper, never()).selectStockList(any(), any());
    }

    @Test
    void triggerNow_shouldForceSend_whenCurrentUserIsAdmin() {
        CurrentUserContext.setRole(UserRoleConstant.ADMIN);
        setField("mailEnabled", true);

        when(stockMapper.selectStockList(null, null)).thenReturn(List.of(stock(1L, "P001", "商品A", 2, 5, 1)));

        String result = service.triggerNow();

        assertTrue(result.contains("低库存预警已发送"));
        assertTrue(result.contains("渠道=mail"));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void checkAndNotify_shouldReturnDisabled_whenAlertDisabled() {
        setField("enabled", false);

        String result = service.checkAndNotify(false);

        assertEquals("低库存预警通知未启用", result);
        verify(stockMapper, never()).selectStockList(any(), any());
    }

    @Test
    void checkAndNotify_shouldReturnNoLowStock_whenListEmpty() {
        when(stockMapper.selectStockList(null, null)).thenReturn(List.of(
                stock(1L, "P001", "商品A", 20, 5, 0),
                stock(2L, "P002", "商品B", 10, 3, null)
        ));

        String result = service.checkAndNotify(false);

        assertEquals("当前无低库存商品", result);
    }

    @Test
    void checkAndNotify_shouldRespectCooldown_whenSameDigestStillCoolingDown() {
        List<StockVO> lowStocks = List.of(stock(1L, "P001", "商品A", 2, 5, 1));
        when(stockMapper.selectStockList(null, null)).thenReturn(lowStocks);

        alignAwayFromSecondBoundary();
        String messageBody = ReflectionTestUtils.invokeMethod(service, "buildAlertMessage", lowStocks);
        String digest = org.springframework.util.DigestUtils.md5DigestAsHex(messageBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        setField("lastDigest", digest);
        setField("lastSentAtMillis", System.currentTimeMillis());

        String result = service.checkAndNotify(false);

        assertEquals("低库存预警未发送：同内容通知处于冷却期", result);
    }

    @Test
    void checkAndNotify_shouldSendThroughMailAndWebhook_whenBothChannelsSucceed() {
        setField("mailEnabled", true);
        setField("webhookEnabled", true);
        List<StockVO> lowStocks = List.of(stock(1L, "P001", "商品A", 2, 5, 1));
        when(stockMapper.selectStockList(null, null)).thenReturn(lowStocks);
        when(restTemplate.postForEntity(eq("http://127.0.0.1:9001/low-stock"), any(String.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        String result = service.checkAndNotify(true);

        assertTrue(result.contains("低库存预警已发送"));
        assertTrue(result.contains("渠道=mail+webhook"));
        assertTrue(result.contains("商品数=1"));
        verify(mailSender).send(any(SimpleMailMessage.class));
        verify(restTemplate).postForEntity(eq("http://127.0.0.1:9001/low-stock"), any(String.class), eq(String.class));
    }

    @Test
    void checkAndNotify_shouldReturnConfigWarning_whenAllChannelsUnavailable() {
        service = createService(null);
        setField("enabled", true);
        setField("mailEnabled", true);
        setField("mailTo", "ops@example.com");
        setField("webhookEnabled", true);
        setField("webhookUrl", "   ");

        when(stockMapper.selectStockList(null, null)).thenReturn(List.of(stock(1L, "P001", "商品A", 2, 5, 1)));

        String result = service.checkAndNotify(true);

        assertEquals("低库存商品存在，但未发送通知：请检查邮件/Webhook配置", result);
    }

    @Test
    void checkAndNotify_shouldBuildMessageWithFallbackTextAndMaxItemsLimit() {
        setField("mailEnabled", true);
        setField("maxItemsInMessage", 2);
        List<StockVO> lowStocks = List.of(
                stock(1L, " ", "商品A", 2, 5, 1),
                stock(2L, "P002", " ", 1, 4, 1),
                stock(3L, "P003", "商品C", null, null, 1),
                stock(4L, "P004", "商品D", 20, 5, 0)
        );
        when(stockMapper.selectStockList(null, null)).thenReturn(lowStocks);

        String result = service.checkAndNotify(true);

        assertTrue(result.contains("商品数=3"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertTrue(message.getText().contains("低库存商品总数：3"));
        assertTrue(message.getText().contains("1. - / 商品A"));
        assertTrue(message.getText().contains("2. P002 / -"));
        assertTrue(message.getText().contains("... 其余 1 条请在系统库存页查看"));
        assertTrue(message.getSubject().contains("低库存预警（共 3 项）"));
    }

    @Test
    void checkAndNotify_shouldIgnoreMailFailureAndKeepWebhookSuccess() {
        setField("mailEnabled", true);
        setField("webhookEnabled", true);
        when(stockMapper.selectStockList(null, null)).thenReturn(List.of(stock(1L, "P001", "商品A", 2, 5, 1)));
        doThrow(new IllegalStateException("mail down")).when(mailSender).send(any(SimpleMailMessage.class));
        when(restTemplate.postForEntity(eq("http://127.0.0.1:9001/low-stock"), any(String.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        String result = service.checkAndNotify(true);

        assertTrue(result.contains("渠道=webhook"));
    }

    @Test
    void scheduledCheck_shouldReturnDirectly_whenDisabled() {
        setField("enabled", false);

        assertDoesNotThrow(() -> service.scheduledCheck());

        verify(stockMapper, never()).selectStockList(any(), any());
    }

    @Test
    void scheduledCheck_shouldSwallowException_whenCheckFails() {
        when(stockMapper.selectStockList(null, null)).thenThrow(new IllegalStateException("db down"));

        assertDoesNotThrow(() -> service.scheduledCheck());
    }

    private LowStockAlertServiceImpl createService(JavaMailSender availableMailSender) {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(availableMailSender);
        return new LowStockAlertServiceImpl(stockMapper, restTemplate, provider);
    }

    private void setField(String fieldName, Object value) {
        ReflectionTestUtils.setField(service, fieldName, value);
    }

    private void alignAwayFromSecondBoundary() {
        long millis = System.currentTimeMillis() % 1000;
        if (millis > 900) {
            try {
                Thread.sleep(1000 - millis + 50);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("等待冷却测试时间窗口失败", interruptedException);
            }
        }
    }

    private StockVO stock(Long productId,
                          String productCode,
                          String productName,
                          Integer quantity,
                          Integer warningQuantity,
                          Integer lowStock) {
        StockVO stock = new StockVO();
        stock.setProductId(productId);
        stock.setProductCode(productCode);
        stock.setProductName(productName);
        stock.setQuantity(quantity);
        stock.setWarningQuantity(warningQuantity);
        stock.setLowStock(lowStock);
        return stock;
    }
}
