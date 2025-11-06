export interface PortalUser {
  id: string;
  username: string;
  fullName: string;
  email: string;
  active: boolean;
  roles: string[];
}

export interface CreatePortalUserPayload {
  username: string;
  fullName: string;
  email: string;
  password: string;
  roles: string[];
}
