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
                // 浏览器 Cookie 的写请求仅接受同源 JSON；拒绝跨站请求，避免表单式 CSRF。
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
                String token = null;
                if (request.getCookies() != null)
                  for (var cookie : request.getCookies())
                    if (cookie.getName().equals("CAMPUS_SESSION")) token = cookie.getValue();
                request.setAttribute("session", auth.authenticate(token));
                return true;
              }
            })
        .addPathPatterns("/api/**");
  }
}
