package com.fourmeme.bot.config;

import com.fourmeme.bot.entity.User;
import com.fourmeme.bot.service.UserService;
import com.fourmeme.bot.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


@Slf4j
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final UserService userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.trim().isEmpty()) {
            writeUnauthorized(response, "用户未登录");
            return false;
        }

        String token = jwtUtil.extractFromHeader(authHeader);
        if (!jwtUtil.isFullToken(token)) {
            writeUnauthorized(response, "登录已过期，请重新登录");
            return false;
        }

        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            writeUnauthorized(response, "token无效，请重新登录");
            return false;
        }

        // ===== 校验 token 版本号 =====
        Integer tokenVersion = jwtUtil.getTokenVersion(token);
        User user = userService.getById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            writeUnauthorized(response, "账号不可用");
            return false;
        }
        int dbVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        int tkVersion = tokenVersion == null ? 0 : tokenVersion;
        if (tkVersion != dbVersion) {
            writeUnauthorized(response, "登录已失效，请重新登录");
            return false;
        }

        request.setAttribute("userId", userId);
        request.setAttribute("username", user.getUsername());
        return true;
    }

    private void writeUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":401,\"msg\":\"" + msg + "\",\"data\":null}");
    }
}