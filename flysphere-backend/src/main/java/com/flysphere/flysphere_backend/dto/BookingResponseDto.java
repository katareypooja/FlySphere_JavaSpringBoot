package com.flysphere.flysphere_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class BookingResponseDto {

    private String bookingId;
    private Double totalAmount;
    private String status;
    private LocalDateTime createdAt;

    private String outboundFlightNo;
    private String returnFlightNo;

    // ✅ Route + Date fields for My Bookings page
    private String departureAirport;
    private String arrivalAirport;
    private java.time.LocalDate departureDate;
    private LocalTime departureTime;

    private String returnDepartureAirport;
    private String returnArrivalAirport;
    private java.time.LocalDate returnDate;
    private LocalTime returnDepartureTime;

    private int passengerCount;

    // ✅ Additional display fields
    private String tripType;
    private String cabinClass;
}
