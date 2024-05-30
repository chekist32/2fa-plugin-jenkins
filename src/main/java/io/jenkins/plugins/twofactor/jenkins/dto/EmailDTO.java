package io.jenkins.plugins.twofactor.jenkins.dto;

public class EmailDTO {
    private final String subject;
    private final String content;
    private final String mailTo;

    public EmailDTO(String subject, String content, String mailTo) {
        this.subject = subject;
        this.content = content;
        this.mailTo = mailTo;
    }

    public String getSubject() {
        return subject;
    }

    public String getContent() {
        return content;
    }

    public String getMailTo() {
        return mailTo;
    }
}
