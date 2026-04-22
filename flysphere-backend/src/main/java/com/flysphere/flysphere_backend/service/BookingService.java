package com.flysphere.flysphere_backend.service;

import com.flysphere.flysphere_backend.dto.BookingRequestDto;
import com.flysphere.flysphere_backend.model.*;
import com.flysphere.flysphere_backend.repository.BookingRepository;
import com.flysphere.flysphere_backend.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    /* ✅ FULL PRODUCTION BOOKING LOGIC */
    @Transactional
    public com.flysphere.flysphere_backend.dto.BookingResponseDto createBooking(BookingRequestDto request) {

        // ✅ Extract logged-in user from JWT (SecurityContext)
        String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

        // ✅ Lock outbound flight row (prevents overbooking)
        Flight outboundFlight = entityManager.find(
                Flight.class,
                request.getOutboundFlightId().intValue(),
                LockModeType.PESSIMISTIC_WRITE
        );

        if (outboundFlight == null) {
            throw new RuntimeException("Outbound flight not found");
        }

        Flight returnFlight = null;

        if ("round".equalsIgnoreCase(request.getTripType())
                && request.getReturnFlightId() != null) {

            returnFlight = entityManager.find(
                    Flight.class,
                    request.getReturnFlightId().intValue(),
                    LockModeType.PESSIMISTIC_WRITE
            );

            if (returnFlight == null) {
                throw new RuntimeException("Return flight not found");
            }
        }

        int seatsToBook = request.getPassengers() != null
                ? request.getPassengers().size()
                : 0;

        if (seatsToBook <= 0) {
            throw new RuntimeException("No passengers provided");
        }

        // ✅ Seat decrement logic
        decrementSeats(outboundFlight, request.getCabinClass(), seatsToBook);

        if (returnFlight != null) {
            decrementSeats(returnFlight, request.getCabinClass(), seatsToBook);
        }

        // ✅ Generate Booking ID
        String bookingId = generateBookingId();

        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .user(user)
                .flight(outboundFlight) // keep outbound for backward compatibility
                .totalAmount(request.getTotalAmount())
                .status("CONFIRMED")
                .build();

        // ✅ Persist booking using same EntityManager (important for FK consistency)
        entityManager.persist(booking);
        entityManager.flush(); // Force insert immediately so DB generates ID

        // ✅ Save outbound segment
        BookingSegment outboundSegment = BookingSegment.builder()
                .booking(booking)
                .segmentNo(1)
                .flight(outboundFlight)
                .build();

        entityManager.persist(outboundSegment);

        // ✅ Save return segment (if round trip)
        if (returnFlight != null) {
            BookingSegment returnSegment = BookingSegment.builder()
                    .booking(booking)
                    .segmentNo(2)
                    .flight(returnFlight)
                    .build();

            entityManager.persist(returnSegment);
        }

        // ✅ Save passengers
        if (request.getPassengers() != null) {
            for (BookingRequestDto.PassengerDto p : request.getPassengers()) {

                Passenger passenger = Passenger.builder()
                        .booking(booking)
                        .title(p.getTitle())
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .age(p.getAge())
                        .type(p.getType())
                        .seatPreference(p.getSeatPreference())
                        .mealPreference(p.getMealPreference())
                        .baggage(p.getBaggage())
                        .build();

                entityManager.persist(passenger);
            }
        }

        return com.flysphere.flysphere_backend.dto.BookingResponseDto.builder()
                .bookingId(booking.getBookingId())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .createdAt(booking.getCreatedAt())
                .outboundFlightNo(outboundFlight.getFlightNo())
                .returnFlightNo(returnFlight != null ? returnFlight.getFlightNo() : null)
                .passengerCount(seatsToBook)
                .build();
    }

    /* ✅ Seat decrement with cabin class handling */
    private void decrementSeats(Flight flight, String cabinClass, int seatsToBook) {

        String cabin = cabinClass != null ? cabinClass.toLowerCase() : "economy";

        switch (cabin) {
            case "business" -> {
                if (flight.getTotalBusinessSeats() < seatsToBook) {
                    throw new RuntimeException("Not enough Business seats available");
                }
                flight.setTotalBusinessSeats(
                        flight.getTotalBusinessSeats() - seatsToBook
                );
            }
            case "first", "first class" -> {
                if (flight.getTotalFirstClassSeats() < seatsToBook) {
                    throw new RuntimeException("Not enough First Class seats available");
                }
                flight.setTotalFirstClassSeats(
                        flight.getTotalFirstClassSeats() - seatsToBook
                );
            }
            default -> {
                if (flight.getTotalEconomySeats() < seatsToBook) {
                    throw new RuntimeException("Not enough Economy seats available");
                }
                flight.setTotalEconomySeats(
                        flight.getTotalEconomySeats() - seatsToBook
                );
            }
        }
    }

    /* ✅ Booking ID generator */
    private String generateBookingId() {
        return "FS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    // ✅ Admin - Get All Bookings
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    // ✅ Full Booking Details (for Confirmation Page)
    public com.flysphere.flysphere_backend.dto.BookingDetailsResponseDto getBookingDetails(String bookingId) {

        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        List<Passenger> passengers = entityManager
                .createQuery("SELECT p FROM Passenger p WHERE p.booking.id = :bookingId", Passenger.class)
                .setParameter("bookingId", booking.getId())
                .getResultList();

        List<BookingSegment> segments = entityManager
                .createNativeQuery(
                        "SELECT * FROM booking_segments bs WHERE bs.booking_id = :bookingId ORDER BY bs.segment_no",
                        BookingSegment.class
                )
                .setParameter("bookingId", booking.getBookingId())
                .getResultList();

        List<com.flysphere.flysphere_backend.dto.BookingDetailsResponseDto.PassengerDto> passengerDtos =
                passengers.stream().map(p ->
                        com.flysphere.flysphere_backend.dto.BookingDetailsResponseDto.PassengerDto.builder()
                                .firstName(p.getFirstName())
                                .lastName(p.getLastName())
                                .age(p.getAge())
                                .type(p.getType())
                                .build()
                ).toList();

        List<com.flysphere.flysphere_backend.dto.BookingDetailsResponseDto.FlightSegmentDto> segmentDtos =
                segments.stream().map(s ->
                        com.flysphere.flysphere_backend.dto.BookingDetailsResponseDto.FlightSegmentDto.builder()
                                .airlineName(s.getFlight().getAirlineName())
                                .flightNo(s.getFlight().getFlightNo())
                                .departureAirport(s.getFlight().getDepartureAirport())
                                .arrivalAirport(s.getFlight().getArrivalAirport())
                                .departureDate(s.getFlight().getDepartureDate())
                                .departureTime(s.getFlight().getDepartureTime())
                                .arrivalTime(s.getFlight().getArrivalTime())
                                .build()
                ).toList();

        return com.flysphere.flysphere_backend.dto.BookingDetailsResponseDto.builder()
                .bookingId(booking.getBookingId())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .passengers(passengerDtos)
                .segments(segmentDtos)
                .build();
    }
}
