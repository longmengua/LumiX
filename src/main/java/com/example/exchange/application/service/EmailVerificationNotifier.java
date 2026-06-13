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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationNotifier {

    private final CustomerAuthProperties properties;
    private static final DateTimeFormatter EMAIL_EXPIRY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    /** Sends or logs the verification URL; SMTP is configured under customer-auth.email-verification.smtp.*. */
    public void sendVerification(
            String email,
            String verificationUrl,
            String verificationCode,
            Instant expiresAt,
            String preferredLanguage,
            String timeZone
    ) {
        if (!properties.getEmailVerification().isEnabled()) {
            return;
        }
        CustomerAuthProperties.Smtp smtp = properties.getEmailVerification().getSmtp();
        if (smtp.isEnabled()) {
            sendSmtp(email, verificationUrl, verificationCode, expiresAt, smtp, language(preferredLanguage), timeZone);
            return;
        }
        // Local fallback keeps demos usable when no SMTP account is configured.
        ZoneId zoneId = normalizeTimeZone(timeZone);
        log.info("Email verification required for {}. language={} timeZone={} code={} expiresAt={} verificationUrl={}",
                email, language(preferredLanguage), zoneId, verificationCode, formatExpiry(expiresAt, zoneId), verificationUrl);
    }

    private void sendSmtp(
            String email,
            String verificationUrl,
            String verificationCode,
            Instant expiresAt,
            CustomerAuthProperties.Smtp smtp,
            String preferredLanguage,
            String timeZone
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
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            EmailContent content = previewContent(preferredLanguage, verificationCode, verificationUrl, expiresAt, timeZone);
            // Sender display name makes mailbox lists show the exchange brand instead of a bare Gmail address.
            if (smtp.getDisplayName() == null || smtp.getDisplayName().isBlank()) {
                helper.setFrom(smtp.getFrom());
            } else {
                helper.setFrom(smtp.getFrom(), smtp.getDisplayName().trim());
            }
            helper.setTo(email);
            helper.setSubject(content.subject());
            helper.setText(content.plainBody(), content.htmlBody());
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

    EmailContent previewContent(
            String preferredLanguage,
            String verificationCode,
            String verificationUrl,
            Instant expiresAt,
            String timeZone
    ) {
        return content(language(preferredLanguage), verificationCode, verificationUrl, expiresAt, normalizeTimeZone(timeZone));
    }

    private EmailContent content(String language, String verificationCode, String verificationUrl, Instant expiresAt, ZoneId timeZone) {
        CustomerAuthProperties.EmailTemplate configured = configuredTemplate(language);
        String localizedExpiresAt = formatExpiry(expiresAt, timeZone);
        if (configured != null && !blank(configured.getSubject()) && !blank(configured.getBody())) {
            // Configured templates let production change customer copy without redeploying; only non-secret placeholders expand.
            String body = configured.getBody()
                    .replace("{code}", verificationCode)
                    .replace("{verificationUrl}", verificationUrl)
                    .replace("{expiresAt}", localizedExpiresAt);
            return new EmailContent(
                    configured.getSubject(),
                    body,
                    textToHtml(body)
            );
        }
        return switch (language) {
            case "zh-TW" -> emailContent("註冊驗證碼", """
                    你的交易平台註冊驗證碼是：

                    %s

                    你也可以使用以下備援連結完成註冊：

                    %s

                    此註冊申請將於 %s 到期。
                    如果這不是你本人操作，請忽略此郵件。
                    """.formatted(verificationCode, verificationUrl, localizedExpiresAt),
                    "你的交易平台註冊驗證碼", "驗證碼", verificationCode, "備援連結", verificationUrl, "到期時間", localizedExpiresAt,
                    "如果這不是你本人操作，請忽略此郵件。");
            case "ms" -> emailContent("Kod pengesahan pendaftaran", """
                    Kod pengesahan pendaftaran exchange anda ialah:

                    %s

                    Anda juga boleh melengkapkan pendaftaran melalui pautan sandaran ini:

                    %s

                    Permintaan pendaftaran ini tamat tempoh pada %s.
                    Jika anda tidak meminta pendaftaran ini, abaikan e-mel ini.
                    """.formatted(verificationCode, verificationUrl, localizedExpiresAt),
                    "Kod pengesahan pendaftaran exchange anda", "Kod Pengesahan", verificationCode, "Pautan sandaran", verificationUrl,
                    "Tamat tempoh", localizedExpiresAt, "Jika anda tidak meminta pendaftaran ini, abaikan e-mel ini.");
            case "ko" -> emailContent("가입 인증 코드", """
                    거래소 가입 인증 코드는 다음과 같습니다:

                    %s

                    아래 예비 링크로도 가입을 완료할 수 있습니다:

                    %s

                    이 가입 요청은 %s 에 만료됩니다.
                    본인이 요청하지 않았다면 이 이메일을 무시하세요.
                    """.formatted(verificationCode, verificationUrl, localizedExpiresAt),
                    "거래소 가입 인증 코드", "인증 코드", verificationCode, "예비 링크", verificationUrl, "만료 시간", localizedExpiresAt,
                    "본인이 요청하지 않았다면 이 이메일을 무시하세요.");
            default -> emailContent("Registration verification code", """
                    Your exchange registration verification code is:

                    %s

                    You can also complete registration with this backup link:

                    %s

                    This registration request expires at %s.
                    If you did not request this registration, you can ignore this email.
                    """.formatted(verificationCode, verificationUrl, localizedExpiresAt),
                    "Your exchange registration verification code", "Verification Code", verificationCode, "Backup link", verificationUrl,
                    "Expires at", localizedExpiresAt, "If you did not request this registration, you can ignore this email.");
        };
    }

    private EmailContent emailContent(
            String subject,
            String plainBody,
            String title,
            String codeLabel,
            String verificationCode,
            String linkLabel,
            String verificationUrl,
            String expiresLabel,
            String expiresAt,
            String footer
    ) {
        // The HTML body makes the primary six-digit code visually dominant while retaining a plain-text alternative.
        String htmlBody = """
                <!doctype html>
                <html>
                <body style="margin:0;background:#f4f6f8;padding:24px;font-family:Arial,'Helvetica Neue',sans-serif;color:#172026;">
                  <div style="max-width:520px;margin:0 auto;background:#ffffff;border:1px solid #e1e7ef;border-radius:12px;padding:28px;">
                    <h1 style="margin:0 0 18px;font-size:20px;line-height:1.35;color:#101820;">%s</h1>
                    <div style="margin:16px 0 22px;padding:18px 20px;border-radius:10px;background:#fff4cc;border:1px solid #f2c94c;text-align:center;">
                      <div style="font-size:13px;line-height:1.4;color:#6b5b12;margin-bottom:8px;">%s</div>
                      <div style="font-size:36px;line-height:1.1;font-weight:800;letter-spacing:6px;color:#101820;">%s</div>
                    </div>
                    <p style="font-size:14px;line-height:1.6;margin:0 0 10px;color:#405263;">%s</p>
                    <p style="font-size:14px;line-height:1.6;margin:0 0 18px;word-break:break-all;"><a href="%s" style="color:#1267d8;text-decoration:none;">%s</a></p>
                    <p style="font-size:13px;line-height:1.5;margin:0 0 18px;color:#5a6b7a;">%s: %s</p>
                    <p style="font-size:12px;line-height:1.5;margin:0;color:#7a8794;">%s</p>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(title),
                escapeHtml(codeLabel),
                escapeHtml(verificationCode),
                escapeHtml(linkLabel),
                escapeHtml(verificationUrl),
                escapeHtml(verificationUrl),
                escapeHtml(expiresLabel),
                escapeHtml(expiresAt),
                escapeHtml(footer)
        );
        return new EmailContent(subject, plainBody, htmlBody);
    }

    private ZoneId normalizeTimeZone(String timeZone) {
        try {
            return ZoneId.of(timeZone == null || timeZone.isBlank() ? "UTC" : timeZone.trim());
        } catch (Exception ignored) {
            // A malformed browser time zone must not block account registration; UTC keeps the expiry unambiguous.
            return ZoneId.of("UTC");
        }
    }

    private String formatExpiry(Instant expiresAt, ZoneId timeZone) {
        return EMAIL_EXPIRY_FORMATTER.format(expiresAt.atZone(timeZone));
    }

    private String textToHtml(String body) {
        return "<html><body><pre style=\"font-family:Arial,'Helvetica Neue',sans-serif;white-space:pre-wrap;\">"
                + escapeHtml(body)
                + "</pre></body></html>";
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
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

    record EmailContent(String subject, String plainBody, String htmlBody) {
    }
}
