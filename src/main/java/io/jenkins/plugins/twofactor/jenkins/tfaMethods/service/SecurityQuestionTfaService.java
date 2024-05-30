package io.jenkins.plugins.twofactor.jenkins.tfaMethods.service;

import hudson.model.User;
import io.jenkins.plugins.twofactor.jenkins.dto.SimpleQADTO;
import io.jenkins.plugins.twofactor.jenkins.tfaMethods.config.SecurityQuestionConfig;

import java.security.SecureRandom;
import java.util.Random;

public class SecurityQuestionTfaService {
    private static SecurityQuestionTfaService instance;

    private final Random RANDOM = new SecureRandom();

    public SimpleQADTO[] getSecurityQuestionsAndAnswers(String userId) {
        var user = User.getById(userId, false);
        if (user == null) return new SimpleQADTO[0];

        var config = user.getProperty(SecurityQuestionConfig.class);
        if (config == null) return new SimpleQADTO[0];

        int i1 = RANDOM.nextInt(3);
        int i2 = RANDOM.nextInt(3);
        while (i1 == i2) i2 = RANDOM.nextInt(3);

        SimpleQADTO[] userTfaQA = config.getUserTfaQA();

        SimpleQADTO[] returnedUserTfaQA = new SimpleQADTO[2];
        returnedUserTfaQA[0] = userTfaQA[i1];
        returnedUserTfaQA[1] = userTfaQA[i2];

        return returnedUserTfaQA;
    }

    public synchronized static SecurityQuestionTfaService getInstance() {
        if (instance == null) instance = new SecurityQuestionTfaService();

        return instance;
    }

}
