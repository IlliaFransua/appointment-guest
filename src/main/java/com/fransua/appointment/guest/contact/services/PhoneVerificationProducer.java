package com.fransua.appointment.guest.contact.services;

import com.fransua.appointment.guest.appointment.Appointment;
import com.fransua.appointment.guest.config.RabbitConfig;
import com.fransua.appointment.guest.contact.dto.PhoneRequest;
import com.fransua.appointment.guest.contact.event.PhoneVerificationEvent;
import com.fransua.appointment.guest.exception.RequestValidationException;
import com.fransua.appointment.guest.exception.ResourceNotFoundException;
import io.netty.util.internal.ThreadLocalRandom;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
public class PhoneVerificationProducer {

  private final RabbitTemplate rabbitTemplate;

  private final StringRedisTemplate redisTemplate;

  private static final String BASE_PREFIX = "appointment:verify:phone:";

  private static String KEY_IS_VERIFY_DISABLED = "system:status:" + BASE_PREFIX + "disabled";

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

  public void sendVerificationCode(Appointment appointment, @Valid PhoneRequest request) {
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
    sendCode(request, code);
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

  private void sendCode(@Valid PhoneRequest request, String code) {
    rabbitTemplate.convertAndSend(
        RabbitConfig.APPOINTMENT_EXCHANGE,
        RabbitConfig.APPOINTMENT_CONFIRM_PHONE_RK,
        new PhoneVerificationEvent(request.phone(), code));
  }

  private Optional<String> findVerificationCode(Long appointmentId) {
    String key = KEY_OTP_PHONE + appointmentId;
    return Optional.ofNullable(redisTemplate.opsForValue().get(key))
        .filter(code -> !code.isBlank());
  }

  public boolean isPhoneVerificationUnvailable(@Valid PhoneRequest request) {
    if (request.phone().startsWith("380")) {
      return false;
    }
    return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_IS_VERIFY_DISABLED));
  }
}
