package com.fransua.appointment.guest.contact.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// RFC 5321
public record EmailRequest(@NotBlank @Email @Size(max = 255) String email) {}
