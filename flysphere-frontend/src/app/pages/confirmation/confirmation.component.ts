import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { BookingNavbarComponent } from '../../shared/booking-navbar/booking-navbar.component';

@Component({
  selector: 'app-confirmation',
  standalone: true,
  imports: [CommonModule, BookingNavbarComponent],
  templateUrl: './confirmation.component.html',
  styleUrls: ['./confirmation.component.css']
})
export class ConfirmationComponent implements OnInit {
  bookingData: any;
  bookingId: string = '';

  passengers: any[] = [];
  totals: any;

  tripType?: string;
  outboundFlight: any;
  returnFlight: any | null = null;

  constructor(
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient,
    private cdr: ChangeDetectorRef   // ✅ add this
  ) {}

  ngOnInit(): void {
    console.log('✅ Confirmation component loaded');
    const bookingIdParam = this.route.snapshot.paramMap.get('bookingId');
    console.log('✅ bookingId from route:', bookingIdParam);

    if (!bookingIdParam) {
      console.warn('No bookingId param, redirecting to /search');
      this.router.navigate(['/search']);
      return;
    }

    this.bookingId = bookingIdParam;

    const url = `http://localhost:8080/api/bookings/${this.bookingId}`;
    console.log('✅ Fetching booking from:', url);

    this.http.get<any>(url).subscribe({
      next: (response) => {
        console.log('✅ Booking API response:', response);

        // ✅ Spring Boot returns Booking entity directly (not wrapped in success/booking)
        if (!response) {
          console.warn('Empty response, redirecting');
          this.router.navigate(['/search']);
          return;
        }

        // ✅ Map new Spring Boot BookingDetailsResponseDto
        this.bookingData = response;
        this.passengers = response.passengers || [];
        this.totals = { grandTotal: response.totalAmount };

        // ✅ Map segments (0 = outbound, 1 = return if exists)
        if (response.segments && response.segments.length > 0) {
          this.outboundFlight = response.segments[0];
          this.returnFlight = response.segments.length > 1 ? response.segments[1] : null;
          this.tripType = response.segments.length > 1 ? 'round' : 'oneway';
        } else {
          this.outboundFlight = undefined;
          this.returnFlight = null;
          this.tripType = undefined;
        }

        console.log('✅ bookingData set in component:', this.bookingData);
        console.log('✅ passengers set in component:', this.passengers);
        console.log('✅ totals set in component:', this.totals);
        console.log('✅ outboundFlight:', this.outboundFlight);
        console.log('✅ returnFlight:', this.returnFlight);

        // ✅ Force Angular to update the view now that data is set
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('Booking fetch error', err);
        this.router.navigate(['/search']);
      }
    });
  }

  printTicket() { window.print(); }

  downloadTicket() {
    if (!this.bookingData?.bookingId) return;

    const url = `http://localhost:8080/api/bookings/${this.bookingData.bookingId}/ticket`;

    this.http.get(url, { responseType: 'blob' }).subscribe((blob) => {
      const file = new Blob([blob], { type: 'application/pdf' });
      const fileURL = window.URL.createObjectURL(file);

      const link = document.createElement('a');
      link.href = fileURL;
      link.download = `ticket-${this.bookingData.bookingId}.pdf`;
      document.body.appendChild(link);
      link.click();

      document.body.removeChild(link);
      window.URL.revokeObjectURL(fileURL);
    });
  }

  goHome() { this.router.navigate(['/search']); }
}
