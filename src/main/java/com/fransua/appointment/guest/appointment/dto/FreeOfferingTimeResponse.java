package com.fransua.appointment.guest.appointment.dto;

import com.fransua.appointment.guest.util.FTimeUtil.FTimeRange;
import java.time.LocalDate;
import java.util.List;

public record FreeOfferingTimeResponse(LocalDate date, List<OfferingTimeRange> shifts) {

  public record OfferingTimeRange(Long shiftId, List<FTimeRange> times) {}
}
