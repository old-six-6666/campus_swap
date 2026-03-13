package com.itcodai.campus_swap.interceptor;

import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.entity.User;
import com.itcodai.campus_swap.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 角色拦截器：校验管理员权限（role >= 1），在 JwtInterceptor 之后执行
 */
@Component
@RequiredArgsConstructor
public class RoleInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null || user.getRole() == null || user.getRole() < 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无管理员权限");
        }
        // 将角色写入请求属性，供 Controller 使用
        request.setAttribute("userRole", user.getRole());
        return true;
    }
}
