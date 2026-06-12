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
    public void sendVerification(String email, String verificationUrl, String verificationCode, Instant expiresAt, String preferredLanguage) {
        if (!properties.getEmailVerification().isEnabled()) {
            return;
        }
        CustomerAuthProperties.Smtp smtp = properties.getEmailVerification().getSmtp();
        if (smtp.isEnabled()) {
            sendSmtp(email, verificationUrl, verificationCode, expiresAt, smtp, language(preferredLanguage));
            return;
        }
        // Local fallback keeps demos usable when no SMTP account is configured.
        log.info("Email verification required for {}. language={} code={} expiresAt={} verificationUrl={}",
                email, language(preferredLanguage), verificationCode, expiresAt, verificationUrl);
    }

    private void sendSmtp(
            String email,
            String verificationUrl,
            String verificationCode,
            Instant expiresAt,
            CustomerAuthProperties.Smtp smtp,
            String preferredLanguage
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
            EmailContent content = previewContent(preferredLanguage, verificationCode, verificationUrl, expiresAt);
            helper.setFrom(smtp.getFrom());
            helper.setTo(email);
            helper.setSubject(content.subject());
            helper.setText(content.body(), false);
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

    private String language(String preferredLanguage) {
        return switch (preferredLanguage == null ? "" : preferredLanguage.trim()) {
            case "zh-TW", "ms", "ko" -> preferredLanguage.trim();
            default -> "en";
        };
    }

    EmailContent previewContent(String preferredLanguage, String verificationCode, String verificationUrl, Instant expiresAt) {
        return content(language(preferredLanguage), verificationCode, verificationUrl, expiresAt);
    }

    private EmailContent content(String language, String verificationCode, String verificationUrl, Instant expiresAt) {
        CustomerAuthProperties.EmailTemplate configured = configuredTemplate(language);
        if (configured != null && !blank(configured.getSubject()) && !blank(configured.getBody())) {
            // Configured templates let production change customer copy without redeploying; only non-secret placeholders expand.
            return new EmailContent(
                    configured.getSubject(),
                    configured.getBody()
                            .replace("{code}", verificationCode)
                            .replace("{verificationUrl}", verificationUrl)
                            .replace("{expiresAt}", String.valueOf(expiresAt))
            );
        }
        // Message bodies intentionally stay plain text so every mailbox displays the primary code consistently.
        return switch (language) {
            case "zh-TW" -> new EmailContent("註冊驗證碼", """
                    你的交易平台註冊驗證碼是：

                    %s

                    你也可以使用以下備援連結完成註冊：

                    %s

                    此註冊申請將於 %s 到期。
                    如果這不是你本人操作，請忽略此郵件。
                    """.formatted(verificationCode, verificationUrl, expiresAt));
            case "ms" -> new EmailContent("Kod pengesahan pendaftaran", """
                    Kod pengesahan pendaftaran exchange anda ialah:

                    %s

                    Anda juga boleh melengkapkan pendaftaran melalui pautan sandaran ini:

                    %s

                    Permintaan pendaftaran ini tamat tempoh pada %s.
                    Jika anda tidak meminta pendaftaran ini, abaikan e-mel ini.
                    """.formatted(verificationCode, verificationUrl, expiresAt));
            case "ko" -> new EmailContent("가입 인증 코드", """
                    거래소 가입 인증 코드는 다음과 같습니다:

                    %s

                    아래 예비 링크로도 가입을 완료할 수 있습니다:

                    %s

                    이 가입 요청은 %s 에 만료됩니다.
                    본인이 요청하지 않았다면 이 이메일을 무시하세요.
                    """.formatted(verificationCode, verificationUrl, expiresAt));
            default -> new EmailContent("Registration verification code", """
                    Your exchange registration verification code is:

                    %s

                    You can also complete registration with this backup link:

                    %s

                    This registration request expires at %s.
                    If you did not request this registration, you can ignore this email.
                    """.formatted(verificationCode, verificationUrl, expiresAt));
        };
    }

    private Throwable rootCause(Exception ex) {
        Throwable current = ex;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }

    private CustomerAuthProperties.EmailTemplate configuredTemplate(String language) {
        CustomerAuthProperties.EmailTemplates templates = properties.getEmailVerification().getTemplates();
        if (templates == null) {
            return null;
        }
        return switch (language) {
            case "zh-TW" -> templates.getZhTw();
            case "ms" -> templates.getMs();
            case "ko" -> templates.getKo();
            default -> templates.getEn();
        };
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    record EmailContent(String subject, String body) {
    }
}
