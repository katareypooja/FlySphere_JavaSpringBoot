package com.flysphere.flysphere_backend.repository;

import com.flysphere.flysphere_backend.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    List<Flight> findByDepartureAirportAndArrivalAirport(String departureAirport, String arrivalAirport);

    List<Flight> findByDepartureAirportAndArrivalAirportAndDepartureDate(
            String departureAirport,
            String arrivalAirport,
            java.time.LocalDate departureDate
    );

    List<Flight> findByDepartureAirport(String departureAirport);

    List<Flight> findByArrivalAirport(String arrivalAirport);

    List<Flight> findByDepartureDate(java.time.LocalDate departureDate);

    // ✅ Composite duplicate check
    boolean existsByAirlineNameAndFlightTypeAndDepartureAirportAndArrivalAirportAndDepartureDateAndDepartureTime(
            String airlineName,
            String flightType,
            String departureAirport,
            String arrivalAirport,
            java.time.LocalDate departureDate,
            java.time.LocalTime departureTime
    );

    boolean existsByFlightNo(String flightNo);
}
