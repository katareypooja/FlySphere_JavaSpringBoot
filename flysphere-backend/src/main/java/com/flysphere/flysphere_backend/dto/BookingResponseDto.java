package com.flysphere.flysphere_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BookingResponseDto {

    private String bookingId;
    private Double totalAmount;
    private String status;
    private LocalDateTime createdAt;

    private String outboundFlightNo;
    private String returnFlightNo;

    private int passengerCount;
}
