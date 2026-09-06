import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<header><a routerLink="/golden-sets" class="brand">LLM <span>WORKBENCH</span></a><p>Golden sets · S1</p><span class="profile">● Perfil workbench · docente demo</span></header><main><router-outlet /></main>`,
  styleUrl: './app.scss',
})
export class App {}
