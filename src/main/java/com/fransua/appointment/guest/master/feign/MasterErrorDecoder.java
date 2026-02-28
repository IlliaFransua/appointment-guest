package com.fransua.appointment.guest.master.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fransua.appointment.guest.exception.infra.ApiErrorResponse;
import com.fransua.appointment.guest.exception.infra.ServiceIntegrationException;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MasterErrorDecoder implements ErrorDecoder {

  private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

  @Override
  public Exception decode(String methodKey, Response response) {
    ApiErrorResponse errorResponse = createFallback(response);

    if (response.body() != null && isJsonResponse(response)) {
      try (InputStream is = response.body().asInputStream()) {
        byte[] bodyBytes = is.readAllBytes();

        if (bodyBytes.length > 0) {
          try {
            errorResponse = mapper.readValue(bodyBytes, ApiErrorResponse.class);
          } catch (IOException ex) {
            log.error(
                "Failed to parse JSON. Raw body: {}",
                new String(bodyBytes, StandardCharsets.UTF_8));
          }
        }
      } catch (IOException e) {
        log.error("Could not parse body from {}: {}", methodKey, e.getMessage());
      }
    }

    return new ServiceIntegrationException(response.status(), errorResponse);
  }

  private ApiErrorResponse createFallback(Response response) {
    return ApiErrorResponse.builder()
        .status(response.status())
        .title("Master Service Error")
        .detail("Communication failed with status: " + response.status())
        .build();
  }

  private boolean isJsonResponse(Response response) {
    return response.headers().getOrDefault("Content-Type", Collections.emptyList()).stream()
        .anyMatch(ct -> ct.contains("application/json"));
  }
}
