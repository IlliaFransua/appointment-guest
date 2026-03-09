package com.fransua.appointment.guest.contact;

import com.fransua.appointment.guest.contact.dto.ContactResponse;
import com.fransua.appointment.guest.contact.dto.PhoneRequest;
import java.util.List;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ContactMapper {

  default Contact toEntity(Long appointmentId, Contact.Status status, PhoneRequest phoneRequest) {
    if (phoneRequest == null) {
      return null;
    }
    return Contact.builder()
        .appointmentId(appointmentId)
        .value(phoneRequest.phone())
        .valueHash(null)
        .status(status)
        .type(Contact.Type.PHONE)
        .build();
  }

  ContactResponse toResponse(Contact contact);

  List<ContactResponse> toResponse(List<Contact> contacts);
}
