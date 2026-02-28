package com.fransua.appointment.guest.master.dto.offering;

import com.fransua.appointment.guest.master.dto.address.AddressResponse;
import java.util.List;

public record OfferingPageResponse(
    List<OfferingResponse> offerings, List<AddressResponse> addresses) {}
