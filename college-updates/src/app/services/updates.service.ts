import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface CollegeUpdate {
  id: number;
  title: string;
  description: string;
  createdAt: string;
}

@Injectable({
  providedIn: 'root'
})
export class UpdatesService {
  private readonly API_URL = 'http://localhost:8080/collegeupdates';

  constructor(private http: HttpClient) {}

  getAllUpdates(): Observable<CollegeUpdate[]> {
    return this.http.get<CollegeUpdate[]>(this.API_URL);
  }

  createUpdate(update: Omit<CollegeUpdate, 'id' | 'createdAt'>): Observable<CollegeUpdate> {
    return this.http.post<CollegeUpdate>(this.API_URL, update);
  }
}
