export interface Product {
  id: string;
  name: string;
  stock: number;
}

export interface Branch {
  id: string;
  name: string;
  active: boolean;
  products: Product[];
}

export interface Franchise {
  id: string;
  name: string;
  branches: Branch[];
}

export interface TopProductPerBranch {
  branchId: string;
  branchName: string;
  product: Product;
}

export interface ApiError {
  message: string;
  status?: number;
}
