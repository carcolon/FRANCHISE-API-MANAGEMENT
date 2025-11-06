import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Franchise } from '../../../../core/models/franchise.model';
import { FranchiseApiService } from '../../../../core/services/franchise-api.service';

@Component({
  selector: 'app-franchise-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './franchise-list.component.html',
  styleUrl: './franchise-list.component.css'
})
export class FranchiseListComponent implements OnInit {
  readonly createForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3)]]
  });

  readonly franchises = signal<Franchise[]>([]);
  readonly loading = signal(false);
  readonly creating = signal(false);
  readonly error = signal<string | null>(null);
  readonly success = signal<string | null>(null);

  readonly totalBranches = computed(() =>
    this.franchises()
      .map((franchise) => franchise.branches?.length ?? 0)
      .reduce((acc, value) => acc + value, 0)
  );

  readonly totalProducts = computed(() =>
    this.franchises()
      .flatMap((franchise) => franchise.branches ?? [])
      .map((branch) => branch.products?.length ?? 0)
      .reduce((acc, value) => acc + value, 0)
  );

  constructor(
    private readonly fb: FormBuilder,
    private readonly api: FranchiseApiService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.loadFranchises();
  }

  loadFranchises(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getFranchises().subscribe({
      next: (data) => {
        this.franchises.set(data);
        this.loading.set(false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible cargar las franquicias.');
        this.loading.set(false);
      }
    });
  }

  onCreateFranchise(): void {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }
    const { name } = this.createForm.getRawValue();
    this.creating.set(true);
    this.error.set(null);
    this.success.set(null);
    this.api.createFranchise(name).subscribe({
      next: (franchise) => {
        this.createForm.reset({ name: '' });
        this.success.set(`Franquicia "${franchise.name}" creada correctamente.`);
        this.creating.set(false);
        this.loadFranchises();
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible crear la franquicia.');
        this.creating.set(false);
      }
    });
  }

  onDeleteFranchise(franchise: Franchise): void {
    if (!confirm(`Eliminar la franquicia "${franchise.name}" y todo su inventario asociado? Esta acción no se puede deshacer.`)) {
      return;
    }
    this.error.set(null);
    this.success.set(null);
    this.api.deleteFranchise(franchise.id).subscribe({
      next: () => {
        this.franchises.set(this.franchises().filter((item) => item.id !== franchise.id));
        this.success.set(`Franquicia "${franchise.name}" eliminada correctamente.`);
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible eliminar la franquicia.');
      }
    });
  }

  onNavigate(franchise: Franchise): void {
    this.router.navigate(['/franchises', franchise.id]);
  }

  getProductCount(franchise: Franchise): number {
    return (franchise.branches ?? []).reduce((total, branch) => total + (branch.products?.length ?? 0), 0);
  }
}
