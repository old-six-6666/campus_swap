package com.itcodai.campus_swap.controller;

import com.itcodai.campus_swap.common.result.Result;
import com.itcodai.campus_swap.dto.*;
import com.itcodai.campus_swap.service.StudentService;
import com.itcodai.campus_swap.service.UserService;
import com.itcodai.campus_swap.service.VerifyCodeService;
import com.itcodai.campus_swap.vo.LoginVO;
import com.itcodai.campus_swap.vo.StudentVerifyVO;
import com.itcodai.campus_swap.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户相关接口
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final VerifyCodeService verifyCodeService;
    private final StudentService studentService;

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

    /** GET /api/user/search — 按昵称搜索用户（需登录），用于添加好友 */
    @GetMapping("/search")
    public Result<List<UserVO>> searchUsers(
            @RequestParam(defaultValue = "") String keyword,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(userService.searchUsers(userId, keyword));
    }

    /** POST /api/user/logout — 退出登录（前端清除 token 即可，后端无状态） */
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    /** POST /api/user/verify — 提交学生认证申请（需登录） */
    @PostMapping("/verify")
    public Result<Void> applyVerify(@Valid @RequestBody StudentVerifyApplyDTO dto,
                                    HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        studentService.applyVerify(userId, dto);
        return Result.success();
    }

    /** GET /api/user/verify — 查询当前用户的认证申请状态（需登录） */
    @GetMapping("/verify")
    public Result<StudentVerifyVO> getMyVerification(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return Result.success(studentService.getMyVerification(userId));
    }

    /** GET /api/user/{id}/profile — 获取指定用户的公开信息（无需登录） */
    @GetMapping("/{id:\\d+}/profile")
    public Result<UserVO> getUserProfile(@PathVariable Long id) {
        return Result.success(userService.getPublicProfile(id));
    }
}
