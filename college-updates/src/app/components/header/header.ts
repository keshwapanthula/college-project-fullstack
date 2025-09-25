import { Component } from '@angular/core';

@Component({
  selector: 'app-header',
  template: `
    <nav class="header-nav">
      <a routerLink="/updates" routerLinkActive="active">Updates</a>
      <a routerLink="/notifications" routerLinkActive="active">Notifications</a>
      <a routerLink="/admin-services" routerLinkActive="active">Admin Services</a>
    </nav>
  `,
  styles: [`
    .header-nav {
      display: flex;
      gap: 1.5rem;
      background-color: #f8f9fa;
      padding: 1rem;
      font-weight: 500;
    }

    .header-nav a {
      text-decoration: none;
      color: #333;
      transition: color 0.3s;
    }

    .header-nav a.active,
    .header-nav a:hover {
      color: #007bff;
    }
  `]
})
export class HeaderComponent {}

