package com.fourmeme.bot.response;

import com.fourmeme.bot.dto.UserDTO;
import lombok.Data;

@Data
public class LoginResponse {

    /** 完整 token（全部流程完成时返回） */
    private String token;

    /** 临时 token（流程中间步骤使用） */
    private String tempToken;

    /** 需要修改初始密码 */
    private boolean requirePasswordChange;

    /** 需要绑定 TOTP */
    private boolean requireBinding;

    /** 需要输入 TOTP 验证码 */
    private boolean requireTotp;

    /** 用户信息（登录成功时返回） */
    private UserDTO user;
}