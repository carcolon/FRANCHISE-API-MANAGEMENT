import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { Branch, Franchise, Product, TopProductPerBranch } from '../models/franchise.model';

interface NamePayload {
  name: string;
}

interface BranchPayload extends NamePayload {
  active?: boolean;
}

interface ProductPayload {
  name: string;
  stock: number;
}

interface StockPayload {
  stock: number;
}

@Injectable({ providedIn: 'root' })
export class FranchiseApiService {
  private readonly baseUrl = `${APP_CONFIG.apiBaseUrl}/franchises`;

  constructor(private readonly http: HttpClient) {}

  getFranchises(): Observable<Franchise[]> {
    return this.http
      .get<Franchise[]>(this.baseUrl)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getFranchise(id: string): Observable<Franchise> {
    return this.http
      .get<Franchise>(`${this.baseUrl}/${id}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  createFranchise(name: string): Observable<Franchise> {
    const payload: NamePayload = { name };
    return this.http
      .post<Franchise>(this.baseUrl, payload)
      .pipe(catchError((error) => this.handleError(error)));
  }

  updateFranchiseName(id: string, name: string): Observable<Franchise> {
    const payload: NamePayload = { name };
    return this.http
      .patch<Franchise>(`${this.baseUrl}/${id}`, payload)
      .pipe(catchError((error) => this.handleError(error)));
  }

  updateFranchiseStatus(id: string, active: boolean): Observable<Franchise> {
    return this.http
      .patch<Franchise>(`${this.baseUrl}/${id}/status`, { active })
      .pipe(catchError((error) => this.handleError(error)));
  }

  deleteFranchise(id: string): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/${id}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  addBranch(franchiseId: string, name: string, active: boolean = true): Observable<Branch> {
    const payload: BranchPayload = { name, active };
    return this.http
      .post<Branch>(`${this.baseUrl}/${franchiseId}/branches`, payload)
      .pipe(catchError((error) => this.handleError(error)));
  }

  updateBranchName(franchiseId: string, branchId: string, name: string): Observable<Branch> {
    const payload: NamePayload = { name };
    return this.http
      .patch<Branch>(`${this.baseUrl}/${franchiseId}/branches/${branchId}`, payload)
      .pipe(catchError((error) => this.handleError(error)));
  }

  updateBranchStatus(franchiseId: string, branchId: string, active: boolean): Observable<Branch> {
    return this.http
      .patch<Branch>(`${this.baseUrl}/${franchiseId}/branches/${branchId}/status`, { active })
      .pipe(catchError((error) => this.handleError(error)));
  }

  deleteBranch(franchiseId: string, branchId: string): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/${franchiseId}/branches/${branchId}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  addProduct(franchiseId: string, branchId: string, product: ProductPayload): Observable<Product> {
    return this.http
      .post<Product>(`${this.baseUrl}/${franchiseId}/branches/${branchId}/products`, product)
      .pipe(catchError((error) => this.handleError(error)));
  }

  updateProductName(franchiseId: string, branchId: string, productId: string, name: string): Observable<Product> {
    const payload: NamePayload = { name };
    return this.http
      .patch<Product>(`${this.baseUrl}/${franchiseId}/branches/${branchId}/products/${productId}`, payload)
      .pipe(catchError((error) => this.handleError(error)));
  }

  updateProductStock(
    franchiseId: string,
    branchId: string,
    productId: string,
    stock: number
  ): Observable<Product> {
    const payload: StockPayload = { stock };
    return this.http
      .patch<Product>(
        `${this.baseUrl}/${franchiseId}/branches/${branchId}/products/${productId}/stock`,
        payload
      )
      .pipe(catchError((error) => this.handleError(error)));
  }

  deleteProduct(franchiseId: string, branchId: string, productId: string): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/${franchiseId}/branches/${branchId}/products/${productId}`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  getTopProductPerBranch(franchiseId: string): Observable<TopProductPerBranch[]> {
    return this.http
      .get<TopProductPerBranch[]>(`${this.baseUrl}/${franchiseId}/branches/top-products`)
      .pipe(catchError((error) => this.handleError(error)));
  }

  private handleError(error: HttpErrorResponse) {
    const fallbackMessage = 'Ha ocurrido un error inesperado. Intenta nuevamente.';
    const message = error.error?.message ?? error.statusText ?? fallbackMessage;
    return throwError(() => ({ message, status: error.status }));
  }
}
