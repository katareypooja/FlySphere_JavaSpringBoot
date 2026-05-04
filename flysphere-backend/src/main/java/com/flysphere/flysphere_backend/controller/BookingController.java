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
    public org.springframework.data.domain.Page<com.flysphere.flysphere_backend.dto.BookingResponseDto> getMyBookings(
            @RequestParam(required = false) String bookingId,
            @RequestParam(required = false) String tripType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            org.springframework.data.domain.Pageable pageable) {

        var page = bookingService.getBookingsForLoggedInUser(
                bookingId,
                tripType,
                status,
                type,
                pageable
        );

        return page.map(booking -> {

            var details = bookingService.getBookingDetails(booking.getBookingId());

            var segments = details.getSegments();

            var outbound = segments.get(0);

            String returnDeparture = null;
            String returnArrival = null;
            java.time.LocalDate returnDate = null;
            java.time.LocalTime returnDepartureTime = null;

            if (segments.size() > 1) {
                var returnSeg = segments.get(1);
                returnDeparture = returnSeg.getDepartureAirport();
                returnArrival = returnSeg.getArrivalAirport();
                returnDate = returnSeg.getDepartureDate();
                returnDepartureTime = returnSeg.getDepartureTime();
            }

            return com.flysphere.flysphere_backend.dto.BookingResponseDto.builder()
                    .bookingId(booking.getBookingId())
                    .totalAmount(booking.getTotalAmount())
                    .status(booking.getStatus())
                    .createdAt(booking.getCreatedAt())
                    .outboundFlightNo(outbound.getFlightNo())

                    .departureAirport(outbound.getDepartureAirport())
                    .arrivalAirport(outbound.getArrivalAirport())
                    .departureDate(outbound.getDepartureDate())
                    .departureTime(outbound.getDepartureTime())

                    .returnDepartureAirport(returnDeparture)
                    .returnArrivalAirport(returnArrival)
                    .returnDate(returnDate)
                    .returnDepartureTime(returnDepartureTime)

                    .tripType(booking.getTripType())
                    .cabinClass(
                        booking.getTripType() != null && booking.getTripType().equalsIgnoreCase("round")
                            ? (booking.getOutboundCabinClass() != null
                                ? booking.getOutboundCabinClass() + " - " + booking.getReturnCabinClass()
                                : booking.getCabinClass())
                            : booking.getCabinClass()
                    )

                    .passengerCount(details.getPassengers().size())
                    .build();
        });
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
