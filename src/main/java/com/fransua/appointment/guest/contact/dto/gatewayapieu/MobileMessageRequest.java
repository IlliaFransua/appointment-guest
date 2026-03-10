package com.fransua.appointment.guest.contact.dto.gatewayapieu;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MobileMessageRequest {

  @Builder.Default
  private String sender = "Fransua"; // min 3 max 11 alphanumeric characters or 15 digits

  private Long recipient;

  private String message; // UTF-8 and max 160 for one paid part

  @Builder.Default private String expiration = "PT10M"; // 10 min

  @Builder.Default private String priority = "urgent"; // "normal", "urgent"
}
