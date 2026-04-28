import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { AuthService } from '../../services/auth';

@Component({
  selector: 'app-booking-navbar',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './booking-navbar.component.html',
  styleUrls: ['./booking-navbar.component.css']
})
export class BookingNavbarComponent implements OnInit {

  currentStep: number = 1;
  user: any = null;
  supportOpen: boolean = false;
  profileMenuOpen: boolean = false;

  constructor(
    private router: Router,
    private auth: AuthService,
    private cdr: ChangeDetectorRef
  ) {
    this.router.events
      .pipe(filter(event => event instanceof NavigationEnd))
      .subscribe(() => {
        this.updateStep();
      });

    this.updateStep();
  }

  ngOnInit(): void {
    this.auth.getProfile().subscribe({
      next: (res) => {
        this.user = res;
        this.cdr.detectChanges();
      },
      error: () => this.logout()
    });
  }

  logout() {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  // ✅ Profile dropdown navigation
  goToSearch() {
    this.profileMenuOpen = false;
    this.router.navigate(['/search']);
  }

  goToMyBookings() {
    this.profileMenuOpen = false;
    this.router.navigate(['/my-bookings']);
  }

  navigateTo(step: number) {
    if (step === 1) {
      this.router.navigate(['/booking']);
    } else if (step === 2) {
      this.router.navigate(['/review']);
    } else if (step === 3) {
      this.router.navigate(['/confirmation']);
    }
  }

  private updateStep() {
    const url = this.router.url;

    if (url.includes('review')) {
      this.currentStep = 2;
    } else if (url.includes('confirmation')) {
      this.currentStep = 3;
    } else {
      this.currentStep = 1;
    }
  }
}
