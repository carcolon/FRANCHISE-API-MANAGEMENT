import { Routes } from '@angular/router';
import { FranchiseListComponent } from './features/franchises/components/franchise-list/franchise-list.component';
import { FranchiseDetailComponent } from './features/franchises/components/franchise-detail/franchise-detail.component';
import { LoginComponent } from './features/auth/login/login.component';
import { ForgotPasswordComponent } from './features/auth/forgot-password/forgot-password.component';
import { ResetPasswordComponent } from './features/auth/reset-password/reset-password.component';
import { authGuard, adminGuard } from './core/guards/auth.guard';
import { UserManagementComponent } from './features/admin/user-management/user-management.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'franchises' },
  { path: 'login', component: LoginComponent, title: 'Iniciar sesion' },
  { path: 'forgot-password', component: ForgotPasswordComponent, title: 'Recuperar contrasena' },
  { path: 'reset-password', component: ResetPasswordComponent, title: 'Restablecer contrasena' },
  { path: 'franchises', component: FranchiseListComponent, title: 'Franquicias - Management', canActivate: [authGuard] },
  { path: 'franchises/:id', component: FranchiseDetailComponent, title: 'Detalle de franquicia', canActivate: [authGuard] },
  { path: 'users', component: UserManagementComponent, title: 'Administrar usuarios', canActivate: [adminGuard] },
  { path: '**', redirectTo: 'franchises' }
];
