package com.fransua.appointment.guest.exception.infra;

import com.fransua.appointment.guest.exception.InvalidAppointmentSlotException;
import com.fransua.appointment.guest.exception.InvalidVerificationCodeException;
import com.fransua.appointment.guest.exception.RequestValidationException;
import com.fransua.appointment.guest.exception.ResourceLimitExceededException;
import com.fransua.appointment.guest.exception.ResourceNotFoundException;
import com.fransua.appointment.guest.exception.ServiceUnavailableException;
import com.fransua.appointment.guest.exception.VerificationCodeExpiredException;
import com.fransua.appointment.guest.exception.VerificationNotRequiredException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ApiErrorResponse handleResourceNotFound(RuntimeException ex) {
    return ApiErrorResponse.from(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(ServiceIntegrationException.class)
  public ResponseEntity<ApiErrorResponse> handleServiceIntegrationException(
      ServiceIntegrationException ex, HttpServletRequest request) {

    ApiErrorResponse original = ex.getErrorResponse();

    ApiErrorResponse responseForClient =
        ApiErrorResponse.builder()
            .type(original.type())
            .title(original.title())
            .status(ex.getStatus())
            .detail(original.detail())
            .instance(request.getRequestURI())
            .timestamp(original.timestamp() != null ? original.timestamp() : Instant.now())
            .build();

    log.error("Integration error: {} -> {}", request.getRequestURI(), original.detail());
    return ResponseEntity.status(ex.getStatus()).body(responseForClient);
  }

  @ExceptionHandler(feign.RetryableException.class)
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public ApiErrorResponse handleRetryableException(
      feign.RetryableException ex, HttpServletRequest request) {
    log.error("Service unavailable at {}: {}", request.getRequestURI(), ex.getMessage());

    return ApiErrorResponse.from(
        HttpStatus.SERVICE_UNAVAILABLE,
        "The required internal service is currently down or unreachable. Please try again"
            + " later.");
  }

  @ExceptionHandler(feign.FeignException.class)
  public ResponseEntity<ApiErrorResponse> handleFeignException(feign.FeignException ex) {
    log.error("Feign communication error", ex);

    ApiErrorResponse error =
        ApiErrorResponse.builder()
            .status(ex.status() > 0 ? ex.status() : 500)
            .title("Integration Communication Error")
            .detail("Error during communication with external service")
            .build();

    return ResponseEntity.status(error.status()).body(error);
  }

  @ExceptionHandler({IllegalStateException.class, Exception.class})
  @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
  public ApiErrorResponse handleAnyException(Exception ex, HttpServletRequest request) {
    log.error("Unexpected error at {}", request.getRequestURI(), ex);
    return ApiErrorResponse.from(
        HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred. Please try again later.");
  }

  @ExceptionHandler(ServiceUnavailableException.class)
  @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
  public ApiErrorResponse handleServiceNotFoundException(Exception ex) {
    return ApiErrorResponse.from(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ApiErrorResponse handleDataIntegrityViolationException(
      DataIntegrityViolationException ex) {
    log.error("Data integrity violation: {}", ex.getMessage());
    return ApiErrorResponse.from(
        HttpStatus.CONFLICT,
        "Action could not be completed due to a data conflict (e.g., duplicate entry or constraint"
            + " violation");
  }

  @ExceptionHandler({
    ResourceLimitExceededException.class,
    InvalidAppointmentSlotException.class,
    VerificationNotRequiredException.class
  })
  @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
  public ApiErrorResponse handleBusinessExceptions(Exception ex) {
    return ApiErrorResponse.from(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
  }

  @ExceptionHandler(VerificationCodeExpiredException.class)
  @ResponseStatus(HttpStatus.GONE)
  public ApiErrorResponse handleVerificationCodeExpiredException(Exception ex) {
    return ApiErrorResponse.from(HttpStatus.GONE, ex.getMessage());
  }

  @ExceptionHandler({RequestValidationException.class, InvalidVerificationCodeException.class})
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ApiErrorResponse handleRequestValudationException(RequestValidationException ex) {
    log.warn("Bad request", ex);
    ex.printStackTrace();
    return ApiErrorResponse.from(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {

    String detail =
        ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.joining("; "));

    HttpStatus httpStatus = HttpStatus.valueOf(status.value());

    ApiErrorResponse apiErrorResponse =
        ApiErrorResponse.from(httpStatus, detail, "Validation Failed");

    return ResponseEntity.status(status).body(apiErrorResponse);
  }
}
