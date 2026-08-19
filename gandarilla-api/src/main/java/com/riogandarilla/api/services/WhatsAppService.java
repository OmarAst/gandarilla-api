package com.riogandarilla.api.services;

import com.riogandarilla.api.entities.ReceiptDocumentData;
import com.riogandarilla.api.entities.WhatsAppSendResult;

public interface WhatsAppService {
    WhatsAppSendResult sendReceipt(ReceiptDocumentData data, byte[] pdf, String filename);
}
