package com.yocaihua.wms.service.impl;

import com.yocaihua.wms.common.BusinessException;
import com.yocaihua.wms.common.CurrentUserContext;
import com.yocaihua.wms.mapper.StockMapper;
import com.yocaihua.wms.service.LowStockAlertService;
import com.yocaihua.wms.vo.StockVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LowStockAlertServiceImpl implements LowStockAlertService {

    private static final Logger log = LoggerFactory.getLogger(LowStockAlertServiceImpl.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StockMapper stockMapper;
    private final RestTemplate restTemplate;
    private final JavaMailSender mailSender;

    @Value("${low-stock-alert.enabled:false}")
    private boolean enabled;

    @Value("${low-stock-alert.cooldown-minutes:60}")
    private long cooldownMinutes;

    @Value("${low-stock-alert.max-items-in-message:20}")
    private int maxItemsInMessage;

    @Value("${low-stock-alert.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${low-stock-alert.mail.to:}")
    private String mailTo;

    @Value("${low-stock-alert.mail.subject-prefix:[WMS低库存预警]}")
    private String mailSubjectPrefix;

    @Value("${low-stock-alert.webhook.enabled:false}")
    private boolean webhookEnabled;

    @Value("${low-stock-alert.webhook.url:}")
    private String webhookUrl;

    private volatile long lastSentAtMillis = 0L;
    private volatile String lastDigest = "";

    public LowStockAlertServiceImpl(StockMapper stockMapper,
                                    RestTemplate restTemplate,
                                    ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.stockMapper = stockMapper;
        this.restTemplate = restTemplate;
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    @Override
    public String triggerNow() {
        if (!CurrentUserContext.isAdmin()) {
            throw new BusinessException("仅管理员可执行：手动触发低库存预警");
        }
        return checkAndNotify(true);
    }

    @Override
    public String checkAndNotify(boolean force) {
        if (!enabled) {
            return "低库存预警通知未启用";
        }

        List<StockVO> lowStocks = loadLowStockItems();
        if (lowStocks.isEmpty()) {
            return "当前无低库存商品";
        }

        String title = "低库存预警（共 " + lowStocks.size() + " 项）";
        String messageBody = buildAlertMessage(lowStocks);
        String digest = DigestUtils.md5DigestAsHex(messageBody.getBytes(StandardCharsets.UTF_8));

        if (!force && isInCooldown(digest)) {
            return "低库存预警未发送：同内容通知处于冷却期";
        }

        List<String> successChannels = new ArrayList<>();
        if (mailEnabled && sendEmail(title, messageBody)) {
            successChannels.add("mail");
        }
        if (webhookEnabled && sendWebhook(title, messageBody)) {
            successChannels.add("webhook");
        }

        if (successChannels.isEmpty()) {
            return "低库存商品存在，但未发送通知：请检查邮件/Webhook配置";
        }

        lastDigest = digest;
        lastSentAtMillis = System.currentTimeMillis();
        return "低库存预警已发送，渠道=" + String.join("+", successChannels) + "，商品数=" + lowStocks.size();
    }

    @Scheduled(cron = "${low-stock-alert.cron:0 */30 * * * *}")
    public void scheduledCheck() {
        if (!enabled) {
            return;
        }
        try {
            String result = checkAndNotify(false);
            log.info("低库存定时检查结果：{}", result);
        } catch (Exception ex) {
            log.error("低库存定时检查失败", ex);
        }
    }

    private List<StockVO> loadLowStockItems() {
        List<StockVO> all = stockMapper.selectStockList(null, null);
        return all.stream()
                .filter(item -> Integer.valueOf(1).equals(item.getLowStock()))
                .collect(Collectors.toList());
    }

    private boolean isInCooldown(String digest) {
        if (digest == null || digest.isEmpty()) {
            return false;
        }
        if (!digest.equals(lastDigest)) {
            return false;
        }
        long cooldownMillis = Math.max(cooldownMinutes, 1L) * 60_000L;
        return System.currentTimeMillis() - lastSentAtMillis < cooldownMillis;
    }

    private String buildAlertMessage(List<StockVO> lowStocks) {
        StringBuilder builder = new StringBuilder();
        builder.append("时间：").append(TIME_FORMATTER.format(LocalDateTime.now())).append('\n');
        builder.append("低库存商品总数：").append(lowStocks.size()).append('\n');
        builder.append("明细（最多展示 ").append(Math.max(maxItemsInMessage, 1)).append(" 条）：").append('\n');

        int limit = Math.max(maxItemsInMessage, 1);
        for (int i = 0; i < lowStocks.size() && i < limit; i++) {
            StockVO item = lowStocks.get(i);
            int quantity = item.getQuantity() == null ? 0 : item.getQuantity();
            int warning = item.getWarningQuantity() == null ? 0 : item.getWarningQuantity();
            builder.append(i + 1).append(". ")
                    .append(defaultText(item.getProductCode())).append(" / ")
                    .append(defaultText(item.getProductName()))
                    .append("，当前=").append(quantity)
                    .append("，预警=").append(warning)
                    .append('\n');
        }
        if (lowStocks.size() > limit) {
            builder.append("... 其余 ").append(lowStocks.size() - limit).append(" 条请在系统库存页查看").append('\n');
        }
        return builder.toString();
    }

    private boolean sendEmail(String title, String body) {
        if (!mailEnabled) {
            return false;
        }
        if (mailSender == null) {
            log.warn("低库存邮件发送跳过：JavaMailSender 不可用");
            return false;
        }
        if (mailTo == null || mailTo.trim().isEmpty()) {
            log.warn("低库存邮件发送跳过：未配置收件人 low-stock-alert.mail.to");
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(mailTo.trim());
            message.setSubject((mailSubjectPrefix == null ? "" : mailSubjectPrefix.trim()) + title);
            message.setText(body);
            mailSender.send(message);
            return true;
        } catch (Exception ex) {
            log.error("低库存邮件发送失败", ex);
            return false;
        }
    }

    private boolean sendWebhook(String title, String body) {
        if (!webhookEnabled) {
            return false;
        }
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.warn("低库存Webhook发送跳过：未配置 low-stock-alert.webhook.url");
            return false;
        }
        try {
            String payload = "{\"title\":\"" + escapeJson(title) + "\",\"content\":\"" + escapeJson(body) + "\"}";
            restTemplate.postForEntity(webhookUrl.trim(), payload, String.class);
            return true;
        } catch (Exception ex) {
            log.error("低库存Webhook发送失败", ex);
            return false;
        }
    }

    private String defaultText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        return value.trim();
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "")
                .replace("\n", "\\n");
    }
}
