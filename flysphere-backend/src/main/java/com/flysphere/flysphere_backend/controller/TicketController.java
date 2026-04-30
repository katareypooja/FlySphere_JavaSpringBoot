package com.flysphere.flysphere_backend.controller;

import com.flysphere.flysphere_backend.model.Booking;
import com.flysphere.flysphere_backend.model.Flight;
import com.flysphere.flysphere_backend.model.User;
import com.flysphere.flysphere_backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class TicketController {

    private final BookingRepository bookingRepository;
    private final jakarta.persistence.EntityManager entityManager;

    @GetMapping("/{bookingId}/ticket")
    public ResponseEntity<byte[]> generateTicket(@PathVariable String bookingId) throws Exception {

        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        User user = booking.getUser();

        java.util.List<com.flysphere.flysphere_backend.model.BookingSegment> segments =
                entityManager.createQuery(
                        "SELECT bs FROM BookingSegment bs WHERE bs.booking.id = :id ORDER BY bs.segmentNo",
                        com.flysphere.flysphere_backend.model.BookingSegment.class
                )
                .setParameter("id", booking.getId())
                .getResultList();

        PDDocument document = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);

        PDPageContentStream content = new PDPageContentStream(document, page);

        float width = page.getMediaBox().getWidth();
        float height = page.getMediaBox().getHeight();
        float margin = 65;
        float usableWidth = width - (margin * 2);
        float y = height - 75;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        /* ================= SOFT GRADIENT BACKGROUND ================= */
        content.setNonStrokingColor(new Color(242, 247, 255));
        content.addRect(0, 0, width, height);
        content.fill();
        content.setNonStrokingColor(Color.BLACK);

        /* ================= WATERMARK ================= */
        content.saveGraphicsState();
        content.setNonStrokingColor(new Color(200, 220, 255)); // light blue watermark
        content.setFont(PDType1Font.HELVETICA_BOLD, 90);

        content.beginText();
        content.setTextMatrix(
                org.apache.pdfbox.util.Matrix.getRotateInstance(
                        Math.toRadians(45),
                        width / 4,
                        height / 3
                )
        );
        content.showText("FlySphere");
        content.endText();

        content.restoreGraphicsState();

        /* ================= HEADER BAND ================= */
        content.setNonStrokingColor(new Color(214, 228, 248));
        content.addRect(0, height - 115, width, 115);
        content.fill();
        content.setNonStrokingColor(Color.BLACK);

        // Brand Header with Flight Icon + Blue Color (#2563eb)
        content.setNonStrokingColor(new Color(37, 99, 235));
        content.setFont(PDType1Font.HELVETICA_BOLD, 26);

        // FlySphere text (Unicode removed to prevent PDFBox font error)
        write(content, "FlySphere", margin, y);

        content.setNonStrokingColor(new Color(100, 110, 120)); // Soft grey subtitle
        content.setFont(PDType1Font.HELVETICA, 12);
        write(content, "Secure Booking", margin + 2, y - 18);

        // Reset color for rest
        content.setNonStrokingColor(Color.BLACK);

        content.setFont(PDType1Font.HELVETICA_BOLD, 16);
        write(content, "E-Ticket", width / 2 - 40, y - 2);

        content.setFont(PDType1Font.HELVETICA_BOLD, 12);
        write(content, "Booking Ref", width - 170, y + 5);
        content.setFont(PDType1Font.HELVETICA_BOLD, 15);
        write(content, booking.getBookingId(), width - 170, y - 15);

        y -= 50;
        y -= 35;

        /* ================= BOOKING INFORMATION ================= */
        content.setNonStrokingColor(new Color(37, 99, 235));
        content.setFont(PDType1Font.HELVETICA_BOLD, 17);
        write(content, "Booking Information", margin, y);
        content.setNonStrokingColor(Color.BLACK);
        y -= 12;
        drawSoftLine(content, margin, y, width - margin);   // ✅ line below Booking Information
        y -= 25;

        // ✅ Fully balanced layout grid (clean proportional spacing across ticket)
        float col1 = margin;                          // 0%
        float col2 = margin + usableWidth * 0.22f;    // 22%
        float col3 = margin + usableWidth * 0.45f;    // 45%
        float col4 = margin + usableWidth * 0.68f;    // 68%
        float col5 = margin + usableWidth * 0.88f;    // 88% (Seat No / right aligned content)

        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        write(content, "Booking ID", col1, y);
        write(content, "Account Owner", col2, y);
        write(content, "Email", col3, y);
        write(content, "Status", col4, y);

        y -= 20;

        content.setFont(PDType1Font.HELVETICA, 11);
        write(content, trim(booking.getBookingId(), 16), col1, y);
        write(content, trim(user.getFirstName() + " " + user.getLastName(), 20), col2, y);
        write(content, trim(user.getEmail(), 24), col3, y);
        write(content, trim(booking.getStatus(), 14), col4, y);

        y -= 40;

        /* ================= FLIGHT DETAILS ================= */
        content.setNonStrokingColor(new Color(37, 99, 235));
        content.setFont(PDType1Font.HELVETICA_BOLD, 17);
        write(content, "Flight Details", margin, y);
        content.setNonStrokingColor(Color.BLACK);
        y -= 12;
        drawSoftLine(content, margin, y, width - margin);
        y -= 25;

        // ✅ Flight rows handled dynamically below

        double combinedTotal = booking.getTotalAmount();

        for (int i = 0; i < segments.size(); i++) {

            com.flysphere.flysphere_backend.model.BookingSegment segment = segments.get(i);
            Flight flight = segment.getFlight();

            if (segments.size() > 1) {
                content.setFont(PDType1Font.HELVETICA_BOLD, 13);
                write(content, (i == 0 ? "Outbound Flight" : "Return Flight"), col1, y);
                y -= 15;
                drawSoftLine(content, col1, y, width - margin);
                y -= 18;
            }

            /* ✅ ROW 1: From | Airline | Aircraft | Seat Type */
            content.setFont(PDType1Font.HELVETICA_BOLD, 11);
            write(content, "From", col1, y);
            write(content, "Airline", col2, y);
            write(content, "Aircraft", col3, y);
            write(content, "Seat Type", col4, y);

            y -= 16;
            content.setFont(PDType1Font.HELVETICA, 11);

            String seatType;

            if ("round".equalsIgnoreCase(booking.getTripType())) {
                seatType = (i == 0)
                        ? booking.getOutboundCabinClass()
                        : booking.getReturnCabinClass();
            } else {
                seatType = booking.getCabinClass();
            }

            write(content, trim(flight.getDepartureAirport(), 14), col1, y);
            write(content, trim(flight.getAirlineName(), 20), col2, y);
            write(content,
                    trim(flight.getFlightType() != null ? flight.getFlightType() : "", 20),
                    col3, y);
            write(content,
                    trim(seatType != null ? seatType : "", 16),
                    col4, y);

            // ✅ Seat number removed from flight section (now shown in passenger section)

            y -= 22;

            /* ✅ ROW 2: To | Flight No | Departure | Arrival */
            content.setFont(PDType1Font.HELVETICA_BOLD, 11);
            write(content, "To", col1, y);
            write(content, "Flight No", col2, y);
            write(content, "Departure", col3, y);
            write(content, "Arrival", col4, y);

            y -= 16;
            content.setFont(PDType1Font.HELVETICA, 11);

            write(content, trim(flight.getArrivalAirport(), 14), col1, y);
            write(content, trim(flight.getFlightNo(), 14), col2, y);
            write(content,
                    trim(flight.getDepartureDate().format(formatter) + " " + flight.getDepartureTime(), 24),
                    col3, y);
            write(content,
                    trim(flight.getArrivalDate().format(formatter) + " " + flight.getArrivalTime(), 24),
                    col4, y);

            y -= 18;

            // ✅ Removed separate seat block (now handled in 5th column above)

            y -= 28;

            // spacing between stacked segments
            if (segments.size() > 1 && i < segments.size() - 1) {
                y -= 10;
            }
        }

        /* ================= PASSENGER DETAILS ================= */
        y -= 10;
        content.setNonStrokingColor(new Color(37, 99, 235));
        content.setFont(PDType1Font.HELVETICA_BOLD, 17);
        write(content, "Passenger Details", margin, y);
        content.setNonStrokingColor(Color.BLACK);
        y -= 12;
        drawSoftLine(content, margin, y, width - margin);
        y -= 20;

        java.util.List<com.flysphere.flysphere_backend.model.Passenger> passengerList =
                entityManager.createQuery(
                        "SELECT p FROM Passenger p WHERE p.booking.id = :id",
                        com.flysphere.flysphere_backend.model.Passenger.class
                )
                .setParameter("id", booking.getId())
                .getResultList();

        boolean isRound = "round".equalsIgnoreCase(booking.getTripType());

        float pCol1 = col1;                                   // Name
        float pCol2 = margin + usableWidth * 0.18f;           // Age
        float pCol3 = margin + usableWidth * 0.28f;           // Type
        float pCol4 = margin + usableWidth * 0.40f;           // Seat Outbound
        float pCol5 = margin + usableWidth * 0.62f;           // Seat Return (only round)
        float pCol6 = margin + usableWidth * (isRound ? 0.82f : 0.70f); // Contact shifts if one-way

        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        write(content, "Name", pCol1, y);
        write(content, "Age", pCol2, y);
        write(content, "Type", pCol3, y);
        write(content, "Seat (Outbound)", pCol4, y);

        if (isRound) {
            write(content, "Seat (Return)", pCol5, y);
        }

        write(content, "Contact No", pCol6, y);

        y -= 16;
        content.setFont(PDType1Font.HELVETICA, 11);

        for (com.flysphere.flysphere_backend.model.Passenger p : passengerList) {

            write(content, trim(p.getFirstName() + " " + p.getLastName(), 20), pCol1, y);
            write(content, String.valueOf(p.getAge()), pCol2, y);
            write(content, p.getType(), pCol3, y);

            String outboundSeatDisplay = "";
            if (p.getOutboundSeat() != null && p.getOutboundSeatNo() != null) {
                outboundSeatDisplay = p.getOutboundSeat() + " (" + p.getOutboundSeatNo() + ")";
            } else if (p.getOutboundSeatNo() != null) {
                outboundSeatDisplay = "*(" + p.getOutboundSeatNo() + ")";
            }

            write(content, trim(outboundSeatDisplay, 20), pCol4, y);

            if (isRound) {
                String returnSeatDisplay = "";
                if (p.getReturnSeat() != null && p.getReturnSeatNo() != null) {
                    returnSeatDisplay = p.getReturnSeat() + " (" + p.getReturnSeatNo() + ")";
                } else if (p.getReturnSeatNo() != null) {
                    returnSeatDisplay = "*(" + p.getReturnSeatNo() + ")";
                }
                write(content, trim(returnSeatDisplay, 20), pCol5, y);
            }

            write(content, trim(p.getPhone(), 15), pCol6, y);

            y -= 16;
        }

        y -= 25;

        /* ================= FARE BREAKDOWN ================= */
        content.setNonStrokingColor(new Color(37, 99, 235));
        content.setFont(PDType1Font.HELVETICA_BOLD, 17);
        write(content, "Fare Breakdown", margin, y);
        content.setNonStrokingColor(Color.BLACK);
        y -= 12;
        drawSoftLine(content, margin, y, width - margin);  // ✅ separator below title
        y -= 25;

        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        write(content, "Base Fare", col1, y);
        write(content, "Taxes & Charges", col2, y);
        write(content, "Total Paid", col3, y);

        y -= 16;
        content.setFont(PDType1Font.HELVETICA, 11);
        double totalPaid = combinedTotal;

        // ✅ Calculate dynamic base fare from flights + passengers
        double baseFare = 0.0;

        // Count passengers
        int passengerCount = passengerList.size();

        for (int i = 0; i < segments.size(); i++) {
            Flight flight = segments.get(i).getFlight();

            double farePerAdult = 0.0;

            // Determine fare based on cabin class
            String cabin;
            if ("round".equalsIgnoreCase(booking.getTripType())) {
                cabin = (i == 0)
                        ? booking.getOutboundCabinClass()
                        : booking.getReturnCabinClass();
            } else {
                cabin = booking.getCabinClass();
            }

            if (cabin != null) {
                if (cabin.toLowerCase().contains("economy")) {
                    farePerAdult = flight.getEconomyAdultFare();
                } else if (cabin.toLowerCase().contains("business")) {
                    farePerAdult = flight.getBusinessAdultFare();
                } else if (cabin.toLowerCase().contains("first")) {
                    farePerAdult = flight.getFirstAdultFare();
                }
            }

            if (farePerAdult != 0.0) {
                baseFare += farePerAdult * passengerCount;
            }
        }

        double taxesAndCharges = totalPaid - baseFare;

        write(content, "Rs " + String.format("%.2f", baseFare), col1, y);
        write(content, "Rs " + String.format("%.2f", taxesAndCharges), col2, y);
        write(content, "Rs " + String.format("%.2f", totalPaid), col3, y);

        /* ================= CENTER FOOTER ================= */
        content.setFont(PDType1Font.HELVETICA_OBLIQUE, 11);
        String footer = "Thank you for choosing FlySphere. Have a pleasant journey!";
        write(content, footer, (width - 300) / 2, 75);


        content.close();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.save(out);
        document.close();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=ticket-" + bookingId + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(out.toByteArray());
    }

    private void write(PDPageContentStream content, String text, float x, float y) throws Exception {
        content.beginText();
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private void drawLine(PDPageContentStream content, float startX, float y, float endX) throws Exception {
        content.moveTo(startX, y);
        content.lineTo(endX, y);
        content.stroke();
    }

    private void drawSoftLine(PDPageContentStream content, float startX, float y, float endX) throws Exception {
        content.setStrokingColor(new Color(180, 195, 215));
        content.setLineWidth(1.2f);
        content.moveTo(startX, y);
        content.lineTo(endX, y);
        content.stroke();
        content.setStrokingColor(Color.BLACK);
        content.setLineWidth(1f);
    }

    private String trim(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max - 2) + ".." : value;
    }
}
