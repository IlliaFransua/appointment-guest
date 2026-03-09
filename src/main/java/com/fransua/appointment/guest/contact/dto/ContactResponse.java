package com.fransua.appointment.guest.contact.dto;

import com.fransua.appointment.guest.contact.Contact;

public record ContactResponse(Long id, String value, Contact.Type type, Contact.Status status) {}
