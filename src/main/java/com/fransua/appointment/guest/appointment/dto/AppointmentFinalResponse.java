package com.fransua.appointment.guest.appointment.dto;

import com.fransua.appointment.guest.contact.dto.ContactResponse;
import java.util.List;

public record AppointmentFinalResponse(
    AppointmentResponse appointment, List<ContactResponse> contacts) {}
