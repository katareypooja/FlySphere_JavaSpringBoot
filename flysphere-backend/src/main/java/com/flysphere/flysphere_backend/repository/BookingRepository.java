package com.flysphere.flysphere_backend.repository;

import com.flysphere.flysphere_backend.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    java.util.Optional<Booking> findByBookingId(String bookingId);

    List<Booking> findByUserId(Long userId);
}
