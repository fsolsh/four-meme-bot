package com.fourmeme.bot.response;

import lombok.Data;

@Data
public class TotpSetupResponse {
    private String secret;
    private String qrCode;      // Base64 PNG
    private String otpAuthUrl;  // otpauth://
}