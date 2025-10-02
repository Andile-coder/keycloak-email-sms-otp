package com.fastkeycloak.otp.providers;

import com.fastkeycloak.otp.OTPDeliveryProvider;
import com.fastkeycloak.otp.enums.OTPChannel;
import org.jboss.logging.Logger;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.HashMap;
import java.util.Map;

public class EmailOTPProvider implements OTPDeliveryProvider {
    private static final Logger logger = Logger.getLogger(EmailOTPProvider.class);
    
    private final KeycloakSession session;
    private final RealmModel realm;
    private final AuthenticationSessionModel authSession;

    public EmailOTPProvider(KeycloakSession session, RealmModel realm, AuthenticationSessionModel authSession) {
        this.session = session;
        this.realm = realm;
        this.authSession = authSession;
    }

    @Override
    public void sendOTP(String code, UserModel user, Map<String, String> config) throws Exception {
        logger.infof("=== EmailOTPProvider.sendOTP started for user: %s ===", user.getUsername());
        
        String emailSubject = config.getOrDefault("emailSubject", "Your Authentication Code");
        int ttl = Integer.parseInt(config.getOrDefault("ttl", "300"));
        
        logger.infof("Email config - Subject: %s, TTL: %d seconds, User email: %s", 
            emailSubject, ttl, user.getEmail());
        
        String realmName = realm.getDisplayName() != null ? realm.getDisplayName() : realm.getName();
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("code", code);
        attributes.put("ttl", Math.floorDiv(ttl, 60));
        attributes.put("realmName", realmName);
        
        logger.infof("Email template attributes - Realm: %s, TTL minutes: %d", realmName, Math.floorDiv(ttl, 60));
        
        try {
            session.getProvider(EmailTemplateProvider.class)
                .setAuthenticationSession(authSession)
                .setRealm(realm)
                .setUser(user)
                .send(emailSubject, "otp-email.ftl", attributes);
            
            logger.infof("Email sent successfully to %s", user.getEmail());
        } catch (Exception e) {
            logger.errorf(e, "Failed to send email to %s: %s", user.getEmail(), e.getMessage());
            throw e;
        }
    }

    @Override
    public OTPChannel getChannel() {
        return OTPChannel.EMAIL;
    }

    @Override
    public boolean isConfigured(Map<String, String> config) {
        return true; // Email is always available via Keycloak
    }
}