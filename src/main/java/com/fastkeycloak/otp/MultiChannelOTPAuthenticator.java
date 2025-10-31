package com.fastkeycloak.otp;

import com.fastkeycloak.otp.enums.OTPChannel;
import com.fastkeycloak.otp.providers.EmailOTPProvider;
import com.fastkeycloak.otp.providers.TwilioSMSProvider;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Map;

public class MultiChannelOTPAuthenticator implements Authenticator {

    private static final String CHANNEL_SELECTION_FORM = "channel-selection.ftl";
    private static final String OTP_FORM = "otp-form.ftl";
    private static final String AUTH_NOTE_CODE = "code";
    private static final String AUTH_NOTE_TTL = "ttl";
    private static final String AUTH_NOTE_CHANNEL = "channel";
    private static final String AUTH_NOTE_REMAINING_RETRIES = "remainingRetries";
    private static final Logger logger = Logger.getLogger(MultiChannelOTPAuthenticator.class);

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        logger.info("=== Multi-Channel OTP Authentication Started ===");
        
        AuthenticatorConfigModel config = context.getAuthenticatorConfig();
        UserModel user = context.getUser();
        AuthenticationSessionModel authSession = context.getAuthenticationSession();
        
        logger.infof("User: %s, Realm: %s", user.getUsername(), context.getRealm().getName());
        logger.infof("Config available: %s", config != null);

        String selectedChannel = authSession.getAuthNote(AUTH_NOTE_CHANNEL);
        logger.infof("Selected channel from session: %s", selectedChannel);
        
        if (selectedChannel == null) {
            boolean allowUserChoice = Boolean.parseBoolean(config.getConfig().getOrDefault("allowUserChoice", "true"));
            String forcedChannel = config.getConfig().get("forcedChannel");
            
            logger.infof("Allow user choice: %s, Forced channel: %s", allowUserChoice, forcedChannel);
            
            boolean hasEmail = user.getEmail() != null && !user.getEmail().trim().isEmpty();
            boolean hasSMS = user.getFirstAttribute("phoneNumber") != null && new TwilioSMSProvider().isConfigured(config.getConfig());
            boolean hasMultiple = hasEmail && hasSMS;
            
            logger.infof("Channel availability - Email: %s, SMS: %s, Multiple: %s", hasEmail, hasSMS, hasMultiple);
            
            if (allowUserChoice && forcedChannel == null && hasMultiple) {
                logger.info("Showing channel selection form to user");
                context.challenge(context.form().createForm(CHANNEL_SELECTION_FORM));
                return;
            } else {
                if (forcedChannel != null) {
                    selectedChannel = forcedChannel;
                } else if (hasEmail) {
                    selectedChannel = "email";
                } else if (hasSMS) {
                    selectedChannel = "sms";
                } else {
                    logger.error("User has no available delivery channels configured");
                    context.failureChallenge(AuthenticationFlowError.INVALID_USER,
                        context.form().setError("noDeliveryChannels", "Please contact your administrator to configure email or phone number for OTP delivery.")
                            .createErrorPage(Response.Status.BAD_REQUEST));
                    return;
                }
                logger.infof("Auto-selected channel: %s", selectedChannel);
                authSession.setAuthNote(AUTH_NOTE_CHANNEL, selectedChannel);
            }
        }

        String code = generateCode(config);
        int ttl = Integer.parseInt(config.getConfig().getOrDefault("ttl", "300"));
        int maxRetries = Integer.parseInt(config.getConfig().getOrDefault("maxRetries", "3"));
        
        logger.infof("Generated OTP code length: %d, TTL: %d seconds, Max retries: %d", code.length(), ttl, maxRetries);

        authSession.setAuthNote(AUTH_NOTE_CODE, code);
        authSession.setAuthNote(AUTH_NOTE_TTL, Long.toString(System.currentTimeMillis() + (ttl * 1000L)));
        authSession.setAuthNote(AUTH_NOTE_REMAINING_RETRIES, Integer.toString(maxRetries));
        
        logger.info("Stored OTP data in authentication session");

        try {
            OTPDeliveryProvider provider = getProvider(selectedChannel, context);
            logger.infof("Got provider for channel %s: %s", selectedChannel, provider.getClass().getSimpleName());
            
            boolean isSimulation = Boolean.parseBoolean(config.getConfig().getOrDefault("simulation", "false"));
            logger.infof("Simulation mode: %s", isSimulation);
            
            if (isSimulation) {
                logger.warn(String.format("***** SIMULATION MODE ***** Would send OTP via %s to user %s with code: %s", 
                    selectedChannel, user.getUsername(), code));
            } else {
                logger.infof("Sending OTP via %s provider", selectedChannel);
                provider.sendOTP(code, user, config.getConfig());
                logger.info("OTP sent successfully");
            }

            logger.info("Showing OTP entry form to user");
            context.challenge(context.form()
                .setAttribute("channel", selectedChannel)
                .createForm(OTP_FORM));
        } catch (Exception e) {
            logger.errorf(e, "Failed to send OTP via %s: %s", selectedChannel, e.getMessage());
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                context.form().setError("otpSendFailed", e.getMessage())
                    .createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
        }
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        logger.info("=== Multi-Channel OTP Action Started ===");
        
