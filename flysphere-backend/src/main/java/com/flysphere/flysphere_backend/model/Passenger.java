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

    @Column(name = "seat_preference", length = 20)
    private String seatPreference;

    @Column(name = "meal_preference", length = 20)
    private String mealPreference;

    @Column(name = "baggage")
    private Boolean baggage;
}
