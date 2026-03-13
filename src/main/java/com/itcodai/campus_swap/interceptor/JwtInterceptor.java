package com.itcodai.campus_swap.interceptor;

import com.itcodai.campus_swap.common.exception.BusinessException;
import com.itcodai.campus_swap.common.result.ResultCode;
import com.itcodai.campus_swap.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 认证拦截器
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 商品详情是公开的只读接口：GET /api/item/{纯数字 id}，无需登录
        if ("GET".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().matches("/api/item/\\d+")) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token) || !token.startsWith("Bearer ")) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String jwt = token.substring(7);
        if (!jwtUtils.validateToken(jwt)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        // 将用户 ID 放入请求属性，供 Controller 使用
        request.setAttribute("userId", jwtUtils.getUserId(jwt));
        return true;
    }
}
