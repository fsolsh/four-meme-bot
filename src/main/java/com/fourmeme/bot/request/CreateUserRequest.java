package com.fourmeme.bot.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "创建新用户（仅admin）")
public class CreateUserRequest {

    @Schema(description = "用户名", required = true, example = "user1")
    private String username;

    @Schema(description = "初始密码（至少6位）", required = true, example = "user123")
    private String password;

    @Schema(description = "角色：ADMIN 或 USER", example = "USER")
    private String role = "USER";
}