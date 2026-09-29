package com.fourmeme.bot.service;

import com.fourmeme.bot.entity.User;

import java.util.List;

public interface UserService {

    User getByUsername(String username);

    User getById(Long id);

    List<User> listAll();

    /** 创建用户（用于 admin 添加用户） */
    User createUser(String username, String rawPassword, String role);

    boolean validatePassword(String rawPassword, String hash);

    /** 修改密码（同时把 password_changed 置为 1） */
    void changePassword(Long userId, String rawPassword);

    void updateLastLogin(Long userId);

    void updateTotpSecret(Long userId, String secret);

    void markTotpBound(Long userId);

    void setStatus(Long userId, Integer status);

    /** 递增 token 版本号，使该用户所有已签发的 token 失效 */
    void bumpTokenVersion(Long userId);

    /** 递增所有用户的 token 版本号（用于服务器重启时强制失效） */
    void bumpAllTokenVersions();
}