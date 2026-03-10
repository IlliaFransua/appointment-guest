package com.fransua.appointment.guest.contact.services;

import com.fransua.appointment.guest.appointment.Appointment;
import com.fransua.appointment.guest.config.RabbitConfig;
import com.fransua.appointment.guest.contact.dto.PhoneRequest;
import com.fransua.appointment.guest.contact.event.PhoneVerificationEvent;
import com.fransua.appointment.guest.exception.RequestValidationException;
import com.fransua.appointment.guest.exception.ResourceNotFoundException;
import com.fransua.appointment.guest.util.PhoneUtil;
import io.netty.util.internal.ThreadLocalRandom;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class PhoneVerificationProducer {

  private final RabbitTemplate rabbitTemplate;

  private final StringRedisTemplate redisTemplate;

  private static final String BASE_PREFIX = "appointment:verify:phone:";

  private static final String KEY_SYSTEM_ERROR_COUNT = "system:error:" + BASE_PREFIX + "count";
  private static String KEY_SYSTEM_IS_VERIFY_DISABLED = "system:status:" + BASE_PREFIX + "disabled";

  private static final String SYSTEM_ERROR_WINDOW_TTL = "600"; // 10 min
  private static final String SYSTEM_ERROR_THRESHOLD = "5";
  private static final String SYSTEM_DISABLED_TTL = "300"; // 5 min

  private static final String BASE_LOCK_PREFIX = BASE_PREFIX + "lock:";

  private static final String KEY_OTP_PHONE = BASE_PREFIX + "code:";
  private static final Duration TTL_OTP_PHONE = Duration.ofMinutes(10);

  // Максимум 3 СМС на один `appointmentId` в течение 10 минут
  private static final String KEY_COUNT_SEND_TO_APPT = BASE_PREFIX + "count:send-to-appt:";
  private static final int MAX_SEND_TO_APPT = 3;
  private static final Duration TTL_SEND_TO_APPT = Duration.ofMinutes(10);

  // 1 попытка в 60 секунд на один `appointmentId`
  private static final String KEY_COUNT_RESEND_TO_APPT = BASE_PREFIX + "count:resend-to-appt:";
  private static final int MAX_RESEND_TO_APPT = 1;
  private static final Duration TTL_RESEND_TO_APPT = Duration.ofSeconds(60);

  // Максимум 5 СМС на конкретный номер телефона за 3 часа
  private static final String KEY_COUNT_SEND_TO_PHONE = BASE_PREFIX + "count:send-to-phone:";
  private static final int MAX_SEND_TO_PHONE = 5;
  private static final Duration TTL_SEND_TO_PHONE = Duration.ofHours(3);

  // Максимум 5 попыток проверки кода на один сгенерированный OTP
  private static final String KEY_COUNT_CHECK_CODE = BASE_PREFIX + "count:check-code:";
  private static final int MAX_CHECK_CODE = 5;
  private static final Duration TTL_CHECK_CODE = TTL_OTP_PHONE;

  private static final RedisScript<Long> INCR_EXPIRE_SCRIPT =
      new DefaultRedisScript<>(
          "local attemptsCount = redis.call('INCR', KEYS[1]) "
              + "if attemptsCount == 1 then "
              + "  redis.call('EXPIRE', KEYS[1], ARGV[1]) "
              + "end "
              + "return attemptsCount",
          Long.class);

  private static final DefaultRedisScript<Long> SYSTEM_FAULT_BREAKER_SCRIPT =
      new DefaultRedisScript<>(
          "local count = redis.call('INCR', KEYS[1]) "
              + "if tonumber(count) == 1 then "
              + "  redis.call('EXPIRE', KEYS[1], ARGV[1]) "
              + "end "
              + "if tonumber(count) >= tonumber(ARGV[2]) then "
              + "  redis.call('SET', KEYS[2], 'true', 'EX', ARGV[3]) "
              + "  redis.call('DEL', KEYS[1]) "
              + "  return 1 "
              + "end "
              + "return 0",
          Long.class);

  public void sendVerificationCode(Appointment appointment, @Valid PhoneRequest request) {
    String lockKey = BASE_LOCK_PREFIX + appointment.getId();
    Boolean acquired =
        redisTemplate.opsForValue().setIfAbsent(lockKey, "locked", Duration.ofSeconds(10));

    if (Boolean.FALSE.equals(acquired)) {
      throw new RequestValidationException("Another request is being processed. Please wait.");
    }

    String phone = request.phone();
    Long apptId = appointment.getId();

    checkRateLimit(
        KEY_COUNT_SEND_TO_APPT + apptId,
        MAX_SEND_TO_APPT,
        TTL_SEND_TO_APPT,
        "Too many phone attempts for this appointment");

    checkRateLimit(
        KEY_COUNT_SEND_TO_PHONE + phone,
        MAX_SEND_TO_PHONE,
        TTL_SEND_TO_PHONE,
        "Too many messages sent to this phone number. Try later");

    String code = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    saveCode(apptId, code);
    sendCode(request, code, appointment.getMasterId());
  }

  public void resendVerificationCode(Appointment appointment, @Valid PhoneRequest request) {
    Long apptId = appointment.getId();

    checkRateLimit(
        KEY_COUNT_RESEND_TO_APPT + apptId,
        MAX_RESEND_TO_APPT,
        TTL_RESEND_TO_APPT,
        "Too many resends for this appointment");

    redisTemplate.delete(List.of(KEY_COUNT_CHECK_CODE + apptId));
    sendVerificationCode(appointment, request);
  }

  public void verifyCode(Long appointmentId, String inputCode) {
    String codeKey = KEY_OTP_PHONE + appointmentId;
    String checkCountKey = KEY_COUNT_CHECK_CODE + appointmentId;

    Long attemptsCount =
        redisTemplate.execute(
            INCR_EXPIRE_SCRIPT,
            Collections.singletonList(checkCountKey),
            String.valueOf(TTL_CHECK_CODE.toSeconds()));

    if (attemptsCount != null && attemptsCount >= MAX_CHECK_CODE) {
      redisTemplate.delete(List.of(codeKey, checkCountKey));
      throw new RequestValidationException(
          "Too many wrong code attempts. Please request a new SMS");
    }

    String savedCode =
        findVerificationCode(appointmentId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Verification code expired or invalidated"));

    if (!savedCode.equals(inputCode)) {
      long left = MAX_CHECK_CODE - attemptsCount;
      throw new RequestValidationException("Invalid verification code. Attempts left: " + left);
    }

    clearAllApptLimits(appointmentId);
  }

  // util

  private void checkRateLimit(String key, int maxCount, Duration duration, String errorMessage) {
    Long attemptsCount =
        redisTemplate.execute(
            INCR_EXPIRE_SCRIPT,
            Collections.singletonList(key),
            String.valueOf(duration.toSeconds()));

    if (attemptsCount != null && attemptsCount > maxCount) {
      throw new RequestValidationException(errorMessage);
    }
  }

  public void clearAllApptLimits(Long appointmentId) {
    redisTemplate.delete(
        List.of(
            KEY_OTP_PHONE + appointmentId,
            KEY_COUNT_CHECK_CODE + appointmentId,
            KEY_COUNT_SEND_TO_APPT + appointmentId,
            KEY_COUNT_RESEND_TO_APPT + appointmentId));
  }

  private void saveCode(Long appointmentId, String code) {
    String key = KEY_OTP_PHONE + appointmentId;
    redisTemplate.opsForValue().set(key, code, TTL_OTP_PHONE);
  }

  private void sendCode(@Valid PhoneRequest request, String code, Long masterId) {
    rabbitTemplate.convertAndSend(
        RabbitConfig.APPOINTMENT_EXCHANGE,
        RabbitConfig.APPOINTMENT_CONFIRM_PHONE_RK,
        new PhoneVerificationEvent(request.phone(), code, masterId));
  }

  private Optional<String> findVerificationCode(Long appointmentId) {
    String key = KEY_OTP_PHONE + appointmentId;
    return Optional.ofNullable(redisTemplate.opsForValue().get(key))
        .filter(code -> !code.isBlank());
  }

  private static final Set<String> SUPPORTED_COUNTRY_CODES =
      Set.of(
          "43", // Austria
          "420", // Czech Republic
          "372", // Estonia
          "33", // France
          "49", // Germany
          "30", // Greece
          "36", // Hungary
          "39", // Italy
          "371", // Latvia
          "423", // Liechtenstein
          "370", // Lithuania
          "47", // Norway
          "48", // Poland
          "40", // Romania
          "34", // Spain
          "46", // Sweden
          "41", // Switzerland
          "971", // United Arab Emirates
          "44", // United Kingdom
          "1" // United States
          );

  public boolean isVerificationSystemDisabled() {
    return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_SYSTEM_IS_VERIFY_DISABLED));
  }

  public boolean isPhoneVerificationUnvailable(@Valid PhoneRequest request) {
    if (isVerificationSystemDisabled()) {
      return true;
    }

    String phone = request.phone();
    boolean isSupported = SUPPORTED_COUNTRY_CODES.stream().anyMatch(phone::startsWith);

    if (!isSupported) {
      log.warn(
          "Phone verification blocked: country code not supported for number {}",
          PhoneUtil.maskPhoneNumber(phone));
      return true;
    }
    return false;
  }

  public void recordSystemFault() {
    try {
      Long result =
          redisTemplate.execute(
              SYSTEM_FAULT_BREAKER_SCRIPT,
              List.of(KEY_SYSTEM_ERROR_COUNT, KEY_SYSTEM_IS_VERIFY_DISABLED),
              SYSTEM_ERROR_WINDOW_TTL,
              SYSTEM_ERROR_THRESHOLD,
              SYSTEM_DISABLED_TTL);

      if (Long.valueOf(1).equals(result)) {
        log.error(
            "System threshold reached ({} errors). Phone verification disabled for {}s",
            SYSTEM_ERROR_THRESHOLD,
            SYSTEM_DISABLED_TTL);
      }
    } catch (Exception e) {
      log.error("Failed to execute System Fault Breaker script", e);
    }
  }
}
