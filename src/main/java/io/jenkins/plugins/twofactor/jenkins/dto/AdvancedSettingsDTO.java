package io.jenkins.plugins.twofactor.jenkins.dto;

import io.jenkins.plugins.twofactor.constants.GlobalConfigConstant;

public class AdvancedSettingsDTO {
    private final String customOTPEmailSubject;
    private final String customOTPEmailTemplate;

    public AdvancedSettingsDTO(String customOTPEmailSubject, String customOTPEmailTemplate) {
        this.customOTPEmailSubject = customOTPEmailSubject;
        this.customOTPEmailTemplate = customOTPEmailTemplate;
    }

    public String getCustomOTPEmailSubject() {
      return customOTPEmailSubject;
    }

    public String getCustomOTPEmailTemplate() {
      return customOTPEmailTemplate;
    }


    public static AdvancedSettingsDTO getDefault() {
        return new AdvancedSettingsDTO(
                GlobalConfigConstant.AdvancedSettingsConstants.DEFAULT_OTP_EMAIL_SUBJECT.getValue(),
                GlobalConfigConstant.AdvancedSettingsConstants.DEFAULT_OTP_EMAIL_TEMPLATE.getValue());
    }
}
