import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ForgotPasswordResponse } from '../../../core/models/auth.model';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './forgot-password.component.html',
  styleUrl: './forgot-password.component.css'
})
export class ForgotPasswordComponent {
  readonly form = this.fb.nonNullable.group({
    username: ['', [Validators.required]]
  });

  readonly submitting = signal(false);
  readonly success = signal<ForgotPasswordResponse | null>(null);
  readonly error = signal<string | null>(null);

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService
  ) {}

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { username } = this.form.getRawValue();
    this.submitting.set(true);
    this.error.set(null);
    this.success.set(null);
    this.authService.forgotPassword(username).subscribe({
      next: (response) => {
        this.success.set(response);
        this.submitting.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No fue posible procesar la solicitud.');
        this.submitting.set(false);
      }
    });
  }
}
