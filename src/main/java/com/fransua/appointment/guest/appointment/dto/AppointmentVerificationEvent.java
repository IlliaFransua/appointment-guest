package com.fransua.appointment.guest.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AppointmentVerificationEvent(
    @NotBlank @Size(min = 7, max = 15) @Pattern(regexp = "^[1-9]\\d{6,14}$") String phoneNumber,
    @NotBlank @Pattern(regexp = "^\\d{4}$") String code) {}