        String enteredCode = context.getHttpRequest().getDecodedFormParameters().getFirst("code");
        String selectedChannel = context.getHttpRequest().getDecodedFormParameters().getFirst("channel");
        
        logger.infof("Entered code: %s, Selected channel: %s", enteredCode != null ? "[PRESENT]" : "[MISSING]", selectedChannel);

        AuthenticationSessionModel authSession = context.getAuthenticationSession();

        if (selectedChannel != null && authSession.getAuthNote(AUTH_NOTE_CHANNEL) == null) {
            logger.infof("Channel selection received: %s, storing and restarting authentication", selectedChannel);
            authSession.setAuthNote(AUTH_NOTE_CHANNEL, selectedChannel);
            authenticate(context);
            return;
        }

        String code = authSession.getAuthNote(AUTH_NOTE_CODE);
        String ttl = authSession.getAuthNote(AUTH_NOTE_TTL);
        String remainingAttemptsStr = authSession.getAuthNote(AUTH_NOTE_REMAINING_RETRIES);
        int remainingAttempts = remainingAttemptsStr == null ? 0 : Integer.parseInt(remainingAttemptsStr);
        
        logger.infof("Stored code: %s, TTL: %s, Remaining attempts: %d", 
            code != null ? "[PRESENT]" : "[MISSING]", ttl, remainingAttempts);

        if (code == null || ttl == null) {
            logger.error("Missing OTP data in session - code or TTL is null");
            context.failureChallenge(AuthenticationFlowError.INTERNAL_ERROR,
                context.form().createErrorPage(Response.Status.INTERNAL_SERVER_ERROR));
            return;
        }

        boolean isValid = enteredCode.equals(code);
        logger.infof("Code validation result: %s", isValid ? "VALID" : "INVALID");
        
        if (isValid) {
            long currentTime = System.currentTimeMillis();
            long expiryTime = Long.parseLong(ttl);
            boolean isExpired = expiryTime < currentTime;
            
            logger.infof("Code expiry check - Current: %d, Expiry: %d, Expired: %s", currentTime, expiryTime, isExpired);
            
            if (isExpired) {
                logger.warn("Valid code but expired");
                context.failureChallenge(AuthenticationFlowError.EXPIRED_CODE,
                    context.form().setError("otpCodeExpired").createErrorPage(Response.Status.BAD_REQUEST));
            } else {
                logger.info("Code validation successful - authentication complete");
                context.success();
            }
        } else {
            logger.warnf("Invalid code entered. Remaining attempts: %d", remainingAttempts);
            if (remainingAttempts > 0) {
                authSession.setAuthNote(AUTH_NOTE_REMAINING_RETRIES, Integer.toString(remainingAttempts - 1));
                logger.infof("Allowing retry. Attempts left: %d", remainingAttempts - 1);
                context.failureChallenge(AuthenticationFlowError.INVALID_CREDENTIALS,
                    context.form()
                        .setAttribute("channel", authSession.getAuthNote(AUTH_NOTE_CHANNEL))
                        .setError("otpCodeInvalid", Integer.toString(remainingAttempts))
                        .createForm(OTP_FORM));
            } else {
                logger.warn("No more attempts remaining - authentication failed");
                context.failure(AuthenticationFlowError.INVALID_CREDENTIALS);
            }
        }
    }

    @Override
    public boolean requiresUser() {
        return true;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return user.getEmail() != null || user.getFirstAttribute("phoneNumber") != null;
    }

    @Override
    public void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {
    }

    @Override
    public void close() {
    }



    private OTPDeliveryProvider getProvider(String channel, AuthenticationFlowContext context) {
        OTPChannel otpChannel = OTPChannel.fromValue(channel);
        logger.infof("Getting provider for channel: %s -> %s", channel, otpChannel);
        
        switch (otpChannel) {
            case SMS:
                logger.info("Creating TwilioSMSProvider");
                return new TwilioSMSProvider();
            case EMAIL:
            default:
                logger.info("Creating EmailOTPProvider");
                return new EmailOTPProvider(context.getSession(), context.getRealm(), context.getAuthenticationSession());
        }
    }

    private String generateCode(AuthenticatorConfigModel config) {
        int length = Integer.parseInt(config.getConfig().getOrDefault("length", "6"));
        boolean allowNumbers = Boolean.parseBoolean(config.getConfig().getOrDefault("allowNumbers", "true"));

        String chars = allowNumbers ? "0123456789" : "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String code = SecretGenerator.getInstance().randomString(length, chars.toCharArray());
        
        logger.infof("Generated code with length %d, using chars: %s", length, allowNumbers ? "numbers" : "letters");
        return code;
    }
}