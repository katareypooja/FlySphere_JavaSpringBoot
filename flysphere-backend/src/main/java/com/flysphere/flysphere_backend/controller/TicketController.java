package com.flysphere.flysphere_backend.controller;

import com.flysphere.flysphere_backend.model.Booking;
import com.flysphere.flysphere_backend.model.Flight;
import com.flysphere.flysphere_backend.model.User;
import com.flysphere.flysphere_backend.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
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

        /* ================= PAGE BACKGROUND (WHITE) ================= */
        content.setNonStrokingColor(Color.WHITE);
        content.addRect(0, 0, width, height);
        content.fill();
        content.setNonStrokingColor(Color.BLACK);

        /* ================= WATERMARK (LIGHT) ================= */
        content.saveGraphicsState();

        // Transparency for watermark
        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(0.06f); // light watermark
        content.setGraphicsStateParameters(gs);

        // Very light blue-grey watermark text
        content.setNonStrokingColor(new Color(30, 64, 175));
        content.setFont(PDType1Font.HELVETICA_BOLD, 90);

        content.beginText();
        content.setTextMatrix(
                org.apache.pdfbox.util.Matrix.getRotateInstance(
                        Math.toRadians(45),
                        width / 4,
                        height / 3
                )
        );
        content.showText("FLYSPHERE");
        content.endText();

        content.restoreGraphicsState();

        /* ================= HEADER BAND (BLUE #799ae1) ================= */
        content.setNonStrokingColor(new Color(121, 154, 225)); // #799ae1
        content.addRect(0, height - 115, width, 115);
        content.fill();

        // Header text in white for contrast
        content.setNonStrokingColor(Color.WHITE);

        // Brand Header
        content.setFont(PDType1Font.HELVETICA_BOLD, 26);
        write(content, "FlySphere", margin, y);

        content.setFont(PDType1Font.HELVETICA, 12);
        write(content, "Secure Booking", margin + 2, y - 18);

        content.setFont(PDType1Font.HELVETICA_BOLD, 16);
        write(content, "E-Ticket", width / 2 - 40, y - 2);

        content.setFont(PDType1Font.HELVETICA_BOLD, 12);
        write(content, "Booking Ref", width - 170, y + 5);
        content.setFont(PDType1Font.HELVETICA_BOLD, 15);
        write(content, booking.getBookingId(), width - 170, y - 15);

        // Reset color for rest of document
        content.setNonStrokingColor(Color.BLACK);

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

            y -= 18;

            // spacing between stacked segments
            if (segments.size() > 1 && i < segments.size() - 1) {
                y -= 4;
            }
        }

        /* ================= PASSENGER DETAILS ================= */
        y -= 10;
        content.setNonStrokingColor(new Color(37, 99, 235));
        content.setFont(PDType1Font.HELVETICA_BOLD, 17);
        write(content, "Passenger Details", margin, y);
        content.setNonStrokingColor(Color.BLACK);
        y -= 12;
        // Keep underline under Passenger Details heading (as in round-trip screenshot)
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

        // Page-break threshold (start a new page if we go below this)
        float minY = 120;

        for (int pi = 0; pi < passengerList.size(); pi++) {
            com.flysphere.flysphere_backend.model.Passenger p = passengerList.get(pi);

            // Each passenger consumes ~70-95px depending on one-way/round-trip.
            float needed = isRound ? 95 : 75;

            if (y - needed < minY) {
                content.close();

                PDPage nextPage = new PDPage(PDRectangle.A4);
                document.addPage(nextPage);
                content = new PDPageContentStream(document, nextPage);

                // Background + watermark only (NO HEADER on next pages)
                drawTicketPageBackgroundAndWatermark(content, width, height);

                // reset y
                y = height - 75;

                // Optional small section title on next pages (no header band)
                content.setNonStrokingColor(new Color(37, 99, 235));
                content.setFont(PDType1Font.HELVETICA_BOLD, 15);
                write(content, "Passenger Details (Cont.)", margin, y);
                content.setNonStrokingColor(Color.BLACK);
                y -= 12;
                drawSoftLine(content, margin, y, width - margin);
                y -= 18;
            }

            if (isRound) {
                y = drawPassengerBlockRoundTrip(content, p, y, margin, usableWidth);
            } else {
                y = drawPassengerBlockOneWay(content, p, y, margin, usableWidth);
            }

            // Draw a separator ONLY between passengers (not after the last passenger)
            if (pi < passengerList.size() - 1) {
                drawSoftLine(content, margin, y, margin + usableWidth);
                y -= 14;
            }
        }

        // Add a little vertical gap before Fare Breakdown (matches spacing used before section headers)
        y -= 10;

        /* ================= FARE BREAKDOWN ================= */
        content.setNonStrokingColor(new Color(37, 99, 235));
        content.setFont(PDType1Font.HELVETICA_BOLD, 17);
        write(content, "Fare Breakdown", margin, y);
        content.setNonStrokingColor(Color.BLACK);
        y -= 12;
        // keep only the section’s internal separator; no extra line between passenger section and fare section
        drawSoftLine(content, margin, y, width - margin);  // separator below title
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

    private void drawTicketPageBackgroundAndWatermark(PDPageContentStream content, float width, float height)
            throws Exception {
        // White background
        content.setNonStrokingColor(Color.WHITE);
        content.addRect(0, 0, width, height);
        content.fill();

        // Light watermark
        content.saveGraphicsState();

        PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
        gs.setNonStrokingAlphaConstant(0.06f);
        content.setGraphicsStateParameters(gs);

        content.setNonStrokingColor(new Color(30, 64, 175));
        content.setFont(PDType1Font.HELVETICA_BOLD, 90);

        content.beginText();
        content.setTextMatrix(
                org.apache.pdfbox.util.Matrix.getRotateInstance(
                        Math.toRadians(45),
                        width / 4,
                        height / 3
                )
        );
        content.showText("FLYSPHERE");
        content.endText();

        content.restoreGraphicsState();

        content.setNonStrokingColor(Color.BLACK);
    }

    private float drawPassengerBlockOneWay(
            PDPageContentStream content,
            com.flysphere.flysphere_backend.model.Passenger p,
            float y,
            float margin,
            float usableWidth
    ) throws Exception {
        // Row 1: Name | Age | Type | Contact No
        float col1 = margin;
        float col2 = margin + usableWidth * 0.22f;
        float col3 = margin + usableWidth * 0.40f;
        float col4 = margin + usableWidth * 0.60f;

        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        write(content, "Name", col1, y);
        write(content, "Age", col2, y);
        write(content, "Type", col3, y);
        write(content, "Contact No", col4, y);

        y -= 14;

        content.setFont(PDType1Font.HELVETICA, 11);
        write(content, trim(p.getFirstName() + " " + p.getLastName(), 28), col1, y);
        write(content, p.getAge() != null ? String.valueOf(p.getAge()) : "—", col2, y);
        write(content, trim(p.getType(), 10), col3, y);
        write(content, trim(p.getPhone(), 18), col4, y);

        y -= 18;

        // Row 2: Seat | Meal | Bag | Insurance | Email
        float c1 = margin;
        float c2 = margin + usableWidth * 0.22f;
        float c3 = margin + usableWidth * 0.40f;
        float c4 = margin + usableWidth * 0.60f;
        float c5 = margin + usableWidth * 0.80f;

        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        write(content, "Seat", c1, y);
        write(content, "Meal", c2, y);
        write(content, "Baggage", c3, y);
        write(content, "Insurance", c4, y);
        write(content, "Email", c5, y);

        y -= 14;

        content.setFont(PDType1Font.HELVETICA, 11);

        String seatDisplay = "";
        if (p.getOutboundSeat() != null && p.getOutboundSeatNo() != null) {
            seatDisplay = p.getOutboundSeat() + " (" + p.getOutboundSeatNo() + ")";
        } else if (p.getOutboundSeatNo() != null) {
            seatDisplay = "*(" + p.getOutboundSeatNo() + ")";
        } else if (p.getOutboundSeat() != null) {
            seatDisplay = p.getOutboundSeat();
        }

        String insurance = Boolean.TRUE.equals(p.getInsuranceSelected()) ? "Yes covered" : "No covered";

        write(content, trim(seatDisplay, 14), c1, y);
        write(content, trim(formatMeal(p.getOutboundMeal()), 14), c2, y);
        write(content, trim(formatBaggage(p.getOutboundBaggage()), 14), c3, y);
        write(content, insurance, c4, y);
        write(content, trim(p.getEmail(), 30), c5, y);

        y -= 22;

        return y;
    }

    private float drawPassengerBlockRoundTrip(
            PDPageContentStream content,
            com.flysphere.flysphere_backend.model.Passenger p,
            float y,
            float margin,
            float usableWidth
    ) throws Exception {
        // Row 1: Name | Age | Type | Contact No
        float col1 = margin;
        float col2 = margin + usableWidth * 0.26f;
        float col3 = margin + usableWidth * 0.52f;
        float col4 = margin + usableWidth * 0.78f;

        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        write(content, "Name", col1, y);
        write(content, "Age", col2, y);
        write(content, "Type", col3, y);
        write(content, "Contact No", col4, y);

        y -= 14;

        content.setFont(PDType1Font.HELVETICA, 11);
        write(content, trim(p.getFirstName() + " " + p.getLastName(), 28), col1, y);
        write(content, p.getAge() != null ? String.valueOf(p.getAge()) : "—", col2, y);
        write(content, trim(p.getType(), 10), col3, y);
        write(content, trim(p.getPhone(), 18), col4, y);

        y -= 18;

        // Row 2: Out Seat | Out Meal | Out Bag | Email
        float c1 = margin;
        float c2 = margin + usableWidth * 0.26f;
        float c3 = margin + usableWidth * 0.52f;
        float c4 = margin + usableWidth * 0.78f;

        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        write(content, "Outound Seat", c1, y);
        write(content, "Outbound Meal", c2, y);
        write(content, "Outbound Baggage", c3, y);
        write(content, "Email", c4, y);

        y -= 14;

        content.setFont(PDType1Font.HELVETICA, 11);

        String outSeatDisplay = "";
        if (p.getOutboundSeat() != null && p.getOutboundSeatNo() != null) {
            outSeatDisplay = p.getOutboundSeat() + " (" + p.getOutboundSeatNo() + ")";
        } else if (p.getOutboundSeatNo() != null) {
            outSeatDisplay = "*(" + p.getOutboundSeatNo() + ")";
        } else if (p.getOutboundSeat() != null) {
            outSeatDisplay = p.getOutboundSeat();
        }

        write(content, trim(outSeatDisplay, 18), c1, y);
        write(content, trim(formatMeal(p.getOutboundMeal()), 18), c2, y);
        write(content, trim(formatBaggage(p.getOutboundBaggage()), 18), c3, y);
        write(content, trim(p.getEmail(), 34), c4, y);

        y -= 18;

        // Row 3: Ret Seat | Ret Meal | Ret Bag | Insurance
        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        write(content, "Return Seat", c1, y);
        write(content, "Return Meal", c2, y);
        write(content, "Return Baggage", c3, y);
        write(content, "Insurance", c4, y);

        y -= 14;

        content.setFont(PDType1Font.HELVETICA, 11);

        String retSeatDisplay = "";
        if (p.getReturnSeat() != null && p.getReturnSeatNo() != null) {
            retSeatDisplay = p.getReturnSeat() + " (" + p.getReturnSeatNo() + ")";
        } else if (p.getReturnSeatNo() != null) {
            retSeatDisplay = "*(" + p.getReturnSeatNo() + ")";
        } else if (p.getReturnSeat() != null) {
            retSeatDisplay = p.getReturnSeat();
        }

        String insurance = Boolean.TRUE.equals(p.getInsuranceSelected()) ? "Yes covered" : "Not covered";

        write(content, trim(retSeatDisplay, 18), c1, y);
        write(content, trim(formatMeal(p.getReturnMeal()), 18), c2, y);
        write(content, trim(formatBaggage(p.getReturnBaggage()), 18), c3, y);
        write(content, insurance, c4, y);

        y -= 22;

        return y;
    }

    private String formatMeal(String meal) {
        if (meal == null || meal.trim().isEmpty()) return "No meal";
        return trim(meal.trim(), 18);
    }

    private String formatBaggage(String baggage) {
        if (baggage == null || baggage.trim().isEmpty()) return "Baggage No";
        // Per requirement: if baggage exists => show fixed label
        return "Yes (+10kgs)";
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
