package fu.sap490.g23.backend.service.payment;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.payment.PaymentOrder;
import fu.sap490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class PaymentReceiptPdfService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] buildCourseReceipt(PaymentOrder order) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 11);

            document.add(new Paragraph("EnglishLab - Bien lai thanh toan khoa hoc", titleFont));
            document.add(new Paragraph(" "));
            document.add(line("Ma don", String.valueOf(order.getOrderCode()), labelFont, bodyFont));
            document.add(line("Trang thai", order.getStatus() == null ? "—" : order.getStatus().name(), labelFont, bodyFont));

            User student = order.getStudent();
            document.add(line("Hoc vien", student == null ? "—" : nullToDash(student.getFullName()), labelFont, bodyFont));
            document.add(line("Email", student == null ? "—" : nullToDash(student.getEmail()), labelFont, bodyFont));
            document.add(line("Mo ta", nullToDash(order.getDescription()), labelFont, bodyFont));
            document.add(line("Khoa hoc", courseTitles(order), labelFont, bodyFont));
            document.add(line("Gia goc", formatVnd(order.getOriginalAmount()), labelFont, bodyFont));
            document.add(line("Giam he thong", formatVnd(order.getSystemDiscountAmount()), labelFont, bodyFont));
            document.add(line("Giam ma", formatVnd(order.getCouponDiscountAmount()), labelFont, bodyFont));
            document.add(line("Ma giam gia", nullToDash(order.getDiscountCodeText()), labelFont, bodyFont));
            document.add(line("Thanh toan", formatVnd(order.getAmount()), labelFont, bodyFont));
            document.add(line(
                    "Thanh toan luc",
                    order.getPaidAt() == null ? "—" : order.getPaidAt().format(DATE_TIME),
                    labelFont,
                    bodyFont
            ));

            if (order.getStatus() == PaymentOrderStatus.REFUNDED) {
                document.add(new Paragraph(" "));
                document.add(line("Da hoan tien", formatVnd(order.getRefundedAmount()), labelFont, bodyFont));
                document.add(line(
                        "Hoan tien luc",
                        order.getRefundedAt() == null ? "—" : order.getRefundedAt().format(DATE_TIME),
                        labelFont,
                        bodyFont
                ));
                document.add(line("Ly do hoan", nullToDash(order.getRefundReason()), labelFont, bodyFont));
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Tai lieu nay duoc tao tu he thong EnglishLab. Tien PayOS neu can chuyen lai se xu ly ngoai ung dung.",
                    bodyFont
            ));
            document.close();
            return output.toByteArray();
        } catch (DocumentException ex) {
            throw new RuntimeException("Không tạo được biên lai PDF.", ex);
        }
    }

    private Paragraph line(String label, String value, Font labelFont, Font bodyFont) {
        Paragraph paragraph = new Paragraph();
        paragraph.add(new com.lowagie.text.Chunk(label + ": ", labelFont));
        paragraph.add(new com.lowagie.text.Chunk(value, bodyFont));
        return paragraph;
    }

    private String courseTitles(PaymentOrder order) {
        if (order.getCourseTitles() == null || order.getCourseTitles().isBlank()) {
            return "—";
        }
        return Arrays.stream(order.getCourseTitles().split("\\|"))
                .map(String::trim)
                .filter(title -> !title.isBlank())
                .collect(Collectors.joining(", "));
    }

    private String formatVnd(Long amount) {
        long value = amount == null ? 0L : amount;
        return String.format(Locale.US, "%,d VND", value).replace(',', '.');
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }
}
