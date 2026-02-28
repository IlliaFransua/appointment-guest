package com.fransua.appointment.guest.master.dto.offering;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record OfferingPriceResponse(Long addressId, BigDecimal price) {}
