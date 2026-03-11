package com.fransua.appointment.guest.appointment.dto;

import com.fransua.appointment.guest.master.dto.booking.GuestFieldsResponse;

public record AppointmentDraftResponse(
    AppointmentResponse appointmentResponse,
    GuestFieldsResponse requiredFields // Required fields to attach and verify
    ) {}
