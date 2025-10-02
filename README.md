# Keycloak Multi-Channel OTP Authenticator

A Keycloak SPI (Service Provider Interface) that provides passwordless OTP authentication via **Email** and **SMS** with support for multiple providers.

## Features

- **Multi-Channel Support**: Email and SMS delivery
- **User Choice**: Users can select their preferred delivery method during login
- **Admin Control**: Force specific channels per realm
- **Multiple Providers**: 
  - Email: Keycloak's built-in EmailTemplateProvider
  - SMS: Twilio integration via HTTP API
- **Extensible**: Easy to add new providers
- **Minimal Dependencies**: Uses Java's built-in HTTP client

## Quick Start

### 1. Build the JAR

```bash
mvn clean package
```

### 2. Deploy to Keycloak

Copy the JAR to your Keycloak providers directory:

```bash
cp target/com.fastkeycloak-keycloak-email-sms-otp-1.0.0.jar /opt/keycloak/providers/
```

### 3. Restart Keycloak

```bash
# Docker
docker restart keycloak

# Standalone
./kc.sh start
```

### 4. Configure Authentication Flow

1. Go to **Authentication** → **Flows** in Keycloak Admin Console
2. Create or edit an authentication flow
3. Add **"Multi-Channel OTP (Email & SMS)"** step
4. Configure the authenticator settings

## Configuration Options

### Basic Settings
- **Simulation Mode**: Test without sending real OTP (logs to console)
- **Allow User Choice**: Let users choose between email/SMS
- **Forced Channel**: Force all users to use specific channel (email/sms)
- **Code Length**: Number of digits (default: 6)
- **Time-to-live**: Code validity in seconds (default: 300)
- **Max Retries**: Maximum retry attempts (default: 3)

### Email Settings
- **Email Subject**: Subject line for OTP emails

### SMS Settings (Twilio)
- **Twilio Account SID**: Your Twilio Account SID
- **Twilio Auth Token**: Your Twilio Auth Token  
- **Twilio From Number**: Sender phone number (e.g., +1234567890)

## User Attributes Required

Ensure users have the required attributes:
- **email**: For email delivery
- **phoneNumber**: For SMS delivery (format: +1234567890)

## Usage Scenarios

### Scenario 1: User Choice
- **Allow User Choice**: `true`
- **Forced Channel**: `(empty)`
- Users see channel selection screen, then enter OTP

### Scenario 2: Admin-Forced Email Only
- **Allow User Choice**: `false` 
- **Forced Channel**: `email`
- All users receive OTP via email

### Scenario 3: Admin-Forced SMS Only
- **Allow User Choice**: `false`
- **Forced Channel**: `sms`  
- All users receive OTP via SMS

## Project Structure

```
src/main/java/com/fastkeycloak/otp/
├── enums/
│   └── OTPChannel.java                    # Email/SMS channel enum
├── providers/
│   ├── EmailOTPProvider.java              # Email delivery implementation
│   └── TwilioSMSProvider.java            # Twilio SMS implementation
├── OTPDeliveryProvider.java              # Provider interface
├── MultiChannelOTPAuthenticator.java     # Main authenticator logic
└── MultiChannelOTPAuthenticatorFactory.java # Factory & configuration

src/main/resources/
├── META-INF/services/
│   └── org.keycloak.authentication.AuthenticatorFactory
└── theme-resources/
    ├── templates/
    │   ├── channel-selection.ftl          # Channel selection form
    │   ├── otp-form.ftl                  # OTP entry form
    │   └── otp-email.ftl                 # Email template
    └── messages/
        └── messages_en.properties         # Localized messages
```

## Adding New Providers

To add a new SMS provider (e.g., AWS SNS):

1. **Create Provider Class:**
   ```java
   public class AWSSNSProvider implements OTPDeliveryProvider {
       @Override
       public void sendOTP(String code, UserModel user, Map<String, String> config) {
           // AWS SNS implementation
       }
       
       @Override
       public OTPChannel getChannel() {
           return OTPChannel.SMS;
       }
       
       @Override
       public boolean isConfigured(Map<String, String> config) {
           return config.containsKey("awsAccessKey") && config.containsKey("awsSecretKey");
       }
   }
   ```

2. **Update Authenticator:**
   Modify `getProvider()` method in `MultiChannelOTPAuthenticator.java`

3. **Add Configuration:**
   Add new properties to `MultiChannelOTPAuthenticatorFactory.java`

## Development

### Requirements
- Java 17+
- Maven 3.6+
- Keycloak 26.3.5+

### Build Commands
```bash
# Compile only
mvn compile

# Run tests
mvn test

# Package JAR
mvn package

# Clean build
mvn clean package
```

### Testing
Enable simulation mode to test without sending real messages:
- Set **Simulation Mode** to `true` in authenticator configuration
- OTP codes will be logged to Keycloak console instead of being sent

## Troubleshooting

### Common Issues

1. **SMS not sending**
   - Check Twilio credentials are correct
   - Verify phone number format (+1234567890)
   - Check Twilio account balance

2. **User choice not showing**
   - Verify user has both `email` and `phoneNumber` attributes
   - Check Twilio configuration is complete

3. **Template errors**
   - Ensure templates are in correct directory structure
   - Check message properties are loaded

### Debug Logs

Enable debug logging in Keycloak:
```bash
# Add to keycloak.conf
log-level=DEBUG
log-console-format=%d{HH:mm:ss,SSS} %-5p [%c{1}] %s%e%n
```

## Security Considerations

- Use HTTPS for all communications
- Store Twilio credentials securely (environment variables recommended)
- Implement rate limiting at Keycloak level
- Validate phone number formats
- Monitor SMS costs and quotas
- Consider implementing IP-based restrictions

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## Support

For issues and questions:
1. Check the troubleshooting section
2. Review Keycloak logs
3. Open an issue on GitHub