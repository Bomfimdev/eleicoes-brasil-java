import { Component, OnInit, effect, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { fmtInt, fmtPct } from '../../core/format';
import { LiveSessionService } from '../../core/live-session.service';
import { CompareDto, OfficeDto } from '../../core/models';
import { DOMESTIC_STATES } from '../../core/states';

const MAX = 8;

@Component({
  selector: 'app-compare-page',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './compare.page.html',
  styleUrl: './compare.page.scss',
})
export class ComparePage implements OnInit {
  private readonly api = inject(ApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly live = inject(LiveSessionService);

  readonly states = DOMESTIC_STATES;
  readonly selected = signal<string[]>(['SP', 'MG', 'RJ']);
  readonly office = signal<string>('');
  readonly offices = signal<OfficeDto[]>([]);
  readonly data = signal<CompareDto | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly fmtPct = fmtPct;
  readonly fmtInt = fmtInt;
  readonly max = MAX;

  constructor() {
    effect(() => {
      const overview = this.live.overview();
      if (!overview) return;
      const offs = overview.round.offices.filter((x) => x.scope !== 'city');
      this.offices.set(offs);
      if (!this.office() && offs[0]) this.office.set(offs[0].slug);
    });

    effect(() => {
      const slug = this.live.roundSlug();
      const selected = this.selected();
      const office = this.office();
      this.live.version();
      if (!selected.length) {
        this.data.set(null);
        this.loading.set(false);
        return;
      }
      this.loading.set(true);
      this.api.compare(slug, selected, office || undefined).subscribe({
        next: (d) => {
          this.data.set(d);
          this.loading.set(false);
          this.error.set(null);
        },
        error: () => {
          this.error.set('Não foi possível comparar os estados.');
          this.loading.set(false);
        },
      });
    });
  }

  ngOnInit(): void {
    this.live.start();
    const q = this.route.snapshot.queryParamMap;
    const estados = q.get('estados');
    if (estados) {
      this.selected.set(
        estados
          .split(',')
          .map((s) => s.trim().toUpperCase())
          .filter((s) => DOMESTIC_STATES.some((d) => d.code === s))
          .slice(0, MAX),
      );
    }
    const cargo = q.get('cargo');
    if (cargo) this.office.set(cargo);
  }

  isOn(uf: string): boolean {
    return this.selected().includes(uf);
  }

  toggle(uf: string): void {
    const cur = this.selected();
    const next = cur.includes(uf) ? cur.filter((x) => x !== uf) : [...cur, uf].slice(0, MAX);
    this.selected.set(next);
    this.writeQuery(next, this.office());
  }

  onOffice(slug: string): void {
    this.office.set(slug);
    this.writeQuery(this.selected(), slug);
  }

  private writeQuery(estados: string[], cargo: string): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        estados: estados.join(',') || null,
        cargo: cargo || null,
      },
      queryParamsHandling: 'merge',
      replaceUrl: true,
    });
  }
}
