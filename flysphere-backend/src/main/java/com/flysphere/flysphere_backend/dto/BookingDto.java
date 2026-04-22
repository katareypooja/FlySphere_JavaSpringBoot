package com.flysphere.flysphere_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDto {

    private Long id;
    private Long userId;
    private Long flightId;
    private Integer seatsBooked;
    private Double totalPrice;
    private String status;
}
