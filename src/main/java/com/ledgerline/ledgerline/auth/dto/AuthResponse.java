package com.ledgerline.ledgerline.auth.dto;

public record AuthResponse(

        String accessToken,

        String tokenType,

        long expiresIn

) {}