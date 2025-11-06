import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { AuthResponse, AuthUser, ForgotPasswordResponse, MessageResponse } from '../models/auth.model';

interface LoginPayload {
  username: string;
  password: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly storageKey = 'franchise-auth';
  private readonly authBaseUrl = `${APP_CONFIG.apiBaseUrl}/auth`;

  readonly currentUser = signal<AuthUser | null>(this.restoreSession());
  readonly authenticating = signal(false);
  readonly error = signal<string | null>(null);

  constructor(private readonly http: HttpClient) {}

  login(username: string, password: string): Observable<AuthResponse> {
    const payload: LoginPayload = { username, password };
    this.authenticating.set(true);
    this.error.set(null);
    return this.http.post<AuthResponse>(`${this.authBaseUrl}/login`, payload).pipe(
      tap({
        next: (response) => {
          const user: AuthUser = {
            username: response.username,
            roles: response.roles,
            token: response.token,
            expiresAt: response.expiresAt,
            passwordChangeRequired: response.passwordChangeRequired
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

  forgotPassword(username: string): Observable<ForgotPasswordResponse> {
    return this.http.post<ForgotPasswordResponse>(`${this.authBaseUrl}/forgot-password`, { username });
  }

  resetPassword(token: string, password: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.authBaseUrl}/reset-password`, {
      token,
      newPassword: password
    });
  }

  validateResetToken(token: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.authBaseUrl}/validate-reset-token`, { token });
  }

  changePassword(currentPassword: string, newPassword: string): Observable<MessageResponse> {
    return this.http.post<MessageResponse>(`${this.authBaseUrl}/change-password`, {
      currentPassword,
      newPassword
    }).pipe(
      tap(() => {
        const user = this.currentUser();
        if (user) {
          const updated: AuthUser = { ...user, passwordChangeRequired: false };
          this.persist(updated);
          this.currentUser.set(updated);
        }
      })
    );
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
        return {
          ...parsed,
          passwordChangeRequired: parsed.passwordChangeRequired ?? false
        };
      }
    } catch {
      localStorage.removeItem(this.storageKey);
    }
    localStorage.removeItem(this.storageKey);
    return null;
  }
}
