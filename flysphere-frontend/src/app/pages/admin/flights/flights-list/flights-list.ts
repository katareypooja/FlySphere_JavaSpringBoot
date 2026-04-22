import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { interval, Subscription } from 'rxjs';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClientModule, HttpClient } from '@angular/common/http';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

@Component({
  selector: 'app-flights-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    HttpClientModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatSnackBarModule
  ],
  templateUrl: './flights-list.html',
  styleUrls: ['./flights-list.css'],
})
export class FlightsList implements OnInit, OnDestroy {

  // ✅ Safe date formatter (no JS Date usage, no timezone mutation)
  formatDate(dateStr: string): string {
    if (!dateStr) return '';

    const parts = dateStr.split('-');
    if (parts.length !== 3) return dateStr;

    const [year, month, day] = parts;

    const monthNames = [
      'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
      'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'
    ];

    const monthIndex = Number(month) - 1;
    if (monthIndex < 0 || monthIndex > 11) return dateStr;

    return `${day}-${monthNames[monthIndex]}-${year}`;
  }

  flights: any[] = [];
  allFlights: any[] = [];           // Original dataset (never mutate)
  filteredFlights: any[] = [];      // Working dataset for filters

  // ✅ Pagination
  currentPage: number = 1;
  pageSize: number = 10;

  // ✅ Search & Filters
  searchText: string = '';
  selectedAirline: string = '';
  selectedStatus: string = '';
  sortColumn: string = '';
  sortDirection: 'asc' | 'desc' = 'asc';

  displayedColumns: string[] = [
    'flightno',
    'airlinename',
    'departure',
    'arrival',
    'flighttype',
    'economy',
    'business',
    'first',
    'flightstatus',
    'actions'
  ];

  constructor(
    private http: HttpClient,
    private router: Router,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef
  ) {}

  private refreshSub!: Subscription;
  private previousStatusMap: Record<number, string> = {};

  ngOnInit(): void {
    this.loadFlights();

    // ✅ RxJS interval polling every 30 seconds
    this.refreshSub = interval(30000).subscribe(() => {
      this.loadFlights(true);
    });
  }

  ngOnDestroy(): void {
    if (this.refreshSub) {
      this.refreshSub.unsubscribe();
    }
  }

  editFlight(id: number) {
    this.router.navigate(['/admin/edit-flight', id]);
  }

  removeFlight(id: number) {
    if (!confirm('Are you sure you want to permanently remove this flight?')) return;

    this.http.delete(`http://localhost:8080/api/flights/${id}`)
      .subscribe({
        next: () => {
          this.snackBar.open('Flight removed successfully', 'Close', { duration: 2500 });
          this.loadFlights();
        },
        error: (err) => {
          console.error('Remove failed', err);
          this.snackBar.open('Failed to remove flight', 'Close', { duration: 3000 });
        }
      });
  }

  // ✅ Optimized Cancel Flight Logic
  cancelFlight(id: number) {
    if (!confirm('Are you sure you want to cancel this flight?')) return;

    // Find flight from already loaded data
    const flight = this.allFlights.find(f => f.flightid === id);
    if (!flight) {
      this.snackBar.open('Flight not found', 'Close', { duration: 3000 });
      return;
    }

    // Send only updated status (backend entity uses camelCase)
    const payload = {
      airlineName: flight.airlinename,
      flightType: flight.flighttype,
      flightNo: flight.flightno,
      departureAirport: flight.departureairport,
      arrivalAirport: flight.arrivalairport,
      departureDate: flight.departuredate,
      arrivalDate: flight.arrivaldate,
      departureTime: flight.departuretime,
      arrivalTime: flight.arrivaltime,
      totalEconomySeats: flight.totaleconomyseats,
      totalBusinessSeats: flight.totalbusinessseats,
      totalFirstClassSeats: flight.totalfirstclassseats,
      economyAdultFare: flight.economyadultfare,
      economyChildFare: flight.economychildfare,
      businessAdultFare: flight.businessadultfare,
      businessChildFare: flight.businesschildfare,
      firstAdultFare: flight.firstadultfare,
      firstChildFare: flight.firstchildfare,
      flightStatus: 'Cancelled'
    };

    this.http.put(`http://localhost:8080/api/flights/${id}`, payload)
      .subscribe({
        next: () => {
          this.snackBar.open('Flight cancelled successfully', 'Close', { duration: 2500 });

          // Update UI instantly without redirect
          const index = this.allFlights.findIndex(f => f.flightid === id);
          if (index !== -1) {
            this.allFlights[index].flightstatus = 'Cancelled';
          }

          this.applySearch();
        },
        error: (err) => {
          console.error('Cancel failed', err);
          this.snackBar.open('Failed to cancel flight', 'Close', { duration: 3000 });
        }
      });
  }

