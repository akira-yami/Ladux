package org.akira.ladux.shared.dto.response;

/**
 * Response for orphan multipart upload endpoints that only return a public path.
 * Domain create/update then attaches this path via JSON ({@code imageUrl}, etc.).
 */
public record UploadUrlResponse(String url) {
}
