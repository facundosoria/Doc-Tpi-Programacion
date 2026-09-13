import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpClient, httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, OnDestroy, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { catchError, finalize, of } from 'rxjs';

function createIdempotencyKey(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID();
  }
  return '00000000-0000-0000-0000-' + Math.random().toString(16).slice(2, 14).padEnd(12, '0');
}

@Component({
  selector: 'app-calibrations-page',
  imports: [ReactiveFormsModule, DatePipe, DecimalPipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './calibrations-page.component.html',
  styleUrl: './calibrations-page.component.scss',
})
export class CalibrationsPage implements OnDestroy {
  private readonly http = inject(HttpClient);
  private readonly fb = inject(NonNullableFormBuilder);

  readonly courseId = input('');

  readonly calibrations = httpResource<CalibrationPageResponse>(
    () => this.courseId() ? `/api/llm/courses/${this.courseId()}/calibrations` : undefined,
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

  readonly models = httpResource<{ items: ModelDeploymentItem[] }>(
    () => this.courseId() ? `/api/llm/courses/${this.courseId()}/model-deployments` : undefined,
    { defaultValue: { items: [] } }
  );

  readonly activeCalibration = httpResource<ActiveCalibration | null>(
    () => this.courseId() ? `/api/llm/courses/${this.courseId()}/active-calibration` : undefined,
    { defaultValue: null }
  );
  readonly activeEvaluator = httpResource<ActiveEvaluator>(() => '/api/llm/admin/evaluator-models/active');
  readonly calibrationTarget = httpResource<ActiveEvaluator>(() => '/api/llm/admin/evaluator-models/calibration-target');

  readonly activeRubric = computed(() => {
    const activeRunId = this.activeCalibration.value()?.calibrationRunId;
    const rubricVersionId = this.calibrations.value().items.find(run => run.id === activeRunId)?.rubricVersionId;
    return this.rubrics.value().items.find(rubric => rubric.id === rubricVersionId) ?? null;
  });

  readonly publishedRubrics = computed(() =>
    this.rubrics.value().items.filter(r => r.state === 'PUBLISHED')
  );

  readonly publishedGoldenSets = computed(() =>
    this.goldenSets.value().items.filter(g => g.state === 'PUBLISHED')
  );

  readonly modelDeployments = computed(() =>
    this.models.value().items
  );

  readonly runningRuns = computed(() =>
    this.calibrations.value().items.filter(r => r.state === 'QUEUED' || r.state === 'RUNNING')
  );

  readonly selectedRunId = signal<string>('');

  readonly selectedCompletedRun = computed(() => {
    const items = this.calibrations.value().items;
    if (this.selectedRunId()) {
      return items.find(r => r.id === this.selectedRunId()) || null;
    }
    return items.find(r => r.state === 'PASSED' || r.state === 'FAILED') || items[0] || null;
  });

  readonly runForm = this.fb.group({
    rubricVersionId: ['', Validators.required],
    goldenSetVersionId: ['', Validators.required],
  });

  readonly selectedRubricId = signal('');
  readonly selectedGoldenSetId = signal('');

  readonly selectedRubric = computed(() =>
    this.publishedRubrics().find(r => r.id === this.selectedRubricId())
  );

  readonly selectedGoldenSet = computed(() =>
    this.publishedGoldenSets().find(g => g.id === this.selectedGoldenSetId())
  );

  constructor() {
    this.runForm.valueChanges.subscribe(val => {
      this.selectedRubricId.set(val.rubricVersionId || '');
      this.selectedGoldenSetId.set(val.goldenSetVersionId || '');
    });
    if (typeof EventSource !== 'undefined') {
      const modelEvents = new EventSource('/api/llm/admin/evaluator-models/events');
      modelEvents.addEventListener('active-model', () => this.activeEvaluator.reload());
    }
  }

  readonly launching = signal(false);
  readonly errorBanner = signal('');
  readonly successBanner = signal('');

  // Activation & migration preview state (GS-0544, GS-0545)
  readonly activationPreview = signal<ActivationPreviewData | null>(null);
  readonly selectedMigrableIds = signal<string[]>([]);
  readonly activating = signal(false);
  private refreshTimer: ReturnType<typeof setInterval> | undefined;

  startCalibration() {
    if (this.runForm.invalid) return;

    this.launching.set(true);
    this.errorBanner.set('');
    this.successBanner.set('');

    const val = this.runForm.getRawValue();
    this.http.post<CalibrationRunItem>(
      `/api/llm/courses/${this.courseId()}/calibrations`,
      {
        rubricVersionId: val.rubricVersionId,
        goldenSetVersionId: val.goldenSetVersionId,
      },
      { headers: { 'Idempotency-Key': createIdempotencyKey() } }
    ).pipe(
      catchError(() => {
        this.errorBanner.set('No se pudo iniciar la calibración.');
        return of(null);
      }),
      finalize(() => this.launching.set(false))
    ).subscribe(created => {
      if (created) {
        this.successBanner.set('Corrida de calibración encolada exitosamente.');
        this.selectedRunId.set(created.id);
        this.calibrations.reload();
        this.startRefreshing();
      }
    });
  }

  ngOnDestroy(): void { this.stopRefreshing(); }

  private startRefreshing(): void {
    this.stopRefreshing();
    this.refreshTimer = setInterval(() => {
      this.calibrations.reload();
      if (!this.runningRuns().length) this.stopRefreshing();
    }, 2_000);
  }

  private stopRefreshing(): void {
    if (this.refreshTimer !== undefined) clearInterval(this.refreshTimer);
    this.refreshTimer = undefined;
  }

  selectRun(id: string) {
    this.selectedRunId.set(id);
    this.cancelActivationPreview();
  }

  startActivationPreview(run: CalibrationRunItem) {
    this.errorBanner.set('');
    this.http.post<ActivationPreviewData>(
      `/api/llm/courses/${this.courseId()}/calibrations/${run.id}/activate-preview`,
      {},
      { headers: { 'Idempotency-Key': createIdempotencyKey() } }
    ).pipe(
      catchError(() => {
        this.errorBanner.set('No se pudo obtener la vista previa de activación.');
        return of(null);
      })
    ).subscribe(preview => {
      if (preview) {
        this.activationPreview.set(preview);
        this.selectedMigrableIds.set([...preview.migrableChallengeIds]);
      }
    });
  }

  cancelActivationPreview() {
    this.activationPreview.set(null);
    this.selectedMigrableIds.set([]);
  }

  toggleMigrable(challengeId: string) {
    const curr = this.selectedMigrableIds();
    if (curr.includes(challengeId)) {
      this.selectedMigrableIds.set(curr.filter(id => id !== challengeId));
    } else {
      this.selectedMigrableIds.set([...curr, challengeId]);
    }
  }

  confirmActivation(run: CalibrationRunItem, preview: ActivationPreviewData) {
    this.activating.set(true);
    this.errorBanner.set('');

    this.http.post<ActiveCalibration>(
      `/api/llm/courses/${this.courseId()}/calibrations/${run.id}/activate`,
      {
        previewToken: preview.previewToken,
        challengeIds: this.selectedMigrableIds(),
      },
      { headers: { 'Idempotency-Key': createIdempotencyKey() } }
    ).pipe(
      catchError(() => {
        this.errorBanner.set('No se pudo activar la calibración.');
        return of(null);
      }),
      finalize(() => this.activating.set(false))
    ).subscribe(res => {
      if (res) {
        this.successBanner.set(`Calibración ${this.shortId(run.id)} activada exitosamente en el curso.`);
        this.cancelActivationPreview();
        this.activeCalibration.reload();
      }
    });
  }

  shortId(id: string): string {
    return id.length > 12 ? `${id.slice(0, 8)}…${id.slice(-4)}` : id;
  }

  triggerReasonLabel(reason: string): string {
    return reason === 'MANUAL' ? 'Manual' : reason === 'MONTHLY' ? 'Recalibración mensual' : 'Cambio de modelo';
  }
}

interface CalibrationPageResponse { items: CalibrationRunItem[]; }
interface CalibrationRunItem {
  id: string;
  state: string;
  progress: number;
  rubricVersionId: string;
  goldenSetVersionId: string;
  modelDeploymentId: string;
  maeFinal?: number | null;
  maxIndividualError?: number | null;
  failureCode?: string | null;
  failureDetail?: string | null;
  reason: string;
  createdAt: string;
  finishedAt?: string | null;
}
interface RubricItem { id: string; name: string; version: number; state: string; }
interface GoldenSetItem { id: string; name: string; version: number; state: string; cases: any[]; }
interface ModelDeploymentItem { id: string; provider: string; modelId: string; modelVersion: string; state: string; }
interface ActiveEvaluator { id: string; provider: string; modelId: string; state: string; }
interface ActiveCalibration { courseId: string; calibrationRunId: string; activatedAt: string; }
interface ActivationPreviewData { previewToken: string; migrableChallengeIds: string[]; lockedChallengeIds: string[]; }
