package com.fransua.appointment.guest.appointment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Builder
@Entity
@Table(name = "appointments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
public class Appointment {

  @Id
  @EqualsAndHashCode.Include
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "appointment_id_sequence")
  @SequenceGenerator(
      name = "appointment_id_sequence",
      sequenceName = "appointment_id_sequence",
      allocationSize = 20)
  private Long id;

  @NotNull
  @Column(nullable = false)
  private Long masterId;

  @NotNull
  @Column(nullable = false)
  private String slug;

  // Guest

  @Size(min = 2, max = 50)
  @Column(nullable = true)
  private String guestName;

  @Size(min = 7, max = 15)
  @Pattern(regexp = "^[1-9]\\d{6,14}$")
  @Column(nullable = true, length = 15)
  private String guestPhone; // E.164

  @Size(max = 64)
  @Column(nullable = true, length = 64)
  private String guestPhoneHash;

  @Size(max = 500)
  @Column(nullable = true)
  private String guestPreAppointmentNotes;

  // Snapshot of shift

  @NotNull
  @Column(nullable = false)
  private Long shiftId;

  @NotNull
  @Column(nullable = false)
  private LocalDate date;

  @NotNull
  @Column(nullable = false)
  private LocalTime startTime;

  @NotNull
  @Column(nullable = false)
  private LocalTime endTime;

  // Snapshot of offering

  @NotNull
  @Column(nullable = false)
  private Long offeringId;

  @NotBlank
  @Size(max = 128)
  @Column(nullable = false)
  private String offeringName;

  @Size(max = 1024)
  @Column(nullable = true)
  private String offeringDescription;

  @NotNull
  @PositiveOrZero
  @Column(nullable = false, precision = 12, scale = 2)
  private BigDecimal offeringPrice;

  // Snapshot of address

  @NotNull
  @Column(nullable = false)
  private Long addressId;

  @NotBlank
  @Size(min = 2, max = 2)
  @Pattern(regexp = "^[A-Z]{2}$")
  @Column(length = 2, nullable = false)
  private String addressCountryCode; // ISO 3166-1 alpha-2

  @NotBlank
  @Size(min = 3, max = 3)
  @Pattern(regexp = "^[A-Z]{3}$")
  @Column(length = 3, nullable = false)
  private String addressCurrencyCode; // ISO 4217

  @NotBlank
  @Size(max = 512)
  @Column(length = 512, nullable = false)
  private String addressFull;

  @Size(max = 200)
  @Column(nullable = true, length = 200)
  private String addressDetails;

  @NotBlank
  @Size(max = 50)
  @Column(length = 50, nullable = false)
  private String addressTimezone; // IANA timezone

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Status status = Status.CREATED;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private Instant updatedAt;

  public enum Status {
    CREATED,
    VERIFICATION_CODE_SENT,
    CONFIRMED,
    CANCELLED_BY_GUEST,
    CANCELLED_BY_MASTER,
    NO_SHOW,
    COMPLETED
  }
}
