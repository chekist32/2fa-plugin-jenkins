package io.jenkins.plugins.twofactor.jenkins.tfaMethods.service;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import hudson.model.User;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.config.TotpConfig;

public class TotpTfaService {
    private static final HashingAlgorithm HASHING_ALGORITHM = HashingAlgorithm.SHA256;
    private static final int DIGITS = 6;
    private static final int PERIOD = 30;

    private final DefaultCodeVerifier codeVerifier;
    private final SecretGenerator secretGenerator;

    private static TotpTfaService instance;

    private TotpTfaService(SecretGenerator secretGenerator) {
        this.secretGenerator = secretGenerator;
        this.codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(HASHING_ALGORITHM, DIGITS), new SystemTimeProvider());
        this.codeVerifier.setTimePeriod(PERIOD);
    }

    private String generateSecretKey() {
        return secretGenerator.generate();
    }

    public boolean verifyTotpCode(String userId, String code) {
        var user = User.getById(userId, false);
        if (user == null) return false;

        var totpConfig = user.getProperty(TotpConfig.class);
        if (totpConfig == null) return false;

        return codeVerifier.isValidCode(totpConfig.getTotpSecret().getPlainText(), code);
    }

    public QrData generateSecretKeyQr() {
        var user = User.current();
        String label = user != null ? user.getId() : "";

        return new QrData.Builder()
                .label(label)
                .issuer("2fa-jenkins-plugin")
                .secret(generateSecretKey())
                .algorithm(HASHING_ALGORITHM)
                .digits(DIGITS)
                .period(PERIOD)
                .build();
    }

    public synchronized static TotpTfaService getInstance() {
        if (instance == null) instance = new TotpTfaService(new DefaultSecretGenerator());

        return instance;
    }

}
