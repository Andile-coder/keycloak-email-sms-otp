package com.fastkeycloak.otp.providers;

import com.fastkeycloak.otp.OTPDeliveryProvider;
import com.fastkeycloak.otp.enums.OTPChannel;
import org.jboss.logging.Logger;
import org.keycloak.models.UserModel;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class TwilioSMSProvider implements OTPDeliveryProvider {
    private static final Logger logger = Logger.getLogger(TwilioSMSProvider.class);

    @Override
    public void sendOTP(String code, UserModel user, Map<String, String> config) throws Exception {
        logger.infof("=== TwilioSMSProvider.sendOTP started for user: %s ===", user.getUsername());
        
        String accountSid = config.get("twilioAccountSid");
        String authToken = config.get("twilioAuthToken");
        String fromNumber = config.get("twilioFromNumber");
        String phoneNumber = user.getFirstAttribute("phoneNumber");
        
        logger.infof("Twilio config - AccountSID: %s, FromNumber: %s, User phone: %s", 
            accountSid != null ? "[PRESENT]" : "[MISSING]", fromNumber, phoneNumber);
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            logger.error("User has no phone number configured");
            throw new RuntimeException("User has no phone number configured");
        }
        
        String originalPhone = phoneNumber;
        phoneNumber = normalizePhoneNumber(phoneNumber, config.getOrDefault("countryCode", "+1"));
        logger.infof("Phone number normalized: %s -> %s", originalPhone, phoneNumber);

        String messageTemplate = config.getOrDefault("smsTemplate", "Your verification code is: %s");
        String message = String.format(messageTemplate, code);
        
        logger.infof("SMS message template: %s, Final message: %s", messageTemplate, message);
        
        String body = String.format("To=%s&From=%s&Body=%s", 
            URLEncoder.encode(phoneNumber, StandardCharsets.UTF_8),
            URLEncoder.encode(fromNumber, StandardCharsets.UTF_8),
            URLEncoder.encode(message, StandardCharsets.UTF_8));

        String auth = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8));
        String apiUrl = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json", accountSid);
        
        logger.infof("Twilio API URL: %s", apiUrl);
        logger.infof("Request body length: %d characters", body.length());
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Authorization", "Basic " + auth)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        logger.info("Sending HTTP request to Twilio API");
        
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            
            int statusCode = response.statusCode();
            logger.infof("Twilio API response - Status: %d, Body length: %d", statusCode, response.body().length());
            
            if (statusCode >= 200 && statusCode < 300) {
                logger.infof("SMS sent successfully to %s via Twilio", phoneNumber);
            } else {
                logger.errorf("Twilio API error - Status: %d, Response: %s", statusCode, response.body());
                throw new RuntimeException(String.format("Failed to send SMS (HTTP %d): %s", statusCode, response.body()));
            }
        } catch (Exception e) {
            logger.errorf(e, "Exception during Twilio API call: %s", e.getMessage());
            throw e;
        }
    }

    @Override
    public OTPChannel getChannel() {
        return OTPChannel.SMS;
    }

    @Override
    public boolean isConfigured(Map<String, String> config) {
        return config.containsKey("twilioAccountSid") && 
               config.containsKey("twilioAuthToken") && 
               config.containsKey("twilioFromNumber");
    }
    
    private String normalizePhoneNumber(String phoneNumber, String countryCode) {
        logger.infof("Normalizing phone number: %s with country code: %s", phoneNumber, countryCode);
        
        if (countryCode == null || countryCode.isEmpty()) {
            logger.warn("No country code provided, returning original phone number");
            return phoneNumber;
        }
        
        String cleanCountryCode = countryCode.startsWith("+") ? countryCode.substring(1) : countryCode;
        logger.infof("Clean country code: %s", cleanCountryCode);
        
        // Remove spaces, dashes, parentheses
        String originalPhone = phoneNumber;
        phoneNumber = phoneNumber.replaceAll("[\\s\\-\\(\\)]", "");
        logger.infof("After cleaning special chars: %s -> %s", originalPhone, phoneNumber);
        
        // Convert 0049 to +49
        if (phoneNumber.startsWith("00" + cleanCountryCode)) {
            phoneNumber = phoneNumber.replaceFirst("00" + cleanCountryCode, "+" + cleanCountryCode);
            logger.infof("Applied 00XX to +XX rule: %s", phoneNumber);
        }
        // Convert 49 to +49 (if doesn't start with +)
        else if (phoneNumber.startsWith(cleanCountryCode) && !phoneNumber.startsWith("+")) {
            phoneNumber = "+" + phoneNumber;
            logger.infof("Applied XX to +XX rule: %s", phoneNumber);
        }
        // Convert 0176 to +49176 (national format)
        else if (phoneNumber.startsWith("0") && !phoneNumber.startsWith("+")) {
            phoneNumber = "+" + cleanCountryCode + phoneNumber.substring(1);
            logger.infof("Applied national 0XXX to +CCXXX rule: %s", phoneNumber);
        }
        // Ensure + prefix
        else if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+" + phoneNumber;
            logger.infof("Added + prefix: %s", phoneNumber);
        }
        
        logger.infof("Final normalized phone number: %s", phoneNumber);
        return phoneNumber;
    }
}