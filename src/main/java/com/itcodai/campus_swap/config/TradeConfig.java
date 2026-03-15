package com.itcodai.campus_swap.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 交易系统全局配置
 * <p>
 * 通过 application.properties 中的 trade.* 前缀配置，无需修改代码即可调整业务参数：
 * <pre>
 * # 是否启用审核阶段（false 时 MATCHED 直接进入 WAITING_DELIVERY）
 * trade.audit.enabled=true
 * # 审核模式：1=平台审核  2=双方互审
 * trade.audit.mode=1
 * # 发货超时小时数（默认 48 小时）
 * trade.timeout.delivery-hours=48
 * # 收货确认超时小时数（默认 72 小时）
 * trade.timeout.receipt-hours=72
 * </pre>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "trade")
public class TradeConfig {

    private Audit audit = new Audit();
    private Timeout timeout = new Timeout();

    @Data
    public static class Audit {
        /** 是否启用审核阶段，false 时跳过审核直接进入 WAITING_DELIVERY */
        private boolean enabled = true;
        /** 审核模式：1=平台审核  2=双方互审 */
        private int mode = 1;
    }

    @Data
    public static class Timeout {
        /** WAITING_DELIVERY 发货超时小时数，超时自动终止交易 */
        private int deliveryHours = 48;
        /** WAITING_CONFIRM_RECEIPT 收货确认超时小时数，超时自动终止交易 */
        private int receiptHours = 72;
    }
}
