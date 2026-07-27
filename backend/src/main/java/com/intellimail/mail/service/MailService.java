package com.intellimail.mail.service;

import com.intellimail.mail.config.MailProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Thin wrapper around Spring's {@link JavaMailSender}. No templating engine
 * exists in this codebase, so the email body is a single hand-built HTML text
 * block (not multipart/plain-text-alt) - reasonable for a one-purpose
 * transactional email sent over a personal Gmail SMTP relay, not a bulk
 * sender where deliverability/spam-score tooling would matter more.
 */
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    /**
     * @throws org.springframework.mail.MailException if message construction fails
     *         (wrapped {@link MessagingException}) or the actual SMTP send fails -
     *         callers should catch this common supertype rather than the checked
     *         {@link MessagingException} directly.
     */
    public void sendPasswordResetEmail(String toEmail, String fullName, String resetLink) {
        MimeMessage message = mailSender.createMimeMessage();
        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, false);
            helper.setTo(toEmail);
            helper.setFrom(mailProperties.fromAddress());
            helper.setSubject("Reset your IntelliMail password");
            helper.setText(buildHtml(fullName, resetLink), true);
        } catch (MessagingException e) {
            throw new MailPreparationException("Could not prepare password reset email", e);
        }
        mailSender.send(message);
    }

    private String buildHtml(String fullName, String resetLink) {
        return """
                <!doctype html>
                <html>
                <body style="margin:0;padding:32px;background-color:#f5f6fa;font-family:Arial,Helvetica,sans-serif;color:#0f172a;">
                  <div style="max-width:480px;margin:0 auto;background:#ffffff;border-radius:16px;padding:32px;">
                    <div style="display:inline-block;width:36px;height:36px;border-radius:10px;background-color:#4F46E5;color:#ffffff;text-align:center;line-height:36px;font-weight:bold;">I</div>
                    <h2 style="margin:16px 0 8px;">Reset your password</h2>
                    <p style="color:#475569;line-height:1.6;">Hi %s,</p>
                    <p style="color:#475569;line-height:1.6;">We received a request to reset your IntelliMail password. Click the button below to choose a new one. This link expires in 1 hour.</p>
                    <p style="text-align:center;margin:32px 0;">
                      <a href="%s" style="background-color:#4F46E5;color:#ffffff;text-decoration:none;padding:12px 28px;border-radius:10px;font-weight:600;display:inline-block;">Reset Password</a>
                    </p>
                    <p style="color:#94a3b8;font-size:13px;line-height:1.6;">If you didn't request this, you can safely ignore this email — your password won't be changed.</p>
                    <p style="color:#94a3b8;font-size:12px;word-break:break-all;">Or paste this link into your browser: %s</p>
                  </div>
                </body>
                </html>
                """.formatted(HtmlUtils.htmlEscape(fullName), resetLink, resetLink);
    }
}
