package com.fastkeycloak.otp;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.Arrays;
import java.util.List;

public class MultiChannelOTPAuthenticatorFactory implements AuthenticatorFactory {

    @Override
    public String getId() {
        return "multi-channel-otp";
    }

    @Override
    public String getDisplayType() {
        return "Multi-Channel OTP (Email & SMS)";
    }

    @Override
    public String getHelpText() {
        return "Sends OTP via email or SMS with support for multiple providers";
    }

    @Override
    public String getReferenceCategory() {
        return "otp";
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
        return new AuthenticationExecutionModel.Requirement[] {
            AuthenticationExecutionModel.Requirement.REQUIRED,
            AuthenticationExecutionModel.Requirement.ALTERNATIVE,
            AuthenticationExecutionModel.Requirement.DISABLED,
        };
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Arrays.asList(
            new ProviderConfigProperty("simulation", "Simulation Mode",
                "Test mode - logs OTP instead of sending", ProviderConfigProperty.BOOLEAN_TYPE, false),
            new ProviderConfigProperty("allowUserChoice", "Allow User Choice",
                "Let users choose between email and SMS", ProviderConfigProperty.BOOLEAN_TYPE, true),
            new ProviderConfigProperty("forcedChannel", "Forced Channel",
                "Force specific channel (email/sms)", ProviderConfigProperty.LIST_TYPE, null, "", "email", "sms"),
            new ProviderConfigProperty("length", "Code Length",
                "Number of digits in OTP code", ProviderConfigProperty.STRING_TYPE, "6"),
            new ProviderConfigProperty("ttl", "Time-to-live (seconds)",
                "Code validity duration", ProviderConfigProperty.STRING_TYPE, "300"),
            new ProviderConfigProperty("maxRetries", "Max Retries",
                "Maximum retry attempts", ProviderConfigProperty.STRING_TYPE, "3"),
            new ProviderConfigProperty("allowNumbers", "Allow Numbers",
                "Include digits in OTP", ProviderConfigProperty.BOOLEAN_TYPE, true),
            new ProviderConfigProperty("emailSubject", "Email Subject",
                "Subject for OTP emails", ProviderConfigProperty.STRING_TYPE, "Your Authentication Code"),
            new ProviderConfigProperty("twilioAccountSid", "Twilio Account SID",
                "Twilio Account SID for SMS", ProviderConfigProperty.STRING_TYPE, null),
            new ProviderConfigProperty("twilioAuthToken", "Twilio Auth Token",
                "Twilio Auth Token for SMS", ProviderConfigProperty.PASSWORD, null),
            new ProviderConfigProperty("twilioFromNumber", "Twilio From Number",
                "SMS sender number (e.g., +1234567890)", ProviderConfigProperty.STRING_TYPE, null),
            new ProviderConfigProperty("countryCode", "Default Country Code",
                "Default country code for phone number normalization (e.g., +1)", ProviderConfigProperty.STRING_TYPE, "+1"),
            new ProviderConfigProperty("smsTemplate", "SMS Message Template",
                "SMS message template with %s placeholder for code", ProviderConfigProperty.STRING_TYPE, "Your verification code is: %s")
        );
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return new MultiChannelOTPAuthenticator();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }
}