import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface Notification {
  id: number;
  title: string;
  date: string;
  description: string;
}

@Injectable({ providedIn: 'root' })
export class NotificationsService {
  private apiUrl = '/api/notifications';

  constructor(private http: HttpClient) {}

  getNotifications(): Observable<Notification[]> {
    return this.http.get<Notification[]>(this.apiUrl);
  }
}
