import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-how-to-use-page',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './how-to-use-page.component.html',
  styleUrl: './how-to-use-page.component.scss',
})
export class HowToUsePage {}
