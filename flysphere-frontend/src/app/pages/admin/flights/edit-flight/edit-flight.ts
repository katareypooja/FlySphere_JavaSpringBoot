import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpClient, HttpClientModule } from '@angular/common/http';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-edit-flight',
  standalone: true,
  imports: [CommonModule, FormsModule, HttpClientModule],
  templateUrl: './edit-flight.html',
  styleUrls: ['./edit-flight.css']
})
export class EditFlight implements OnInit {

  flightId: number = 0;
  flight: any = {};

  constructor(
    private route: ActivatedRoute,
    private http: HttpClient,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.flightId = Number(params.get('id'));
      this.loadFlight();
    });
  }

  loadFlight() {
    this.http.get<any>(`http://localhost:8080/api/flights/${this.flightId}`)
      .subscribe(response => {

        if (!response) {
          console.error('No flight data found');
          return;
        }

        // ✅ Backend returns camelCase → use directly
        this.flight = { ...response };

        // ✅ Force UI update immediately
        this.cdr.detectChanges();

        // ✅ Normalize date/time for input fields
        if (this.flight.departureDate) {
          this.flight.departureDate = this.flight.departureDate.substring(0, 10);
        }

        if (this.flight.arrivalDate) {
          this.flight.arrivalDate = this.flight.arrivalDate.substring(0, 10);
        }

        if (this.flight.departureTime) {
          this.flight.departureTime = this.flight.departureTime.substring(0, 5);
        }

        if (this.flight.arrivalTime) {
          this.flight.arrivalTime = this.flight.arrivalTime.substring(0, 5);
        }
      });
  }

  minDurationMinutes = 120;

  onDepartureChange() {
    if (!this.flight.departureDate || !this.flight.departureTime) return;

    const depDateTime = new Date(
      `${this.flight.departureDate}T${this.flight.departureTime}`
    );

    const arrivalDateTime = new Date(
      depDateTime.getTime() + this.minDurationMinutes * 60000
    );

    const yyyy = arrivalDateTime.getFullYear();
    const mm = String(arrivalDateTime.getMonth() + 1).padStart(2, '0');
    const dd = String(arrivalDateTime.getDate()).padStart(2, '0');

    this.flight.arrivalDate = `${yyyy}-${mm}-${dd}`;

    const hh = String(arrivalDateTime.getHours()).padStart(2, '0');
    const min = String(arrivalDateTime.getMinutes()).padStart(2, '0');

    this.flight.arrivalTime = `${hh}:${min}`;
  }

  updateFlight() {

    const payload = {
      airlineName: this.flight.airlineName,
      flightType: this.flight.flightType,
      flightNo: this.flight.flightNo,
      departureAirport: this.flight.departureAirport,
      arrivalAirport: this.flight.arrivalAirport,
      departureDate: this.flight.departureDate,
      arrivalDate: this.flight.arrivalDate,
      departureTime: this.flight.departureTime,
      arrivalTime: this.flight.arrivalTime,
      totalEconomySeats: this.flight.totalEconomySeats,
      totalBusinessSeats: this.flight.totalBusinessSeats,
      totalFirstClassSeats: this.flight.totalFirstClassSeats,
      economyAdultFare: this.flight.economyAdultFare,
      economyChildFare: this.flight.economyChildFare,
      businessAdultFare: this.flight.businessAdultFare,
      businessChildFare: this.flight.businessChildFare,
      firstAdultFare: this.flight.firstAdultFare,
      firstChildFare: this.flight.firstChildFare,
      flightStatus: 'Scheduled'
    };

    this.http.put(`http://localhost:8080/api/flights/${this.flightId}`, payload)
      .subscribe({
        next: () => {
          alert('Flight updated successfully');
          this.router.navigate(['/admin/flights']);
        },
        error: (err) => {
          console.error('Update failed:', err);
          alert('Update failed. Check console for details.');
        }
      });
  }
}
