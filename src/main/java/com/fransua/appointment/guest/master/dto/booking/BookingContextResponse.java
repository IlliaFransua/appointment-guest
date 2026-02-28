package com.fransua.appointment.guest.master.dto.booking;

import com.fransua.appointment.guest.master.dto.address.AddressResponse;
import com.fransua.appointment.guest.master.dto.offering.OfferingResponse;
import com.fransua.appointment.guest.master.dto.shift.ShiftResponse;

public record BookingContextResponse(
    Long masterId, AddressResponse address, OfferingResponse offering, ShiftResponse shift) {}
