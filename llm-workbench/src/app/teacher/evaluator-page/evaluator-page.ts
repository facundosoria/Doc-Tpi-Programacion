import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'app-evaluator-page',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './evaluator-page.component.html',
  styleUrl: './evaluator-page.component.scss',
})
export class EvaluatorPage {
  readonly eyebrow = input.required<string>();
  readonly title = input.required<string>();
  readonly description = input.required<string>();
  readonly nextStep = input.required<string>();
}
