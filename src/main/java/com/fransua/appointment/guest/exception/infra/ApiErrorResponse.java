package com.fransua.appointment.guest.exception.infra;

import java.time.Instant;
import lombok.Builder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

@Builder
public record ApiErrorResponse(
    String type, String title, int status, String detail, String instance, Instant timestamp) {

  public static ApiErrorResponse from(HttpStatus status, String message) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, message);

    String currentPath =
        org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequestUri()
            .build()
            .getPath();

    return ApiErrorResponse.builder()
        .type(pd.getType().toString())
        .title(status.getReasonPhrase())
        .status(status.value())
        .detail(message)
        .instance(currentPath)
        .timestamp(Instant.now())
        .build();
  }

  public static ApiErrorResponse from(HttpStatus status, String message, String title) {
    ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, message);

    String currentPath =
        org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentRequestUri()
            .build()
            .getPath();

    return ApiErrorResponse.builder()
        .type(pd.getType().toString())
        .title(title)
        .status(status.value())
        .detail(message)
        .instance(currentPath)
        .timestamp(Instant.now())
        .build();
  }
}
