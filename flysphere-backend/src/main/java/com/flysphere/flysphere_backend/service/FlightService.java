package com.flysphere.flysphere_backend.service;

import com.flysphere.flysphere_backend.model.Flight;
import com.flysphere.flysphere_backend.repository.FlightRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FlightService {

    private final FlightRepository flightRepository;

    // ✅ Create Flight
    public Flight createFlight(Flight flight) {

        // ✅ Block creation ONLY if:
        // Airline + FlightType + DepartureAirport + ArrivalAirport
        // + DepartureDate + DepartureTime are ALL identical
        boolean duplicateFlight = flightRepository
                .existsByAirlineNameAndFlightTypeAndDepartureAirportAndArrivalAirportAndDepartureDateAndDepartureTime(
                        flight.getAirlineName(),
                        flight.getFlightType(),
                        flight.getDepartureAirport(),
                        flight.getArrivalAirport(),
                        flight.getDepartureDate(),
                        flight.getDepartureTime()
                );

        if (duplicateFlight) {
            throw new RuntimeException("Same flight already exists with identical details");
        }

        // ✅ Default status to Scheduled if not provided
        if (flight.getFlightStatus() == null || flight.getFlightStatus().isBlank()) {
            flight.setFlightStatus("Scheduled");
        }

        return flightRepository.save(flight);
    }

    // ✅ Get Flights with optional filtering + lifecycle update
    public List<Flight> getFlights(String from, String to, LocalDate date) {

        List<Flight> flights;

        if (from != null && to != null && date != null) {
            flights = flightRepository
                    .findByDepartureAirportAndArrivalAirportAndDepartureDate(from, to, date);
        } else if (from != null && to != null) {
            flights = flightRepository
                    .findByDepartureAirportAndArrivalAirport(from, to);
        } else if (from != null) {
            flights = flightRepository.findByDepartureAirport(from);
        } else if (to != null) {
            flights = flightRepository.findByArrivalAirport(to);
        } else if (date != null) {
            flights = flightRepository.findByDepartureDate(date);
        } else {
            flights = flightRepository.findAll(
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC,
                    "flightId"
                )
            );
        }

        // ✅ Lifecycle status update
        for (Flight flight : flights) {
            updateLifecycleStatus(flight);
        }

        return flights;
    }

    // ✅ Get Flight By Id
    public Flight getFlightById(Long id) {
        return flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found"));
    }

    // ✅ Update Flight
    public Flight updateFlight(Long id, Flight updatedFlight) {

        Flight existing = getFlightById(id);

        existing.setAirlineName(updatedFlight.getAirlineName());
        existing.setFlightType(updatedFlight.getFlightType());
        existing.setFlightNo(updatedFlight.getFlightNo());
        existing.setDepartureAirport(updatedFlight.getDepartureAirport());
        existing.setArrivalAirport(updatedFlight.getArrivalAirport());
        existing.setDepartureDate(updatedFlight.getDepartureDate());
        existing.setArrivalDate(updatedFlight.getArrivalDate());
        existing.setDepartureTime(updatedFlight.getDepartureTime());
        existing.setArrivalTime(updatedFlight.getArrivalTime());
        existing.setTotalEconomySeats(updatedFlight.getTotalEconomySeats());
        existing.setTotalBusinessSeats(updatedFlight.getTotalBusinessSeats());
        existing.setTotalFirstClassSeats(updatedFlight.getTotalFirstClassSeats());
        existing.setEconomyAdultFare(updatedFlight.getEconomyAdultFare());
        existing.setEconomyChildFare(updatedFlight.getEconomyChildFare());
        existing.setBusinessAdultFare(updatedFlight.getBusinessAdultFare());
        existing.setBusinessChildFare(updatedFlight.getBusinessChildFare());
        existing.setFirstAdultFare(updatedFlight.getFirstAdultFare());
        existing.setFirstChildFare(updatedFlight.getFirstChildFare());
        existing.setFlightStatus(updatedFlight.getFlightStatus());
        existing.setAircraftType(updatedFlight.getAircraftType());

        return flightRepository.save(existing);
    }

    // ✅ Delete Flight
    public void deleteFlight(Long id) {
        flightRepository.deleteById(id);
    }

    // ✅ Lifecycle Logic (Matches Node.js)
    private void updateLifecycleStatus(Flight flight) {

        String currentStatus = flight.getFlightStatus();

        if (currentStatus == null) return;

        // ✅ Skip manual statuses
        if (currentStatus.equalsIgnoreCase("Cancelled") ||
                currentStatus.equalsIgnoreCase("Delayed") ||
                currentStatus.equalsIgnoreCase("Rescheduled")) {
            return;
        }

        if (flight.getDepartureDate() == null ||
                flight.getArrivalDate() == null ||
                flight.getDepartureTime() == null ||
                flight.getArrivalTime() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        LocalDateTime departureDateTime =
                LocalDateTime.of(flight.getDepartureDate(), flight.getDepartureTime());

        LocalDateTime arrivalDateTime =
                LocalDateTime.of(flight.getArrivalDate(), flight.getArrivalTime());

        String calculatedStatus = currentStatus;

        if (currentStatus.equalsIgnoreCase("Scheduled")) {

            if (now.isAfter(departureDateTime) && now.isBefore(arrivalDateTime)) {
                calculatedStatus = "Departed";
            } else if (now.isAfter(arrivalDateTime)) {
                calculatedStatus = "Completed";
            }

        } else if (currentStatus.equalsIgnoreCase("Departed")) {

            if (now.isAfter(arrivalDateTime)) {
                calculatedStatus = "Completed";
            }
        }

        if (!calculatedStatus.equalsIgnoreCase(currentStatus)) {
            flight.setFlightStatus(calculatedStatus);
            flightRepository.save(flight);
        }
    }
}
