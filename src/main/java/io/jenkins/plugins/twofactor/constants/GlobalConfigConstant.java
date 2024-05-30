package io.jenkins.plugins.twofactor.constants;

public class GlobalConfigConstant {
  private GlobalConfigConstant() {}

  public enum AdvancedSettingsConstants {
    DEFAULT_OTP_EMAIL_SUBJECT("Jenkins 2FA Verification Code"),
    DEFAULT_OTP_EMAIL_TEMPLATE("<html><body><h1>Jenkins Account Verification Code</h1><p>Dear $username, <br></p><p>Your two factor verification code is: $otp </p><p>Please use this passcode to complete your action.</p> <br><br>Thank you.</body></html>");

    private final String value;

    AdvancedSettingsConstants(String setting) {
      this.value = setting;
    }

    public String getValue() {
      return value;
    }

  }
}
