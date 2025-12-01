import { CommonModule } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CreatePortalUserPayload, PortalUser } from '../../../core/models/user.model';
import { UserApiService } from '../../../core/services/user-api.service';
import { AuthService } from '../../../core/services/auth.service';

interface ToggleState {
  [userId: string]: boolean;
}

@Component({
  selector: 'app-user-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './user-management.component.html',
  styleUrl: './user-management.component.css'
})
export class UserManagementComponent implements OnInit {
  readonly createForm = this.fb.nonNullable.group({
    username: ['', [Validators.required, Validators.minLength(3)]],
    fullName: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    confirmPassword: ['', [Validators.required]],
    roleAdmin: [false],
    roleUser: [true]
  });

  readonly users = signal<PortalUser[]>([]);
  readonly loading = signal(false);
  readonly creating = signal(false);
  readonly toggling = signal<ToggleState>({});
  readonly deleting = signal<ToggleState>({});
  readonly resetting = signal<ToggleState>({});
  readonly passwordFormsOpen = signal<ToggleState>({});
  readonly passwordMismatch = signal<ToggleState>({});
  readonly success = signal<string | null>(null);
  readonly error = signal<string | null>(null);
  private readonly resetForms = new Map<string, FormGroup>();

  constructor(
    private readonly fb: FormBuilder,
    private readonly userApi: UserApiService,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.error.set(null);
    this.userApi.list().subscribe({
      next: (users) => {
        this.users.set(users);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err?.message ?? 'No fue posible obtener los usuarios.');
        this.loading.set(false);
      }
    });
  }

  onCreateUser(): void {
    this.error.set(null);
    this.success.set(null);
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    const { password, confirmPassword } = this.createForm.getRawValue();
    if (password !== confirmPassword) {
      this.error.set('Las contrasenas no coinciden.');
      return;
    }
    const payload = this.buildPayload();
    this.creating.set(true);
    this.error.set(null);
    this.success.set(null);
    this.userApi.create(payload).subscribe({
      next: (user) => {
        this.success.set(`Usuario "${user.username}" creado correctamente.`);
        this.creating.set(false);
        this.resetForm();
        const current = this.users();
        this.users.set([user, ...current]);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No fue posible crear el usuario.');
        this.creating.set(false);
      }
    });
  }

  toggleStatus(user: PortalUser): void {
    const currentState = this.toggling();
    this.error.set(null);
    this.success.set(null);
    this.toggling.set({ ...currentState, [user.id]: true });
    this.userApi.updateStatus(user.id, !user.active).subscribe({
      next: (updated) => {
        const nextUsers = this.users().map((item) => (item.id === updated.id ? updated : item));
        this.users.set(nextUsers);
        this.success.set(
          updated.active
            ? `Usuario "${updated.username}" activado correctamente.`
            : `Usuario "${updated.username}" desactivado correctamente.`
        );
        this.toggling.set({ ...this.toggling(), [user.id]: false });
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No fue posible actualizar el estado.');
        this.toggling.set({ ...this.toggling(), [user.id]: false });
      }
    });
  }

  isToggling(userId: string): boolean {
    return this.toggling()[userId] ?? false;
  }

  isDeleting(userId: string): boolean {
    return this.deleting()[userId] ?? false;
  }

  isResetting(userId: string): boolean {
    return this.resetting()[userId] ?? false;
  }

  isPasswordFormOpen(userId: string): boolean {
    return this.passwordFormsOpen()[userId] ?? false;
  }

  getResetForm(userId: string): FormGroup {
    if (!this.resetForms.has(userId)) {
      this.resetForms.set(userId, this.buildResetForm());
    }
    return this.resetForms.get(userId)!;
  }

  togglePasswordForm(userId: string): void {
    const nextValue = !this.isPasswordFormOpen(userId);
    this.setPasswordFormOpen(userId, nextValue);
    if (nextValue) {
      this.getResetForm(userId);
    }
  }

  hasPasswordMismatch(userId: string): boolean {
    return this.passwordMismatch()[userId] ?? false;
  }

  onResetPassword(user: PortalUser): void {
    const form = this.getResetForm(user.id);
    form.markAllAsTouched();
    this.error.set(null);
    this.success.set(null);
    if (form.invalid) {
      return;
    }
    const { newPassword, confirmPassword } = form.getRawValue() as { newPassword: string; confirmPassword: string };
    if (newPassword !== confirmPassword) {
      this.setPasswordMismatch(user.id, true);
      return;
    }
    this.setPasswordMismatch(user.id, false);
    this.setResetting(user.id, true);
    this.userApi.resetPassword(user.id, newPassword).subscribe({
      next: (updated) => {
        const nextUsers = this.users().map((item) => (item.id === updated.id ? updated : item));
        this.users.set(nextUsers);
        this.success.set(`Contrasena de "${updated.username}" actualizada correctamente.`);
        this.setResetting(user.id, false);
        this.setPasswordFormOpen(user.id, false);
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No fue posible actualizar la contrasena.');
        this.setResetting(user.id, false);
      }
    });
  }

  deleteUser(user: PortalUser): void {
    if (!confirm(`Eliminar al usuario "${user.username}"? Esta accion no se puede deshacer.`)) {
      return;
    }
    this.error.set(null);
    this.success.set(null);
    const current = this.deleting();
    this.deleting.set({ ...current, [user.id]: true });
    this.userApi.delete(user.id).subscribe({
      next: () => {
        this.users.set(this.users().filter((item) => item.id !== user.id));
        this.success.set(`Usuario "${user.username}" eliminado correctamente.`);
        this.deleting.set({ ...this.deleting(), [user.id]: false });
      },
      error: (err) => {
        this.error.set(err?.error?.message ?? 'No fue posible eliminar el usuario.');
        this.deleting.set({ ...this.deleting(), [user.id]: false });
      }
    });
  }

  isCurrentUser(user: PortalUser): boolean {
    return this.authService.currentUser()?.username === user.username;
  }

  getRolesLabel(user: PortalUser): string {
    return user.roles.join(', ');
  }

  private buildResetForm(): FormGroup {
    return this.fb.nonNullable.group({
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]]
    });
  }

  private setPasswordFormOpen(userId: string, value: boolean): void {
    this.passwordFormsOpen.set({ ...this.passwordFormsOpen(), [userId]: value });
    if (!value) {
      const form = this.resetForms.get(userId);
      form?.reset({
        newPassword: '',
        confirmPassword: ''
      });
      this.setPasswordMismatch(userId, false);
    }
  }

  private setPasswordMismatch(userId: string, value: boolean): void {
    this.passwordMismatch.set({ ...this.passwordMismatch(), [userId]: value });
  }

  private setResetting(userId: string, value: boolean): void {
    this.resetting.set({ ...this.resetting(), [userId]: value });
  }

  private buildPayload(): CreatePortalUserPayload {
    const { username, fullName, email, password, roleAdmin, roleUser } = this.createForm.getRawValue();
    const roles: string[] = [];
    if (roleUser) {
      roles.push('USER');
    }
    if (roleAdmin) {
      roles.push('ADMIN');
    }
    if (roles.length === 0) {
      roles.push('USER');
    }
    return {
      username: username.trim(),
      fullName: fullName.trim(),
      email: email.trim(),
      password,
      roles
    };
  }

  private resetForm(): void {
    this.createForm.reset({
      username: '',
      fullName: '',
      email: '',
      password: '',
      confirmPassword: '',
      roleAdmin: false,
      roleUser: true
    });
  }
}
