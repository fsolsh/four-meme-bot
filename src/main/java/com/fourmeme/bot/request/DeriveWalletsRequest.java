package com.fourmeme.bot.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "通过助记词批量派生钱包")
public class DeriveWalletsRequest {

    @Schema(description = "助记词", required = true,
            example = "abandon ability able about above absent absorb abstract absurd abuse access accident")
    private String mnemonic;

    @Schema(description = "派生数量（1-100）", required = true, example = "10", minimum = "1", maximum = "100")
    private Integer count = 1;
}