package com.itcodai.campus_swap.service.impl;

import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.dto.*;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.UserMapper;
import com.itcodai.campus_swap.service.UserService;
import com.itcodai.campus_swap.service.VerifyCodeService;
import com.itcodai.campus_swap.utils.JwtUtils;
import com.itcodai.campus_swap.vo.LoginVO;
import com.itcodai.campus_swap.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtils jwtUtils;
    private final VerifyCodeService verifyCodeService;

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail())
        );
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "邮箱或密码错误");
        }
        if (Integer.valueOf(1).equals(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被禁用，请联系管理员");
        }

        String token = jwtUtils.generateToken(user.getId());

        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfo(userVO);
        return loginVO;
    }

    @Override
    public void register(RegisterDTO dto) {
        // 1. 验证邮箱验证码
        verifyCodeService.verifyCode(dto.getEmail(), "REGISTER", dto.getCode());

        // 2. 检查邮箱是否已注册
        long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail())
        );
        if (count > 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "该邮箱已被注册");
        }

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getNickname());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        userMapper.insert(user);
    }

    @Override
    public UserVO getProfile(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }

    @Override
    public void updateProfile(Long userId, UserVO vo) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (vo.getNickname() != null) user.setNickname(vo.getNickname());
        if (vo.getSchool() != null) user.setSchool(vo.getSchool());
        if (vo.getAvatar() != null) user.setAvatar(vo.getAvatar());
        if (vo.getPhone() != null) user.setPhone(vo.getPhone());
        userMapper.updateById(user);
    }

    @Override
    public void forgotPassword(ForgotPasswordDTO dto) {
        // 1. 验证码校验
        verifyCodeService.verifyCode(dto.getEmail(), "FORGOT_PASSWORD", dto.getCode());

        // 2. 查用户
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail())
        );
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该邮箱未注册");
        }

        // 3. 更新密码
        user.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public void changePasswordByOld(Long userId, ChangePasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "旧密码错误");
        }
        user.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public void changePasswordByEmail(Long userId, ChangePasswordByEmailDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }

        // 用当前用户的邮箱验证验证码
        verifyCodeService.verifyCode(user.getEmail(), "CHANGE_PASSWORD", dto.getCode());

        user.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        userMapper.updateById(user);
    }

    @Override
    public List<UserVO> searchUsers(Long currentUserId, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .ne(User::getId, currentUserId)
                        .eq(User::getStatus, 0)
                        .like(User::getNickname, keyword)
                        .last("LIMIT 10")
        );
        return users.stream().map(u -> {
            UserVO vo = new UserVO();
            vo.setId(u.getId());
            vo.setNickname(u.getNickname());
            vo.setAvatar(u.getAvatar());
            vo.setSchool(u.getSchool());
            return vo;
        }).collect(Collectors.toList());
    }
}
