package com.fourmeme.bot.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fourmeme.bot.service.FourMemeApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class FourMemeApiServiceImpl implements FourMemeApiService {

    private final RestTemplate restTemplate;

    @Value("${four-meme.api.base-url}")
    private String baseUrl;

    @Override
    public JsonNode getPublicConfig() {
        try {
            String url = baseUrl + "/public/config";
            return restTemplate.getForObject(url, JsonNode.class);
        } catch (Exception e) {
            log.warn("获取Four.meme公共配置失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public JsonNode getTokenByAddress(String tokenAddress, String accessToken) {
        try {
            String url = baseUrl + "/token/" + tokenAddress;
            HttpHeaders headers = new HttpHeaders();
            if (accessToken != null && !accessToken.isEmpty()) {
                headers.setBearerAuth(accessToken);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, JsonNode.class);
            return response.getBody();
        } catch (Exception e) {
            log.warn("获取代币信息失败 {}: {}", tokenAddress, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isGraduatedViaApi(String tokenAddress) {
        JsonNode tokenInfo = getTokenByAddress(tokenAddress, null);
        if (tokenInfo != null && tokenInfo.has("graduated")) {
            return tokenInfo.get("graduated").asBoolean();
        }
        return false;
    }

    @Override
    public BigDecimal getGraduationThresholdBnb() {
        JsonNode config = getPublicConfig();
        if (config != null && config.has("graduationThresholdBnb")) {
            return config.get("graduationThresholdBnb").decimalValue();
        }
        return BigDecimal.valueOf(18);
    }
}