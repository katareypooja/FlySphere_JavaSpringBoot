package com.flysphere.flysphere_backend.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "flightmgtable")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flightid")
    private Integer flightId;

    @Column(name = "airlinename")
    private String airlineName;

    @Column(name = "flighttype")
    private String flightType;

    @Column(name = "flightno")
    private String flightNo;

    @Column(name = "departureairport")
    private String departureAirport;

    @Column(name = "arrivalairport")
    private String arrivalAirport;

    @Column(name = "departuredate")
    private LocalDate departureDate;

    @Column(name = "arrivaldate")
    private LocalDate arrivalDate;

    @Column(name = "departuretime")
    private LocalTime departureTime;

    @Column(name = "arrivaltime")
    private LocalTime arrivalTime;

    @Column(name = "totaleconomyseats")
    private Integer totalEconomySeats;

    @Column(name = "totalbusinessseats")
    private Integer totalBusinessSeats;

    @Column(name = "totalfirstclassseats")
    private Integer totalFirstClassSeats;

    @Column(name = "economyadultfare")
    private Double economyAdultFare;

    @Column(name = "economychildfare")
    private Double economyChildFare;

    @Column(name = "businessadultfare")
    private Double businessAdultFare;

    @Column(name = "businesschildfare")
    private Double businessChildFare;

    @Column(name = "firstadultfare")
    private Double firstAdultFare;

    @Column(name = "firstchildfare")
    private Double firstChildFare;

    @Column(name = "flightstatus")
    private String flightStatus;

    @Column(name = "aircraft_type")
    private String aircraftType;
}
