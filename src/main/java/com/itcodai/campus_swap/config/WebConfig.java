package com.itcodai.campus_swap.config;

import com.itcodai.campus_swap.interceptor.JwtInterceptor;
import com.itcodai.campus_swap.interceptor.RoleInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC 配置（注册拦截器）
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final RoleInterceptor roleInterceptor;

    @Value("${file.upload-dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 转换为绝对 URI（Windows 需要 file:///D:/... 格式，且必须以 / 结尾）
        String location = Paths.get(uploadDir).toAbsolutePath().toUri().toString();
        if (!location.endsWith("/")) location += "/";
        registry.addResourceHandler("/uploads/**").addResourceLocations(location);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT 拦截器：验证登录状态（对所有 /api/** 路由生效）
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/user/send-code",
                        "/api/user/forgot-password",
                        "/api/item/list",
                        "/api/trade/item/*"
                );

        // 角色拦截器：验证管理员权限（在 JWT 拦截器之后执行）
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/api/admin/**");
    }
}
