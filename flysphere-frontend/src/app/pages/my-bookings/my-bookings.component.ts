import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BookingNavbarComponent } from '../../shared/booking-navbar/booking-navbar.component';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule, HttpClientModule, FormsModule, BookingNavbarComponent],
  template: `
    <app-booking-navbar></app-booking-navbar>

    <div class="page-container">

      <!-- ✅ Title + Filters Row -->
      <div class="top-bar">
        <h2 class="page-title">My Bookings</h2>

        <div class="filters">
          <input
            type="text"
            placeholder="Search by Booking ID"
            [(ngModel)]="searchBookingId"
            (ngModelChange)="applyFilters()"
          />

          <select [(ngModel)]="statusFilter"
                  (change)="applyFilters()">
            <option value="">All Status</option>
            <option value="CONFIRMED">CONFIRMED</option>
            <option value="CANCELLED">CANCELLED</option>
          </select>

          <button (click)="resetFilters()" class="reset-btn">Reset</button>
        </div>
      </div>

      <!-- ✅ Upcoming / Past Tabs -->
      <div class="tabs">
        <button 
          [class.active]="activeTab === 'UPCOMING'"
          (click)="switchTab('UPCOMING')">
          Upcoming
        </button>
        <button 
          [class.active]="activeTab === 'PAST'"
          (click)="switchTab('PAST')">
          Past
        </button>
      </div>

      <div class="bookings-card">

        <!-- ✅ Premium Skeleton Loading -->
        <div *ngIf="loading" class="skeleton-container">
          <div class="skeleton-card" *ngFor="let item of [1,2,3]">
            <div class="skeleton-line skeleton-title"></div>
            <div class="skeleton-line skeleton-subtitle"></div>
            <div class="skeleton-route"></div>
            <div class="skeleton-line"></div>
            <div class="skeleton-line short"></div>
            <div class="skeleton-buttons">
              <div class="skeleton-btn"></div>
              <div class="skeleton-btn"></div>
              <div class="skeleton-btn"></div>
            </div>
          </div>
        </div>

        <div *ngIf="!loading && bookings.length === 0" class="empty">
          You have no bookings yet.
        </div>

        <div *ngFor="let booking of bookings; let i = index" class="booking-row">

          <div class="booking-number">
            {{ i + 1 }}
          </div>

          <div class="booking-item">

          <!-- Header -->
          <div class="booking-header">
            <div>
              <div class="booking-id">
                Booking ID: {{ booking.bookingId }}
              </div>
              <div class="booking-date">
                Booked on: {{ booking.createdAt | date:'dd MMM yyyy' }}
              </div>
            </div>

            <div class="status-badge"
                 [ngClass]="getStatusClass(booking.status)">
              {{ booking.status }}
            </div>
          </div>

          <!-- Route Section -->
          <div class="route-section">
            <div class="route-text" *ngIf="!booking.returnDepartureAirport">
              {{ booking.departureAirport }} → {{ booking.arrivalAirport }}
            </div>

            <div class="route-text route-split" *ngIf="booking.returnDepartureAirport">
              <span>{{ booking.departureAirport }} → {{ booking.arrivalAirport }}</span>
              <span class="route-line"></span>
              <span>{{ booking.returnDepartureAirport }} → {{ booking.returnArrivalAirport }}</span>
            </div>

            <!-- ✅ Date Layout -->
            <div class="trip-date" *ngIf="!booking.returnDate">
              {{ getTripDateDisplay(booking) }}
            </div>

            <div class="trip-date route-split" *ngIf="booking.returnDate">
              <span>
                {{ booking.departureDate | date:'dd MMM yyyy' }}
              </span>
              <span>
                {{ booking.returnDate | date:'dd MMM yyyy' }}
              </span>
            </div>
          </div>

          <!-- Booking Body -->
          <div class="booking-body">
            <div>
              <strong>Trip:</strong>
              {{ booking.tripType ? (booking.tripType | titlecase) : 'One Way' }}
            </div>
            <div>
              <strong>Cabin:</strong>
              {{ booking.cabinClass ? booking.cabinClass : 'Economy' }}
            </div>
            <div><strong>Total:</strong> ₹ {{ booking.totalAmount }}</div>
          </div>

          <!-- Footer -->
          <div class="booking-footer">
            <button (click)="viewDetails(booking.bookingId)">
              View Details
            </button>

            <button (click)="downloadTicket(booking.bookingId)"
                    style="margin-left:10px;">
              Download Ticket
            </button>

            <button *ngIf="booking.status === 'CONFIRMED'"
                    (click)="handleCancellationClick(booking)"
                    class="cancel-btn">
              {{ canCancel(booking) ? 'Cancel Booking' : 'Cancellation Closed' }}
            </button>
          </div>
        </div>

      </div>

      </div>

      <!-- Pagination -->
      <div *ngIf="!loading && totalPages > 1" class="pagination">

        <button 
          (click)="changePage(currentPage - 1)" 
          [disabled]="currentPage === 0">
          «
        </button>

        <button 
          *ngFor="let page of pages"
          (click)="changePage(page)"
          [class.active]="currentPage === page">
          {{ page + 1 }}
        </button>

        <button 
          (click)="changePage(currentPage + 1)" 
          [disabled]="currentPage === totalPages - 1">
          »
        </button>

      </div>

    </div>
  `,
  styles: [`
    .page-container {
      min-height: 100vh;
      padding: 40px 0 80px 0; /* ✅ adjusted top gap */
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .top-bar {
      width: 100%;
      max-width: 900px; /* ✅ align with bookings card */
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 25px;
    }

    .page-title {
      font-size: 30px;
      font-weight: 700;
      color: white;
      margin: 0;
    }

    .filters {
      display: flex;
      gap: 12px;
      align-items: center;
    }

    .filters input,
    .filters select,
    .filters button {
      height: 36px;               /* ✅ equal height */
      box-sizing: border-box;
      border-radius: 999px;
      display: flex;
      align-items: center;
    }

    .filters input {
      flex: 1;
      padding: 0 12px;
      border: none;
      outline: none;
    }

    .filters select {
      padding: 0 12px;
      border: none;
      outline: none;
    }

    .filters button {
      padding: 0 16px;
      border: none;
      cursor: pointer;
      font-weight: 600;
    }

    .filters button:first-of-type {
      background: #3b82f6;
      color: white;
    }

    .reset-btn {
      background: #e5e7eb;
    }

    /* ✅ Skeleton Loading Styles */
    .skeleton-container {
      width: 100%;
    }

    .skeleton-card {
      background: #ffffff;
      border-radius: 14px;
      padding: 22px;
      margin-bottom: 20px;
      box-shadow: 0 8px 25px rgba(0,0,0,0.08);
      border: 1px solid #e6edf7;
    }

    .skeleton-line,
    .skeleton-route,
    .skeleton-btn {
      background: linear-gradient(
        90deg,
        #f0f0f0 25%,
        #e0e0e0 37%,
        #f0f0f0 63%
      );
      background-size: 400% 100%;
      animation: shimmer 1.4s ease infinite;
      border-radius: 6px;
    }

    .skeleton-title {
      height: 16px;
      width: 40%;
      margin-bottom: 8px;
    }

    .skeleton-subtitle {
      height: 12px;
      width: 30%;
      margin-bottom: 15px;
    }

    .skeleton-route {
      height: 40px;
      width: 100%;
      border-radius: 10px;
      margin-bottom: 15px;
    }

    .skeleton-line {
      height: 12px;
      width: 60%;
      margin-bottom: 8px;
    }

    .skeleton-line.short {
      width: 35%;
    }

    .skeleton-buttons {
      display: flex;
      gap: 12px;
      margin-top: 15px;
    }

    .skeleton-btn {
      height: 32px;
      width: 110px;
      border-radius: 999px;
    }

    @keyframes shimmer {
      0% { background-position: -400px 0; }
      100% { background-position: 400px 0; }
    }

    /* ✅ Toggle Style Tabs (Full Width Like Screenshot) */
    .tabs {
      width: 100%;
      max-width: 900px;
      display: flex;
      margin-bottom: 20px;
      background: rgba(255,255,255,0.15);
      padding: 6px;
      border-radius: 999px;
    }

    .tabs button {
      flex: 1;
      padding: 10px 0;
      border-radius: 999px;
      border: none;
      background: transparent;
      color: white;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s ease;
    }

    .tabs button.active {
      background: white;
      color: #2563eb;
      box-shadow: 0 4px 10px rgba(0,0,0,0.15);
    }

    .bookings-card {
      width: 100%;
      max-width: 900px;
      background: transparent; /* ✅ removed white column background */
      border-radius: 0;
      padding: 0;
      box-shadow: none;
    }

    .booking-row {
      display: flex;
      align-items: center;
      gap: 15px;
    }

    .booking-number {
      font-size: 22px;
      font-weight: 700;
      color: white;
      width: 35px;
      text-align: center;
    }

    .booking-item {
      flex: 1;
      background: #ffffff;
      border-radius: 14px;
      padding: 22px;
      margin-bottom: 20px;
      box-shadow: 0 8px 25px rgba(0,0,0,0.08);
      border: 1px solid #e6edf7;
    }

    .booking-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;
    }

    .booking-id {
      font-weight: 600;
      font-size: 15px;
    }

    .booking-date {
      font-size: 13px;
      color: #6b7280;
      margin-top: 3px;
    }

    .status-badge {
      padding: 6px 14px;
      border-radius: 999px;
      font-size: 12px;
      font-weight: 600;
      color: white;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .status-confirmed {
      background: #16a34a;
    }

    .status-cancelled {
      background: #dc2626;
    }

    .status-pending {
      background: #f59e0b;
    }

    .route-section {
      margin: 15px 0;
      padding: 12px 16px;
      background: #f8fafc;
      border-radius: 12px;
    }

    .route-text {
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
    }

    /* ✅ Side-by-side round trip layout */
    .route-split {
      display: flex;
      align-items: center;
      justify-content: space-between; /* ✅ push right item fully right */
      width: 100%;
    }

    /* ✅ Middle line for round trip */
    .route-line {
      flex: 1;
      height: 2px;
      background: #e5e7eb;
      border-radius: 2px;
      margin: 0 12px; /* ✅ spacing between routes */
    }

    .trip-date {
      font-size: 14px;
      color: #4b5563;
      margin-top: 4px;
    }

    /* ✅ Ensure return date fully right-aligned */
    .trip-date.route-split span:last-child {
      margin-left: auto;
      text-align: right;
    }

    .booking-body {
      margin-top: 10px;
      font-size: 14px;
      color: #374151;
    }

    .booking-footer {
      margin-top: 18px;
      text-align: right;
    }

    .booking-footer button {
      padding: 8px 18px;
      border-radius: 999px;
      border: 1px solid #3b82f6;
      background: transparent;
      color: #3b82f6;
      font-weight: 600;
      cursor: pointer;
    }

    .cancel-btn {
      margin-left: 10px;
      border-color: #dc2626;
      color: #dc2626;
    }

    .pagination {
      margin-top: 25px;
      display: flex;
      gap: 10px;
      background: rgba(255,255,255,0.85);
      padding: 10px 20px;
      border-radius: 999px;
    }

    .pagination button {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      border: none;
      background: #e2e8f0;
      cursor: pointer;
      font-weight: 600;
    }

    .pagination button.active {
      background: #3b82f6;
      color: white;
    }

    .pagination button:disabled {
      opacity: 0.4;
      cursor: not-allowed;
    }
  `]
})
export class MyBookingsComponent implements OnInit {

