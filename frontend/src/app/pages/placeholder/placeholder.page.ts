import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-placeholder-page',
  standalone: true,
  imports: [RouterLink],
  template: `
    <header class="page-head">
      <h1>{{ title }}</h1>
      <p class="sub">{{ subtitle }}</p>
    </header>
    <div class="card">
      <p>
        Esta seção existe no layout (igual ao original), mas o conteúdo completo fica para o pós-MVP.
      </p>
      <a routerLink="/">Voltar à visão geral</a>
    </div>
  `,
  styles: [
    `
      .page-head {
        margin-bottom: 1.25rem;
      }
      h1 {
        margin: 0;
        font-size: 1.4rem;
        font-weight: 650;
        letter-spacing: -0.02em;
      }
      .sub {
        margin: 0.35rem 0 0;
        color: var(--muted);
        font-size: 0.85rem;
      }
      .card {
        border: 1px solid var(--line);
        border-radius: 0.65rem;
        background: var(--surface);
        padding: 1.25rem;
        color: var(--ink-2);
      }
      a {
        display: inline-block;
        margin-top: 0.75rem;
        color: var(--info);
      }
    `,
  ],
})
export class PlaceholderPage {
  @Input() title = 'Em breve';
  @Input() subtitle = 'Seção em preparação.';
}

@Component({
  selector: 'app-live-page',
  standalone: true,
  imports: [PlaceholderPage],
  template: `<app-placeholder-page title="Ao vivo" subtitle="Feed de operações e ingestão — pós-MVP" />`,
})
export class LivePage {}

@Component({
  selector: 'app-history-page',
  standalone: true,
  imports: [PlaceholderPage],
  template: `<app-placeholder-page title="Histórico" subtitle="Evolução da apuração — pós-MVP" />`,
})
export class HistoryPage {}

@Component({
  selector: 'app-compare-page',
  standalone: true,
  imports: [PlaceholderPage],
  template: `<app-placeholder-page title="Comparar" subtitle="Comparação entre estados — pós-MVP" />`,
})
export class ComparePage {}
