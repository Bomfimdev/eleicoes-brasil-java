import { Routes } from '@angular/router';
import { NationalPage } from './pages/national/national.page';
import { ComparePage, HistoryPage, LivePage } from './pages/placeholder/placeholder.page';
import { StatePage } from './pages/state/state.page';
import { StatesPage } from './pages/states/states.page';

export const routes: Routes = [
  { path: '', component: NationalPage },
  { path: 'estados', component: StatesPage },
  { path: 'ao-vivo', component: LivePage },
  { path: 'historico', component: HistoryPage },
  { path: 'comparar', component: ComparePage },
  { path: 'estado/:uf', component: StatePage },
  { path: '**', redirectTo: '' },
];
