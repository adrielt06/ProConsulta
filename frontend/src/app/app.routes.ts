import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard';
import { PlaceholderComponent } from './features/placeholder/placeholder';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'metricas' },
  { path: 'metricas', component: DashboardComponent },
  { path: 'pacientes', component: PlaceholderComponent, data: { navKey: 'pacientes' } },
  { path: 'agenda', component: PlaceholderComponent, data: { navKey: 'agenda' } },
  { path: 'pagos', component: PlaceholderComponent, data: { navKey: 'pagos' } },
  { path: 'servicios', component: PlaceholderComponent, data: { navKey: 'servicios' } },
  { path: 'perfil', component: PlaceholderComponent, data: { navKey: 'perfil' } },
  { path: '**', redirectTo: 'metricas' },
];