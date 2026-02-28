package com.fransua.appointment.guest.master.dto.category;

import lombok.Builder;

@Builder
public record CategoryResponse(Long id, String name, String icon, CategoryColor color) {}
