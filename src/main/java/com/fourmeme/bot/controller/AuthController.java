package com.fourmeme.bot.controller;

import com.fourmeme.bot.dto.UserDTO;
import com.fourmeme.bot.entity.User;
import com.fourmeme.bot.request.ChangePasswordRequest;
import com.fourmeme.bot.request.CreateUserRequest;
import com.fourmeme.bot.request.LoginRequest;
import com.fourmeme.bot.request.TotpBindRequest;
import com.fourmeme.bot.response.LoginResponse;
import com.fourmeme.bot.response.R;
import com.fourmeme.bot.response.TotpSetupResponse;
import com.fourmeme.bot.service.AuthService;
import com.fourmeme.bot.service.UserService;
import com.fourmeme.bot.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证", description = "登录、修改密码、TOTP、用户管理")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    // ==================== 登录流程 ====================

    @PostMapping("/login")
    @Operation(summary = "登录")
    public R<LoginResponse> login(@RequestBody LoginRequest req) {
        return R.success(authService.login(req.getUsername(), req.getPassword(), req.getTotpCode()));
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改初始密码", description = "需要临时 token")
    public R<LoginResponse> changePassword(@RequestHeader("Authorization") String authHeader,
                                           @RequestBody ChangePasswordRequest req) {
        String tempToken = jwtUtil.extractFromHeader(authHeader);
        return R.success(authService.changePassword(tempToken, req.getOldPassword(), req.getNewPassword()));
    }

    @PostMapping("/totp/setup")
    @Operation(summary = "获取 TOTP 绑定信息")
    public R<TotpSetupResponse> totpSetup(@RequestHeader("Authorization") String authHeader) {
        String tempToken = jwtUtil.extractFromHeader(authHeader);
        return R.success(authService.generateTotpSetup(tempToken));
    }

    @PostMapping("/totp/bind")
    @Operation(summary = "确认绑定 TOTP")
    public R<LoginResponse> totpBind(@RequestHeader("Authorization") String authHeader,
                                     @RequestBody TotpBindRequest req) {
        String tempToken = jwtUtil.extractFromHeader(authHeader);
        return R.success(authService.bindTotp(tempToken, req.getTotpCode()));
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息")
    public R<UserDTO> me(@RequestHeader("Authorization") String authHeader) {
        String token = jwtUtil.extractFromHeader(authHeader);
        return R.success(authService.getCurrentUser(token));
    }

    // ==================== 用户管理（仅 admin） ====================

    @GetMapping("/users")
    @Operation(summary = "用户列表（仅admin）")
    public R<List<UserDTO>> listUsers(@RequestHeader("Authorization") String authHeader) {
        requireAdmin(authHeader);
        List<UserDTO> list = userService.listAll().stream()
                .map(this::toUserInfo)
                .collect(Collectors.toList());
        return R.success(list);
    }

    @PostMapping("/users")
    @Operation(summary = "创建用户（仅admin）")
    public R<UserDTO> createUser(@RequestHeader("Authorization") String authHeader,
                                 @RequestBody CreateUserRequest req) {
        requireAdmin(authHeader);
        User user = userService.createUser(req.getUsername(), req.getPassword(), req.getRole());
        return R.success(toUserInfo(user));
    }

    @PostMapping("/logout")
    @Operation(summary = "登出")
    public R<Void> logout(@RequestHeader("Authorization") String authHeader) {
        String token = jwtUtil.extractFromHeader(authHeader);
        authService.logout(token);
        return R.success();
    }

    // ==================== 私有 ====================

    private void requireAdmin(String authHeader) {
        String token = jwtUtil.extractFromHeader(authHeader);
        if (!jwtUtil.isFullToken(token)) {
            throw new IllegalArgumentException("未登录");
        }
        Long userId = jwtUtil.getUserId(token);
        User user = userService.getById(userId);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("无权限，仅管理员可操作");
        }
    }

    private UserDTO toUserInfo(User user) {
        UserDTO info = new UserDTO();
        info.setId(user.getId());
        info.setUsername(user.getUsername());
        info.setRole(user.getRole());
        info.setTotpBound(user.getTotpBound() != null && user.getTotpBound() == 1);
        info.setLastLoginAt(user.getLastLoginAt());
        return info;
    }
}