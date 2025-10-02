package com.fastkeycloak.otp.enums;

public enum OTPChannel {
    EMAIL("email", "Email"),
    SMS("sms", "SMS");

    private final String value;
    private final String displayName;

    OTPChannel(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static OTPChannel fromValue(String value) {
        for (OTPChannel channel : values()) {
            if (channel.value.equals(value)) {
                return channel;
            }
        }
        return EMAIL;
    }
}