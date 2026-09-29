package com.fourmeme.bot.service.impl;

import com.fourmeme.bot.dto.UserDTO;
import com.fourmeme.bot.entity.User;
import com.fourmeme.bot.response.LoginResponse;
import com.fourmeme.bot.response.TotpSetupResponse;
import com.fourmeme.bot.service.AuthService;
import com.fourmeme.bot.service.UserService;
import com.fourmeme.bot.util.JwtUtil;
import com.fourmeme.bot.util.TotpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final TotpUtil totpUtil;

    @Override
    public LoginResponse login(String username, String password, String totpCode) {
        // 1. 用户名密码校验
        User user = userService.getByUsername(username);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (!userService.validatePassword(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        LoginResponse resp = new LoginResponse();

        // 2. 未修改初始密码 → 强制改密码
        if (user.getPasswordChanged() == null || user.getPasswordChanged() != 1) {
            resp.setRequirePasswordChange(true);
            resp.setTempToken(jwtUtil.generateTempToken(user.getId(), user.getUsername()));
            log.info("用户 {} 使用初始密码登录，要求先修改密码", username);
            return resp;
        }

        // 3. 未绑定 TOTP → 强制绑定
        if (user.getTotpBound() == null || user.getTotpBound() != 1) {
            resp.setRequireBinding(true);
            resp.setTempToken(jwtUtil.generateTempToken(user.getId(), user.getUsername()));
            log.info("用户 {} 未绑定 TOTP", username);
            return resp;
        }

        // 4. 已绑定但未传验证码 → 要求输入
        if (totpCode == null || totpCode.trim().isEmpty()) {
            resp.setRequireTotp(true);
            resp.setTempToken(jwtUtil.generateTempToken(user.getId(), user.getUsername()));
            return resp;
        }

        // 5. 校验 TOTP
        checkTotpCode(user, totpCode);

        // 6. 全部通过，签发完整 token
        userService.updateLastLogin(user.getId());
        User latest = userService.getById(user.getId());
        resp.setToken(issueFullToken(latest));
        resp.setUser(toUserInfo(latest));
        log.info("用户 {} 登录成功", username);
        return resp;
    }

    @Override
    public LoginResponse changePassword(String tempToken, String oldPassword, String newPassword) {
        if (!jwtUtil.isTempToken(tempToken)) {
            throw new IllegalArgumentException("临时 token 无效或已过期");
        }
        Long userId = jwtUtil.getUserId(tempToken);
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        if (!userService.validatePassword(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("新密码长度至少 6 位");
        }
        if (oldPassword.equals(newPassword)) {
            throw new IllegalArgumentException("新密码不能与原密码相同");
        }

        userService.changePassword(userId, newPassword);
        // 修改密码后，所有已签发的 token 全部失效
        userService.bumpTokenVersion(userId);

        // 修改完后重新加载用户，决定下一步
        user = userService.getById(userId);
        LoginResponse resp = new LoginResponse();
        resp.setTempToken(jwtUtil.generateTempToken(userId, user.getUsername()));

        if (user.getTotpBound() == null || user.getTotpBound() != 1) {
            resp.setRequireBinding(true);
        } else {
            resp.setRequireTotp(true);
        }
        log.info("用户 {} 密码已修改，进入下一步", user.getUsername());
        return resp;
    }

    @Override
    public TotpSetupResponse generateTotpSetup(String tempToken) {
        if (!jwtUtil.isTempToken(tempToken)) {
            throw new IllegalArgumentException("临时 token 无效或已过期");
        }
        Long userId = jwtUtil.getUserId(tempToken);
        String username = jwtUtil.getUsername(tempToken);
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        String secret = totpUtil.generateSecret();
        String otpAuthUrl = totpUtil.buildOtpAuthUrl(username, secret);
        String qrCode = totpUtil.generateQrCodeBase64(otpAuthUrl, 240);

        userService.updateTotpSecret(userId, secret);

        TotpSetupResponse resp = new TotpSetupResponse();
        resp.setSecret(secret);
        resp.setOtpAuthUrl(otpAuthUrl);
        resp.setQrCode(qrCode);
        return resp;
    }

    @Override
    public LoginResponse bindTotp(String tempToken, String totpCode) {
        if (!jwtUtil.isTempToken(tempToken)) {
            throw new IllegalArgumentException("临时 token 无效或已过期");
        }
        Long userId = jwtUtil.getUserId(tempToken);
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        if (user.getTotpSecret() == null || user.getTotpSecret().isEmpty()) {
            throw new IllegalArgumentException("请先获取密钥");
        }

        checkTotpCode(user, totpCode);

        userService.markTotpBound(userId);
        userService.updateLastLogin(userId);
        // 绑定完成后先递增版本号，再签发新 token
        userService.bumpTokenVersion(userId);

        User updated = userService.getById(userId);
        LoginResponse resp = new LoginResponse();
        resp.setToken(issueFullToken(updated));
        resp.setUser(toUserInfo(updated));
        log.info("用户 {} 完成 TOTP 绑定", user.getUsername());
        return resp;
    }

    @Override
    public UserDTO getCurrentUser(String token) {
        if (!jwtUtil.isFullToken(token)) {
            throw new IllegalArgumentException("token 无效");
        }
        Long userId = jwtUtil.getUserId(token);
        User user = userService.getById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");

        // 校验版本号
        Integer tokenVersion = jwtUtil.getTokenVersion(token);
        int dbVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        int tkVersion = tokenVersion == null ? 0 : tokenVersion;
        if (tkVersion != dbVersion) {
            throw new IllegalArgumentException("登录已失效，请重新登录");
        }
        return toUserInfo(user);
    }

    @Override
    public void logout(String token) {
        if (!jwtUtil.isFullToken(token)) {
            return;
        }
        Long userId = jwtUtil.getUserId(token);
        if (userId != null) {
            userService.bumpTokenVersion(userId);
            log.info("用户 {} 已登出", userId);
        }
    }

    // ==================== 私有方法 ====================

    private void checkTotpCode(User user, String totpCode) {
        if (totpCode == null || totpCode.trim().isEmpty()) {
            throw new IllegalArgumentException("请输入验证码");
        }
        int code;
        try {
            code = Integer.parseInt(totpCode.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("验证码格式不正确");
        }
        if (!totpUtil.verify(user.getTotpSecret(), code)) {
            throw new IllegalArgumentException("验证码错误");
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

    private String issueFullToken(User user) {
        int ver = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        return jwtUtil.generateToken(user.getId(), user.getUsername(), ver);
    }
}