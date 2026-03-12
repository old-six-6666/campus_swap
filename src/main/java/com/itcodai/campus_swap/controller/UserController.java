package com.itcodai.campus_swap.controller;

import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.dto.*;
import com.itcodai.campus_swap.service.UserService;
import com.itcodai.campus_swap.service.VerifyCodeService;
import com.itcodai.campus_swap.vo.LoginVO;
import com.itcodai.campus_swap.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户相关接口
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final VerifyCodeService verifyCodeService;

    /** POST /api/user/send-code — 发送验证码（公开） */
    @PostMapping("/send-code")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO dto) {
        verifyCodeService.sendCode(dto.getEmail(), dto.getScene());
        return Result.success();
    }

    /** POST /api/user/login — 登录（公开） */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    /** POST /api/user/register — 注册（公开，需验证码） */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success();
    }

    /** POST /api/user/forgot-password — 忘记密码，邮箱验证码重置（公开） */
    @PostMapping("/forgot-password")
    public Result<Void> forgotPassword(@Valid @RequestBody ForgotPasswordDTO dto) {
        userService.forgotPassword(dto);
        return Result.success();
    }

    /** GET /api/user/profile — 获取当前用户信息（需登录） */
    @GetMapping("/profile")
    public Result<UserVO> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userService.getProfile(userId));
    }

    /** PUT /api/user/profile — 更新用户信息（需登录） */
    @PutMapping("/profile")
    public Result<Void> updateProfile(@RequestBody UserVO vo, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateProfile(userId, vo);
        return Result.success();
    }

    /** PUT /api/user/password — 通过旧密码修改密码（需登录） */
    @PutMapping("/password")
    public Result<Void> changePasswordByOld(@Valid @RequestBody ChangePasswordDTO dto,
                                            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.changePasswordByOld(userId, dto);
        return Result.success();
    }

    /** PUT /api/user/password/by-email — 通过邮箱验证码修改密码（需登录） */
    @PutMapping("/password/by-email")
    public Result<Void> changePasswordByEmail(@Valid @RequestBody ChangePasswordByEmailDTO dto,
                                              HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        userService.changePasswordByEmail(userId, dto);
        return Result.success();
    }

    /** POST /api/user/logout — 退出登录（前端清除 token 即可，后端无状态） */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
