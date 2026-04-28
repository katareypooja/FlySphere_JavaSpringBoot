package com.flysphere.flysphere_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {

    @Column(name = "booking_id", length = 20, nullable = false)
    private String bookingId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "status", length = 20)
    private String status;

    // ✅ Trip & Cabin Class Support
    @Column(name = "trip_type")
    private String tripType;

    @Column(name = "cabin_class")
    private String cabinClass;

    @Column(name = "outbound_cabin_class")
    private String outboundCabinClass;

    @Column(name = "return_cabin_class")
    private String returnCabinClass;

    // ✅ Contact phone entered during booking (not account phone)
    @Column(name = "contact_phone")
    private String contactPhone;

    // ✅ Contact email entered during booking (not user account email)
    @Column(name = "contact_email")
    private String contactEmail;

    // ✅ Whether insurance was selected for this booking
    @Column(name = "insurance_selected")
    private Boolean insuranceSelected;
}
