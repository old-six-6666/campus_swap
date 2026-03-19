package com.itcodai.campus_swap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // 启用定时任务（交易超时自动处理）
@EnableAsync        // 启用异步任务（AI 回复异步调用）
public class CampusSwapApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusSwapApplication.class, args);
    }

}
