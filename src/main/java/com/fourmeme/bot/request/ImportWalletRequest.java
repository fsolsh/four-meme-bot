package com.fourmeme.bot.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "通过助记词导入钱包")
public class ImportWalletRequest {

    @Schema(description = "12或24个单词的助记词", required = true,
            example = "abandon ability able about above absent absorb abstract absurd abuse access accident")
    private String mnemonic;
}