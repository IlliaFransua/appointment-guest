package com.fransua.appointment.guest.appointment;

import com.fransua.appointment.guest.appointment.dto.AppointmentResponse;
import com.fransua.appointment.guest.appointment.dto.CreateAppointmentRequest;
import com.fransua.appointment.guest.exception.ResourceNotFoundException;
import com.fransua.appointment.guest.master.dto.booking.BookingContextResponse;
import java.math.BigDecimal;
import java.time.LocalTime;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {

  AppointmentResponse toResponse(Appointment appointment);

  default Appointment toEntity(
      String slug,
      BookingContextResponse context,
      CreateAppointmentRequest createAppointmentRequest) {
    return Appointment.builder()
        .id(null)
        .masterId(context.masterId())
        .slug(slug)

        // Guest
        .guestName(createAppointmentRequest.guestName())
        .guestPhone(createAppointmentRequest.guestPhone())
        .guestPhoneHash(null)
        .guestPreAppointmentNotes(createAppointmentRequest.guestPreAppointmentNotes())

        // Shift
        .shiftId(context.shift().id())
        .date(context.shift().date())
        .startTime(createAppointmentRequest.startTime())
        .endTime(extractEndTime(context, createAppointmentRequest))

        // Offering
        .offeringId(context.offering().id())
        .offeringName(context.offering().name())
        .offeringDescription(context.offering().description())
        .offeringPrice(extractPriceByAddressId(context))

        // Address
        .addressId(context.address().id())
        .addressCountryCode(context.address().countryCode())
        .addressCurrencyCode(context.address().currencyCode())
        .addressFull(context.address().fullAddress())
        .addressDetails(context.address().details())
        .addressTimezone(context.address().timezone())

        // Status
        .status(Appointment.Status.CREATED)
        .createdAt(null)
        .updatedAt(null)
        .build();
  }

  private LocalTime extractEndTime(
      BookingContextResponse context, CreateAppointmentRequest createAppointmentRequest) {
    return createAppointmentRequest
        .startTime()
        .plusMinutes(context.offering().durationMaxMinutes().longValue());
  }

  private BigDecimal extractPriceByAddressId(BookingContextResponse context) {
    return context.offering().prices().stream()
        .filter(price -> price.addressId().equals(context.address().id()))
        .map(price -> price.price())
        .findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Address", context.address().id()));
  }
}
