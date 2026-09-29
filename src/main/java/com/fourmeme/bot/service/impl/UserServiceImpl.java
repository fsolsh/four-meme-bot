package com.fourmeme.bot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fourmeme.bot.entity.User;
import com.fourmeme.bot.mapper.UserMapper;
import com.fourmeme.bot.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, ApplicationRunner {

    private final UserMapper userMapper;

    /**
     * 启动时检查并初始化 admin 账号
     * 默认：admin / admin（首次登录会强制修改密码）
     */
    @Override
    public void run(ApplicationArguments args) {
        User admin = getByUsername("admin");
        if (admin == null) {
            User user = new User();
            user.setUsername("admin");
            user.setPassword(BCrypt.hashpw("admin", BCrypt.gensalt(10)));
            user.setRole("ADMIN");
            user.setTotpBound(0);
            user.setPasswordChanged(0);
            user.setStatus(1);
            userMapper.insert(user);
            log.info("════════════════════════════════════════════════════");
            log.info("  已创建默认管理员账号");
            log.info("  用户名: admin");
            log.info("  密码:   admin");
            log.info("  首次登录后必须修改密码并绑定 Google 验证器");
            log.info("════════════════════════════════════════════════════");
        }
        // ===== 启动时让所有旧会话失效 =====
        bumpAllTokenVersions();
    }

    @Override
    public User getByUsername(String username) {
        if (username == null || username.trim().isEmpty()) return null;
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, username.trim()));
    }

    @Override
    public User getById(Long id) {
        return id == null ? null : userMapper.selectById(id);
    }

    @Override
    public List<User> listAll() {
        return userMapper.selectList(
                new LambdaQueryWrapper<User>().orderByAsc(User::getId));
    }

    @Override
    public User createUser(String username, String rawPassword, String role) {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("密码长度至少 6 位");
        }
        if (getByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }

        String userRole = (role == null || role.trim().isEmpty()) ? "USER" : role.trim().toUpperCase();
        if (!"ADMIN".equals(userRole) && !"USER".equals(userRole)) {
            throw new IllegalArgumentException("角色只能是 ADMIN 或 USER");
        }

        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt(10)));
        user.setRole(userRole);
        user.setTotpBound(0);
        // 由 admin 创建的用户密码初始为已修改状态（不强制改）
        // 如果希望新用户也强制改密码，把这里改成 0
        user.setPasswordChanged(1);
        user.setStatus(1);
        userMapper.insert(user);
        log.info("管理员创建用户: {} (role={})", username, userRole);
        return user;
    }

    @Override
    public boolean validatePassword(String rawPassword, String hash) {
        if (rawPassword == null || hash == null) return false;
        try {
            return BCrypt.checkpw(rawPassword, hash);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void changePassword(Long userId, String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException("密码长度至少 6 位");
        }
        User user = new User();
        user.setId(userId);
        user.setPassword(BCrypt.hashpw(rawPassword, BCrypt.gensalt(10)));
        user.setPasswordChanged(1);
        userMapper.updateById(user);
        log.info("用户 {} 修改了密码", userId);
    }

    @Override
    public void updateLastLogin(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public void updateTotpSecret(Long userId, String secret) {
        User user = new User();
        user.setId(userId);
        user.setTotpSecret(secret);
        user.setTotpBound(0);
        userMapper.updateById(user);
    }

    @Override
    public void markTotpBound(Long userId) {
        User user = new User();
        user.setId(userId);
        user.setTotpBound(1);
        userMapper.updateById(user);
    }

    @Override
    public void setStatus(Long userId, Integer status) {
        User user = new User();
        user.setId(userId);
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    public void bumpTokenVersion(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return;
        User update = new User();
        update.setId(userId);
        update.setTokenVersion(
                user.getTokenVersion() == null ? 1 : user.getTokenVersion() + 1);
        userMapper.updateById(update);
    }

    @Override
    public void bumpAllTokenVersions() {
        // 使用 SQL 直接 UPDATE，避免逐个查询
        userMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<User>()
                        .setSql("token_version = COALESCE(token_version, 0) + 1"));
        log.warn("已递增所有用户的 token 版本号，所有已登录会话将失效");
    }
}