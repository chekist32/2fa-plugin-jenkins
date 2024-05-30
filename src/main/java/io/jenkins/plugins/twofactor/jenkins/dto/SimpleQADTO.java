package io.jenkins.plugins.twofactor.jenkins.dto;

import hudson.util.Secret;
import io.jenkins.plugins.twofactor.constants.SecurityQuestions;

import java.io.Serializable;

public class SimpleQADTO implements Serializable {
    private final SecurityQuestions question;
    private final Secret answer;

    public SimpleQADTO(
            SecurityQuestions question,
            Secret answer
    ) {
        this.question = question;
        this.answer = answer;
    }

    public SecurityQuestions getQuestion() {
        return question;
    }

    public Secret getAnswer() {
        return answer;
    }
}
