import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../services/auth';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterModule, CommonModule],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.css'
})
export class Dashboard implements OnInit {

  user: any;
  role: string | null = null;

  constructor(private auth: AuthService) {}

  ngOnInit() {
    const token = this.auth.getToken();

    if (!token) {
      window.location.href = '/login';
      return;
    }

    this.auth.getProfile().subscribe({
      next: (res: any) => {
        this.user = res;
        const payload = JSON.parse(atob(token.split('.')[1]));
        this.role = payload.role;
      },
      error: () => {
        this.auth.logout();
        window.location.href = '/login';
      }
    });
  }

  logout() {
    this.auth.logout();
    window.location.href = '/login';
  }
}
