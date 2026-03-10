package com.fransua.appointment.guest.util;

public class PhoneUtil {

  public static String maskPhoneNumber(String phone) {
    if (phone == null || phone.length() < 4) {
      return "****";
    }
    return "*".repeat(phone.length() - 4) + phone.substring(phone.length() - 4);
  }
}
