import { Routes } from '@angular/router';
import { FranchiseListComponent } from './features/franchises/components/franchise-list/franchise-list.component';
import { FranchiseDetailComponent } from './features/franchises/components/franchise-detail/franchise-detail.component';
import { LoginComponent } from './features/auth/login/login.component';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'franchises' },
  { path: 'login', component: LoginComponent, title: 'Iniciar sesión' },
  { path: 'franchises', component: FranchiseListComponent, title: 'Franquicias - Management', canActivate: [authGuard] },
  { path: 'franchises/:id', component: FranchiseDetailComponent, title: 'Detalle de franquicia', canActivate: [authGuard] },
  { path: '**', redirectTo: 'franchises' }
];
