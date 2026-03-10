package com.fransua.appointment.guest.contact.dto.gatewayapieu;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MobileMessageResponse(@JsonProperty("msg_id") String msgId, Long recipient) {}
