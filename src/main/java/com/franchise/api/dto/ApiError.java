package com.franchise.api.dto;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(String path, int status, String error, String message, List<String> details, OffsetDateTime timestamp) {
}
