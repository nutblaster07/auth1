package com.abhishek.config;



import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * MailConfig — configures the JavaMailSender bean used to send emails.
 *
 * WHY A SEPARATE CONFIG CLASS?
 * Spring Boot auto-configures JavaMailSender from application.properties,
 * but we create it explicitly here so we can customise it and test it easily.
 *
 * HOW IT WORKS:
 * JavaMailSenderImpl connects to the SMTP server (e.g. smtp.gmail.com:587),
 * authenticates with the username/password, and sends emails.
 *
 * FOR GMAIL:
 * 1. Enable 2-Factor Authentication on your Google account
 * 2. Go to: Google Account → Security → App Passwords
 * 3. Generate a 16-character App Password
 * 4. Put that password in application.properties (spring.mail.password)
 */
@Configuration
public class MailConfig {

    // These @Value annotations read directly from application.properties
    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // SMTP server address
        mailSender.setHost(host);

        // Port 587 = STARTTLS (most common for Gmail)
        // Port 465 = SSL/TLS (older)
        mailSender.setPort(port);

        // Your Gmail account credentials
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        // Default encoding for email content
        mailSender.setDefaultEncoding("UTF-8");

        // Additional SMTP properties
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");          // Must authenticate
        props.put("mail.smtp.starttls.enable", "true"); // Encrypt with TLS
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.debug", "false"); // Set to "true" to debug email issues

        return mailSender;
    }
}