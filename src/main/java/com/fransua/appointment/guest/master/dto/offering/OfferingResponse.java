package com.fransua.appointment.guest.master.dto.offering;

import java.util.List;
import lombok.Builder;

@Builder
public record OfferingResponse(
    Long id,
    Long categoryId,
    String name,
    String description,
    Integer durationMinMinutes,
    Integer durationMaxMinutes,
    List<OfferingPriceResponse> prices) {}
