package com.flysphere.flysphere_backend.controller;

import com.flysphere.flysphere_backend.model.Flight;
import com.flysphere.flysphere_backend.service.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/flights")
@RequiredArgsConstructor
public class FlightController {

    private final FlightService flightService;

    // ✅ Create Flight
    @PostMapping
    public Flight createFlight(@RequestBody Flight flight) {
        return flightService.createFlight(flight);
    }

    // ✅ Get Flights with optional filtering
    @GetMapping("")
    public List<Flight> getFlights(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) LocalDate date
    ) {
        return flightService.getFlights(from, to, date);
    }

    // ✅ Get Flight By Id
    @GetMapping("/{id}")
    public Flight getFlightById(@PathVariable Long id) {
        return flightService.getFlightById(id);
    }

    // ✅ Update Flight
    @PutMapping("/{id}")
    public Flight updateFlight(@PathVariable Long id,
                               @RequestBody Flight flight) {
        return flightService.updateFlight(id, flight);
    }

    // ✅ Delete Flight
    @DeleteMapping("/{id}")
    public void deleteFlight(@PathVariable Long id) {
        flightService.deleteFlight(id);
    }
}
