package com.itcodai.campus_swap.service;

/**
 * 邮件发送服务
 */
public interface EmailService {

    /** 发送 HTML 邮件 */
    void sendHtml(String to, String subject, String htmlContent);
}
