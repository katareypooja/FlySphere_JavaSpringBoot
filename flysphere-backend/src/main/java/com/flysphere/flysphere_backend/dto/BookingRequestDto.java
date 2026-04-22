package com.flysphere.flysphere_backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class BookingRequestDto {

    private Long outboundFlightId;
    private Long returnFlightId;

    private Double totalAmount;

    private String tripType;   // oneway / round
    private String cabinClass; // Economy / Business / First

    private List<PassengerDto> passengers;

    @Data
    public static class PassengerDto {
        private String title;
        private String firstName;
        private String lastName;
        private Integer age;
        private String type; // adult / child
        private String seatPreference;
        private String mealPreference;
        private Boolean baggage;
    }
}
