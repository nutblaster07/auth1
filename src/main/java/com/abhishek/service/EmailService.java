package com.abhishek.service;


import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * EmailService — sends OTP emails FROM noreply@abhishek.com.
 *
 * We use MimeMessage (instead of SimpleMailMessage) because it supports:
 * - HTML email content (styled OTP box)
 * - Custom "From" display name ("Abhishek Team <noreply@abhishek.com>")
 * - UTF-8 encoding
 *
 * HOW THE FROM ADDRESS WORKS:
 * Gmail SMTP authenticates with YOUR Gmail account (spring.mail.username).
 * But the "From" header in the email says "noreply@abhishek.com".
 * Recipients see noreply@abhishek.com as the sender.
 * Note: for this to work reliably without spam issues, set up SPF/DKIM records
 * for abhishek.com pointing to Google's mail servers if using Gmail SMTP.
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;        // noreply@abhishek.com

    @Value("${app.mail.fromName}")
    private String fromName;           // Abhishek Team

    @Value("${app.otp.expiryMinutes:10}")
    private int otpExpiryMinutes;

    /**
     * Send an OTP email to the given address.
     *
     * The email contains:
     * - A clear subject line
     * - The 6-digit OTP in large text
     * - Expiry notice
     * - Warning not to share the code
     *
     * @param toEmail   recipient's email address
     * @param otp       the 6-digit OTP string
     * @param purpose   "signup" or "login" — affects the email subject line
     */
    public void sendOtp(String toEmail, String otp, String purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            // MimeMessageHelper simplifies building MimeMessage
            // true = multipart (needed for HTML)
            // "UTF-8" = encoding
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            // Set sender: appears as "Abhishek Team <noreply@abhishek.com>"
            helper.setFrom(fromAddress, fromName);

            // Recipient
            helper.setTo(toEmail);

            // Subject line
            String subject = purpose.equals("signup")
                    ? "Verify your email — Abhishek"
                    : "Your login OTP — Abhishek";
            helper.setSubject(subject);

            // HTML body — styled email
            helper.setText(buildEmailHtml(otp, purpose), true); // true = isHtml

            mailSender.send(message);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            // In production, use a proper logger (SLF4J/Logback)
            System.err.println("Failed to send email to " + toEmail + ": " + e.getMessage());
            throw new RuntimeException("Failed to send verification email. Please try again.");
        }
    }

    /**
     * Builds the HTML content for the OTP email.
     * Clean, minimal design that works across email clients.
     *
     * @param otp     the 6-digit code
     * @param purpose "signup" or "login"
     * @return HTML string
     */
    private String buildEmailHtml(String otp, String purpose) {
        String actionText = purpose.equals("signup")
                ? "verify your email address"
                : "log in to your account";

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0;padding:0;background-color:#f4f4f4;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f4f4f4;padding:40px 0;">
                <tr>
                  <td align="center">
                    <table width="500" cellpadding="0" cellspacing="0"
                           style="background:#ffffff;border-radius:8px;overflow:hidden;
                                  box-shadow:0 2px 8px rgba(0,0,0,0.1);">

                      <!-- Header -->
                      <tr>
                        <td style="background:#1a1a2e;padding:30px;text-align:center;">
                          <h1 style="color:#ffffff;margin:0;font-size:24px;font-weight:600;">
                            Abhishek
                          </h1>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding:40px 30px;">
                          <p style="color:#333333;font-size:16px;margin:0 0 20px;">
                            Use the code below to %s:
                          </p>

                          <!-- OTP Box -->
                          <div style="background:#f8f9fa;border:2px solid #e9ecef;
                                      border-radius:8px;padding:20px;text-align:center;
                                      margin:20px 0;">
                            <span style="font-size:36px;font-weight:700;
                                         letter-spacing:8px;color:#1a1a2e;">
                              %s
                            </span>
                          </div>

                          <p style="color:#666666;font-size:14px;margin:20px 0 0;">
                            This code expires in <strong>%d minutes</strong>.
                          </p>
                          <p style="color:#666666;font-size:14px;margin:10px 0 0;">
                            If you did not request this, please ignore this email.
                            Do not share this code with anyone.
                          </p>
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background:#f8f9fa;padding:20px 30px;text-align:center;
                                   border-top:1px solid #e9ecef;">
                          <p style="color:#999999;font-size:12px;margin:0;">
                            Sent by Abhishek &nbsp;|&nbsp; noreply@abhishek.com
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(actionText, otp, otpExpiryMinutes);
    }
}