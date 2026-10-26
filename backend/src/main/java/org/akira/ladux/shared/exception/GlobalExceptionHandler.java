package org.akira.ladux.shared.exception;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import jakarta.validation.ConstraintViolationException;

// Bat moi exception tu Controller va tra ErrorResponse JSON thong nhat.
// Mapping: 404 not found, 400 validation/business rule, 401 auth, 403 access denied, 409 DB constraint, 500 fallback.
// Bao mat: loi dang nhap tra message chung, khong lo user ton tai hay khong.
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Pattern NULL_COLUMN_PATTERN = Pattern.compile("null value in column \"([^\"]+)\"");
    private static final Pattern UNIQUE_CONSTRAINT_PATTERN = Pattern.compile("unique constraint \"([^\"]+)\"");

    @ExceptionHandler(org.springframework.dao.OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleConcurrentUpdate() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now()).status(409).error("Conflict")
                .message("Dữ liệu đã được thay đổi bởi thao tác khác. Vui lòng tải lại và thử lại").build());
    }

    @ExceptionHandler({HandlerMethodValidationException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ErrorResponse> handleInvalidRequest(Exception ex) {
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now()).status(400).error("Validation Error")
                .message("Tham số yêu cầu không hợp lệ").build());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({IllegalArgumentException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class})
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception exception) {
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now()).status(400).error("Bad Request")
                .message("Tham số yêu cầu không hợp lệ").build());
    }

    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleOversizedUpload() {
        return ResponseEntity.status(413).body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now()).status(413).error("Payload Too Large")
                .message("Tệp tải lên vượt quá dung lượng cho phép").build());
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleMissingResource() {
        return ResponseEntity.status(404).body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now()).status(404).error("Not Found")
                .message("Không tìm thấy tài nguyên").build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message(errorMessage)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleException(BusinessRuleException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String errorMessage = ex.getConstraintViolations().stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.joining(", "));
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Error")
                .message(errorMessage)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound() {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Username hoac password khong dung")
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials() {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Username hoac password khong dung")
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationFailure() {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message("Username hoac password khong dung")
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthenticated(UnauthenticatedException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(RateLimitExceededException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message(ex.getMessage())
                .build();
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        // Domain code (vd: CAPTCHA) dung message tieng Viet co chu dich cho nguoi dung; giu nguyen.
        String message = (ex != null && ex.getMessage() != null && !ex.getMessage().isBlank()
                && !"Access Denied".equalsIgnoreCase(ex.getMessage().trim()))
                ? ex.getMessage()
                : "Bạn không có quyền thực hiện thao tác này";
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(message)
                .build();
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(resolveDataIntegrityMessage(ex))
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Cac ngoai le chuan cua Spring MVC (sai method, sai content-type, thieu header/cookie,
     * ResponseStatusException...) da mang san HTTP status. Truoc day chung roi vao handler
     * Exception -> 500 + log ERROR, gay bao dong gia. Tra dung status voi thong diep chung.
     */
    @ExceptionHandler({
            org.springframework.web.HttpRequestMethodNotSupportedException.class,
            org.springframework.web.HttpMediaTypeNotSupportedException.class,
            org.springframework.web.HttpMediaTypeNotAcceptableException.class,
            org.springframework.web.bind.ServletRequestBindingException.class,
            org.springframework.web.ErrorResponseException.class,
            org.springframework.web.context.request.async.AsyncRequestTimeoutException.class
    })
    public ResponseEntity<ErrorResponse> handleSpringWebException(Exception ex) {
        int status = ex instanceof org.springframework.web.ErrorResponse errorResponse
                ? errorResponse.getStatusCode().value()
                : HttpStatus.BAD_REQUEST.value();
        HttpStatus resolved = HttpStatus.resolve(status);
        String reason = resolved == null ? "Error" : resolved.getReasonPhrase();
        String message = status >= 500 ? "Đã xảy ra lỗi hệ thống" : "Yêu cầu không hợp lệ";
        if (status >= 500) {
            log.warn("Spring web exception mapped to {}: {}", status, ex.getClass().getSimpleName());
        }
        return ResponseEntity.status(status).body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(reason)
                .message(message)
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex) {
        log.error("Ngoại lệ chưa được xử lý trong yêu cầu", ex);
        ErrorResponse error = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("Đã xảy ra lỗi hệ thống")
                .build();
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private String resolveDataIntegrityMessage(DataIntegrityViolationException ex) {
        SQLException sqlException = findSqlException(ex);
        if (sqlException == null || sqlException.getSQLState() == null) {
            return "Du lieu vi pham rang buoc";
        }
        return switch (sqlException.getSQLState()) {
            case "23505" -> resolveUniqueConstraintMessage(sqlException);
            case "23503" -> "Du lieu dang duoc tham chieu boi ban ghi khac";
            case "23502" -> resolveNotNullMessage(sqlException);
            case "23514" -> "Du lieu khong thoa man rang buoc kiem tra";
            default -> "Du lieu vi pham rang buoc";
        };
    }

    private String resolveNotNullMessage(SQLException sqlException) {
        Matcher matcher = NULL_COLUMN_PATTERN.matcher(sqlException.getMessage());
        if (!matcher.find()) {
            return "Thieu truong bat buoc trong DB";
        }
        return "Thiếu dữ liệu bắt buộc";
    }

    private String resolveUniqueConstraintMessage(SQLException sqlException) {
        Matcher matcher = UNIQUE_CONSTRAINT_PATTERN.matcher(sqlException.getMessage());
        if (!matcher.find()) {
            return "Du lieu bi trung voi rang buoc unique";
        }
        String constraint = matcher.group(1);
        if (constraint.contains("email")) {
            return "Email nay da ton tai trong DB. Hay dung email khac.";
        }
        if (constraint.contains("username")) {
            return "Username nay da ton tai trong DB. Hay dung username khac.";
        }
        return "Dữ liệu đã tồn tại";
    }

    private SQLException findSqlException(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof SQLException sqlException) {
                return sqlException;
            }
            cursor = cursor.getCause();
        }
        return null;
    }
}
