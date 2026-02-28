package com.fransua.appointment.guest.appointment;

import com.fransua.appointment.guest.appointment.dao.AppointmentDao;
import com.fransua.appointment.guest.appointment.dao.FreeOfferingTimeResponse;
import com.fransua.appointment.guest.appointment.dto.AppointmentResponse;
import com.fransua.appointment.guest.appointment.dto.CreateAppointmentRequest;
import com.fransua.appointment.guest.appointment.service.AppointmentProducer;
import com.fransua.appointment.guest.exception.InvalidAppointmentSlotException;
import com.fransua.appointment.guest.exception.InvalidVerificationCodeException;
import com.fransua.appointment.guest.exception.ResourceLimitExceededException;
import com.fransua.appointment.guest.exception.ResourceNotFoundException;
import com.fransua.appointment.guest.exception.VerificationCodeExpiredException;
import com.fransua.appointment.guest.master.MasterClient;
import com.fransua.appointment.guest.master.dto.booking.BookingContextResponse;
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
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AppointmentService {

  private final AppointmentDao appointmentDao;

  private final MasterClient masterClient;

  private final AppointmentValidator appointmentValidator;

  private final AppointmentMapper appointmentMapper;

  private final AppointmentProducer appointmentProducer;

  private final StringRedisTemplate redisTemplate;

  private final String REDIS_APPOINTMENT_VERIFY_SMS_PREFIX = "appointment:verify:sms:";

  private void saveVerificationCode(Long appointmentId, String code) {
    String key = REDIS_APPOINTMENT_VERIFY_SMS_PREFIX + appointmentId;
    redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(5));
  }

  private Optional<String> getVerificationCodeByAppointmentId(Long appointmentId) {
    String key = REDIS_APPOINTMENT_VERIFY_SMS_PREFIX + appointmentId;
    return Optional.ofNullable(redisTemplate.opsForValue().get(key))
        .filter(code -> !code.isBlank());
  }

  private void deleteVerificationCode(Long appointmentId) {
    String key = REDIS_APPOINTMENT_VERIFY_SMS_PREFIX + appointmentId;
    redisTemplate.delete(key);
  }

  @Transactional
  public void confirmAppointment(String slug, Long appointmentId, String code) {
    Appointment appointment =
        appointmentDao
            .findByIdAndSlug(appointmentId, slug)
            .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

    String savedCode =
        getVerificationCodeByAppointmentId(appointment.getId())
            .orElseThrow(() -> new VerificationCodeExpiredException(appointment.getId()));

    if (!savedCode.equals(code)) {
      throw new InvalidVerificationCodeException(code);
    }

    deleteVerificationCode(appointmentId);
    appointment.setStatus(Appointment.Status.COMPLETED);
  }

  @Transactional
  public AppointmentResponse createAppointment(String slug, CreateAppointmentRequest request) {
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
      appt = appointmentDao.save(appointmentMapper.toEntity(slug, context, request));
      System.out.println(appt);
    } catch (DataIntegrityViolationException e) {
      throw new ResourceLimitExceededException("This slot was just taken by another guest");
    }

    String code = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    saveVerificationCode(appt.getId(), code);
    appointmentProducer.sendSmsVerfication(appt.getGuestPhone(), code);

    return appointmentMapper.toResponse(appt);
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
