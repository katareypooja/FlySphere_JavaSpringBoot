import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { BookingNavbarComponent } from '../../shared/booking-navbar/booking-navbar.component';

@Component({
  selector: 'app-confirmation',
  standalone: true,
  imports: [CommonModule, BookingNavbarComponent, HttpClientModule],
  templateUrl: './confirmation.component.html',
  styleUrls: ['./confirmation.component.css']
})
export class ConfirmationComponent implements OnInit {
  bookingData: any;
  bookingId: string = '';

  passengers: any[] = [];
  totals: any;

  private computeFareBreakup(params: {
    grandTotal: number;
    passengers: any[];
    tripType?: string;
    insuranceSelected?: boolean;
  }) {
    const paxCount = params.passengers?.length ?? 0;
    const isRound = (params.tripType || '').toLowerCase() === 'round';

    // Constants (must match Booking/Review pricing)
    const convenienceFee = paxCount > 0 ? 249 : 0;
    const baggagePrice = 799;
    const insuranceOneWay = 199;
    const insuranceRoundTrip = 299;

    const seatPrice = (pref?: string) => {
      if (!pref) return 0;
      const p = pref.toLowerCase();
      if (p.includes('middle')) return 299;
      if (p.includes('aisle')) return 399;
      if (p.includes('window')) return 499;
      return 0;
    };

    const mealPrice = (pref?: string) => {
      if (!pref) return 0;
      const p = pref.toLowerCase();
      if (p.includes('veg')) return 199;
      if (p.includes('jain')) return 249;
      if (p.includes('nonveg')) return 299;
      return 0;
    };

    let addonsTotal = 0;

    for (const p of params.passengers || []) {
      addonsTotal += seatPrice(p.outboundSeat);
      addonsTotal += mealPrice(p.outboundMeal);
      if (p.outboundBaggage) addonsTotal += baggagePrice;

      if (isRound) {
        addonsTotal += seatPrice(p.returnSeat);
        addonsTotal += mealPrice(p.returnMeal);
        if (p.returnBaggage) addonsTotal += baggagePrice;
      }
    }

    if (params.insuranceSelected) {
      addonsTotal += (isRound ? insuranceRoundTrip : insuranceOneWay) * paxCount;
    }

    const grandTotal = params.grandTotal ?? 0;

    // Solve tax/base similar to backend (tax=round((base+addons)*0.12))
    let preTax = Math.max(0, grandTotal - convenienceFee);
    let taxAmount = 0;

    for (let i = 0; i < 3; i++) {
      taxAmount = Math.round(preTax * 0.12);
      preTax = Math.max(0, grandTotal - convenienceFee - taxAmount);
    }

    const baseTotal = Math.max(0, preTax - addonsTotal);

    return {
      baseTotal: Math.round(baseTotal),
      addonsTotal: Math.round(addonsTotal),
      taxAmount: Math.round(taxAmount),
      convenienceFee: Math.round(convenienceFee),
      grandTotal: Math.round(grandTotal)
    };
  }

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

        // ✅ Totals for Fare Breakdown
        // Prefer backend-provided breakup if present; otherwise compute on frontend (fallback)
        const backendTotals = {
          baseTotal: response.baseTotal,
          addonsTotal: response.addonsTotal,
          taxAmount: response.taxAmount,
          convenienceFee: response.convenienceFee,
          grandTotal: response.grandTotal ?? response.totalAmount
        };

        const hasBackendBreakup =
          backendTotals.baseTotal != null ||
          backendTotals.addonsTotal != null ||
          backendTotals.taxAmount != null ||
          backendTotals.convenienceFee != null;

        this.totals = hasBackendBreakup
          ? {
              baseTotal: backendTotals.baseTotal ?? 0,
              addonsTotal: backendTotals.addonsTotal ?? 0,
              taxAmount: backendTotals.taxAmount ?? 0,
              convenienceFee: backendTotals.convenienceFee ?? 0,
              grandTotal: backendTotals.grandTotal ?? 0
            }
          : this.computeFareBreakup({
              grandTotal: response.totalAmount ?? 0,
              passengers: this.passengers,
              tripType: response.tripType,
              insuranceSelected: response.insuranceSelected
            });

        // ✅ Map segments (0 = outbound, 1 = return if exists)
        if (response.segments && response.segments.length > 0) {
          this.outboundFlight = response.segments[0];
          this.returnFlight = response.segments.length > 1 ? response.segments[1] : null;

          // ✅ Use backend tripType instead of deriving from segments
          this.tripType = response.tripType;
        } else {
          this.outboundFlight = undefined;
          this.returnFlight = null;
          this.tripType = response.tripType;
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

  /* ================= FORMATTERS FOR PASSENGER DROPDOWN ================= */

  formatSeat(seatType: any, seatNo: any): string {
    const no = seatNo ? String(seatNo) : '';
    const type = seatType ? String(seatType) : '';

    if (!type && !no) return '—';
    if (!type && no) return `*(${no})`;

    return no ? `${this.titleCase(type)} (${no})` : this.titleCase(type);
  }

  formatMeal(meal: any): string {
    if (!meal) return 'No Meal';
    return this.titleCase(String(meal));
  }

  formatBaggage(b: any): string {
    if (!b) return 'No';
    // In some responses this could already be "Yes" or "Yes (10kgs)" or a string.
    const val = String(b);
    if (val.toLowerCase() === 'true') return 'Yes (10 kgs)';
    if (val.toLowerCase() === 'yes') return 'Yes (10 kgs)';
    return this.titleCase(val);
  }

  getOutboundAddonSummary(p: any): string {
    return `${this.formatSeat(p?.outboundSeat, p?.outboundSeatNo)} / ${this.formatMeal(p?.outboundMeal)} / ${this.formatBaggage(p?.outboundBaggage)}`;
  }

  getReturnAddonSummary(p: any): string {
    return `${this.formatSeat(p?.returnSeat, p?.returnSeatNo)} / ${this.formatMeal(p?.returnMeal)} / ${this.formatBaggage(p?.returnBaggage)}`;
  }

  private titleCase(input: string): string {
    if (!input) return input;
    return input
      .split(' ')
      .map((w) => (w ? w[0].toUpperCase() + w.slice(1).toLowerCase() : w))
      .join(' ');
  }

  goHome() { this.router.navigate(['/search']); }
}
