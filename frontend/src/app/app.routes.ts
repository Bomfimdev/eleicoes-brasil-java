import { Routes } from '@angular/router';
import { NationalPage } from './pages/national/national.page';
import { StatePage } from './pages/state/state.page';

export const routes: Routes = [
  { path: '', component: NationalPage },
  { path: 'estado/:uf', component: StatePage },
  { path: '**', redirectTo: '' },
];
