package com.riogandarilla.api.services.impl;

import com.riogandarilla.api.configs.properties.AppProperties;
import com.riogandarilla.api.entities.ReceiptDocumentData;
import com.riogandarilla.api.exception.ServiceUnavailableException;
import com.riogandarilla.api.services.ReceiptArchiveService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Log4j2
@Service
public class ReceiptArchiveServiceImpl implements ReceiptArchiveService {

    private final AppProperties properties;

    public ReceiptArchiveServiceImpl(AppProperties properties) {
        this.properties = properties;
    }

    @Override
    public String archive(ReceiptDocumentData data, String filename, byte[] pdf) {
        if (!properties.archiveEnabled()) {
            return null;
        }

        try {
            Path base = Path.of(properties.archivePath()).toAbsolutePath().normalize();
            Path monthDirectory = base
                    .resolve(String.valueOf(data.anio()))
                    .resolve(String.format("%02d", data.mes()))
                    .normalize();
            if (!monthDirectory.startsWith(base)) {
                throw new SecurityException("Ruta de archivo inválida");
            }

            Files.createDirectories(monthDirectory);
            Path destination = monthDirectory.resolve(filename).normalize();
            if (!destination.startsWith(monthDirectory)) {
                throw new SecurityException("Nombre de archivo inválido");
            }
            Files.write(destination, pdf, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return base.relativize(destination).toString().replace('\\', '/');
        } catch (IOException | SecurityException exception) {
            log.error("No fue posible archivar el recibo receiptId={}", data.receiptId(), exception);
            throw new ServiceUnavailableException(
                    "RECEIPT_ARCHIVE_ERROR",
                    "El comprobante se generó, pero no fue posible archivarlo de forma segura"
            );
        }
    }

    @Override
    public String archiveMonthly(ReceiptDocumentData data, String filename, byte[] pdf) {
        try {
            Path base = Path.of(properties.monthlyArchivePath()).toAbsolutePath().normalize();
            Path periodDirectory = base
                    .resolve(String.format("%02d-%04d", data.mes(), data.anio()))
                    .normalize();
            if (!periodDirectory.startsWith(base)) {
                throw new SecurityException("Ruta mensual inválida");
            }

            Files.createDirectories(periodDirectory);
            Path destination = periodDirectory.resolve(filename).normalize();
            if (!destination.startsWith(periodDirectory)) {
                throw new SecurityException("Nombre de archivo inválido");
            }
            Files.write(
                    destination,
                    pdf,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
            return base.getFileName() + "/" + base.relativize(destination).toString().replace('\\', '/');
        } catch (IOException | SecurityException exception) {
            log.error("No fue posible archivar el recibo mensual movementId={}", data.movementId(), exception);
            throw new ServiceUnavailableException(
                    "MONTHLY_RECEIPT_ARCHIVE_ERROR",
                    "No fue posible guardar el comprobante mensual"
            );
        }
    }
}
