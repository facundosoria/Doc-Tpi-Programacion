import { Routes } from '@angular/router';
import { WorkbenchComponent } from './workbench.component';

export const routes: Routes = [
  { path: 'golden-sets/new', component: WorkbenchComponent },
  { path: 'golden-sets/:id', component: WorkbenchComponent },
  { path: 'golden-sets', component: WorkbenchComponent },
  { path: '', pathMatch: 'full', redirectTo: 'golden-sets' },
  { path: '**', redirectTo: 'golden-sets' },
];
