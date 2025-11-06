import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-password-change-modal',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './password-change.component.html',
  styleUrl: './password-change.component.css'
})
export class PasswordChangeComponent {
  @Input() visible = false;
  @Output() completed = new EventEmitter<void>();

  readonly form = this.fb.nonNullable.group({
    currentPassword: ['', [Validators.required]],
    newPassword: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]]
  });

  readonly submitting = signal(false);
  readonly success = signal<string | null>(null);
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
    const { currentPassword, newPassword, confirmPassword } = this.form.getRawValue();
    if (newPassword !== confirmPassword) {
      this.error.set('Las contraseñas nuevas no coinciden.');
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.success.set(null);
    this.authService.changePassword(currentPassword, newPassword).subscribe({
      next: (response) => {
        this.success.set(response.message);
        this.submitting.set(false);
        this.completed.emit();
        this.form.reset({
          currentPassword: '',
          newPassword: '',
          confirmPassword: ''
        });
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No fue posible actualizar la contraseña.');
        this.submitting.set(false);
      }
    });
  }
}
