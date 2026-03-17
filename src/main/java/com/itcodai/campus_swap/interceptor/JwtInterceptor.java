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
        String uri = request.getRequestURI();

        // 可选认证接口：有 token 就解析 userId，没有也放行（未登录用户可浏览）
        boolean optionalAuth = "GET".equalsIgnoreCase(request.getMethod())
                && (uri.matches("/api/item/\\d+")
                    || uri.equals("/post/list")
                    || uri.equals("/api/post/list")
                    || uri.matches("/post/\\d+")
                    || uri.matches("/api/post/\\d+")
                    || uri.matches("/api/user/\\d+/profile")
                    || uri.matches("/api/item/user/\\d+")
                    || uri.matches("/post/user/\\d+")
                    || uri.matches("/api/post/user/\\d+")
                    || uri.equals("/api/user/search"));
        if (optionalAuth) {
            String token = request.getHeader("Authorization");
            if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
                String jwt = token.substring(7);
                if (jwtUtils.validateToken(jwt)) {
                    request.setAttribute("userId", jwtUtils.getUserId(jwt));
                }
            }
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
