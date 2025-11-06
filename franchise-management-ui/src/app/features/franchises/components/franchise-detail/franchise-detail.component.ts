import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription } from 'rxjs';
import { Branch, Franchise, TopProductPerBranch } from '../../../../core/models/franchise.model';
import { FranchiseApiService } from '../../../../core/services/franchise-api.service';
import { AuthService } from '../../../../core/services/auth.service';
import { BranchCardComponent } from '../branch-card/branch-card.component';

@Component({
  selector: 'app-franchise-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, BranchCardComponent],
  templateUrl: './franchise-detail.component.html',
  styleUrl: './franchise-detail.component.css'
})
export class FranchiseDetailComponent implements OnInit, OnDestroy {
  readonly franchise = signal<Franchise | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);
  readonly topProducts = signal<TopProductPerBranch[]>([]);
  readonly topProductsLoading = signal(false);
  readonly branchCreating = signal(false);
  readonly franchiseSaving = signal(false);
  readonly franchiseStatusUpdating = signal(false);
  readonly deletingFranchise = signal(false);
  readonly isAdmin = computed(() => this.authService.currentUser()?.roles.includes('ADMIN') ?? false);

  readonly franchiseForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3)]]
  });

  readonly branchForm = this.fb.group({
    name: this.fb.nonNullable.control('', [Validators.required, Validators.minLength(3)]),
    active: this.fb.nonNullable.control(true)
  });

  private franchiseId: string | null = null;
  private routeSub?: Subscription;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly fb: FormBuilder,
    private readonly api: FranchiseApiService,
    private readonly router: Router,
    private readonly authService: AuthService
  ) {}

  ngOnInit(): void {
    this.routeSub = this.route.paramMap.subscribe((params) => {
      const id = params.get('id');
      if (!id) {
        this.router.navigate(['/franchises']);
        return;
      }
      this.franchiseId = id;
      this.loadFranchise(id);
    });
  }

  ngOnDestroy(): void {
    this.routeSub?.unsubscribe();
  }

  loadFranchise(id: string): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getFranchise(id).subscribe({
      next: (data) => {
        this.franchise.set(data);
        this.franchiseForm.patchValue({ name: data.name });
        this.loading.set(false);
        if (data.active) {
          this.fetchTopProducts();
        } else {
          this.topProducts.set([]);
        }
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible obtener la franquicia.');
        this.loading.set(false);
      }
    });
  }

  onUpdateFranchise(): void {
    if (!this.franchiseId) {
      return;
    }
    if (!this.isAdmin()) {
      this.error.set('Acción permitida solo para administradores.');
      return;
    }
    if (this.franchiseForm.invalid) {
      this.franchiseForm.markAllAsTouched();
      return;
    }
    const { name } = this.franchiseForm.getRawValue();
    this.franchiseSaving.set(true);
    this.error.set(null);
    this.success.set(null);
    this.api.updateFranchiseName(this.franchiseId, name).subscribe({
      next: (updated) => {
        this.franchise.set({ ...updated });
        this.success.set('Nombre de franquicia actualizado.');
        this.franchiseSaving.set(false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible actualizar la franquicia.');
        this.franchiseSaving.set(false);
      }
    });
  }

  onCreateBranch(): void {
    if (!this.franchiseId) {
      return;
    }
    if (!this.isAdmin()) {
      this.error.set('Acción permitida solo para administradores.');
      return;
    }
    const current = this.franchise();
    if (current && !current.active) {
      this.error.set('Activa la franquicia para crear nuevas sucursales.');
      return;
    }
    if (this.branchForm.invalid) {
      this.branchForm.markAllAsTouched();
      return;
    }
    const { name, active } = this.branchForm.getRawValue();
    this.branchCreating.set(true);
    this.error.set(null);
    this.success.set(null);
    this.api.addBranch(this.franchiseId, name ?? '', active ?? true).subscribe({
      next: (branch) => {
        const current = this.franchise();
        if (current) {
          const updated: Franchise = {
            ...current,
            branches: [branch, ...(current.branches ?? [])]
          };
          this.franchise.set(updated);
        }
        this.branchForm.reset({ name: '', active: true });
        this.success.set('Sucursal creada exitosamente.');
        this.branchCreating.set(false);
        this.fetchTopProducts();
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible crear la sucursal.');
        this.branchCreating.set(false);
      }
    });
  }

  onBranchUpdated(updatedBranch: Branch): void {
    const current = this.franchise();
    if (!current) {
      return;
    }
    const branches = (current.branches ?? []).map((branch) =>
      branch.id === updatedBranch.id ? { ...updatedBranch } : branch
    );
    this.franchise.set({ ...current, branches });
    this.fetchTopProducts(false);
  }

  onBranchDeleted(branchId: string): void {
    const current = this.franchise();
    if (!current) {
      return;
    }
    const branches = (current.branches ?? []).filter((branch) => branch.id !== branchId);
    this.franchise.set({ ...current, branches });
    this.success.set('Sucursal eliminada correctamente.');
    this.fetchTopProducts();
  }

  onDeleteFranchise(): void {
    if (!this.franchiseId) {
      return;
    }
    if (!this.isAdmin()) {
      this.error.set('Acción permitida solo para administradores.');
      return;
    }
    if (!confirm('Eliminar esta franquicia eliminará todas sus sucursales y productos. ¿Deseas continuar?')) {
      return;
    }
    this.deletingFranchise.set(true);
    this.error.set(null);
    this.success.set(null);
    this.api.deleteFranchise(this.franchiseId).subscribe({
      next: () => {
        this.success.set('Franquicia eliminada correctamente.');
        this.deletingFranchise.set(false);
        this.router.navigate(['/franchises']);
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible eliminar la franquicia.');
        this.deletingFranchise.set(false);
      }
    });
  }

  onToggleFranchiseStatus(): void {
    const franchise = this.franchise();
    if (!this.franchiseId || !franchise) {
      return;
    }
    if (!this.isAdmin()) {
      this.error.set('Acción permitida solo para administradores.');
      return;
    }
    const targetStatus = !franchise.active;
    this.franchiseStatusUpdating.set(true);
    this.error.set(null);
    this.success.set(null);
    this.api.updateFranchiseStatus(this.franchiseId, targetStatus).subscribe({
      next: (updated) => {
        this.franchise.set(updated);
        this.success.set(updated.active ? 'Franquicia activada.' : 'Franquicia desactivada.');
        this.franchiseStatusUpdating.set(false);
        if (!updated.active) {
          this.topProducts.set([]);
        } else {
          this.fetchTopProducts();
        }
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible actualizar el estado de la franquicia.');
        this.franchiseStatusUpdating.set(false);
      }
    });
  }

  fetchTopProducts(reset: boolean = true): void {
    if (!this.franchiseId) {
      return;
    }
    if (reset) {
      this.topProducts.set([]);
    }
    this.topProductsLoading.set(true);
    this.api.getTopProductPerBranch(this.franchiseId).subscribe({
      next: (data) => {
        this.topProducts.set(data);
        this.topProductsLoading.set(false);
      },
      error: () => {
        this.topProducts.set([]);
        this.topProductsLoading.set(false);
      }
    });
  }
}
