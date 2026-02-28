package com.fransua.appointment.guest.master.dto.address;

import lombok.Builder;

@Builder
public record AddressResponse(
    Long id,
    String countryCode,
    String currencyCode,
    String fullAddress,
    String details,
    String timezone) {}
