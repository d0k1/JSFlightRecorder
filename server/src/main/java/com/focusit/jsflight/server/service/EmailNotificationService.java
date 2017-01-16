package com.focusit.jsflight.server.service;

import com.focusit.jsflight.server.model.Settings;
import com.focusit.jsflight.server.scenario.MongoDbScenario;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Properties;

/**
 * Email sender.
 * Notifies about any possible problem by email
 * Created by doki on 14.05.16.
 */
@Service
public class EmailNotificationService
{
    private static final Logger LOG = LoggerFactory.getLogger(EmailNotificationService.class);
    private SettingsService settingsService;
    private JavaMailSender sender;

    @Inject
    public EmailNotificationService(SettingsService settingsService)
    {
        this.settingsService = settingsService;
    }

    @PostConstruct
    public void init()
    {
        Settings settings = settingsService.getSettings();
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();

        if (!settings.getSmtpServer().isEmpty())
        {
            javaMailSender.setHost(settings.getSmtpServer());
        }
        if (!settings.getSmtpPort().isEmpty())
        {
            javaMailSender.setPort(Integer.parseInt(settings.getSmtpPort()));
        }

        javaMailSender.setUsername(settings.getStmpUser());
        javaMailSender.setPassword(settings.getStmpPassword());

        javaMailSender.setJavaMailProperties(getMailProperties(settings));

        sender = javaMailSender;
    }

    private Properties getMailProperties(Settings settings)
    {
        Properties properties = new Properties();
        properties.setProperty("mail.transport.protocol", "smtp");
        properties.setProperty("mail.smtp.auth", settings.getSmtpAuth());
        properties.setProperty("mail.smtp.starttls.enable", settings.getSmtpStarttls());
        properties.setProperty("mail.debug", settings.getSmtpMailDebug());
        return properties;
    }

    private SimpleMailMessage getMessageTemplate(MongoDbScenario scenario, Throwable ex)
    {
        Settings settings = settingsService.getSettings();

        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setFrom(settings.getSmtpFrom());
        mailMessage.setTo(settings.getAlarmEmails().split(","));
        String body = "";
        if (ex != null)
        {
            body += ex.toString() + "\n\n\n";
        }
        body += new JSONObject((scenario)).toString(4);
        mailMessage.setText(body);
        return mailMessage;
    }

    private void sendMail(SimpleMailMessage mailMessage)
    {
        try
        {
            sender.send(mailMessage);
        }
        catch (Exception e)
        {
            LOG.error(e.toString(), e);
            throw e;
        }
    }

    public void notifySubscribers(MongoDbScenario scenario, Throwable ex, String message)
    {
        SimpleMailMessage mailMessage = getMessageTemplate(scenario, ex);
        mailMessage.setSubject("[" + scenario.getScenarioFilename() + "] Scenario " + message);
        sendMail(mailMessage);
    }

    public void notifySubscribers(MongoDbScenario scenario, Throwable ex, EventType eventType)
    {
        notifySubscribers(scenario, ex, eventType.toString());
    }

    public enum EventType
    {
        PAUSED("paused"), TERMINATED("terminated"), DONE("done"), ERROR_IN_BROWSER("error in browser"), UNKNOWN_ERROR(
                "unknown exception");

        private String text;

        EventType(String text)
        {
            this.text = text;
        }

        @Override
        public String toString()
        {
            return text;
        }
    }
}
