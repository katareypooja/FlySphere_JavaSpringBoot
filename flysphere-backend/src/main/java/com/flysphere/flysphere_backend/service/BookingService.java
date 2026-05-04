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
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final EntityManager entityManager;

    @Transactional
    public com.flysphere.flysphere_backend.dto.BookingResponseDto createBooking(BookingRequestDto request) {

        String email = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));

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

        if ("round".equalsIgnoreCase(request.getTripType()) && returnFlight != null) {

            String outboundCabin = request.getOutboundCabinClass() != null
                    ? request.getOutboundCabinClass()
                    : request.getCabinClass();

            String returnCabin = request.getReturnCabinClass() != null
                    ? request.getReturnCabinClass()
                    : request.getCabinClass();

            decrementSeats(outboundFlight, outboundCabin, seatsToBook);
            decrementSeats(returnFlight, returnCabin, seatsToBook);

        } else {
            decrementSeats(outboundFlight, request.getCabinClass(), seatsToBook);
        }

        String bookingId = generateBookingId();

        Booking booking = Booking.builder()
                .bookingId(bookingId)
                .user(user)
                .flight(outboundFlight)
                .totalAmount(request.getTotalAmount())
                .status("CONFIRMED")
                // ✅ save booking contact + insurance from booking page (not user profile)
                .contactPhone(request.getContactPhone())
                .contactEmail(request.getContactEmail())
                .insuranceSelected(request.getInsuranceSelected())
                .build();

        booking.setTripType(request.getTripType());
        booking.setCabinClass(request.getCabinClass());
        booking.setOutboundCabinClass(request.getOutboundCabinClass());
        booking.setReturnCabinClass(request.getReturnCabinClass());

        entityManager.persist(booking);
        entityManager.flush();

        BookingSegment outboundSegment = BookingSegment.builder()
                .booking(booking)
                .segmentNo(1)
                .flight(outboundFlight)
                .build();

        entityManager.persist(outboundSegment);

        if (returnFlight != null) {
            BookingSegment returnSegment = BookingSegment.builder()
                    .booking(booking)
                    .segmentNo(2)
                    .flight(returnFlight)
                    .build();

            entityManager.persist(returnSegment);
        }

        if (request.getPassengers() != null) {
            for (BookingRequestDto.PassengerDto p : request.getPassengers()) {

                // ✅ Generate seat numbers based on availability + preference
                String outboundSeatNo = generateAvailableSeatNumber(
                        outboundFlight,
                        p.getOutboundSeat(),
                        true
                );

                String returnSeatNo = null;

                if (returnFlight != null) {
                    returnSeatNo = generateAvailableSeatNumber(
                            returnFlight,
                            p.getReturnSeat(), // may be null (as per availability)
                            false
                    );
                }

                Passenger passenger = Passenger.builder()
                        .booking(booking)
                        .title(p.getTitle())
                        .firstName(p.getFirstName())
                        .lastName(p.getLastName())
                        .age(p.getAge())
                        .type(p.getType())

                        // ✅ Outbound
                        .outboundSeatNo(outboundSeatNo)
                        .outboundSeat(p.getOutboundSeat())
                        .outboundMeal(p.getOutboundMeal())
                        .outboundBaggage(p.getOutboundBaggage())

                        // ✅ Return
                        .returnSeatNo(returnSeatNo)
                        .returnSeat(p.getReturnSeat())
                        .returnMeal(p.getReturnMeal())
                        .returnBaggage(p.getReturnBaggage())

                        // ✅ Extras
                        .insuranceSelected(p.getInsuranceSelected())
                        .email(p.getEmail())
                        .phone(p.getPhone())
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

                // ✅ Route + Date mapping
                .departureAirport(outboundFlight.getDepartureAirport())
                .arrivalAirport(outboundFlight.getArrivalAirport())
                .departureDate(outboundFlight.getDepartureDate())

                .returnDepartureAirport(returnFlight != null ? returnFlight.getDepartureAirport() : null)
                .returnArrivalAirport(returnFlight != null ? returnFlight.getArrivalAirport() : null)
                .returnDate(returnFlight != null ? returnFlight.getDepartureDate() : null)

                .passengerCount(seatsToBook)
                .build();
    }

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

    // ✅ Availability-based seat allocation
    private String generateAvailableSeatNumber(Flight flight, String seatPreference, boolean isOutbound) {

        List<String> bookedSeats;

        if (isOutbound) {
            bookedSeats = entityManager.createQuery(
                    "SELECT p.outboundSeatNo FROM Passenger p " +
                            "JOIN BookingSegment bs ON bs.booking = p.booking " +
                            "WHERE bs.flight.flightId = :flightId",
                    String.class
            ).setParameter("flightId", flight.getFlightId())
                    .getResultList();
        } else {
            bookedSeats = entityManager.createQuery(
                    "SELECT p.returnSeatNo FROM Passenger p " +
                            "JOIN BookingSegment bs ON bs.booking = p.booking " +
                            "WHERE bs.flight.flightId = :flightId",
                    String.class
            ).setParameter("flightId", flight.getFlightId())
                    .getResultList();
        }

        String[] windowSeats = {"A", "F"};
        String[] aisleSeats = {"C", "D"};
        String[] middleSeats = {"B", "E"};
        String[] allSeats = {"A", "B", "C", "D", "E", "F"};

        for (int row = 10; row <= 29; row++) {

            String[] seatPool;

            if (seatPreference == null || seatPreference.isBlank()) {
                seatPool = allSeats;
            } else {
                String pref = seatPreference.toLowerCase();
                if (pref.contains("window")) {
                    seatPool = windowSeats;
                } else if (pref.contains("aisle")) {
                    seatPool = aisleSeats;
                } else if (pref.contains("middle")) {
                    seatPool = middleSeats;
                } else {
                    seatPool = allSeats;
                }
            }

            for (String letter : seatPool) {
                String candidate = row + letter;
                if (!bookedSeats.contains(candidate)) {
                    return candidate;
                }
            }
        }

        throw new RuntimeException("No seats available for flight " + flight.getFlightNo());
    }

    private String generateBookingId() {
        return "FS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public List<Booking> getBookingsByUser(Long userId) {
        return bookingRepository.findByUserId(userId);
    }

    // ✅ Get bookings for currently logged-in user with filtering + pagination
    public org.springframework.data.domain.Page<Booking> getBookingsForLoggedInUser(
            String bookingId,
            String tripType,
            String status,
            String type,
            org.springframework.data.domain.Pageable pageable) {

        var authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null) {
            throw new RuntimeException("Authentication is NULL");
        }

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found for email: " + email));

        org.springframework.data.jpa.domain.Specification<Booking> spec =
                (root, query, cb) -> cb.equal(root.get("user").get("id"), user.getId());

        if (bookingId != null && !bookingId.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("bookingId")), "%" + bookingId.toLowerCase() + "%"));
        }

        if (tripType != null && !tripType.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("tripType")), tripType.toLowerCase()));
        }

        if (status != null && !status.isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("status")), status.toLowerCase()));
        }

        // ✅ Upcoming / Past filter (before pagination)
        // Compare using full departure datetime (departureDate + departureTime).
        // Your Flight entity stores:
        //   departureDate: LocalDate
        //   departureTime: LocalTime
        // so we should NOT compare departureDate with LocalDateTime.
        if (type != null && !type.isBlank()) {

            LocalDateTime now = LocalDateTime.now();

            if ("UPCOMING".equalsIgnoreCase(type)) {
                spec = spec.and((root, query, cb) -> {
                    var flight = root.get("flight");
                    var depDate = flight.get("departureDate");   // LocalDate
                    var depTime = flight.get("departureTime");   // LocalTime (nullable)
                    var safeTime = cb.coalesce(depTime, LocalTime.MIDNIGHT);

                    // Build comparable datetime using SQL TIMESTAMP(date, time)
                    var depDateTime = cb.function(
                            "timestamp",
                            LocalDateTime.class,
                            depDate,
                            safeTime
                    );

                    return cb.greaterThan(depDateTime, now);
                });
            }

            if ("PAST".equalsIgnoreCase(type)) {
                spec = spec.and((root, query, cb) -> {
                    var flight = root.get("flight");
                    var depDate = flight.get("departureDate");
                    var depTime = flight.get("departureTime");
                    var safeTime = cb.coalesce(depTime, LocalTime.MIDNIGHT);

                    var depDateTime = cb.function(
                            "timestamp",
                            LocalDateTime.class,
                            depDate,
                            safeTime
                    );

                    return cb.lessThanOrEqualTo(depDateTime, now);
                });
            }
        }

        return bookingRepository.findAll(spec, pageable);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    @Transactional
    public void cancelBooking(String bookingId) {

        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        if ("CANCELLED".equalsIgnoreCase(booking.getStatus())) {
            throw new RuntimeException("Booking is already cancelled");
        }

        // Count passengers
        List<Passenger> passengers = entityManager
                .createQuery("SELECT p FROM Passenger p WHERE p.booking.id = :bookingId", Passenger.class)
                .setParameter("bookingId", booking.getId())
                .getResultList();

        int seatsToRestore = passengers.size();

        // Restore seats for all segments (handles round trip automatically)
        List<BookingSegment> segments = entityManager
                .createQuery("SELECT bs FROM BookingSegment bs WHERE bs.booking.id = :bookingId", BookingSegment.class)
                .setParameter("bookingId", booking.getId())
                .getResultList();

        for (BookingSegment segment : segments) {

            Flight flight = entityManager.find(
                    Flight.class,
                    segment.getFlight().getFlightId(),
                    LockModeType.PESSIMISTIC_WRITE
            );

            String cabinToRestore;

            if (segment.getSegmentNo() == 1) {
                cabinToRestore = booking.getOutboundCabinClass() != null
                        ? booking.getOutboundCabinClass()
                        : booking.getCabinClass();
            } else {
                cabinToRestore = booking.getReturnCabinClass() != null
                        ? booking.getReturnCabinClass()
                        : booking.getCabinClass();
            }

            incrementSeats(flight, cabinToRestore, seatsToRestore);
        }

        booking.setStatus("CANCELLED");
        bookingRepository.save(booking);
    }

    private void incrementSeats(Flight flight, String cabinClass, int seatsToRestore) {

        String cabin = cabinClass != null ? cabinClass.toLowerCase() : "economy";

        switch (cabin) {
            case "business" -> flight.setTotalBusinessSeats(
                    flight.getTotalBusinessSeats() + seatsToRestore
            );
            case "first", "first class" -> flight.setTotalFirstClassSeats(
                    flight.getTotalFirstClassSeats() + seatsToRestore
            );
            default -> flight.setTotalEconomySeats(
                    flight.getTotalEconomySeats() + seatsToRestore
            );
        }
    }

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
                passengers.stream()
                        .map(p -> com.flysphere.flysphere_backend.dto.BookingDetailsResponseDto.PassengerDto.builder()
                                .firstName(p.getFirstName())
                                .lastName(p.getLastName())
                                .age(p.getAge())
                                .type(p.getType())

                                // ✅ Outbound
                                .outboundSeatNo(p.getOutboundSeatNo())
                                .outboundSeat(p.getOutboundSeat())
                                .outboundMeal(p.getOutboundMeal())
                                .outboundBaggage(p.getOutboundBaggage())

                                // ✅ Return
                                .returnSeatNo(p.getReturnSeatNo())
                                .returnSeat(p.getReturnSeat())
                                .returnMeal(p.getReturnMeal())
                                .returnBaggage(p.getReturnBaggage())

                                // ✅ Extras
                                .insuranceSelected(p.getInsuranceSelected())
                                .email(p.getEmail())
                                .phone(p.getPhone())
                                .build())
                        .toList();

        List<com.flysphere.flysphere_backend.dto.BookingDetailsResponseDto.FlightSegmentDto> segmentDtos =
                segments.stream().map(s ->
                        com.flysphere.flysphere_backend.dto.BookingDetailsResponseDto.FlightSegmentDto.builder()
                                .airlineName(s.getFlight().getAirlineName())
                                .flightNo(s.getFlight().getFlightNo())
                                .flightType(s.getFlight().getFlightType())
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
                .tripType(booking.getTripType())
                .cabinClass(booking.getCabinClass())
                .outboundCabinClass(booking.getOutboundCabinClass())
                .returnCabinClass(booking.getReturnCabinClass())
                // ✅ contact + insurance from Booking (values from booking page)
                .contactPhone(booking.getContactPhone())
                .contactEmail(booking.getContactEmail())
                .insuranceSelected(booking.getInsuranceSelected())
                .passengers(passengerDtos)
                .segments(segmentDtos)
                .build();
    }
}
