package com.fourmeme.bot.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "登录请求")
public class LoginRequest {

    @Schema(description = "用户名", required = true, example = "admin")
    private String username;

    @Schema(description = "密码", required = true, example = "admin123")
    private String password;

    @Schema(description = "Google 验证码（已绑定用户必填）", example = "123456")
    private String totpCode;
}