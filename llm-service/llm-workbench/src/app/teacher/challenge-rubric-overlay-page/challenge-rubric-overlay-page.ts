import { HttpErrorResponse, HttpClient, httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
import { catchError, Observable, of } from 'rxjs';
import { ToastService } from '../toast.service';
import { ChallengeSelector } from '../challenge-selector/challenge-selector';
import { RubricDimensionsEditor, EditorDimension } from '../rubric-dimensions-editor/rubric-dimensions-editor';
import { ChallengeExercise, challengeUuidV5 } from '../challenge-catalog/challenge-catalog';

@Component({
  selector: 'app-challenge-rubric-overlay-page',
  imports: [ChallengeSelector, RubricDimensionsEditor],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './challenge-rubric-overlay-page.component.html',
  styleUrl: './challenge-rubric-overlay-page.component.scss',
})
export class ChallengeRubricOverlayPage {
  private readonly http = inject(HttpClient);
  private readonly toast = inject(ToastService);

  readonly courseId = input('');

  readonly selectedExercise = signal<ChallengeExercise | null>(null);
  readonly selectedUuid = signal('');
  readonly overlay = signal<OverlayVersion | null>(null);
  readonly effective = signal<EffectiveDimension[]>([]);
  readonly editing = signal(false);
  readonly creating = signal(false);
  readonly newOverlayName = signal('');
  readonly baselineVersionId = signal('');
  readonly error = signal('');
  readonly effectiveVisible = signal(false);

  readonly overlays = httpResource<OverlayPage>(() => {
    const courseId = this.courseId();
    const challengeId = this.selectedUuid();
    return courseId && challengeId ? `/api/llm/courses/${courseId}/challenges/${challengeId}/rubric-overlay` : undefined;
  }, { defaultValue: { items: [] } });

  readonly courseRubrics = httpResource<RubricPage>(() => this.courseId() ? `/api/llm/courses/${this.courseId()}/rubrics` : undefined, { defaultValue: { items: [] } });

  readonly publishedBaselines = computed(() => this.courseRubrics.value().items.filter((rubric) => rubric.state === 'PUBLISHED'));

  readonly overlayLoadError = computed(() => {
    const error = this.overlays.error() as HttpErrorResponse | null;
    const detail = error?.error?.detail as string | undefined;
    return detail ?? 'No se pudieron cargar los overlays.';
  });

  onSelect(exercise: ChallengeExercise): void {
    this.selectedExercise.set(exercise);
    this.selectedUuid.set(challengeUuidV5(exercise.id));
    this.overlay.set(null);
    this.effective.set([]);
    this.effectiveVisible.set(false);
    this.editing.set(false);
  }

  clearSelection(): void {
    this.selectedExercise.set(null);
    this.selectedUuid.set('');
    this.overlay.set(null);
  }

  edit(version: OverlayVersion): void {
    this.overlay.set(version);
    this.editing.set(true);
  }

  closeEditor(): void {
    this.editing.set(false);
  }

  setNewOverlayName(event: Event): void {
    this.newOverlayName.set((event.target as HTMLInputElement).value);
  }

  setBaseline(event: Event): void {
    this.baselineVersionId.set((event.target as HTMLSelectElement).value);
  }

  createDraft(): void {
    if (!this.newOverlayName().trim()) { this.toast.error('Ingresá un nombre para el overlay.'); return; }
    if (!this.baselineVersionId()) { this.toast.error('Elegí la rúbrica base del curso.'); return; }
    this.creating.set(true);
    this.httpPost<OverlayVersion>(`/api/llm/courses/${this.courseId()}/challenges/${this.selectedUuid()}/rubric-overlay`,
      { name: this.newOverlayName().trim(), baselineVersionId: this.baselineVersionId() })
      .subscribe((created) => {
        this.creating.set(false);
        if (!created) return;
        this.toast.success('Borrador del overlay creado.');
        this.overlays.reload();
        this.overlay.set(created);
        this.editing.set(true);
      });
  }

  save(version: OverlayVersion, dimensions: EditorDimension[]): void {
    const body = {
      name: version.name,
      userPrompt: version.userPrompt,
      customDimensions: dimensions.map((dimension) => ({
        key: dimension.key,
        label: dimension.label,
        criterion: dimension.criterion,
        anchors: dimension.anchors,
        weight: dimension.weight,
      })),
    };
    this.httpPatch<OverlayVersion>(`/api/llm/courses/${this.courseId()}/challenges/${this.selectedUuid()}/rubric-overlay/${version.id}`,
      body, version.revision)
      .subscribe((updated) => {
        if (!updated) return;
        this.overlay.set(updated);
        this.editing.set(false);
        this.toast.success('Overlay guardado.');
        this.overlays.reload();
      });
  }

  publish(version: OverlayVersion): void {
    this.httpPost<void>(`/api/llm/courses/${this.courseId()}/challenges/${this.selectedUuid()}/rubric-overlay/${version.id}/publish`, {})
      .subscribe((ok) => {
        if (!ok) return;
        this.toast.success(`Overlay v${version.version} publicado.`);
        this.overlays.reload();
      });
  }

  nextVersion(version: OverlayVersion): void {
    this.httpPost<OverlayVersion>(`/api/llm/courses/${this.courseId()}/challenges/${this.selectedUuid()}/rubric-overlay/${version.id}/next-version`, {})
      .subscribe((created) => {
        if (!created) return;
        this.toast.success(`Nueva versión borrador v${created.version} creada.`);
        this.overlays.reload();
      });
  }

  toggleEffective(): void {
    if (this.effectiveVisible()) { this.effectiveVisible.set(false); return; }
    const overlay = this.overlay() ?? this.overlays.value().items[0];
    if (!overlay) return;
    this.httpGet<EffectiveDimension[]>(`/api/llm/courses/${this.courseId()}/challenges/${this.selectedUuid()}/rubric-overlay/${overlay.id}/effective`)
      .subscribe((dims) => {
        this.effective.set(dims);
        this.effectiveVisible.set(true);
      });
  }

  private httpPost<T>(url: string, body: unknown): Observable<T | null> {
    return this.http.post<T>(url, body).pipe(catchError((error) => { this.notifyError(error); return of(null); }));
  }

  private httpPatch<T>(url: string, body: unknown, revision: number): Observable<T | null> {
    return this.http.patch<T>(url, body, { headers: { 'If-Match': String(revision) } }).pipe(catchError((error) => { this.notifyError(error); return of(null); }));
  }

  private httpGet<T>(url: string): Observable<T> {
    return this.http.get<T>(url).pipe(catchError((error) => { this.notifyError(error); return of([] as unknown as T); }));
  }

  private notifyError(error: unknown): void {
    const message = (error as { error?: { detail?: string } })?.error?.detail ?? 'No se pudo completar la operación.';
    this.error.set(message);
    this.toast.error(message);
  }

  stateLabel(state: string): string { return state === 'DRAFT' ? 'Borrador' : state === 'PUBLISHED' ? 'Publicada' : state; }
}

interface OverlayPage { items: OverlayVersion[]; }
interface OverlayVersion {
  id: string; familyId: string; version: number; name: string; state: string; revision: number;
  rubricKind: string; userPrompt: string; customDimensions: EditorDimension[]; baselineVersionId?: string;
}
interface EffectiveDimension { key: string; label: string; criterion: string; weight: number; origin: string; }
interface RubricPage { items: { id: string; version: number; name: string; state: string }[]; }