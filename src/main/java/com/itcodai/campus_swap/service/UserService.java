package com.itcodai.campus_swap.service;

import com.itcodai.campus_swap.dto.*;
import com.itcodai.campus_swap.vo.LoginVO;
import com.itcodai.campus_swap.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /** 登录，返回 token 和用户信息 */
    LoginVO login(LoginDTO dto);

    /** 注册新用户（含邮箱验证码校验） */
    void register(RegisterDTO dto);

    /** 根据用户 ID 查询个人信息 */
    UserVO getProfile(Long userId);

    /** 更新用户信息（昵称、学校、头像） */
    void updateProfile(Long userId, UserVO vo);

    /** 忘记密码：通过邮箱验证码重置密码（公开接口） */
    void forgotPassword(ForgotPasswordDTO dto);

    /** 修改密码：通过旧密码验证（需登录） */
    void changePasswordByOld(Long userId, ChangePasswordDTO dto);

    /** 修改密码：通过邮箱验证码验证（需登录） */
    void changePasswordByEmail(Long userId, ChangePasswordByEmailDTO dto);

    /** 按昵称搜索用户（排除自己，仅正常状态），用于添加好友 */
    java.util.List<UserVO> searchUsers(Long currentUserId, String keyword);
}
