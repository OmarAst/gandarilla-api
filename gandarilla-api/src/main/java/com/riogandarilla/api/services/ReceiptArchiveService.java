package com.riogandarilla.api.services;

import com.riogandarilla.api.entities.ReceiptDocumentData;

public interface ReceiptArchiveService {
    String archive(ReceiptDocumentData data, String filename, byte[] pdf);

    String archiveMonthly(ReceiptDocumentData data, String filename, byte[] pdf);
}
