package fu.sap490.g23.backend.service.mail;

import java.time.LocalDateTime;

public class EmailTemplateUtil {

    private static final String FONT_FAMILY = "'Segoe UI', Tahoma, Geneva, Verdana, sans-serif";

    public static String buildBrandedEmailHtml(
            String recipientName,
            String heading,
            String description,
            String highlightBoxContent,
            String actionUrl,
            String actionLabel,
            String supportEmail,
            String footerNote
    ) {
        String safeName = escapeHtml(valueOrDefault(recipientName, "bạn"));
        String safeHeading = escapeHtml(heading);
        String safeDescription = escapeHtml(description);
        String safeSupportEmail = escapeHtml(valueOrDefault(supportEmail, "support@englishlab.vn"));
        String year = String.valueOf(LocalDateTime.now().getYear());

        String buttonHtml = (actionUrl != null && !actionUrl.isBlank() && actionLabel != null && !actionLabel.isBlank())
                ? """
                  <p style="margin:22px 0 0;text-align:center;">
                    <a href="%s" style="display:inline-block;padding:12px 22px;border-radius:10px;background:#730014;color:#ffffff;text-decoration:none;font-size:14px;font-weight:600;font-family:%s;">%s</a>
                  </p>
                  """.formatted(escapeHtml(actionUrl), FONT_FAMILY, escapeHtml(actionLabel))
                : "";

        String highlightHtml = (highlightBoxContent != null && !highlightBoxContent.isBlank())
                ? """
                  <div style="margin-top:24px;padding:18px 24px;border-radius:18px;background:#fff1f3;border:1px solid #dfbfbd;">
                    %s
                  </div>
                  """.formatted(highlightBoxContent)
                : "";

        String noteHtml = (footerNote != null && !footerNote.isBlank())
                ? """
                  <p style="margin:18px 0 0;font-size:13px;line-height:22px;color:#7a5c59;font-family:%s;">%s</p>
                  """.formatted(FONT_FAMILY, escapeHtml(footerNote))
                : "";

        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>EnglishLab</title>
                </head>
                <body style="margin:0;padding:0;background:#f7f3f2;font-family:%s;color:#2b1f1f;-webkit-font-smoothing:antialiased;">
                  <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:24px 12px;font-family:%s;">
                    <tr>
                      <td align="center">
                        <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="max-width:620px;background:#ffffff;border-radius:24px;overflow:hidden;border:1px solid #ead8d5;font-family:%s;">
                          <tr>
                            <td style="padding:32px;background:linear-gradient(135deg,#fff7f5 0%%,#ffffff 52%%,#f6e3e0 100%%);">
                              <table role="presentation" cellspacing="0" cellpadding="0" style="border-collapse:collapse;">
                                <tr>
                                  <td style="padding:0;vertical-align:middle;">
                                    <span style="display:inline-block;width:12px;height:28px;background:#8a0018;border-radius:2px;"></span>
                                    <span style="display:inline-block;width:10px;height:20px;background:#c45a64;border-radius:2px;margin-left:4px;"></span>
                                  </td>
                                  <td style="padding:0 0 0 10px;vertical-align:middle;font-size:24px;line-height:1;font-weight:600;color:#1f1f24;font-family:%s;">
                                    English<span style="color:#8a0018;">Lab</span>
                                  </td>
                                </tr>
                              </table>
                              <p style="margin:24px 0 0;font-size:14px;line-height:22px;color:#7a5c59;font-family:%s;">Xin chào %s,</p>
                              <h1 style="margin:10px 0 0;font-size:24px;line-height:34px;color:#4b0009;font-weight:600;font-family:%s;">%s</h1>
                              <p style="margin:12px 0 0;font-size:15px;line-height:26px;color:#5f4745;font-family:%s;">%s</p>
                              %s
                              %s
                              %s
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:20px 32px 32px;border-top:1px solid #f2e4e1;">
                              <p style="margin:0;font-size:13px;line-height:22px;color:#7a5c59;font-family:%s;">
                                Cần hỗ trợ? Liên hệ <a href="mailto:%s" style="color:#730014;text-decoration:none;font-weight:600;">%s</a>.
                              </p>
                              <p style="margin:12px 0 0;font-size:12px;line-height:20px;color:#9b807d;font-family:%s;">© %s EnglishLab. All rights reserved.</p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                FONT_FAMILY,
                FONT_FAMILY,
                FONT_FAMILY,
                FONT_FAMILY,
                FONT_FAMILY,
                safeName,
                FONT_FAMILY,
                safeHeading,
                FONT_FAMILY,
                safeDescription,
                highlightHtml,
                buttonHtml,
                noteHtml,
                FONT_FAMILY,
                safeSupportEmail,
                safeSupportEmail,
                FONT_FAMILY,
                year
        );
    }

    public static String valueOrDefault(String value, String fallback) {
        return (value == null || value.trim().isEmpty()) ? fallback : value.trim();
    }

    public static String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
