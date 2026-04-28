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

    // ✅ Logged-in user - Get My Bookings with filtering + pagination
    @GetMapping("/my")
    public org.springframework.data.domain.Page<Booking> getMyBookings(
            @RequestParam(required = false) String bookingId,
            @RequestParam(required = false) String tripType,
            @RequestParam(required = false) String status,
            org.springframework.data.domain.Pageable pageable) {

        return bookingService.getBookingsForLoggedInUser(
                bookingId,
                tripType,
                status,
                pageable
        );
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

    // ✅ Cancel Booking (Seat Restore Logic Included)
    @PutMapping("/{bookingId}/cancel")
    public void cancelBooking(@PathVariable String bookingId) {
        bookingService.cancelBooking(bookingId);
    }
}
