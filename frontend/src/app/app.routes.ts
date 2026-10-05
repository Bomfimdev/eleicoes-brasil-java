import { Routes } from '@angular/router';
import { CityPage } from './pages/city/city.page';
import { ComparePage } from './pages/compare/compare.page';
import { HistoryPage } from './pages/history/history.page';
import { LivePage } from './pages/live/live.page';
import { NationalPage } from './pages/national/national.page';
import { StatePage } from './pages/state/state.page';
import { StatesPage } from './pages/states/states.page';

export const routes: Routes = [
  { path: '', component: NationalPage },
  { path: 'estados', component: StatesPage },
  { path: 'ao-vivo', component: LivePage },
  { path: 'historico', component: HistoryPage },
  { path: 'comparar', component: ComparePage },
  { path: 'estado/:uf/municipio/:code', component: CityPage },
  { path: 'estado/:uf', component: StatePage },
  { path: '**', redirectTo: '' },
];
