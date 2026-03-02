import { Routes } from '@angular/router';
import { AdminServices } from './components/admin-services/admin-services';
import { Notifications } from './components/notifications/notifications';
import { UpdatesList } from './components/updates-list/updates-list';

export const routes: Routes = [
  { path: '', redirectTo: 'updates', pathMatch: 'full' },
  { path: 'updates', component: UpdatesList },
  { path: 'notifications', component: Notifications },
  { path: 'admin', component: AdminServices },
  { path: '**', redirectTo: 'updates' }  // catch-all route
];
