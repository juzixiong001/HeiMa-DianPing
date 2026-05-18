package com.hmdp.config;

import com.hmdp.utils.LoginIntercepter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class MvcConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginIntercepter())
                .excludePathPatterns(
                        "user/code",
                        "user/login",
                        "blog/hot",
                        "shop/**",
                        "shop-type/**"
                );
    }
}