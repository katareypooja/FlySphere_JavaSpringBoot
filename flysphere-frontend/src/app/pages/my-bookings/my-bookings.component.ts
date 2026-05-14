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
            <option value="COMPLETED">COMPLETED</option>
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

            <div class="booking-header-right">
              <div class="status-badge"
                   [ngClass]="getStatusClass(getDerivedStatus(booking))">
                {{ getDerivedStatus(booking) }}
              </div>
            </div>
          </div>

          <!-- Route Section -->
          <div class="route-section">
            <!-- One-way -->
            <div *ngIf="!booking.returnDepartureAirport" class="route-block">
              <div class="flight-meta">
                {{ booking.outboundAirlineName || 'Airline' }} • {{ booking.outboundFlightType || 'Flight' }}
              </div>
              <div class="route-text">
                {{ booking.departureAirport }} → {{ booking.arrivalAirport }}
              </div>
              <div class="trip-date">
                {{ booking.departureDate | date:'dd MMM yyyy' }} • {{ booking.departureTime || '—' }}
                <br>
                {{ booking.arrivalDate || booking.departureDate | date:'dd MMM yyyy' }} • {{ booking.arrivalTime || '—' }}
              </div>
            </div>

            <!-- Round-trip -->
            <div *ngIf="booking.returnDepartureAirport" class="route-split">
              <div class="route-block">
                <div class="flight-meta">
                  {{ booking.outboundAirlineName || 'Airline' }} • {{ booking.outboundFlightType || 'Flight' }}
                </div>
                <div class="route-text">
                  {{ booking.departureAirport }} → {{ booking.arrivalAirport }}
                </div>
                <div class="trip-date">
                  {{ booking.departureDate | date:'dd MMM yyyy' }} • {{ booking.departureTime || '—' }}
                  <br>
                  {{ booking.arrivalDate || booking.departureDate | date:'dd MMM yyyy' }} • {{ booking.arrivalTime || '—' }}
                </div>
              </div>

              <span class="route-line"></span>

              <div class="route-block route-block-right">
                <div class="flight-meta">
                  {{ booking.returnAirlineName || 'Airline' }} • {{ booking.returnFlightType || 'Flight' }}
                </div>
                <div class="route-text">
                  {{ booking.returnDepartureAirport }} → {{ booking.returnArrivalAirport }}
                </div>
                <div class="trip-date">
                  {{ booking.returnDate | date:'dd MMM yyyy' }} • {{ booking.returnDepartureTime || '—' }}
                  <br>
                  {{ booking.returnArrivalDate || booking.returnDate | date:'dd MMM yyyy' }} • {{ booking.returnArrivalTime || '—' }}
                </div>
              </div>
            </div>
          </div>

          <!-- Facts + Passengers -->
          <div class="facts-row">

            <details class="passenger-details passenger-details-compact">
              <summary class="passenger-summary">
                <span class="passenger-icon" aria-hidden="true" style="color:#929497;">👤</span>
                <span class="passenger-title">Passengers</span>
                <span class="passenger-count">({{ booking.passengerCount ?? (booking.passengers?.length ?? 0) }})</span>
                <span class="summary-meta">View</span>
              </summary>

              <div class="passenger-list" *ngIf="booking.passengers?.length; else noPassengers">
                <div class="passenger-pill" *ngFor="let p of booking.passengers">
                  <div class="pill-top">
                    <span class="passenger-name">
                      {{ formatTitle(p) }} {{ p.firstName }} {{ p.lastName }}
                    </span>
                    <span class="pill-sep">.</span>
                    <span class="pill-age" *ngIf="p.age != null">Age:{{ p.age }}</span>
                    <span class="pill-sep">.</span>
                    <span class="pill-type">{{ p.type | titlecase }}</span>
                  </div>

                  <div class="pill-sub">
                    <span class="sub-label">Outbound:</span>
                    <span class="sub-value">
                      {{ formatCabin(booking, 'outbound') }} • Seat: {{ formatSeat(p.outboundSeat, p.outboundSeatNo) }} • Meal: {{ formatMeal(p.outboundMeal) }} • Baggage: {{ formatBaggage(p.outboundBaggage) }}
                    </span>
                  </div>

                  <div class="pill-sub" *ngIf="booking.tripType === 'round'">
                    <span class="sub-label">Return:</span>
                    <span class="sub-value">
                      {{ formatCabin(booking, 'return') }} • Seat: {{ formatSeat(p.returnSeat, p.returnSeatNo) }} • Meal: {{ formatMeal(p.returnMeal) }} • Baggage: {{ formatBaggage(p.returnBaggage) }}
                    </span>
                  </div>
                </div>
              </div>

              <ng-template #noPassengers>
                <div class="passenger-empty">Passenger details not available.</div>
              </ng-template>
            </details>
          </div>

          <!-- Footer -->
          <div class="booking-footer">
            <div class="footer-total-text">
              <span class="footer-total-label">Total:</span>
              <span class="footer-total-value">₹ {{ booking.totalAmount }}</span>
            </div>

            <button class="btn btn-outline" (click)="viewDetails(booking.bookingId)">
              View Details
            </button>

            <button class="btn btn-outline" *ngIf="booking.status === 'CONFIRMED'"
                    (click)="downloadTicket(booking.bookingId)">
              Download Ticket
            </button>

            <button class="btn btn-danger" *ngIf="booking.status === 'CONFIRMED'"
                    (click)="handleCancellationClick(booking)"
                    [disabled]="!canCancel(booking)">
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
      gap: 6px; /* remove extra spacing between Search, Status, Reset */
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
      margin: 0 !important; /* remove any default/user-agent margin seen in inspect */
    }

    .filters select {
      padding: 0 12px;
      border: none;
      outline: none;
    }

    .filters button {
      padding: 0 10px; /* tighter reset button */
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
      align-items: flex-start;
      gap: 14px;
      margin-bottom: 12px;
    }

    .booking-header-right {
      display: flex;
      align-items: center;
      gap: 14px;
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

    .status-completed {
      background: #0ea5e9; /* blue for completed */
    }

    .route-section {
      margin: 15px 0;
      padding: 12px 16px;
      background: #f8fafc;
      border-radius: 12px;
    }

    .route-block {
      display: flex;
      flex-direction: column;
      gap: 2px;
    }

    .flight-meta {
      font-size: 12px;
      font-weight: 700;
      color: #64748b;
    }

    .route-text {
      font-size: 14px;
      font-weight: 600;
      color: #1a1b1f;
    }

    /* ✅ Side-by-side round trip layout */
    .route-split {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      gap: 12px;
    }

    .route-block-right {
      text-align: right;
    }

    /* ✅ Middle line for round trip */
    .route-line {
      flex: 1;
      height: 2px;
      background: #e5e7eb;
      border-radius: 2px;
      margin: 0 6px;
    }

    .trip-date {
      font-size: 12px;
      color: #4b5563;
      margin-top: 0;
      line-height: 1.35;
    }

    .facts-row {
      margin-top: 12px;
      display: flex;
      gap: 14px;
      align-items: center;
      justify-content: space-between;
      flex-wrap: nowrap;
    }

    .fact {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 13px;
      color: #334155;
      flex-wrap: wrap;
    }

    .fact-label {
      font-weight: 700;
      color: #64748b;
    }

    /* Make the values inside the highlighted Cabin/Passengers box use the same gray */
    .badge-pill,
    .passenger-summary,
    .summary-meta,
    .passenger-pill,
    .pill-type,
    .passenger-title,
    .passenger-count,
    .passenger-name {
      color: #64748b !important;
    }

    .badge-pill {
      padding: 6px 12px;
      border-radius: 999px;
      background: #f8fafc;
      color: #0f172a; /* black like Cabin label */
      font-weight: 700;
      font-size: 12px;
      white-space: nowrap;
    }

    .passenger-details {
      border: 0;
      min-width: 320px;
      flex: 1;
    }

    .passenger-details-compact {
      flex: 1;              /* ✅ allow passengers section to take more space */
      min-width: 520px;     /* ✅ enough room to keep Outbound/Return in one line */
      max-width: 100%;
      margin-left: 0; /* ✅ keep passenger section on the left */
    }

    .passenger-summary {
      cursor: pointer;
      display: flex;
      align-items: baseline;
      gap: 8px;
      list-style: none;
      font-size: 13px;
      color: #0f172a; /* same as Cabin text */
      font-weight: 500;
      background: #f8fafc;
      border: 0;
      border-radius: 12px;
      padding: 10px 12px;
    }

    .passenger-title {
      font-weight: 800;
      color: #0f172a;
    }

    .passenger-count {
      font-weight: 800;
      color: #0f172a;
    }

    .passenger-summary::-webkit-details-marker {
      display: none;
    }

    .summary-meta {
      margin-left: auto; /* push View to right inside pill */
      font-size: 11px;   /* small view */
      font-weight: 700;
      color: #0f172a;    /* same as Cabin */
      white-space: nowrap;
      opacity: 0.8;
    }

    .passenger-list {
      margin-top: 10px;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .passenger-pill {
      background: #ffffff;
      border: 1px solid #e2e8f0;
      border-radius: 14px;
      padding: 10px 12px;
      font-size: 12px;
      font-weight: 700;
      color: #0f172a; /* same as Cabin */
      display: flex;
      flex-direction: column;
      gap: 6px;
      align-items: flex-start;
    }

    .pill-top {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
      align-items: baseline;
    }

    .pill-sep {
      font-weight: 900;
      color: #cbd5e1;
    }

    .pill-age {
      font-weight: 800;
      color: #64748b;
    }

    .pill-sub {
      display: flex;
      gap: 6px;
      flex-wrap: nowrap;   /* ✅ keep label + value on same line */
      line-height: 1.35;
      white-space: nowrap; /* ✅ keep outbound/return content on one line */
    }

    .sub-label {
      color: #64748b;
      font-weight: 800;
    }

    .sub-value {
      color: #64748b;
      font-weight: 700;
    }

    .passenger-name {
      color: #0f172a;
      font-weight: 800;
    }

    .pill-type {
      font-weight: 800;
      color: #64748b;
      opacity: 1;
    }

    .passenger-icon {
      font-size: 14px;
      line-height: 1;
      margin-right: 2px;
      display: inline-block;
      filter: opacity(1); /* avoid browser quirks */
      color: #929497 !important;
    }

    .passenger-empty {
      margin-top: 10px;
      font-size: 12px;
      color: #64748b;
      padding: 8px 2px 0;
    }

    .booking-footer {
      margin-top: 16px;
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      flex-wrap: wrap;
      align-items: center;
    }

    .footer-total-text {
      margin-right: auto;
      display: flex;
      align-items: baseline;
      gap: 8px;
    }

    .footer-total-label {
      font-size: 12px;
      font-weight: 800;
      color: #64748b;
      text-transform: uppercase;
      letter-spacing: 0.04em;
    }

    .footer-total-value {
      font-size: 14px;
      font-weight: 600;
      color: #64748b;
      white-space: nowrap;
    }

    .btn {
      padding: 8px 18px;
      border-radius: 999px;
      font-weight: 700;
      cursor: pointer;
      border: 1px solid transparent;
      transition: 0.2s ease;
      font-size: 13px;
    }


    .btn-outline {
      background: transparent;
      color: #2563eb;
      border-color: #2563eb;
    }

    .btn-outline:hover {
      background: rgba(37, 99, 235, 0.08);
    }


    .btn-danger {
      background: transparent;
      color: #dc2626;
      border-color: #dc2626;
    }

    .btn-danger:hover:not(:disabled) {
      background: rgba(220, 38, 38, 0.08);
    }

    .btn:disabled {
      opacity: 0.55;
      cursor: not-allowed;
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

    // Derived statuses mean backend doesn't know COMPLETED.
    // For Upcoming tab we allow CONFIRMED/CANCELLED (pass through to backend).
    // For Past tab we allow CANCELLED only (COMPLETED will be derived client-side).
    if (this.statusFilter) {
      const f = this.statusFilter.toUpperCase();
      const isDerivedCompleted = f === 'COMPLETED';

      if (this.activeTab === 'UPCOMING') {
        if (!isDerivedCompleted) {
          url += `&status=${f}`;
        }
      } else {
        // PAST
        if (f === 'CANCELLED') {
          url += `&status=${f}`;
        }
      }
    }

    this.http.get<any>(url).subscribe({
      next: (data) => {
        const raw: any[] = data?.content ?? [];

        // Ensure the correct tab even if backend filter uses departure time:
        // Past/Upcoming is based on arrival time per requirement.
        const tabFiltered = raw.filter((b) =>
          this.activeTab === 'PAST' ? this.isPastBooking(b) : !this.isPastBooking(b)
        );

        // Apply derived-status filter on top
        const statusFiltered =
          this.statusFilter && this.statusFilter.trim().length > 0
            ? tabFiltered.filter(
                (b) => this.getDerivedStatus(b) === this.statusFilter.toUpperCase()
              )
            : tabFiltered;

        this.bookings = statusFiltered;
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

  private toLocalDateTimeMsFromParts(dateVal: any, timeVal?: any): number | null {
    if (!dateVal) return null;

    let y: number | undefined;
    let m: number | undefined;
    let d: number | undefined;

    if (typeof dateVal === 'string') {
      const parts = dateVal.split('-').map((v) => parseInt(v, 10));
      y = parts[0];
      m = parts[1];
      d = parts[2];
    } else if (typeof dateVal === 'object') {
      y = dateVal.year ?? dateVal.y;
      m = dateVal.month ?? dateVal.m;
      d = dateVal.day ?? dateVal.dayOfMonth ?? dateVal.d;
    }

    if (!y || !m || !d) return null;

    let hh = 0;
    let mm = 0;
    let ss = 0;

    if (typeof timeVal === 'string' && timeVal.trim()) {
      const t = timeVal.split(':').map((v) => parseInt(v, 10));
      hh = t[0] ?? 0;
      mm = t[1] ?? 0;
      ss = t[2] ?? 0;
    } else if (timeVal && typeof timeVal === 'object') {
      hh = timeVal.hour ?? 0;
      mm = timeVal.minute ?? 0;
      ss = timeVal.second ?? 0;
    }

    return new Date(y, m - 1, d, hh, mm, ss, 0).getTime();
  }

  private getArrivalMs(booking: any): number | null {
    if (!booking) return null;

    const tripType = String(booking.tripType ?? '').toLowerCase();
    if (tripType === 'round') {
      const ms = this.toLocalDateTimeMsFromParts(
        booking.returnArrivalDate ?? booking.returnDate,
        booking.returnArrivalTime ?? booking.returnDepartureTime
      );
      if (ms != null) return ms;
    }

    return this.toLocalDateTimeMsFromParts(
      booking.arrivalDate ?? booking.departureDate,
      booking.arrivalTime ?? booking.departureTime
    );
  }

  isPastBooking(booking: any): boolean {
    const arrivalMs = this.getArrivalMs(booking);
    if (arrivalMs == null) return false;
    return arrivalMs < Date.now();
  }

  getDerivedStatus(booking: any): 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' {
    const baseStatus = String(booking?.status ?? '').toUpperCase();
    if (baseStatus === 'CANCELLED') return 'CANCELLED';
    return this.isPastBooking(booking) ? 'COMPLETED' : 'CONFIRMED';
  }

  getStatusClass(status: string): string {
    const s = String(status ?? '').toUpperCase();
    if (s === 'CONFIRMED') return 'status-confirmed';
    if (s === 'CANCELLED') return 'status-cancelled';
    if (s === 'COMPLETED') return 'status-completed';
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

  /* ================= PASSENGER FORMATTERS (My Bookings) ================= */

  // No title stored in backend yet; using UI default as per requirement.
  formatTitle(_p: any): string {
    return 'Ms';
  }

  formatCabin(booking: any, leg: 'outbound' | 'return'): string {
    if (!booking) return '—';

    const tripType = String(booking.tripType ?? '').toLowerCase();
    const toTitle = (v: any) => this.titleCase(String(v ?? '').replace(/_/g, ' '));

    if (tripType === 'round') {
      const val = leg === 'outbound' ? booking.outboundCabinClass : booking.returnCabinClass;
      return val ? toTitle(val) : '—';
    }

    // one-way
    return booking.cabinClass ? toTitle(booking.cabinClass) : '—';
  }

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
    if (b == null) return 'No';

    const val = String(b).trim();
    if (!val) return 'No';

    const lower = val.toLowerCase();
    if (lower === 'false' || lower === 'no') return 'No';
    if (lower === 'true' || lower === 'yes') return 'Yes (+10 kgs)';

    // fallback
    return this.titleCase(val);
  }

  private titleCase(input: string): string {
    if (!input) return input;
    return input
      .split(' ')
      .map((w) => (w ? w[0].toUpperCase() + w.slice(1).toLowerCase() : w))
      .join(' ');
  }

  viewDetails(bookingId: string) {
    this.router.navigate(['/confirmation', bookingId]);
  }

  private toLocalDateTimeMs(dateStr: string, timeStr?: string | null): number {
    // dateStr: "YYYY-MM-DD"
    const [y, m, d] = dateStr.split('-').map((v) => parseInt(v, 10));

    // If backend sends time => "HH:mm" or "HH:mm:ss"
    // If time missing, use 23:59:59 so we don't close cancellation too early.
    let hh = 23;
    let mm = 59;
    let ss = 59;

    if (timeStr && timeStr.trim().length > 0) {
      const parts = timeStr.split(':').map((v) => parseInt(v, 10));
      hh = parts[0] ?? 0;
      mm = parts[1] ?? 0;
      ss = parts[2] ?? 0;
    }

    return new Date(y, (m ?? 1) - 1, d ?? 1, hh, mm, ss, 0).getTime();
  }

  // ✅ Disable cancellation if flight departed or within 24 hours
  canCancel(booking: any): boolean {
    if (!booking?.departureDate) return false;

    const now = Date.now();

    // booking.departureTime is now returned by backend (BookingResponseDto)
    // but keep safe fallback if older data doesn't have it.
    const departureMs = this.toLocalDateTimeMs(
      booking.departureDate,
      booking.departureTime
    );

    const diffInMs = departureMs - now;
    const diffInHours = diffInMs / (1000 * 60 * 60);

    if (diffInMs <= 0) return false; // already departed
    if (diffInHours <= 24) return false; // within 24 hours
    return true;
  }

  // ✅ Show different messages
  getCancellationMessage(booking: any): string {
    if (!booking?.departureDate) return '';

    const now = Date.now();
    const departureMs = this.toLocalDateTimeMs(
      booking.departureDate,
      booking.departureTime
    );

    const diffInMs = departureMs - now;
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
