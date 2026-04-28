package com.flysphere.flysphere_backend.repository;

import com.flysphere.flysphere_backend.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    java.util.Optional<Booking> findByBookingId(String bookingId);

    // ✅ Support multiple bookings with same bookingId (round-trip without DB change)
    List<Booking> findAllByBookingId(String bookingId);

    List<Booking> findByUserId(Long userId);
}
