package com.flysphere.flysphere_backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class BookingRequestDto {

    private Long outboundFlightId;
    private Long returnFlightId;

    private Double totalAmount;

    // ✅ Contact details entered during booking (not account profile)
    private String contactPhone;
    private String contactEmail;

    // ✅ Insurance selection
    private Boolean insuranceSelected;

    // ✅ Trip & Cabin Class Support (clean version - no duplicates)
    private String tripType;              // oneway / round
    private String cabinClass;            // for one-way
    private String outboundCabinClass;    // for round-trip outbound
    private String returnCabinClass;      // for round-trip return

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
