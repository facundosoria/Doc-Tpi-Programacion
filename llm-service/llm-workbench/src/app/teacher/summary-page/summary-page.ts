import { DatePipe } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-summary-page',
  imports: [RouterLink, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './summary-page.component.html',
  styleUrl: './summary-page.component.scss',
})
export class SummaryPage {
  readonly courseId = input('');

  readonly activeCalibration = httpResource<ActiveCalibration | null>(
    () => this.courseId() ? `/api/llm/courses/${this.courseId()}/active-calibration` : undefined,
    { defaultValue: null }
  );

  readonly pendingEvaluations = httpResource<{ items: any[] }>(
    () => this.courseId() ? `/api/llm/courses/${this.courseId()}/pending-evaluations` : undefined,
    { defaultValue: { items: [] } }
  );

  readonly baseProposals = httpResource<{ items: any[] }>(
    () => this.courseId() ? `/api/llm/courses/${this.courseId()}/base-update-proposals` : undefined,
    { defaultValue: { items: [] } }
  );

  readonly rubrics = httpResource<{ items: RubricItem[] }>(
    () => this.courseId() ? `/api/llm/courses/${this.courseId()}/rubrics` : undefined,
    { defaultValue: { items: [] } }
  );

  readonly goldenSets = httpResource<{ items: GoldenSetItem[] }>(
    () => this.courseId() ? `/api/llm/courses/${this.courseId()}/golden-sets` : undefined,
    { defaultValue: { items: [] } }
  );

  readonly calibrations = httpResource<{ items: CalibrationRunItem[] }>(
    () => this.courseId() ? `/api/llm/courses/${this.courseId()}/calibrations` : undefined,
    { defaultValue: { items: [] } }
  );

  readonly hasActiveCalibration = computed(() => !!this.activeCalibration.value());
  readonly pendingCount = computed(() => this.pendingEvaluations.value().items.length);
  readonly hasBaseProposal = computed(() => this.baseProposals.value().items.length > 0);

  readonly draftRubrics = computed(() =>
    this.rubrics.value().items.filter(r => r.state === 'DRAFT')
  );

  readonly draftGoldenSets = computed(() =>
    this.goldenSets.value().items.filter(g => g.state === 'DRAFT')
  );

  readonly recentCalibrations = computed(() =>
    this.calibrations.value().items.slice(0, 3)
  );

  readonly hasRecentFailedCalibration = computed(() => {
    const first = this.calibrations.value().items[0];
    return first ? first.state === 'FAILED' : false;
  });
  readonly nextAction = computed(() => {
    if (!this.rubrics.value().items.some(item => item.state === 'PUBLISHED')) return { label: 'Ir a rúbricas', detail: 'Elegí una plantilla institucional y adaptala para tu curso.', link: '../rubricas' };
    if (!this.goldenSets.value().items.some(item => item.state === 'PUBLISHED')) return { label: 'Crear Golden Set', detail: 'Incorporá casos de referencia humana antes de calibrar.', link: '../golden-set/new' };
    if (!this.hasActiveCalibration()) return { label: 'Crear calibración', detail: 'Validá el evaluador con las versiones publicadas.', link: '../calibraciones' };
    return { label: 'Ver asignaciones', detail: 'La calibración activa está lista para los desafíos del curso.', link: '../asignaciones' };
  });

  shortId(id: string): string {
    return id.length > 12 ? `${id.slice(0, 8)}…${id.slice(-4)}` : id;
  }
}

interface ActiveCalibration { courseId: string; calibrationRunId: string; activatedAt: string; }
interface RubricItem { id: string; name: string; version: number; state: string; }
interface GoldenSetItem { id: string; name: string; version: number; state: string; cases: any[]; }
interface CalibrationRunItem { id: string; state: string; progress: number; createdAt: string; }
