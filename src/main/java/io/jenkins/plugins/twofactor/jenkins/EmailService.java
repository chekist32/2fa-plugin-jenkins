package io.jenkins.plugins.twofactor.jenkins;

import hudson.tasks.Mailer;
import io.jenkins.plugins.twofactor.jenkins.dto.EmailDTO;
import io.jenkins.plugins.twofactor.jenkins.globalConfig.GlobalConfig;
import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Date;
import java.util.logging.Logger;

public class EmailService {
    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    private static EmailService instance;

    private Address stringToAddress(String email) throws AddressException {
        return new InternetAddress(email);
    }

    public void sendMail(EmailDTO email) {
        var globalConfig = GlobalConfig.get();

        try {
            var mailerDescriptor = Mailer.descriptor();
            var msg = new MimeMessage(mailerDescriptor.createSession());

            msg.setSubject(email.getSubject());

            msg.setContent(email.getContent(), "text/html");
            msg.setFrom(globalConfig.getSenderEmailAddress());

            msg.setSentDate(new Date());
            msg.setRecipient(Message.RecipientType.TO, stringToAddress(email.getMailTo()));

            Transport.send(msg);
        }
        catch (Exception e) {
            LOGGER.fine("Failed in sending mail, error is " + e.getMessage());
        }
    }

    public synchronized static EmailService getInstance() {
        if (instance == null) return new EmailService();

        return instance;
    }
}
