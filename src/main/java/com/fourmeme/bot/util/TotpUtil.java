package com.fourmeme.bot.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class TotpUtil {

    private static final String ISSUER = "FourMemeBot";

    private final GoogleAuthenticator gAuth = new GoogleAuthenticator();

    /** 生成新的 TOTP 密钥（Base32） */
    public String generateSecret() {
        GoogleAuthenticatorKey key = gAuth.createCredentials();
        return key.getKey();
    }

    /** 校验 TOTP 验证码 */
    public boolean verify(String secret, int code) {
        try {
            return gAuth.authorize(secret, code);
        } catch (Exception e) {
            log.warn("TOTP 校验异常: {}", e.getMessage());
            return false;
        }
    }

    /** 生成 otpauth:// URL（扫码用） */
    public String buildOtpAuthUrl(String username, String secret) {
        try {
            String label = URLEncoder.encode(ISSUER + ":" + username, StandardCharsets.UTF_8);
            String issuer = URLEncoder.encode(ISSUER, StandardCharsets.UTF_8);
            return String.format(
                    "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=6&period=30",
                    label, secret, issuer);
        } catch (Exception e) {
            throw new RuntimeException("生成 otpauth URL 失败", e);
        }
    }

    /** 生成二维码 PNG 的 Base64（不带 data: 前缀） */
    public String generateQrCodeBase64(String content, int size) {
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1);

            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("生成二维码失败", e);
        }
    }
}