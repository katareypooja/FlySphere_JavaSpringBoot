package com.flysphere.flysphere_backend.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booking_segments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSegment extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "booking_id", referencedColumnName = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "segment_no", nullable = false)
    private Integer segmentNo;

    @ManyToOne
    @JoinColumn(name = "flight_id", nullable = false)
    private Flight flight;
}
