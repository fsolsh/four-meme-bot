package com.fourmeme.bot.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "验证助记词并返回派生地址")
public class VerifyMnemonicRequest {

    @Schema(description = "助记词", required = true,
            example = "abandon ability able about above absent absorb abstract absurd abuse access accident")
    private String mnemonic;
}