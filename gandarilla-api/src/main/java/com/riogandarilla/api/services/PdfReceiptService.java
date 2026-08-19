package com.riogandarilla.api.services;

import com.riogandarilla.api.entities.ReceiptDocumentData;

public interface PdfReceiptService {
    byte[] generate(ReceiptDocumentData data);
}
