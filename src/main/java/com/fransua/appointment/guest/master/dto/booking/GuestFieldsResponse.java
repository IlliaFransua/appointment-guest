package com.fransua.appointment.guest.master.dto.booking;

import java.util.List;

public record GuestFieldsResponse(
    List<GuestFields> guestRequiredFields, List<GuestFields> guestFieldsToVerify) {}
