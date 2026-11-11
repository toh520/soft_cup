package com.toh520.campus.config;

import com.toh520.campus.common.BusinessException;
import com.toh520.campus.module.auth.service.AuthService;
import jakarta.servlet.http.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class AuthenticationConfig implements WebMvcConfigurer {
  private final AuthService auth;

  public AuthenticationConfig(AuthService auth) {
    this.auth = auth;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(
            new HandlerInterceptor() {
              @Override
              public boolean preHandle(
                  HttpServletRequest request, HttpServletResponse response, Object handler) {
                response.setHeader("Cache-Control", "no-store");
                // 写请求仍限制同源 JSON；身份只接受标签页显式提交的 Bearer 凭据。
                if (!request.getMethod().equals("GET") && !request.getMethod().equals("OPTIONS")) {
                  if ("cross-site".equals(request.getHeader("Sec-Fetch-Site")))
                    throw new BusinessException(403, "不接受跨站操作");
                  if (request.getContentType() == null
                      || !request.getContentType().startsWith("application/json"))
                    throw new BusinessException(415, "请使用 JSON 请求");
                }
                String path = request.getRequestURI();
                if (path.equals("/api/auth/login")
                    || path.equals("/api/auth/register")
                    || path.equals("/api/health")) return true;
                String header = request.getHeader("Authorization");
                String token =
                    header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
                request.setAttribute("sessionToken", token);
                request.setAttribute("session", auth.authenticate(token));
                return true;
              }
            })
        .addPathPatterns("/api/**");
  }
}
