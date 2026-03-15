package com.itcodai.campus_swap.task;

import com.itcodai.campus_swap.service.TradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 交易超时定时任务
 * <p>
 * 每 10 分钟扫描一次：
 * <ul>
 *   <li>WAITING_DELIVERY 且 delivery_deadline &lt; 当前时间 → 自动终止</li>
 *   <li>WAITING_CONFIRM_RECEIPT 且 receipt_deadline &lt; 当前时间 → 自动终止</li>
 * </ul>
 * 需在主启动类添加 {@code @EnableScheduling} 注解才能生效。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TradeTimeoutTask {

    private final TradeService tradeService;

    /**
     * 每 10 分钟执行一次（固定频率，系统启动后 10 分钟开始第一次执行）
     * 可通过修改 fixedRate 调整扫描频率（单位：毫秒）
     */
    @Scheduled(fixedRate = 600_000, initialDelay = 60_000)
    public void checkTimeoutTrades() {
        log.debug("[定时任务] 开始扫描超时交易单...");
        try {
            tradeService.processTimeoutTrades();
        } catch (Exception e) {
            log.error("[定时任务] 超时交易扫描异常", e);
        }
        log.debug("[定时任务] 超时交易扫描完成");
    }
}
