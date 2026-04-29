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

        // ✅ Outbound Details
        private String outboundSeatNo;
        private String outboundSeat;
        private String outboundMeal;
        private String outboundBaggage;

        // ✅ Return Details
        private String returnSeatNo;
        private String returnSeat;
        private String returnMeal;
        private String returnBaggage;

        // ✅ Extras
        private Boolean insuranceSelected;
        private String email;
        private String phone;
    }
}
