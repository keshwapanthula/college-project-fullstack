import { Routes } from '@angular/router';
import { AdminServicesComponent } from './components/admin-services/admin-services.component';
import { NotificationsComponent } from './components/notifications/notifications.component';
import { UpdatesListComponent } from './components/updates-list/updates-list.component';

export const routes: Routes = [
  { path: '', redirectTo: 'updates', pathMatch: 'full' },
  { path: 'updates', component: UpdatesListComponent },
  { path: 'notifications', component: NotificationsComponent },
  { path: 'admin', component: AdminServicesComponent },
  { path: '**', redirectTo: 'updates' }  // catch-all route
];
