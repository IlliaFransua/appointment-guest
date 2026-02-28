package com.fransua.appointment.guest.appointment;

import com.fransua.appointment.guest.appointment.dao.FreeOfferingTimeResponse;
import com.fransua.appointment.guest.appointment.dto.AppointmentResponse;
import com.fransua.appointment.guest.appointment.dto.CreateAppointmentRequest;
import com.fransua.appointment.guest.master.MasterClient;
import com.fransua.appointment.guest.master.dto.category.CategoryResponse;
import com.fransua.appointment.guest.master.dto.offering.OfferingPageResponse;
import jakarta.validation.Valid;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointment")
@RequiredArgsConstructor
public class AppointmentController {

  private final AppointmentService appointmentService;

  private final MasterClient masterClient;

  @GetMapping("/categories/{slug}")
  @ResponseStatus(HttpStatus.OK)
  public List<CategoryResponse> getCategoriesBySlug(@PathVariable String slug) {
    return masterClient.getAllCategories(slug);
  }

  @GetMapping("/offerings/{slug}")
  @ResponseStatus(HttpStatus.OK)
  public OfferingPageResponse getOfferingsPageByCategory(
      @PathVariable String slug, @RequestParam Long categoryId) {
    return masterClient.getOfferingsPageByCategory(slug, categoryId);
  }

  @GetMapping("/shifts/{slug}")
  @ResponseStatus(HttpStatus.OK)
  List<FreeOfferingTimeResponse> getFreeOfferingTime(
      @PathVariable String slug,
      @RequestParam Long addressId,
      @RequestParam Long offeringId,
      @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth) {
    return appointmentService.getFreeOfferingTime(slug, addressId, offeringId, yearMonth);
  }

  @PostMapping("/appointments/{slug}")
  @ResponseStatus(HttpStatus.CREATED)
  public AppointmentResponse createAppointment(
      @PathVariable String slug, @RequestBody @Valid CreateAppointmentRequest request) {
    return appointmentService.createAppointment(slug, request);
  }

  @PostMapping("/appointments/{slug}/{id}/confirm")
  @ResponseStatus(HttpStatus.OK)
  public void confirmAppointment(
      @PathVariable String slug, @PathVariable Long id, @RequestParam String code) {
    appointmentService.confirmAppointment(slug, id, code);
  }
}
