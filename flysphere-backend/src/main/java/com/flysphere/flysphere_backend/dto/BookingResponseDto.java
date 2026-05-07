package com.flysphere.flysphere_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class BookingResponseDto {

    @Data
    @Builder
    public static class PassengerSummaryDto {
        private String firstName;
        private String lastName;
        private String type; // ADULT / CHILD / INFANT
    }

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

    // ✅ Display: Airline + Flight Type (My Bookings)
    private String outboundAirlineName;
    private String outboundFlightType;
    private String returnAirlineName;
    private String returnFlightType;

    private int passengerCount;

    // ✅ Passenger list for expandable UI (My Bookings)
    private List<PassengerSummaryDto> passengers;

    // ✅ Additional display fields
    private String tripType;

    // One-way cabin
    private String cabinClass;

    // Round-trip cabin (more specific)
    private String outboundCabinClass;
    private String returnCabinClass;
}
