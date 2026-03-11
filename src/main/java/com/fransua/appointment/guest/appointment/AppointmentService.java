package com.fransua.appointment.guest.appointment;

import com.fransua.appointment.guest.appointment.dao.AppointmentDao;
import com.fransua.appointment.guest.appointment.dto.AppointmentDraftResponse;
import com.fransua.appointment.guest.appointment.dto.AppointmentFinalResponse;
import com.fransua.appointment.guest.appointment.dto.AppointmentResponse;
import com.fransua.appointment.guest.appointment.dto.CreateAppointmentRequest;
import com.fransua.appointment.guest.appointment.dto.FreeOfferingTimeResponse;
import com.fransua.appointment.guest.contact.Contact;
import com.fransua.appointment.guest.contact.dto.ContactResponse;
import com.fransua.appointment.guest.contact.internal.ContactInternalService;
import com.fransua.appointment.guest.exception.InvalidAppointmentSlotException;
import com.fransua.appointment.guest.exception.RequestValidationException;
import com.fransua.appointment.guest.exception.ResourceLimitExceededException;
import com.fransua.appointment.guest.exception.ResourceNotFoundException;
import com.fransua.appointment.guest.master.MasterClient;
import com.fransua.appointment.guest.master.dto.booking.BookingContextResponse;
import com.fransua.appointment.guest.master.dto.booking.GuestFields;
import com.fransua.appointment.guest.master.dto.booking.GuestFieldsResponse;
import com.fransua.appointment.guest.master.dto.offering.OfferingResponse;
import com.fransua.appointment.guest.master.dto.shift.ShiftResponse;
import com.fransua.appointment.guest.util.FTimeUtil;
import com.fransua.appointment.guest.util.FTimeUtil.FTimeRange;
import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

  private final AppointmentDao appointmentDao;

  private final MasterClient masterClient;

  private final AppointmentValidator appointmentValidator;

  private final AppointmentMapper appointmentMapper;

  private final ContactInternalService contactInternalService;

  @Transactional
  public AppointmentDraftResponse createAppointment(String slug, CreateAppointmentRequest request) {
    BookingContextResponse context =
        masterClient.getBookingContext(
            slug, request.addressId(), request.shiftId(), request.offeringId());

    boolean isValidTimeSlot =
        getFreeOfferingTime(
                slug,
                context.address().id(),
                context.offering().id(),
                YearMonth.from(context.shift().date()))
            .stream()
            .filter(res -> res.date().equals(context.shift().date()))
            .flatMap(res -> res.shifts().stream())
            .flatMap(shift -> shift.times().stream())
            .anyMatch(range -> range.start().equals(request.startTime()));

    if (!isValidTimeSlot) {
      throw new InvalidAppointmentSlotException(
          request.startTime(),
          request.startTime().plusMinutes(context.offering().durationMaxMinutes()),
          YearMonth.from(context.shift().date()));
    }

    appointmentValidator.validate(context, request);

    Appointment appt;
    try {
      appt =
          appointmentDao.save(
              appointmentMapper.toEntity(slug, Appointment.Status.CREATED, context, request));
    } catch (DataIntegrityViolationException e) {
      throw new ResourceLimitExceededException("This time slot was just taken by another guest");
    }

    AppointmentResponse appointmentResponse = appointmentMapper.toResponse(appt);
    GuestFieldsResponse requiredFields = masterClient.getRequiredGuestFields(slug);
    return new AppointmentDraftResponse(appointmentResponse, requiredFields);
  }

  @Transactional(readOnly = true)
  public GuestFieldsResponse getRequiredGuestFields(String slug) {
    GuestFieldsResponse response = masterClient.getRequiredGuestFields(slug);

    if (contactInternalService.isVerificationSystemDisabled()) {
      List<GuestFields> filteredToVerify =
          response.guestFieldsToVerify().stream()
              .filter(field -> !GuestFields.PHONE.equals(field))
              .toList();
      return new GuestFieldsResponse(response.guestRequiredFields(), filteredToVerify);
    }

    return response;
  }

  @Transactional
  public AppointmentFinalResponse confirmAppointment(Long appointmentId, String slug) {
    Appointment appt = getAppointment(appointmentId, slug, Appointment.Status.CREATED);
    List<ContactResponse> contacts = contactInternalService.getAllContacts(appt.getId());
    GuestFieldsResponse requirements = getRequiredGuestFields(appt.getSlug());

    Map<String, Contact.Status> contactMap =
        contacts.stream()
            .collect(
                Collectors.toMap(
                    contact -> contact.type().name(), ContactResponse::status, (s1, s2) -> s1));

    for (GuestFields field : requirements.guestRequiredFields()) {
      Contact.Status status = contactMap.get(field.name());
      if (status == null) {
        throw new RequestValidationException("Missing field to attach: " + field.getDisplayName());
      }
    }

    for (GuestFields field : requirements.guestFieldsToVerify()) {
      Contact.Status status = contactMap.get(field.name());
      if (status != Contact.Status.VERIFIED) {
        throw new RequestValidationException("Missing field to verify: " + field.getDisplayName());
      }
    }

    contactInternalService.finalizePendingContacts(appt.getId());
    var correctContacts = contactInternalService.getAllContacts(appt.getId());
    appt.setStatus(Appointment.Status.CONFIRMED);
    return new AppointmentFinalResponse(appointmentMapper.toResponse(appt), correctContacts);
  }

  private Appointment getAppointment(Long appointmentId, String slug, Appointment.Status status) {
    return appointmentDao
        .findByIdAndSlugAndStatus(appointmentId, slug, status)
        .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
  }

  @Transactional(readOnly = true)
  public List<FreeOfferingTimeResponse> getFreeOfferingTime(
      String slug, Long addressId, Long offeringId, YearMonth yearMonth) {
    List<ShiftResponse> availableShifts =
        masterClient.getAllShiftsByAddressIdAndYearMonth(slug, addressId, yearMonth);

    if (availableShifts.isEmpty()) {
      return List.of();
    }

    OfferingResponse targetOffering = masterClient.getOfferingBySlugAndId(slug, offeringId);
    long duration = targetOffering.durationMaxMinutes();

    Set<LocalDate> dates =
        availableShifts.stream().map(ShiftResponse::date).collect(Collectors.toSet());
    Map<LocalDate, List<Appointment>> appointmentsByDate =
        appointmentDao
            .findAllByAddressIdAndDateInAndStatusNotIn(
                addressId,
                dates,
                Set.of(
                    Appointment.Status.CANCELLED_BY_GUEST, Appointment.Status.CANCELLED_BY_MASTER))
            .stream()
            .collect(Collectors.groupingBy(Appointment::getDate));

    return availableShifts.stream()
        .collect(Collectors.groupingBy(ShiftResponse::date))
        .entrySet()
        .stream()
        .map(
            entry -> {
              LocalDate date = entry.getKey();
              List<ShiftResponse> shiftsForDate = entry.getValue();
              List<Appointment> dayAppointments = appointmentsByDate.getOrDefault(date, List.of());

              List<FreeOfferingTimeResponse.OfferingTimeRange> offeringShifts =
                  shiftsForDate.stream()
                      .map(
                          shift -> {
                            FTimeRange shiftRange =
                                new FTimeRange(shift.startTime(), shift.endTime());
                            List<FTimeRange> busyInShift = new ArrayList<>();

                            shift
                                .pauses()
                                .forEach(
                                    pause ->
                                        busyInShift.add(
                                            new FTimeRange(pause.startTime(), pause.endTime())));

                            dayAppointments.stream()
                                .filter(
                                    appt ->
                                        FTimeUtil.containsInclusive(
                                            shift.startTime(),
                                            shift.endTime(),
                                            appt.getStartTime(),
                                            appt.getEndTime()))
                                .forEach(
                                    appt ->
                                        busyInShift.add(
                                            new FTimeRange(
                                                appt.getStartTime(), appt.getEndTime())));

                            List<FTimeRange> freeSlots =
                                FTimeUtil.subtractBusyRangesInclusive(
                                        List.of(shiftRange), busyInShift)
                                    .stream()
                                    .flatMap(
                                        range ->
                                            FTimeUtil.splitByStep(
                                                range.start(),
                                                range.end(),
                                                Duration.ofMinutes(duration))
                                                .stream())
                                    .toList();

                            return new FreeOfferingTimeResponse.OfferingTimeRange(
                                shift.id(), freeSlots);
                          })
                      .filter(offeringShift -> !offeringShift.times().isEmpty())
                      .toList();

              return new FreeOfferingTimeResponse(date, offeringShifts);
            })
        .filter(response -> !response.shifts().isEmpty())
        .sorted(Comparator.comparing(FreeOfferingTimeResponse::date))
        .toList();
  }
}
