package com.itcodai.campus_swap.service.impl;

import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.util.RandomUtil;
import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.service.EmailService;
import com.itcodai.campus_swap.service.VerifyCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 验证码服务（Hutool TimedCache 内存缓存，过期自动失效）
 */
@Service
@RequiredArgsConstructor
public class VerifyCodeServiceImpl implements VerifyCodeService {

    private final EmailService emailService;

    @Value("${verify.code.expire-minutes:5}")
    private int expireMinutes;

    /**
     * 主缓存：key="email:scene" → 验证码，过期时间由 expireMinutes 决定
     * 冷却缓存：key="cool:email:scene" → 固定值，60 秒过期，防频繁发送
     */
    private final TimedCache<String, String> codeCache = new TimedCache<>(5 * 60 * 1000L);
    private final TimedCache<String, String> coolCache = new TimedCache<>(60 * 1000L);

    @Override
    public void sendCode(String email, String scene) {
        String coolKey = "cool:" + email + ":" + scene;
        if (coolCache.get(coolKey) != null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "发送太频繁，请 60 秒后重试");
        }

        String code = RandomUtil.randomNumbers(6);
        long ttl = (long) expireMinutes * 60 * 1000;

        String codeKey = email + ":" + scene;
        codeCache.put(codeKey, code, ttl);
        coolCache.put(coolKey, "1", 60 * 1000L);

        emailService.sendHtml(email, buildSubject(scene), buildContent(code, expireMinutes));
    }

    @Override
    public boolean verifyCode(String email, String scene, String code) {
        String codeKey = email + ":" + scene;
        String cached = codeCache.get(codeKey);
        if (cached == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码已过期，请重新获取");
        }
        if (!cached.equals(code)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "验证码错误");
        }
        codeCache.remove(codeKey); // 验证通过后立即删除，防止重复使用
        return true;
    }

    private String buildSubject(String scene) {
        return switch (scene) {
            case "REGISTER"        -> "【校园置换】注册验证码";
            case "FORGOT_PASSWORD" -> "【校园置换】重置密码验证码";
            case "CHANGE_PASSWORD" -> "【校园置换】修改密码验证码";
            default                -> "【校园置换】验证码";
        };
    }

    private String buildContent(String code, int expireMinutes) {
        return """
                <div style="font-family:Arial,sans-serif;max-width:480px;margin:0 auto;padding:32px;border:1px solid #e4e7ed;border-radius:8px;">
                  <h2 style="color:#409eff;margin-top:0;">校园置换平台</h2>
                  <p style="color:#303133;font-size:15px;">您的验证码为：</p>
                  <div style="font-size:36px;font-weight:bold;letter-spacing:8px;color:#409eff;text-align:center;padding:16px 0;">%s</div>
                  <p style="color:#909399;font-size:13px;">验证码 %d 分钟内有效，请勿泄露给他人。</p>
                  <p style="color:#c0c4cc;font-size:12px;margin-bottom:0;">如非本人操作，请忽略此邮件。</p>
                </div>
                """.formatted(code, expireMinutes);
    }
}
