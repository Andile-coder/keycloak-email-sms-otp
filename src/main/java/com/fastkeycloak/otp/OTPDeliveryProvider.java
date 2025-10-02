package com.fastkeycloak.otp;

import com.fastkeycloak.otp.enums.OTPChannel;
import org.keycloak.models.UserModel;
import java.util.Map;

public interface OTPDeliveryProvider {
    void sendOTP(String code, UserModel user, Map<String, String> config) throws Exception;
    OTPChannel getChannel();
    boolean isConfigured(Map<String, String> config);
}