package com.itcodai.campus_swap.service;

/**
 * 验证码服务：发送 + 校验（内存缓存，5 分钟过期）
 */
public interface VerifyCodeService {

    /**
     * 发送验证码到指定邮箱
     * @param email 目标邮箱
     * @param scene 场景：REGISTER / FORGOT_PASSWORD / CHANGE_PASSWORD
     */
    void sendCode(String email, String scene);

    /**
     * 校验验证码（验证通过后自动删除，防重放）
     * @return true=通过，false=错误或已过期
     */
    boolean verifyCode(String email, String scene, String code);
}
