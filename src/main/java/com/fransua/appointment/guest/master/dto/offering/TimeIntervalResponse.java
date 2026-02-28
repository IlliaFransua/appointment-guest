package com.fransua.appointment.guest.master.dto.offering;

import java.time.LocalTime;

public record TimeIntervalResponse(LocalTime start, LocalTime end) {}
