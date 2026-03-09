package com.fransua.appointment.guest.appointment.service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentScheduler {

  private final AppointmentCleanupService appointmentCleanupService;

  @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
  void processPendingVerifications() {
    log.info("Starting scheduled cleanup of unconfirmed appointments");

    int totalProcessed = 0;
    int batchesCount = 0;
    boolean hasMore = true;

    try {
      while (hasMore) {
        int processedInBatch = appointmentCleanupService.cleanNextBatch();

        if (processedInBatch > 0) {
          totalProcessed += processedInBatch;
          batchesCount++;
          log.debug("Batch {}: cleaned {} appointments", batchesCount, processedInBatch);
        }

        hasMore = processedInBatch > 0;
      }

      if (totalProcessed > 0) {
        log.info(
            "Cleanup finished. Total appointments removed: {} in {} batches",
            totalProcessed,
            batchesCount);
      } else {
        log.info("Cleanup finished. No expired appointments found");
      }
    } catch (Exception ex) {
      log.error("Critical error during appointment cleanup: {}", ex.getMessage(), ex);
    }
  }
}
