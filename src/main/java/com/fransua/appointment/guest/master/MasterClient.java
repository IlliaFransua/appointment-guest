package com.fransua.appointment.guest.master;

import com.fransua.appointment.guest.master.dto.booking.BookingContextResponse;
import com.fransua.appointment.guest.master.dto.category.CategoryResponse;
import com.fransua.appointment.guest.master.dto.offering.OfferingPageResponse;
import com.fransua.appointment.guest.master.dto.offering.OfferingResponse;
import com.fransua.appointment.guest.master.dto.shift.ShiftResponse;
import com.fransua.appointment.guest.master.feign.MasterClientConfiguration;
import jakarta.validation.constraints.NotNull;
import java.time.YearMonth;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = "master-service",
    url = "${services.master.url}",
    configuration = MasterClientConfiguration.class)
public interface MasterClient {

  @GetMapping("/api/internal/v1/categories/{slug}")
  List<CategoryResponse> getAllCategories(@PathVariable String slug);

  @GetMapping("/api/internal/v1/offerings/{slug}/page")
  OfferingPageResponse getOfferingsPageByCategory(
      @PathVariable String slug, @RequestParam Long categoryId);

  @GetMapping("/api/internal/v1/offerings/{slug}/details")
  OfferingResponse getOfferingBySlugAndId(@PathVariable String slug, @RequestParam Long offeringId);

  @GetMapping("/api/internal/v1/shifts/{slug}")
  List<ShiftResponse> getAllShiftsByAddressIdAndYearMonth(
      @PathVariable String slug,
      @RequestParam Long addressId,
      @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth yearMonth);

  @GetMapping("/api/internal/v1/bookinglink/{slug}")
  BookingContextResponse getBookingContext(
      @PathVariable String slug,
      @RequestParam @NotNull Long addressId,
      @RequestParam @NotNull Long shiftId,
      @RequestParam @NotNull Long offeringId);
}
