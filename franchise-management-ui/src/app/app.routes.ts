import { Routes } from '@angular/router';
import { FranchiseListComponent } from './features/franchises/components/franchise-list/franchise-list.component';
import { FranchiseDetailComponent } from './features/franchises/components/franchise-detail/franchise-detail.component';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'franchises' },
  { path: 'franchises', component: FranchiseListComponent, title: 'Franquicias - Management' },
  { path: 'franchises/:id', component: FranchiseDetailComponent, title: 'Detalle de franquicia' },
  { path: '**', redirectTo: 'franchises' }
];
