import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { AuthResponse, AuthUser } from '../models/auth.model';

interface LoginPayload {
  username: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'franchise-auth';
  private readonly authUrl = `${APP_CONFIG.apiBaseUrl}/auth/login`;

  readonly currentUser = signal<AuthUser | null>(this.restoreSession());
  readonly authenticating = signal(false);
  readonly error = signal<string | null>(null);

  constructor(private readonly http: HttpClient) {}

  login(username: string, password: string): Observable<AuthResponse> {
    const payload: LoginPayload = { username, password };
    this.authenticating.set(true);
    this.error.set(null);
    return this.http.post<AuthResponse>(this.authUrl, payload).pipe(
      tap({
        next: (response) => {
          const user: AuthUser = {
            username: response.username,
            roles: response.roles,
            token: response.token,
            expiresAt: response.expiresAt
          };
          this.persist(user);
          this.currentUser.set(user);
          this.authenticating.set(false);
        },
        error: (err) => {
          this.error.set(err?.error?.message ?? 'Credenciales invalidas.');
          this.authenticating.set(false);
        }
      })
    );
  }

  logout(): void {
    this.currentUser.set(null);
    localStorage.removeItem(this.storageKey);
  }

  getToken(): string | null {
    return this.currentUser()?.token ?? null;
  }

  isAuthenticated(): boolean {
    return !!this.currentUser();
  }

  hasRole(role: string): boolean {
    return this.currentUser()?.roles.includes(role) ?? false;
  }

  isAdmin(): boolean {
    return this.hasRole('ADMIN');
  }

  private persist(user: AuthUser): void {
    localStorage.setItem(this.storageKey, JSON.stringify(user));
  }

  private restoreSession(): AuthUser | null {
    const stored = localStorage.getItem(this.storageKey);
    if (!stored) {
      return null;
    }
    try {
      const parsed = JSON.parse(stored) as AuthUser;
      if (parsed.expiresAt && parsed.expiresAt > Date.now()) {
        return parsed;
      }
    } catch {
      localStorage.removeItem(this.storageKey);
    }
    localStorage.removeItem(this.storageKey);
    return null;
  }
}
