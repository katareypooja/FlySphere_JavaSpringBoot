package com.flysphere.flysphere_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "passengers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Passenger extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "title", length = 10)
    private String title;

    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    @Column(name = "age")
    private Integer age;

    @Column(name = "type", length = 10)
    private String type;

    // ✅ Outbound Details
    @Column(name = "outbound_seat_no", length = 20)
    private String outboundSeatNo;

    @Column(name = "outbound_seat", length = 50)
    private String outboundSeat;

    @Column(name = "outbound_meal", length = 50)
    private String outboundMeal;

    @Column(name = "outbound_baggage", length = 50)
    private String outboundBaggage;

    // ✅ Return Details
    @Column(name = "return_seat_no", length = 20)
    private String returnSeatNo;

    @Column(name = "return_seat", length = 50)
    private String returnSeat;

    @Column(name = "return_meal", length = 50)
    private String returnMeal;

    @Column(name = "return_baggage", length = 50)
    private String returnBaggage;

    // ✅ Extras
    @Column(name = "insurance_selected")
    private Boolean insuranceSelected;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone", length = 20)
    private String phone;
}
