package com.group1.cloudlibrary.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group1.cloudlibrary.common.Result;
import com.group1.cloudlibrary.config.CloudLibraryProperties;
import com.group1.cloudlibrary.entity.User;
import com.group1.cloudlibrary.mapper.UserMapper;
import com.group1.cloudlibrary.util.JwtUtils;
import com.group1.cloudlibrary.util.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * JWT 认证拦截器：校验令牌 + 角色鉴权
 */
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtils jwtUtils;
    private final CloudLibraryProperties properties;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * JWT 认证拦截器：校验令牌 + 角色鉴权
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = properties.getJwt().getHeader();
        String prefix = properties.getJwt().getPrefix();
        String token = request.getHeader(header);
        if (StringUtils.hasText(token) && token.startsWith(prefix)) {
            token = token.substring(prefix.length());
        }

        if (!StringUtils.hasText(token)) {
            writeError(response, Result.UNAUTHORIZED, "未登录或登录已失效，请重新登录");
            return false;
        }

        Claims claims = jwtUtils.parseToken(token);
        if (claims == null) {
            writeError(response, Result.UNAUTHORIZED, "登录已过期，请重新登录");
            return false;
        }

        Long userId = Long.valueOf(claims.getSubject());

        // 校验用户是否仍然有效（被注销 / 禁用已过期的自动恢复）
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == 0) {
            writeError(response, Result.UNAUTHORIZED, "账号状态异常，请重新登录");
            return false;
        }

        // 角色一律以数据库为准：这样被设为 / 取消管理员后无需重新登录即可生效
        Integer role = user.getRole() == null ? 0 : user.getRole();
        boolean pendingAdmin = user.getPendingAdmin() != null && user.getPendingAdmin() == 1;

        // 管理端接口需要「正式管理员」角色；待激活管理员还在用户端，不能访问管理端
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/admin")) {
            if (role != 1) {
                writeError(response, Result.FORBIDDEN, "无权限访问管理端接口");
                return false;
            }
            if (pendingAdmin) {
                writeError(response, Result.FORBIDDEN, "您还未完成管理员激活，请先归还全部图书");
                return false;
            }
        }

        UserContext.set(userId, role, user.getEmail());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(code, message)));
    }
}
