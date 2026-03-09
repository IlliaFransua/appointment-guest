package com.fransua.appointment.guest.contact.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// E.164
public record PhoneRequest(
    @NotBlank @Size(min = 7, max = 15) @Pattern(regexp = "^[1-9]\\d{6,14}$") String phone) {}