  bookings: any[] = [];
  loading = true;

  activeTab: 'UPCOMING' | 'PAST' = 'UPCOMING';

  // ✅ Filters
  searchBookingId: string = '';
  statusFilter: string = '';

  currentPage = 0;
  totalPages = 0;
  pageSize = 5;
  pages: number[] = [];

  constructor(
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.fetchBookings();
  }

  // ✅ Switch tab and reload data immediately
  switchTab(tab: 'UPCOMING' | 'PAST') {
    if (this.activeTab === tab) return;

    this.activeTab = tab;
    this.currentPage = 0; // reset pagination
    this.fetchBookings();
  }

  // ✅ Backend now handles Upcoming / Past filtering

  fetchBookings() {
    this.loading = true;

    let url = `http://localhost:8080/api/bookings/my?page=${this.currentPage}&size=${this.pageSize}&sort=createdAt,desc&type=${this.activeTab}`;

    if (this.searchBookingId) {
      url += `&bookingId=${this.searchBookingId}`;
    }

    if (this.statusFilter) {
      url += `&status=${this.statusFilter}`;
    }

    this.http.get<any>(url).subscribe({
      next: (data) => {
        this.bookings = data?.content ?? [];
        this.totalPages = data?.totalPages ?? 0;

        const start = Math.max(0, this.currentPage - 1);
        const end = Math.min(this.totalPages - 1, start + 2);
        const adjustedStart = Math.max(0, end - 2);

        this.pages = Array.from(
          { length: end - adjustedStart + 1 },
          (_, i) => adjustedStart + i
        );

        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.bookings = [];
        this.totalPages = 0;
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  getRouteDisplay(booking: any): string {
    if (booking.returnDepartureAirport) {
      return `${booking.departureAirport} → ${booking.arrivalAirport}<br>
              ${booking.returnDepartureAirport} → ${booking.returnArrivalAirport}`;
    }
    return `${booking.departureAirport} → ${booking.arrivalAirport}`;
  }

  getTripDateDisplay(booking: any): string {
    if (booking.returnDate) {
      return `${new Date(booking.departureDate).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric'})}
       – ${new Date(booking.returnDate).toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric'})}`;
    }
    return new Date(booking.departureDate)
      .toLocaleDateString('en-GB', { day:'2-digit', month:'short', year:'numeric'});
  }

  getStatusClass(status: string): string {
    if (status === 'CONFIRMED') return 'status-confirmed';
    if (status === 'CANCELLED') return 'status-cancelled';
    return 'status-pending';
  }

  changePage(page: number) {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.fetchBookings();
  }

  applyFilters() {
    this.currentPage = 0;
    this.fetchBookings();
  }

  resetFilters() {
    this.searchBookingId = '';
    this.statusFilter = '';
    this.currentPage = 0;
    this.fetchBookings();
  }

  viewDetails(bookingId: string) {
    this.router.navigate(['/confirmation', bookingId]);
  }

  // ✅ Disable cancellation if flight departed or within 24 hours
  canCancel(booking: any): boolean {
    if (!booking.departureDate) return false;

    const now = new Date().getTime();
    const departure = new Date(booking.departureDate).getTime();
    const diffInMs = departure - now;
    const diffInHours = diffInMs / (1000 * 60 * 60);

    if (diffInMs <= 0) return false;        // already departed
    if (diffInHours <= 24) return false;    // within 24 hours

    return true;
  }

  // ✅ Show different messages
  getCancellationMessage(booking: any): string {
    if (!booking.departureDate) return '';

    const now = new Date().getTime();
    const departure = new Date(booking.departureDate).getTime();
    const diffInMs = departure - now;
    const diffInHours = diffInMs / (1000 * 60 * 60);

    if (diffInMs <= 0) {
      return 'This flight has already departed. Cancellation is no longer available.';
    }

    if (diffInHours <= 24) {
      return 'Cancellation is allowed only up to 24 hours before departure.';
    }

    return '';
  }

  // ✅ Handle cancel click (show message if not allowed)
  handleCancellationClick(booking: any) {
    if (!this.canCancel(booking)) {
      alert(this.getCancellationMessage(booking));
      return;
    }

    if (!confirm('Are you sure you want to cancel this booking?')) return;

    this.http.put(
      `http://localhost:8080/api/bookings/${booking.bookingId}/cancel`,
      {}
    ).subscribe({
      next: () => this.fetchBookings(),
      error: () => alert('Failed to cancel booking.')
    });
  }

  downloadTicket(bookingId: string) {
    const url = `http://localhost:8080/api/bookings/${bookingId}/ticket`;

    this.http.get(url, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const file = new Blob([blob], { type: 'application/pdf' });
        const fileURL = window.URL.createObjectURL(file);

        const link = document.createElement('a');
        link.href = fileURL;
        link.download = `ticket-${bookingId}.pdf`;
        document.body.appendChild(link);
        link.click();

        document.body.removeChild(link);
        window.URL.revokeObjectURL(fileURL);
      },
      error: () => alert('Unable to download ticket.')
    });
  }
}
