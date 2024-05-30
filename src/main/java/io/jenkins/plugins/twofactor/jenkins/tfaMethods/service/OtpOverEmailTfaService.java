package io.jenkins.plugins.twofactor.jenkins.tfaMethods.service;

import hudson.model.User;
import hudson.tasks.Mailer;
import io.jenkins.plugins.twofactor.jenkins.EmailService;
import io.jenkins.plugins.twofactor.jenkins.dto.EmailDTO;
import io.jenkins.plugins.twofactor.jenkins.globalConfig.GlobalConfig;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static io.jenkins.plugins.twofactor.constants.GlobalConfigConstant.AdvancedSettingsConstants.DEFAULT_OTP_EMAIL_SUBJECT;
import static io.jenkins.plugins.twofactor.constants.GlobalConfigConstant.AdvancedSettingsConstants.DEFAULT_OTP_EMAIL_TEMPLATE;
import static org.apache.commons.lang.StringUtils.isBlank;

public class OtpOverEmailTfaService {
    private static OtpOverEmailTfaService instance;

    private static final Random RANDOM = new SecureRandom();
    private static final String NUMBERS = "0123456789";

    private static final Map<String, String> sentOtp = new HashMap<>();

    private final EmailService emailService;

    public OtpOverEmailTfaService() { this(EmailService.getInstance()); }

    public OtpOverEmailTfaService(EmailService emailService) {
        this.emailService = emailService;
    }


    public boolean isOtpSent(String userId) {
        return !sentOtp.getOrDefault(userId, "").isEmpty();
    }

    private String createOtp(int len) {
        char[] otp = new char[len];

        for (int i = 0; i < len; i++) {
            otp[i] = NUMBERS.charAt(RANDOM.nextInt(NUMBERS.length()));
        }

        return String.valueOf(otp);
    }

    private EmailDTO generateOtpEmail(String userId) {
        String subject = GlobalConfig.get().getAdvancedSettings().getCustomOTPEmailSubject();
        if(isBlank(subject)) subject = DEFAULT_OTP_EMAIL_SUBJECT.getValue();

        String template = GlobalConfig.get().getAdvancedSettings().getCustomOTPEmailTemplate();
        if(isBlank(template)) template = DEFAULT_OTP_EMAIL_TEMPLATE.getValue();

        if (template.contains("$username")) {
            template = template.replace("$username", userId);
        }
        if (template.contains("$otp")) {
            template = template.replace("$otp", sentOtp.get(userId));
        }

        String mailTo = "";
        var user = User.getById(userId, false);
        if (user != null) {
            var mailerProp = user.getProperty(Mailer.UserProperty.class);
            if (mailerProp != null && mailerProp.getEmailAddress() != null)
                mailTo = mailerProp.getEmailAddress();
        }

        return new EmailDTO(subject, template, mailTo);
    }

    public void sendOtp(String userId) {
        if (userId == null) return;
        sentOtp.put(userId, createOtp(5));

        emailService.sendMail(generateOtpEmail(userId));
    }

    public boolean validateOtp(String userId, String otp) {
        var expectedOtp = sentOtp.getOrDefault(userId, "");

        boolean result = !expectedOtp.isBlank() && expectedOtp.equals(otp);
        if (result) sentOtp.remove(userId);

        return result;
    }

    public synchronized static OtpOverEmailTfaService getInstance() {
        if (instance == null) instance = new OtpOverEmailTfaService();

        return instance;
    }
}
