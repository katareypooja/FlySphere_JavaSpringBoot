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

    @GetMapping("/{bookingId}/ticket")
    public ResponseEntity<byte[]> generateTicket(@PathVariable String bookingId) throws Exception {

        Booking booking = bookingRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));

        User user = booking.getUser();
        Flight flight = booking.getFlight();

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

        /* ================= HEADER BAND ================= */
        content.setNonStrokingColor(new Color(214, 228, 248));
        content.addRect(0, height - 115, width, 115);
        content.fill();
        content.setNonStrokingColor(Color.BLACK);

        content.setFont(PDType1Font.HELVETICA_BOLD, 24);
        write(content, "FlySphere E-Ticket", margin, y);

        content.setFont(PDType1Font.HELVETICA_BOLD, 12);
        write(content, "Booking Ref", width - 170, y + 5);
        content.setFont(PDType1Font.HELVETICA_BOLD, 15);
        write(content, booking.getBookingId(), width - 170, y - 15);

        y -= 50;
        drawSoftLine(content, margin, y, width - margin);
        y -= 35;

        /* ================= BOOKING INFORMATION ================= */
        content.setFont(PDType1Font.HELVETICA_BOLD, 17);
        write(content, "Booking Information", margin, y);
        y -= 28;

        float col1 = margin;
        float col2 = margin + usableWidth * 0.25f;
        float col3 = margin + usableWidth * 0.50f;
        float col4 = margin + usableWidth * 0.75f;

        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        write(content, "Booking ID", col1, y);
        write(content, "Passenger", col2, y);
        write(content, "Email", col3, y);
        write(content, "Status", col4, y);

        y -= 12;
        drawLine(content, margin, y, width - margin);
        y -= 18;

        content.setFont(PDType1Font.HELVETICA, 11);
        write(content, trim(booking.getBookingId(), 18), col1, y);
        write(content, trim(user.getFirstName() + " " + user.getLastName(), 18), col2, y);
        write(content, trim(user.getEmail(), 22), col3, y);
        write(content, trim(booking.getStatus(), 12), col4, y);

        y -= 40;

        /* ================= FLIGHT DETAILS ================= */
        content.setFont(PDType1Font.HELVETICA_BOLD, 17);
        write(content, "Flight Details", margin, y);
        y -= 18;

        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        write(content, "Route", margin, y);
        y -= 12;

        drawSoftLine(content, margin, y, width - margin);
        y -= 22;

        // Row 1 headers
        write(content, "From", col1, y);
        write(content, "Airline", col2, y);
        write(content, "Departure Date", col3, y);
        write(content, "Arrival Date", col4, y);

        y -= 16;
        content.setFont(PDType1Font.HELVETICA, 11);

        write(content, trim(flight.getDepartureAirport(), 12), col1, y);
        write(content, trim(flight.getAirlineName(), 15), col2, y);
        write(content,
                trim(flight.getDepartureDate().format(formatter) + " " + flight.getDepartureTime(), 20),
                col3, y);
        write(content,
                trim(flight.getArrivalDate().format(formatter) + " " + flight.getArrivalTime(), 20),
                col4, y);

        y -= 25;
        drawLine(content, margin, y, width - margin);
        y -= 22;

        // Row 2 headers
        content.setFont(PDType1Font.HELVETICA_BOLD, 11);
        write(content, "To", col1, y);
        write(content, "Flight Number", col2, y);
        write(content, "Departure Terminal", col3, y);
        write(content, "Arrival Terminal", col4, y);

        y -= 16;
        content.setFont(PDType1Font.HELVETICA, 11);

        write(content, trim(flight.getArrivalAirport(), 12), col1, y);
        write(content, trim(flight.getFlightNo(), 12), col2, y);
        write(content, "N/A", col3, y);
        write(content, "N/A", col4, y);

        y -= 45;

        /* ================= FARE BREAKDOWN ================= */
        content.setFont(PDType1Font.HELVETICA_BOLD, 17);
        write(content, "Fare Breakdown", margin, y);
        y -= 22;

        content.setFont(PDType1Font.HELVETICA, 11);
        write(content, "Base Fare: Rs " + booking.getTotalAmount(), margin, y);
        y -= 16;
        write(content, "Taxes & Charges: Included", margin, y);
        y -= 16;
        write(content, "Total Paid: Rs " + booking.getTotalAmount(), margin, y);

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
