package com.flysphere.flysphere_backend.controller;

import com.flysphere.flysphere_backend.model.Booking;
import com.flysphere.flysphere_backend.service.BookingService;
import com.flysphere.flysphere_backend.dto.BookingRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public com.flysphere.flysphere_backend.dto.BookingResponseDto createBooking(@RequestBody BookingRequestDto request) {
        return bookingService.createBooking(request);
    }

    @GetMapping("/user/{userId}")
    public List<Booking> getBookingsByUser(@PathVariable Long userId) {
        return bookingService.getBookingsByUser(userId);
    }

    // ✅ Admin Dashboard - Get All Bookings
    @GetMapping
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    // ✅ Full Booking Details (for Confirmation Page)
    @GetMapping("/{bookingId}")
    public com.flysphere.flysphere_backend.dto.BookingDetailsResponseDto getBookingDetails(
            @PathVariable String bookingId) {
        return bookingService.getBookingDetails(bookingId);
    }
}
