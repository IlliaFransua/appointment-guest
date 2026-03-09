package com.fransua.appointment.guest.contact.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VerificationCodeRequest(@NotBlank @Size(min = 4, max = 4) String code) {}
