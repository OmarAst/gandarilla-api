package com.riogandarilla.api.services.impl;

import com.riogandarilla.api.configs.properties.AppProperties;
import com.riogandarilla.api.entities.ReceiptDocumentData;
import com.riogandarilla.api.exception.PdfGenerationException;
import com.riogandarilla.api.services.PdfReceiptService;
import lombok.extern.log4j.Log4j2;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Log4j2
@Service
public class PdfReceiptServiceImpl implements PdfReceiptService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Locale ES_MX = Locale.forLanguageTag("es-MX");

    private final AppProperties properties;
    private final ResourceLoader resourceLoader;

    public PdfReceiptServiceImpl(AppProperties properties, ResourceLoader resourceLoader) {
        this.properties = properties;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public byte[] generate(ReceiptDocumentData data) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDFont regular = loadFont(document);
            PDFont bold = regular;
            PDFont titleFont = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawBackground(document, content, page);
                PDImageXObject logo = loadLogo(document);
                drawHeader(content, page, logo, regular, bold, data);
                drawTitle(content, page, regular, titleFont);
                drawHolderSection(content, regular, bold, data);
                drawPaymentSection(content, regular, bold, data);
                drawSignature(content, regular, bold, data);
                drawFooter(content, regular, data);
            }

            document.getDocumentInformation().setTitle("Comprobante de pago casa " + data.casa());
            document.getDocumentInformation().setAuthor(properties.organizationName());
            document.getDocumentInformation().setSubject("Pago de mantenimiento");
            document.save(output);
            return output.toByteArray();
        } catch (IOException | IllegalArgumentException exception) {
            log.error("No fue posible generar el PDF receiptId={}", data.receiptId(), exception);
            throw new PdfGenerationException(
                    "PDF_GENERATION_ERROR",
                    "No fue posible generar el comprobante de pago"
            );
        }
    }

    private PDFont loadFont(PDDocument document) throws IOException {
        Resource resource = resourceLoader.getResource(properties.fontPath());
        if (!resource.exists()) {
            throw new IOException("No se encontró la fuente Unicode configurada");
        }
        try (var input = resource.getInputStream()) {
            return PDType0Font.load(document, input);
        }
    }

    private void drawBackground(PDDocument document, PDPageContentStream content, PDPage page) throws IOException {
        float width = page.getMediaBox().getWidth();
        float height = page.getMediaBox().getHeight();
        Resource backgroundResource = resourceLoader.getResource(properties.backgroundPath());
        if (backgroundResource.exists()) {
            try (var input = backgroundResource.getInputStream()) {
                PDImageXObject background = PDImageXObject.createFromByteArray(
                        document, input.readAllBytes(), "receipt-background"
                );
                content.drawImage(background, 0, 0, width, height);
            }
        } else {
            content.setNonStrokingColor(Color.WHITE);
            content.addRect(0, 0, width, height);
            content.fill();
        }

        content.saveGraphicsState();
        PDExtendedGraphicsState overlay = new PDExtendedGraphicsState();
        overlay.setNonStrokingAlphaConstant(0.68f);
        content.setGraphicsStateParameters(overlay);
        content.setNonStrokingColor(Color.WHITE);
        content.addRect(0, 0, width, height);
        content.fill();
        content.restoreGraphicsState();
    }

    private void drawHeader(
            PDPageContentStream content,
            PDPage page,
            PDImageXObject logo,
            PDFont regular,
            PDFont bold,
            ReceiptDocumentData data
    ) throws IOException {
        float height = page.getMediaBox().getHeight();
        if (logo != null) {
            content.drawImage(logo, 72, height - 128, 72, 72);
        } else {
            content.setStrokingColor(new Color(70, 82, 88));
            content.setLineWidth(1.2f);
            content.addRect(58, height - 126, 70, 70);
            content.stroke();
            centeredText(content, bold, 11, "R.G.", 58, height - 93, 70);
        }

        centeredText(content, bold, 9, properties.organizationName(), 38, height - 151, 145);
        centeredText(content, regular, 7, properties.organizationArea(), 38, height - 166, 145);

        text(content, regular, 9, "Fecha", 375, height - 82);
        line(content, 420, height - 86, 520, height - 86, new Color(145, 151, 154), 0.5f);
        centeredText(content, regular, 8, data.fechaPago().format(DATE_FORMAT), 420, height - 82, 100);

        text(content, regular, 9, "Casa", 380, height - 122);
        line(content, 420, height - 126, 520, height - 126, new Color(145, 151, 154), 0.5f);
        centeredText(content, regular, 8, String.valueOf(data.casa()), 420, height - 122, 100);
    }

    private void drawTitle(
            PDPageContentStream content,
            PDPage page,
            PDFont regular,
            PDFont bold
    ) throws IOException {
        float height = page.getMediaBox().getHeight();
        centeredText(content, bold, 19, "COMPROBANTE DE PAGO", 60, height - 205, 475);
        centeredText(content, regular, 8,
                "Documento interno para control de pagos de mantenimiento",
                60, height - 222, 475);
    }

    private void drawHolderSection(
            PDPageContentStream content,
            PDFont regular,
            PDFont bold,
            ReceiptDocumentData data
    ) throws IOException {
        float x = 36;
        float y = 472;
        float width = 523;
        float height = 112;
        roundedRect(content, x, y, width, height, 18, new Color(100, 104, 105), 1.4f);
        text(content, bold, 7, "DATOS DEL TITULAR DE LA VIVIENDA", x + 38, y + height - 22);

        labelValue(content, regular, 8, "Titular", data.titular(), x + 38, y + 70, 145, 320);
        labelValue(content, regular, 8, "Concepto", data.concepto(), x + 38, y + 46, 145, 320);
        labelValue(content, regular, 8, "Periodo correspondiente",
                data.mesNombre() + " " + data.anio(), x + 38, y + 22, 145, 320);
    }

    private void drawPaymentSection(
            PDPageContentStream content,
            PDFont regular,
            PDFont bold,
            ReceiptDocumentData data
    ) throws IOException {
        float x = 30;
        float y = 245;
        float width = 535;
        float height = 165;
        roundedRect(content, x, y, width, height, 18, new Color(100, 104, 105), 1.4f);
        text(content, bold, 7, "DETALLE DEL PAGO", x + 42, y + height - 22);

        NumberFormat currency = NumberFormat.getCurrencyInstance(ES_MX);
        labelValue(content, regular, 8, "Monto pagado", currency.format(data.monto()), x + 42, y + 120, 150, 315);
        labelValue(content, regular, 8, "Monto con letra", capitalize(data.montoConLetra()), x + 42, y + 96, 150, 315);
        labelValue(content, regular, 8, "Forma de pago", data.formaPago(), x + 42, y + 72, 150, 315);
        labelValue(content, regular, 8, "Referencia / comprobante", emptyAsDash(data.referencia()), x + 42, y + 48, 150, 315);
        labelValue(content, regular, 8, "Observaciones", emptyAsDash(data.observaciones()), x + 42, y + 24, 150, 315);
    }

    private void drawSignature(
            PDPageContentStream content,
            PDFont regular,
            PDFont bold,
            ReceiptDocumentData data
    ) throws IOException {
        centeredText(content, regular, 8, data.validadoPor(), 180, 155, 235);
        line(content, 205, 145, 390, 145, new Color(115, 120, 122), 0.7f);
        centeredText(content, bold, 7, "VALIDACIÓN Y FIRMA", 180, 130, 235);
    }

    private void drawFooter(PDPageContentStream content, PDFont regular, ReceiptDocumentData data) throws IOException {
        String footer = "Folio: " + data.folio()
                + "  |  Movimiento: " + data.movementId()
                + "  |  Generado: " + data.generatedAt().format(DATE_TIME_FORMAT);
        centeredText(content, regular, 6, footer, 40, 40, 515);
    }

    private PDImageXObject loadLogo(PDDocument document) {
        Resource resource = resourceLoader.getResource(properties.logoPath());
        if (!resource.exists()) {
            log.warn("Logo no encontrado path={}", properties.logoPath());
            return null;
        }
        try (var input = resource.getInputStream()) {
            return PDImageXObject.createFromByteArray(document, input.readAllBytes(), "logo");
        } catch (IOException exception) {
            log.warn("No fue posible cargar el logo", exception);
            return null;
        }
    }

    private void labelValue(
            PDPageContentStream content,
            PDFont font,
            float size,
            String label,
            String value,
            float x,
            float y,
            float labelWidth,
            float valueWidth
    ) throws IOException {
        text(content, font, size, label + ":", x, y);
        fittedText(content, font, size, safe(value), x + labelWidth, y, valueWidth);
    }

    private void fittedText(
            PDPageContentStream content,
            PDFont font,
            float preferredSize,
            String value,
            float x,
            float y,
            float maxWidth
    ) throws IOException {
        String fitted = safe(value);
        float size = preferredSize;
        while (size > 6f && textWidth(font, size, fitted) > maxWidth) {
            size -= 0.25f;
        }
        if (textWidth(font, size, fitted) > maxWidth) {
            String suffix = "…";
            while (!fitted.isEmpty() && textWidth(font, size, fitted + suffix) > maxWidth) {
                fitted = fitted.substring(0, fitted.length() - 1);
            }
            fitted += suffix;
        }
        text(content, font, size, fitted, x, y);
    }

    private float textWidth(PDFont font, float size, String value) throws IOException {
        return font.getStringWidth(safe(value)) / 1000f * size;
    }

    private void text(PDPageContentStream content, PDFont font, float size, String value, float x, float y)
            throws IOException {
        content.beginText();
        content.setNonStrokingColor(new Color(60, 65, 68));
        content.setFont(font, size);
        content.newLineAtOffset(x, y);
        content.showText(safe(value));
        content.endText();
    }

    private void centeredText(
            PDPageContentStream content,
            PDFont font,
            float size,
            String value,
            float x,
            float y,
            float width
    ) throws IOException {
        String safe = safe(value);
        float textWidth = font.getStringWidth(safe) / 1000f * size;
        text(content, font, size, safe, x + Math.max(0, (width - textWidth) / 2f), y);
    }

    private void line(
            PDPageContentStream content,
            float x1,
            float y1,
            float x2,
            float y2,
            Color color,
            float lineWidth
    ) throws IOException {
        content.setStrokingColor(color);
        content.setLineWidth(lineWidth);
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private void roundedRect(
            PDPageContentStream content,
            float x,
            float y,
            float width,
            float height,
            float radius,
            Color color,
            float lineWidth
    ) throws IOException {
        float c = 0.552284749831f;
        content.setStrokingColor(color);
        content.setLineWidth(lineWidth);
        content.moveTo(x + radius, y);
        content.lineTo(x + width - radius, y);
        content.curveTo(x + width - radius + radius * c, y, x + width, y + radius - radius * c, x + width, y + radius);
        content.lineTo(x + width, y + height - radius);
        content.curveTo(x + width, y + height - radius + radius * c, x + width - radius + radius * c,
                y + height, x + width - radius, y + height);
        content.lineTo(x + radius, y + height);
        content.curveTo(x + radius - radius * c, y + height, x, y + height - radius + radius * c,
                x, y + height - radius);
        content.lineTo(x, y + radius);
        content.curveTo(x, y + radius - radius * c, x + radius - radius * c, y, x + radius, y);
        content.closePath();
        content.stroke();
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\p{Cc}\\p{Cf}]", "")
                .replace("\n", " ")
                .replace("\r", " ")
                .trim();
    }

    private String emptyAsDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
