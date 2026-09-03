package pt.glsmanagement.platform.web;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public record ApiError(OffsetDateTime timestamp, int status, String message) {
    public static ApiError of(int status, String message) {
        return new ApiError(OffsetDateTime.now(ZoneOffset.UTC), status, message);
    }
}

