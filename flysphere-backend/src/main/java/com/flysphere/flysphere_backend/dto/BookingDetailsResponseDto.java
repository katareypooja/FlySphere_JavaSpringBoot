package com.flysphere.flysphere_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class BookingDetailsResponseDto {

    private String bookingId;
    private Double totalAmount;
    private String status;

    // ✅ Booking metadata + booker (User) details
    private LocalDateTime bookedOn;
    private String bookedByName;
    private String userEmail;
    private String userPhone;

    // ✅ Fare breakup for Fare Breakdown UI
    private Double baseTotal;
    private Double addonsTotal;
    private Double taxAmount;
    private Double convenienceFee;

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
        private String title;
        private String firstName;
        private String lastName;
        private Integer age;
        private String type;

        // ✅ Outbound
        private String outboundSeatNo;
        private String outboundSeat;
        private String outboundMeal;
        private String outboundBaggage;

        // ✅ Return
        private String returnSeatNo;
        private String returnSeat;
        private String returnMeal;
        private String returnBaggage;

        // ✅ Extras
        private Boolean insuranceSelected;
        private String email;
        private String phone;
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
