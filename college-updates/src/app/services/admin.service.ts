import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface AdminEntity {
  id: number;
  name: string;
  role: string;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly API_URL = 'http://localhost:8082/admin';

  constructor(private readonly http: HttpClient) {}

  getAllAdmins(): Observable<AdminEntity[]> {
    return this.http.get<AdminEntity[]>(this.API_URL);
  }

  createAdmin(admin: Omit<AdminEntity, 'id'>): Observable<AdminEntity> {
    return this.http.post<AdminEntity>(this.API_URL, admin);
  }
}
