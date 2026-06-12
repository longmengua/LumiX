/*
 * File purpose: Dispatch email-verification links without exposing tokens in normal API responses.
 */
package com.example.exchange.application.service;

import com.example.exchange.infra.config.CustomerAuthProperties;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationNotifier {

    private final CustomerAuthProperties properties;

    /** Sends or logs the verification URL; SMTP is configured under customer-auth.email-verification.smtp.*. */
    public void sendVerification(String email, String verificationUrl, String verificationCode, Instant expiresAt) {
        if (!properties.getEmailVerification().isEnabled()) {
            return;
        }
        CustomerAuthProperties.Smtp smtp = properties.getEmailVerification().getSmtp();
        if (smtp.isEnabled()) {
            sendSmtp(email, verificationUrl, verificationCode, expiresAt, smtp);
            return;
        }
        // Local fallback keeps demos usable when no SMTP account is configured.
        log.info("Email verification required for {}. code={} expiresAt={} verificationUrl={}",
                email, verificationCode, expiresAt, verificationUrl);
    }

    private void sendSmtp(
            String email,
            String verificationUrl,
            String verificationCode,
            Instant expiresAt,
            CustomerAuthProperties.Smtp smtp
    ) {
        if (smtp.getHost() == null || smtp.getHost().isBlank()) {
            throw new IllegalStateException("email verification smtp host is not configured");
        }
        if (smtp.getFrom() == null || smtp.getFrom().isBlank()) {
            throw new IllegalStateException("email verification smtp from is not configured");
        }
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(smtp.getHost());
        sender.setPort(smtp.getPort());
        sender.setUsername(blankToNull(smtp.getUsername()));
        sender.setPassword(blankToNull(smtp.getPassword()));
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        sender.setJavaMailProperties(mailProperties(smtp));
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
            helper.setFrom(smtp.getFrom());
            helper.setTo(email);
            helper.setSubject(smtp.getSubject());
            helper.setText("""
                    Your exchange registration verification code is:

                    %s

                    You can also complete registration with this link:

                    %s

                    This registration request expires at %s.
                    If you did not request this registration, you can ignore this email.
                    """.formatted(verificationCode, verificationUrl, expiresAt), false);
            sender.send(message);
        } catch (Exception ex) {
            Throwable rootCause = rootCause(ex);
            // SMTP failures must be diagnosable without leaking passwords, raw codes, or backup verification tokens.
            log.warn(
                    "Email verification SMTP send failed host={} port={} auth={} startTls={} ssl={} from={} to={} rootCause={}: {}",
                    smtp.getHost(),
                    smtp.getPort(),
                    smtp.isAuth(),
                    smtp.isStartTls(),
                    smtp.isSsl(),
                    smtp.getFrom(),
                    email,
                    rootCause.getClass().getName(),
                    rootCause.getMessage());
            throw new IllegalStateException("email verification smtp send failed", ex);
        }
    }

    private Properties mailProperties(CustomerAuthProperties.Smtp smtp) {
        Properties props = new Properties();
        // Timeouts keep registration from hanging indefinitely when the SMTP provider is unreachable.
        props.put("mail.smtp.auth", String.valueOf(smtp.isAuth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(smtp.isStartTls()));
        props.put("mail.smtp.ssl.enable", String.valueOf(smtp.isSsl()));
        props.put("mail.smtp.connectiontimeout", String.valueOf(smtp.getTimeoutMs()));
        props.put("mail.smtp.timeout", String.valueOf(smtp.getTimeoutMs()));
        props.put("mail.smtp.writetimeout", String.valueOf(smtp.getTimeoutMs()));
        return props;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private Throwable rootCause(Exception ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
