import { CommonModule } from '@angular/common';
import { Component, computed } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { PasswordChangeComponent } from './features/auth/password-change/password-change.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, PasswordChangeComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  readonly currentYear = new Date().getFullYear();

  readonly user = computed(() => this.authService.currentUser());
  readonly isAuthenticated = computed(() => !!this.user());
  readonly isAdmin = computed(() => this.user()?.roles.includes('ADMIN') ?? false);
  readonly mustChangePassword = computed(() => this.user()?.passwordChangeRequired ?? false);
  readonly navigation = computed(() => {
    const base = [{ label: 'Franquicias', path: '/franchises' }];
    if (this.isAdmin()) {
      base.push({ label: 'Usuarios', path: '/users' });
    }
    return base;
  });

  constructor(private readonly authService: AuthService, private readonly router: Router) {}

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
