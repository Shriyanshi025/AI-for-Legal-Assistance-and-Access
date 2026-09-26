export interface UserResponse {
  id: string;
  publicUserId: string;
  name: string | null;
  email: string | null;
  createdAt: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}
