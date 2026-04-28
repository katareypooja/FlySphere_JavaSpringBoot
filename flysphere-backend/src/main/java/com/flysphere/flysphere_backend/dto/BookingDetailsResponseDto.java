package com.flysphere.flysphere_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class BookingDetailsResponseDto {

    private String bookingId;
    private Double totalAmount;
    private String status;

    // ✅ Trip & Cabin Class Support
    private String tripType;
    private String cabinClass;
    private String outboundCabinClass;
    private String returnCabinClass;

    // ✅ Contact & insurance details for confirmation page
    private String contactPhone;
    private String contactEmail;
    private Boolean insuranceSelected;

    private List<PassengerDto> passengers;
    private List<FlightSegmentDto> segments;

    @Data
    @Builder
    public static class PassengerDto {
        private String firstName;
        private String lastName;
        private Integer age;
        private String type;

        // Optional passenger-level fields if you later need them on confirmation
        private String seatPreference;
        private String mealPreference;
        private String baggage;
        private String outboundSeatNumber;
        private String returnSeatNumber;
    }

    @Data
    @Builder
    public static class FlightSegmentDto {
        private String airlineName;
        private String flightNo;
        private String flightType;     // ✅ From flighttype column
        private String departureAirport;
        private String arrivalAirport;
        private LocalDate departureDate;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
    }
}
