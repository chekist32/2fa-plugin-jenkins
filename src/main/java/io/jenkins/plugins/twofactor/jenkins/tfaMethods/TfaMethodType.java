package io.jenkins.plugins.twofactor.jenkins.tfaMethods;

public enum TfaMethodType {
    SECURITY_QUESTION("securityQuestionConfig", "securityQuestionAuth"),
    OTP_OVER_EMAIL("otpOverEmailConfig", "otpOverEmailAuth"),
    TOTP("totpConfig", "totpAuth");

    private final String configPath;
    private final String authPath;

    TfaMethodType(
            String configPath,
            String authPath
    ) {
        this.configPath = configPath;
        this.authPath = authPath;
    }

    public String getConfigPath() {
        return configPath;
    }

    public String getAuthPath() {
        return authPath;
    }
}
