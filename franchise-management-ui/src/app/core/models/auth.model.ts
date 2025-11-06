export interface AuthUser {
  username: string;
  roles: string[];
  token: string;
  expiresAt: number;
}

export interface AuthResponse {
  token: string;
  username: string;
  roles: string[];
  expiresAt: number;
}
