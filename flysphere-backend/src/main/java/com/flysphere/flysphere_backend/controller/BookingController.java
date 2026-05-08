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

            java.time.LocalDate returnArrivalDate = null;
            java.time.LocalTime returnArrivalTime = null;

            String returnAirlineName = null;
            String returnFlightType = null;

            if (segments.size() > 1) {
                var returnSeg = segments.get(1);
                returnDeparture = returnSeg.getDepartureAirport();
                returnArrival = returnSeg.getArrivalAirport();
                returnDate = returnSeg.getDepartureDate();
                returnDepartureTime = returnSeg.getDepartureTime();

                returnArrivalDate = returnSeg.getArrivalDate();
                returnArrivalTime = returnSeg.getArrivalTime();

                returnAirlineName = returnSeg.getAirlineName();
                returnFlightType = returnSeg.getFlightType();
            }

            var passengers = details.getPassengers().stream()
                    .map(p -> com.flysphere.flysphere_backend.dto.BookingResponseDto.PassengerSummaryDto.builder()
                            .firstName(p.getFirstName())
                            .lastName(p.getLastName())
                            .type(p.getType())
                            .age(p.getAge())

                            // Outbound add-ons
                            .outboundSeatNo(p.getOutboundSeatNo())
                            .outboundSeat(p.getOutboundSeat())
                            .outboundMeal(p.getOutboundMeal())
                            .outboundBaggage(p.getOutboundBaggage())

                            // Return add-ons
                            .returnSeatNo(p.getReturnSeatNo())
                            .returnSeat(p.getReturnSeat())
                            .returnMeal(p.getReturnMeal())
                            .returnBaggage(p.getReturnBaggage())
                            .build())
                    .toList();

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
                    .arrivalDate(outbound.getArrivalDate())
                    .arrivalTime(outbound.getArrivalTime())

                    .returnDepartureAirport(returnDeparture)
                    .returnArrivalAirport(returnArrival)
                    .returnDate(returnDate)
                    .returnDepartureTime(returnDepartureTime)
                    .returnArrivalDate(returnArrivalDate)
                    .returnArrivalTime(returnArrivalTime)

                    .outboundAirlineName(outbound.getAirlineName())
                    .outboundFlightType(outbound.getFlightType())
                    .returnAirlineName(returnAirlineName)
                    .returnFlightType(returnFlightType)

                    .tripType(booking.getTripType())
                    .cabinClass(booking.getCabinClass())
                    .outboundCabinClass(booking.getOutboundCabinClass())
                    .returnCabinClass(booking.getReturnCabinClass())

                    .passengerCount(details.getPassengers().size())
                    .passengers(passengers)
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
