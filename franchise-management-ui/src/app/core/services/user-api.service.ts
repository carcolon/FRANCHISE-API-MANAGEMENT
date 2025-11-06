import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { APP_CONFIG } from '../config/app-config';
import { CreatePortalUserPayload, PortalUser } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserApiService {
  private readonly baseUrl = `${APP_CONFIG.apiBaseUrl}/users`;

  constructor(private readonly http: HttpClient) {}

  list(): Observable<PortalUser[]> {
    return this.http.get<PortalUser[]>(this.baseUrl);
  }

  create(payload: CreatePortalUserPayload): Observable<PortalUser> {
    return this.http.post<PortalUser>(this.baseUrl, payload);
  }

  updateStatus(userId: string, active: boolean): Observable<PortalUser> {
    return this.http.patch<PortalUser>(`${this.baseUrl}/${userId}/status`, { active });
  }

  delete(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${userId}`);
  }
}
