package com.fourmeme.bot.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserDTO {
    private Long id;
    private String username;
    private String role;
    private boolean totpBound;
    private LocalDateTime lastLoginAt;
}