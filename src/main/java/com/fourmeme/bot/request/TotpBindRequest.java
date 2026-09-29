package com.fourmeme.bot.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "绑定 Google 验证器")
public class TotpBindRequest {

    @Schema(description = "6 位验证码", required = true, example = "123456")
    private String totpCode;
}