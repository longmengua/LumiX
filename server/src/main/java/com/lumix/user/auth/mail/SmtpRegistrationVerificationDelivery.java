package com.lumix.user.auth.mail;

import com.lumix.user.auth.application.RegistrationVerificationDeliveryPort;
import com.lumix.user.auth.config.UserAuthenticationProperties;
import com.lumix.user.auth.domain.RegistrationVerificationSecret;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * SMTP 註冊驗證碼 adapter。
 *
 * <p>刻意沿用已受控的 SMTP 設定，不接受瀏覽器提供的收件者、寄件者或信件內容。數字與字母碼需要同時正確，
 * 降低單一短碼被猜中的風險。</p>
 */
@Component
@Profile("infrastructure")
@ConditionalOnProperty(prefix = "lumix.auth.password-reset", name = "smtp-enabled", havingValue = "true")
public class SmtpRegistrationVerificationDelivery implements RegistrationVerificationDeliveryPort {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpRegistrationVerificationDelivery(JavaMailSender mailSender, UserAuthenticationProperties properties) {
        this.mailSender = mailSender;
        this.fromAddress = properties.getPasswordReset().getFromAddress();
        Assert.hasText(fromAddress, "lumix.auth.passwordReset.fromAddress is required when SMTP is enabled");
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void deliver(String email, RegistrationVerificationSecret secret) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("LumiX 註冊驗證碼");
        message.setText("您正在建立 LumiX 帳號。請在註冊頁同時輸入下列兩組驗證碼：\n\n"
            + "數字驗證碼：" + secret.numericCode() + "\n"
            + "英文字母驗證碼（5 碼）：" + secret.letterCode() + "\n\n"
            + "請勿將驗證碼提供給任何人。若非您本人操作，請忽略此信件。");
        mailSender.send(message);
    }
}
