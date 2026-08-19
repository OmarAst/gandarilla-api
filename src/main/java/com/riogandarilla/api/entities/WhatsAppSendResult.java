package com.riogandarilla.api.entities;

public record WhatsAppSendResult(
        String status,
        String messageId,
        String mediaId
) {
}
