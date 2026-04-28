import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { Router } from '@angular/router';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule, HttpClientModule],
  template: `
    <div class="page-container">

      <div class="title-wrapper">
        <h2 class="page-title">My Bookings</h2>
      </div>

      <div class="bookings-card">

        <div *ngIf="loading" class="loading">
          Loading bookings...
        </div>

        <div *ngIf="!loading && bookings.length === 0" class="empty">
          You have no bookings yet.
        </div>

        <div *ngFor="let booking of bookings" class="booking-item">
          <div class="booking-header">
            <div>
              <strong>Booking ID:</strong> {{ booking.bookingId }}
            </div>
          <div class="status"
               [ngStyle]="{
                 color: booking.status === 'CANCELLED' ? '#dc2626' : '#16a34a'
               }">
              {{ booking.status }}
            </div>
          </div>

          <div class="booking-body">
            <div><strong>Trip Type:</strong> {{ booking.tripType }}</div>
            <div><strong>Total Amount:</strong> ₹ {{ booking.totalAmount }}</div>
            <div><strong>Cabin:</strong> {{ booking.cabinClass }}</div>
          </div>

          <div class="booking-footer">
            <button (click)="viewDetails(booking.bookingId)">
              View Details
            </button>

            <button (click)="downloadTicket(booking.bookingId)"
                    style="margin-left:10px;">
              Download Ticket
            </button>

            <button *ngIf="booking.status === 'CONFIRMED'"
                    (click)="cancelBooking(booking.bookingId)"
                    style="margin-left:10px; border-color:#dc2626; color:#dc2626;">
              Cancel Booking
            </button>
          </div>
        </div>

      </div>

      <!-- ✅ Backend Pagination -->
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
      padding: 80px 0;
      display: flex;
      flex-direction: column;
      align-items: center;
    }

    .title-wrapper {
      width: 100%;
      max-width: 900px;
      display: flex;
      justify-content: flex-start;
    }

    .page-title {
      font-size: 28px;
      font-weight: 700;
      color: white;
      margin-bottom: 20px;
    }

    .bookings-card {
      width: 100%;
      max-width: 900px;
      background: rgba(255,255,255,0.95);
      border-radius: 20px;
      padding: 25px;
      box-shadow: 0 15px 40px rgba(0,0,0,0.15);
    }

    .booking-item {
      background: #ffffff;
      border-radius: 14px;
      padding: 18px 22px;
      margin-bottom: 18px;
      box-shadow: 0 6px 20px rgba(0,0,0,0.08);
      border: 1px solid #e6edf7;
    }

    .booking-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 10px;
    }

    .status {
      font-weight: 600;
      color: #16a34a;
    }

    .booking-footer {
      margin-top: 12px;
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

  fetchBookings() {
    this.loading = true;

    this.http.get<any>(
      `http://localhost:8080/api/bookings/my?page=${this.currentPage}&size=${this.pageSize}&sort=createdAt,desc`
    ).subscribe({
      next: (data) => {
        console.log("Bookings API response:", data);

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
      error: (err) => {
        console.error("Bookings API error:", err);
        this.bookings = [];
        this.totalPages = 0;
        this.loading = false;

        this.cdr.detectChanges();
      }
    });
  }

  changePage(page: number) {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page;
    this.fetchBookings();
  }

  viewDetails(bookingId: string) {
    this.router.navigate(['/confirmation', bookingId]);
  }

  cancelBooking(bookingId: string) {
    if (!confirm('Are you sure you want to cancel this booking?')) return;

    this.http.put(
      `http://localhost:8080/api/bookings/${bookingId}/cancel`,
      {}
    ).subscribe({
      next: () => {
        this.fetchBookings();
      },
      error: (err) => {
        console.error('Cancel failed', err);
        alert('Failed to cancel booking.');
      }
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
      error: (err) => {
        console.error('Ticket download failed', err);
        alert('Unable to download ticket.');
      }
    });
  }
}
