import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges, signal } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Branch, Product } from '../../../../core/models/franchise.model';
import { FranchiseApiService } from '../../../../core/services/franchise-api.service';

interface ProductState {
  name: string;
  stock: number;
}

@Component({
  selector: 'app-branch-card',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './branch-card.component.html',
  styleUrl: './branch-card.component.css'
})
export class BranchCardComponent implements OnChanges {
  @Input({ required: true }) franchiseId!: string;
  @Input({ required: true }) branch!: Branch;
  @Input() adminMode = false;
  @Output() branchUpdated = new EventEmitter<Branch>();
  @Output() branchDeleted = new EventEmitter<string>();

  readonly branchState = signal<Branch | null>(null);
  readonly branchUpdating = signal(false);
  readonly branchStatusPending = signal(false);
  readonly branchDeleting = signal(false);
  readonly addProductPending = signal(false);
  readonly productPending = signal<Record<string, boolean>>({});
  readonly success = signal<string | null>(null);
  readonly error = signal<string | null>(null);

  readonly branchForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(3)]]
  });

  readonly addProductForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    stock: [0, [Validators.required, Validators.min(0)]]
  });

  readonly productStates = signal<Record<string, ProductState>>({});

  constructor(
    private readonly fb: FormBuilder,
    private readonly api: FranchiseApiService
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['branch'] && this.branch) {
      const branchCopy: Branch = {
        ...this.branch,
        products: (this.branch.products ?? []).map((product) => ({ ...product }))
      };
      this.branchState.set(branchCopy);
      this.branchForm.patchValue({ name: this.branch.name });
      if (this.adminMode) {
        this.branchForm.enable({ emitEvent: false });
      } else {
        this.branchForm.disable({ emitEvent: false });
      }
      this.resetProductStates(this.branch.products ?? []);
      this.syncAddProductFormState(branchCopy.active);
    }
  }

  onRenameBranch(): void {
    if (!this.adminMode) {
      return;
    }
    if (this.branchForm.invalid || !this.branchState()) {
      this.branchForm.markAllAsTouched();
      return;
    }
    const branchName = this.branchForm.controls.name.value;
    this.branchUpdating.set(true);
    this.clearMessages();
    this.api.updateBranchName(this.franchiseId, this.branch.id, branchName).subscribe({
      next: (updated) => {
        this.applyBranchUpdate(updated);
        this.success.set('Nombre de sucursal actualizado.');
        this.branchUpdating.set(false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible actualizar la sucursal.');
        this.branchUpdating.set(false);
      }
    });
  }

  onToggleBranchStatus(): void {
    if (!this.adminMode) {
      return;
    }
    const current = this.branchState();
    if (!current) {
      return;
    }
    this.branchStatusPending.set(true);
    this.clearMessages();
    this.api.updateBranchStatus(this.franchiseId, current.id, !current.active).subscribe({
      next: (updated) => {
        this.applyBranchUpdate(updated);
        this.success.set(updated.active ? 'Sucursal activada.' : 'Sucursal desactivada.');
        this.branchStatusPending.set(false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible actualizar el estado de la sucursal.');
        this.branchStatusPending.set(false);
      }
    });
  }

  onDeleteBranch(): void {
    if (!this.adminMode) {
      return;
    }
    const current = this.branchState();
    if (!current) {
      return;
    }
    if (!confirm(`Eliminar la sucursal "${current.name}"? Se eliminarán sus productos asociados.`)) {
      return;
    }
    this.branchDeleting.set(true);
    this.clearMessages();
    this.api.deleteBranch(this.franchiseId, current.id).subscribe({
      next: () => {
        this.branchDeleted.emit(current.id);
        this.success.set('Sucursal eliminada correctamente.');
        this.branchDeleting.set(false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible eliminar la sucursal.');
        this.branchDeleting.set(false);
      }
    });
  }

  onAddProduct(): void {
    if (!this.adminMode) {
      return;
    }
    if (this.addProductForm.invalid || !this.branchState()) {
      this.addProductForm.markAllAsTouched();
      return;
    }
    if (!this.branchState()?.active) {
      this.error.set('La sucursal está inactiva. Actívala para agregar productos.');
      return;
    }
    const { name, stock } = this.addProductForm.getRawValue();
    this.addProductPending.set(true);
    this.clearMessages();
    this.api.addProduct(this.franchiseId, this.branch.id, { name, stock }).subscribe({
      next: (product) => {
        const current = this.branchState();
        if (current) {
          const updatedBranch: Branch = {
            ...current,
            products: [product, ...(current.products ?? [])]
          };
          this.applyBranchUpdate(updatedBranch);
          this.success.set('Producto agregado a la sucursal.');
        }
        this.addProductForm.reset({ name: '', stock: 0 });
        this.addProductPending.set(false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible agregar el producto.');
        this.addProductPending.set(false);
      }
    });
  }

  onUpdateProductName(product: Product): void {
    if (!this.adminMode) {
      return;
    }
    const state = this.productStates()[product.id];
    if (!state || !state.name?.trim()) {
      this.error.set('El nombre del producto es obligatorio.');
      return;
    }
    this.setProductPending(product.id, true);
    this.clearMessages();
    this.api.updateProductName(this.franchiseId, this.branch.id, product.id, state.name.trim()).subscribe({
      next: (updated) => {
        this.applyProductUpdate(updated);
        this.success.set('Nombre del producto actualizado.');
        this.setProductPending(product.id, false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible actualizar el nombre.');
        this.setProductPending(product.id, false);
      }
    });
  }

  onUpdateProductStock(product: Product): void {
    if (!this.adminMode) {
      return;
    }
    const state = this.productStates()[product.id];
    if (state === undefined || state.stock === null || state.stock < 0 || Number.isNaN(state.stock)) {
      this.error.set('El stock debe ser un numero mayor o igual a 0.');
      return;
    }
    this.setProductPending(product.id, true);
    this.clearMessages();
    this.api.updateProductStock(this.franchiseId, this.branch.id, product.id, state.stock).subscribe({
      next: (updated) => {
        this.applyProductUpdate(updated);
        this.success.set('Stock del producto actualizado.');
        this.setProductPending(product.id, false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible actualizar el stock.');
        this.setProductPending(product.id, false);
      }
    });
  }

  onDeleteProduct(product: Product): void {
    if (!this.adminMode) {
      return;
    }
    if (!confirm(`Eliminar el producto "${product.name}"? Esta accion no se puede deshacer.`)) {
      return;
    }
    this.setProductPending(product.id, true);
    this.clearMessages();
    this.api.deleteProduct(this.franchiseId, this.branch.id, product.id).subscribe({
      next: () => {
        const current = this.branchState();
        if (current) {
          const updatedBranch: Branch = {
            ...current,
            products: (current.products ?? []).filter((item) => item.id !== product.id)
          };
          this.applyBranchUpdate(updatedBranch);
          this.success.set('Producto eliminado correctamente.');
        }
        this.setProductPending(product.id, false);
      },
      error: (err) => {
        this.error.set(err.message ?? 'No fue posible eliminar el producto.');
        this.setProductPending(product.id, false);
      }
    });
  }

  onStateChange(product: Product, key: keyof ProductState, value: string | number): void {
    const snapshot = { ...this.productStates() };
    const current = snapshot[product.id] ?? { name: product.name, stock: product.stock };
    const nextValue = key === 'stock' ? Number(value) : String(value);
    const updated: ProductState = {
      name: key === 'name' ? (nextValue as string) : current.name,
      stock: key === 'stock'
        ? (Number.isNaN(nextValue as number) ? current.stock : (nextValue as number))
        : current.stock
    };
    snapshot[product.id] = updated;
    this.productStates.set(snapshot);
  }

  private applyProductUpdate(updated: Product): void {
    const current = this.branchState();
    if (!current) {
      return;
    }
    const products = (current.products ?? []).map((item) => (item.id === updated.id ? updated : item));
    const updatedBranch: Branch = { ...current, products };
    this.applyBranchUpdate(updatedBranch);
  }

  private resetProductStates(products: Product[]): void {
    const entries: Record<string, ProductState> = {};
    for (const product of products) {
      entries[product.id] = { name: product.name, stock: product.stock };
    }
    this.productStates.set(entries);
  }

  private setProductPending(productId: string, pending: boolean): void {
    const nextState = { ...this.productPending(), [productId]: pending };
    this.productPending.set(nextState);
  }

  private clearMessages(): void {
    this.success.set(null);
    this.error.set(null);
  }

  private applyBranchUpdate(updated: Branch): void {
    const normalized: Branch = {
      ...updated,
      products: (updated.products ?? []).map((product) => ({ ...product }))
    };
    this.branchState.set(normalized);
    this.branchForm.patchValue({ name: normalized.name });
    this.syncAddProductFormState(normalized.active);
    this.resetProductStates(normalized.products ?? []);
    this.branchUpdated.emit(normalized);
  }

  private syncAddProductFormState(active: boolean): void {
    if (!this.adminMode) {
      this.addProductForm.disable({ emitEvent: false });
      return;
    }
    if (active) {
      this.addProductForm.enable({ emitEvent: false });
    } else {
      this.addProductForm.disable({ emitEvent: false });
    }
  }
}
