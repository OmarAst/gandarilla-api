package com.riogandarilla.api.utils;

import org.apache.logging.log4j.ThreadContext;

public final class RequestIdSupport {
    private RequestIdSupport() {
    }

    public static String current() {
        String requestId = ThreadContext.get("requestId");
        return requestId == null || requestId.isBlank() ? "unknown" : requestId;
    }
}
