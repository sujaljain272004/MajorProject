package com.chargeup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "razorpay")
public record RazorpayProperties(
    Key key,
    String currency,
    Webhook webhook
) {
    public record Key(String id, String secret) {}
    public record Webhook(String secret) {}
}