  loadFlights(checkStatusOnly: boolean = false) {
    this.http.get<any>(`http://localhost:8080/api/flights?ts=${Date.now()}`)
      .subscribe(response => {

        console.log('API Response:', response);

        if (Array.isArray(response)) {
          this.allFlights = response.map((f: any) => ({
            flightid: f.flightId,
            flightno: f.flightNo,
            airlinename: f.airlineName,
            departureairport: f.departureAirport,
            arrivalairport: f.arrivalAirport,
            departuredate: f.departureDate,
            arrivaldate: f.arrivalDate,
            departuretime: f.departureTime,
            arrivaltime: f.arrivalTime,
            flighttype: f.flightType,
            totaleconomyseats: f.totalEconomySeats,
            totalbusinessseats: f.totalBusinessSeats,
            totalfirstclassseats: f.totalFirstClassSeats,
            economyadultfare: f.economyAdultFare,
            economychildfare: f.economyChildFare,
            businessadultfare: f.businessAdultFare,
            businesschildfare: f.businessChildFare,
            firstadultfare: f.firstAdultFare,
            firstchildfare: f.firstChildFare,
            flightstatus: f.flightStatus
          }));
        } else if (response?.rows) {
          this.allFlights = response.rows;
        } else {
          this.allFlights = [];
        }

        const newFlights = [...this.allFlights];

        // ✅ Refresh only if status changed
        let statusChanged = false;

        newFlights.forEach(f => {
          const id = f.flightid || f.FlightId;
          const status = f.flightstatus || f.FlightStatus;

          if (this.previousStatusMap[id] && this.previousStatusMap[id] !== status) {
            statusChanged = true;
          }

          this.previousStatusMap[id] = status;
        });

        // ✅ Always reapply filters after refresh (remove conditional refresh logic)
        this.allFlights = newFlights;
        this.filteredFlights = [...this.allFlights];
        this.applySearch(); // ✅ Always reapply filters after refresh
        this.cdr.detectChanges();
      });
  }

  get airlines(): string[] {
    const list = this.allFlights.map(f => f.airlinename || f.AirlineName);
    return [...new Set(list)];
  }

  get statuses(): string[] {
    const list = this.allFlights.map(f => f.flightstatus || f.FlightStatus);
    return [...new Set(list)];
  }

  // ✅ Simple search by Flight Number
  applySearch() {
    this.currentPage = 1;

    let filtered = [...this.allFlights]; // ✅ Always start from original dataset

    if (this.searchText) {
      const term = this.searchText.toLowerCase();

      filtered = filtered.filter(f => {
        const flightNo = (f.flightno || f.FlightNo || '').toLowerCase();
        const airline = (f.airlinename || f.AirlineName || '').toLowerCase();

        return flightNo.includes(term) || airline.includes(term);
      });
    }

    if (this.selectedAirline) {
      filtered = filtered.filter(f =>
        (f.airlinename || f.AirlineName) === this.selectedAirline
      );
    }

    if (this.selectedStatus) {
      filtered = filtered.filter(f =>
        (f.flightstatus || f.FlightStatus) === this.selectedStatus
      );
    }

    if (this.sortColumn) {
      filtered.sort((a, b) => {
        const valA = (a[this.sortColumn] || '').toString().toLowerCase();
        const valB = (b[this.sortColumn] || '').toString().toLowerCase();
        return this.sortDirection === 'asc'
          ? valA.localeCompare(valB)
          : valB.localeCompare(valA);
      });
    }

    this.filteredFlights = filtered; // ✅ Do NOT mutate allFlights
    this.updatePagination();
  }

  // ✅ Clear filters (Airline + Status + Search)
  clearFilters() {
    this.selectedAirline = '';
    this.selectedStatus = '';
    this.searchText = '';
    this.applySearch();
  }

  sort(column: string) {
    if (this.sortColumn === column) {
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortColumn = column;
      this.sortDirection = 'asc';
    }
    this.applySearch();
  }

  get totalPages(): number {
    return Math.ceil(this.filteredFlights.length / this.pageSize) || 1;
  }

  get totalRecords(): number {
    return this.filteredFlights.length;
  }

  get startRecord(): number {
    if (this.totalRecords === 0) return 0;
    return (this.currentPage - 1) * this.pageSize + 1;
  }

  get endRecord(): number {
    return Math.min(this.currentPage * this.pageSize, this.totalRecords);
  }

  updatePagination() {
    const start = (this.currentPage - 1) * this.pageSize;
    const end = start + this.pageSize;
    this.flights = this.filteredFlights.slice(start, end);
  }

  nextPage() {
    if (this.currentPage < this.totalPages) {
      this.currentPage++;
      this.updatePagination();
    }
  }

  prevPage() {
    if (this.currentPage > 1) {
      this.currentPage--;
      this.updatePagination();
    }
  }

  // ✅ Export to CSV
  exportToCSV() {
    if (!this.allFlights.length) return;

    const headers = [
      'Flight No',
      'Airline',
      'Departure',
      'Arrival',
      'Status'
    ];

    const rows = this.allFlights.map(f => [
      f.flightno || f.FlightNo,
      f.airlinename || f.AirlineName,
      f.departureairport || f.DepartureAirport,
      f.arrivalairport || f.ArrivalAirport,
      f.flightstatus || f.FlightStatus
    ]);

    const csvContent =
      [headers, ...rows]
        .map(e => e.join(','))
        .join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);

    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', 'flights.csv');
    link.click();
  }

  /**
   * Returns true when arrival's calendar date is after departure's calendar date.
   * Used to show 'Arrives next day' badge.
   */
  isNextDay(element: any): boolean {
    try {
      const depVal = element.departuredate || element.DepartureDate;
      const arrVal = element.arrivaldate || element.ArrivalDate;
      if (!depVal || !arrVal) return false;

      const dep = new Date(depVal);
      const arr = new Date(arrVal);

      // Normalize to local date (midnight) to compare calendar day only
      const depDateOnly = new Date(dep.getFullYear(), dep.getMonth(), dep.getDate()).getTime();
      const arrDateOnly = new Date(arr.getFullYear(), arr.getMonth(), arr.getDate()).getTime();

      return arrDateOnly > depDateOnly;
    } catch (e) {
      console.warn('isNextDay check failed', e);
      return false;
    }
  }

}
