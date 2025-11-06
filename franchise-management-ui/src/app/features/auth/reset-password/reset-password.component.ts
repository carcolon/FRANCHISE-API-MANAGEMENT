import { CommonModule } from '@angular/common';
import { Component, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.css'
})
export class ResetPasswordComponent {
  readonly form = this.fb.nonNullable.group({
    token: ['', [Validators.required]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  });

  readonly submitting = signal(false);
  readonly validatingToken = signal(false);
  readonly success = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  readonly tokenValid = signal(false);
  readonly tokenInfo = signal<string | null>(null);

  readonly passwordSectionDisabled = computed(() => !this.tokenValid());

  constructor(
    private readonly fb: FormBuilder,
    private readonly authService: AuthService,
    private readonly route: ActivatedRoute,
    private readonly router: Router
  ) {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (token) {
      this.form.patchValue({ token });
      this.tokenValid.set(true);
      this.tokenInfo.set('Token prellenado desde el enlace.');
    }

    this.form.controls.token.valueChanges.subscribe(() => {
      this.tokenValid.set(false);
      this.tokenInfo.set(null);
    });
  }

  onValidateToken(): void {
    const tokenControl = this.form.controls.token;
    if (tokenControl.invalid) {
      tokenControl.markAsTouched();
      return;
    }
    const token = tokenControl.value;
    this.validatingToken.set(true);
    this.tokenInfo.set(null);
    this.error.set(null);
    this.success.set(null);
    this.authService.validateResetToken(token).subscribe({
      next: (response) => {
        this.tokenValid.set(true);
        this.tokenInfo.set(response.message);
        this.validatingToken.set(false);
      },
      error: (err) => {
        this.tokenValid.set(false);
        this.tokenInfo.set(null);
        this.error.set(err?.error?.message ?? 'El token no es valido.');
        this.validatingToken.set(false);
      }
    });
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (!this.tokenValid()) {
      this.error.set('Valida el token antes de restablecer la contrasena.');
      return;
    }
    const { token, password, confirmPassword } = this.form.getRawValue();
    if (password !== confirmPassword) {
      this.error.set('Las contrasenas no coinciden.');
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.success.set(null);
    this.authService.resetPassword(token, password).subscribe({
      next: (response) => {
        this.success.set(response.message);
        this.submitting.set(false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No fue posible restablecer la contrasena.');
        this.submitting.set(false);
      }
    });
  }

  goToLogin(): void {
    this.router.navigate(['/login']);
  }
}
