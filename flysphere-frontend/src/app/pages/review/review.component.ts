import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { BookingNavbarComponent } from '../../shared/booking-navbar/booking-navbar.component';

@Component({
  selector: 'app-review',
  standalone: true,
  imports: [CommonModule, BookingNavbarComponent],
  templateUrl: './review.component.html',
  styleUrls: ['./review.component.css']
})
export class ReviewComponent implements OnInit {

  bookingData: any;
  passengers: any[] = [];
  contact: any;
  totals: any;

  constructor(private router: Router, private http: HttpClient) {}

  ngOnInit(): void {
    const state = history.state;

    if (!state || !state.bookingData) {
      this.router.navigate(['/search']);
      return;
    }

    this.bookingData = state.bookingData;
    this.passengers = state.passengers || [];
    this.contact = state.contact || {};
    this.totals = state.totals || {};
  }

 confirmBooking() {

  const isRound = this.bookingData?.tripType === 'round';

  const outboundFlightId = isRound
    ? this.bookingData?.departure?.flight?.id
    : this.bookingData?.flight?.id;

  const payload = {
    outboundFlightId: outboundFlightId,
    returnFlightId: isRound ? this.bookingData?.return?.flight?.id : null,
    tripType: this.bookingData?.tripType || (isRound ? 'round' : 'oneway'),

    cabinClass: !isRound
      ? (this.bookingData?.cabinClass || 'Economy')
      : null,

    outboundCabinClass: isRound
      ? this.bookingData?.departure?.fare?.name
      : null,

    returnCabinClass: isRound
      ? this.bookingData?.return?.fare?.name
      : null,

    totalAmount: this.totals?.grandTotal,

    // ✅ Send contact phone entered in booking page
    contactPhone: this.contact?.phone,

    // ✅ Flatten passenger preferences to match backend String fields
    passengers: this.passengers.map(p => ({
      ...p,
      seatPreference: isRound
        ? `${p.seatPreference?.outbound || ''}|${p.seatPreference?.return || ''}`
        : (p.seatPreference?.outbound || ''),

      mealPreference: isRound
        ? `${p.mealPreference?.outbound || ''}|${p.mealPreference?.return || ''}`
        : (p.mealPreference?.outbound || ''),

      baggage: isRound
        ? (p.baggage?.outbound || p.baggage?.return)
        : p.baggage?.outbound
    }))
  };

  console.log('🚀 Sending booking payload (Spring Boot):', payload);

  this.http.post('http://localhost:8080/api/bookings', payload)
    .subscribe({
      next: (response: any) => {
        console.log('✅ Booking API response:', response);

        if (!response?.bookingId) {
          alert('Booking succeeded but response format is unexpected.');
          return;
        }

        this.router.navigate([
          '/confirmation',
          response.bookingId
        ]);
      },
      error: (error) => {
        console.error('❌ Booking API failed:', error);
        alert(error?.error?.message || 'Booking failed. Please try again.');
      }
    });
  }

  goBackToBooking() {
    this.router.navigate(['/booking'], {
      state: {
        bookingData: this.bookingData,
        passengers: this.passengers,
        contact: this.contact,
        totals: this.totals
      }
    });
  }
}
