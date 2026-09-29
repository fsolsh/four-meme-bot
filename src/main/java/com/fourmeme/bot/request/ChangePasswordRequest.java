package com.fourmeme.bot.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "修改密码")
public class ChangePasswordRequest {

    @Schema(description = "原密码", required = true)
    private String oldPassword;

    @Schema(description = "新密码（至少6位）", required = true)
    private String newPassword;
}