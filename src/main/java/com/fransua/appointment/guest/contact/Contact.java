package com.fransua.appointment.guest.contact;

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
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
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
@Table(name = "contacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
public class Contact {

  @Id
  @EqualsAndHashCode.Include
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "contact_id_sequence")
  @SequenceGenerator(
      name = "contact_id_sequence",
      sequenceName = "contact_id_sequence",
      allocationSize = 20)
  private Long id;

  @NotNull
  @Column(nullable = false)
  private Long appointmentId;

  @Size(max = 255)
  @Column(nullable = true)
  private String value;

  @Size(max = 64)
  @Column(nullable = true, length = 64)
  private String valueHash; // SHA-256

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Contact.Type type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Contact.Status status;

  @CreatedDate
  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @LastModifiedDate
  @Column(nullable = false)
  private Instant updatedAt;

  public enum Type {
    PHONE,
    EMAIL,
    TELEGRAM,
    WHATSAPP
  }

  public enum Status {
    ATTACHED,
    PENDING_VERIFICATION,
    VERIFIED
  }
}
