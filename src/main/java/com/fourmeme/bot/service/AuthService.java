package com.fourmeme.bot.service;

import com.fourmeme.bot.dto.UserDTO;
import com.fourmeme.bot.response.LoginResponse;
import com.fourmeme.bot.response.TotpSetupResponse;

public interface AuthService {

    LoginResponse login(String username, String password, String totpCode);

    /** 修改密码（用 tempToken 授权），修改完成后返回下一步所需状态 */
    LoginResponse changePassword(String tempToken, String oldPassword, String newPassword);

    TotpSetupResponse generateTotpSetup(String tempToken);

    LoginResponse bindTotp(String tempToken, String totpCode);

    UserDTO getCurrentUser(String token);

    void logout(String token);
}