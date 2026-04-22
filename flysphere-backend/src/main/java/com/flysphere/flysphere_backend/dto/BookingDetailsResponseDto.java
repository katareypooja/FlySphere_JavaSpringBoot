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

    private List<PassengerDto> passengers;
    private List<FlightSegmentDto> segments;

    @Data
    @Builder
    public static class PassengerDto {
        private String firstName;
        private String lastName;
        private Integer age;
        private String type;
    }

    @Data
    @Builder
    public static class FlightSegmentDto {
        private String airlineName;
        private String flightNo;
        private String departureAirport;
        private String arrivalAirport;
        private LocalDate departureDate;
        private LocalTime departureTime;
        private LocalTime arrivalTime;
    }
}
