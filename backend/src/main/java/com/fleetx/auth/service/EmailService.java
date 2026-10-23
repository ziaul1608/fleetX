package com.fleetx.auth.service;

public interface EmailService {
    void sendOtp(String toEmail, String otpCode, String purpose, String recipientName);
}
