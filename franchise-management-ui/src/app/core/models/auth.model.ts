export interface AuthUser {
  username: string;
  roles: string[];
  token: string;
  expiresAt: number;
  passwordChangeRequired: boolean;
}

export interface AuthResponse {
  token: string;
  username: string;
  roles: string[];
  expiresAt: number;
  passwordChangeRequired: boolean;
}

export interface ForgotPasswordResponse {
  message: string;
  resetToken?: string | null;
}

export interface MessageResponse {
  message: string;
}
