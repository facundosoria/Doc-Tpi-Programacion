import { DOCUMENT } from '@angular/common';
import { httpResource, HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, DestroyRef, computed, effect, inject, input, signal } from '@angular/core';
import { AbstractControl, FormArray, FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import { ToastService } from '../toast.service';

type AnchorLevel = 'low' | 'medium' | 'high';
interface Anchor { behavior: string; referenceScore: number; example: string; }
interface Anchors { low: Anchor; medium: Anchor; high: Anchor; }
type AnchorControls = { behavior: FormControl<string>; referenceScore: FormControl<number>; example: FormControl<string>; };
type DimensionControls = { key: FormControl<string>; label: FormControl<string>; criterion: FormControl<string>; anchors: FormGroup<{ low: FormGroup<AnchorControls>; medium: FormGroup<AnchorControls>; high: FormGroup<AnchorControls> }>; weight: FormControl<number>; };
type DimensionForm = FormGroup<DimensionControls>;

const anchorsOrdered: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const anchors = control.value as Anchors;
  return anchors?.low?.referenceScore < anchors?.medium?.referenceScore && anchors?.medium?.referenceScore < anchors?.high?.referenceScore ? null : { anchorsOrder: true };
};
const weightsTotal100: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const dimensions = control.value as { weight: number }[];
  return dimensions.reduce((total, dimension) => total + Number(dimension.weight || 0), 0) === 100 ? null : { weightsTotal: true };
};

@Component({
  selector: 'app-rubrics-page',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './rubrics-page.component.html',
  styleUrl: './rubrics-page.component.scss',
})
export class RubricsPage {
  private readonly http = inject(HttpClient); private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly document = inject(DOCUMENT);
  private readonly route = inject(ActivatedRoute, { optional: true }); private readonly router = inject(Router, { optional: true });
  private readonly toast = inject(ToastService); private readonly destroyRef = inject(DestroyRef);
  readonly courseId = input(''); readonly rubrics = httpResource<RubricPage>(() => this.courseId() ? `/api/llm/courses/${this.courseId()}/rubrics` : undefined, { defaultValue: { items: [] } });
  readonly editorRoutePath = this.route?.snapshot.routeConfig?.path ?? 'rubricas';
  readonly editorRoute = this.editorRoutePath !== 'rubricas';
  readonly creatingRoute = this.editorRoutePath === 'rubricas/new';
  readonly editingVersionId = this.route?.snapshot.paramMap.get('versionId') ?? null;
  readonly rubricsPath = computed(() => `/docente/cursos/${this.courseId()}/evaluador/rubricas`);
  readonly selected = signal<RubricVersion | null>(null); readonly saving = signal(false); readonly saved = signal(false); readonly saveError = signal('');
  readonly returningToRubrics = signal(false);
  readonly activeDimensionIndex = signal(0);
  readonly creating = signal(false);
  readonly comparingRubric = signal<RubricVersion | null>(null);
  readonly templates = httpResource<RubricPage>(() => '/api/llm/rubric-templates', { defaultValue: { items: [] } });
  readonly templateVersionId = signal('');
  readonly newRubricName = signal('');
  readonly form = this.formBuilder.group({ name: ['', Validators.required], dimensions: this.formBuilder.array<DimensionForm>([], { validators: weightsTotal100 }) });
  private returnToListTimer: ReturnType<typeof setTimeout> | null = null;
  constructor() {
    effect(() => { const id = this.editingVersionId; const rubric = this.rubrics.value().items.find(item => item.id === id); if (id && rubric && this.selected()?.id !== id) this.editDraft(rubric); });
    this.destroyRef.onDestroy(() => this.clearReturnToListTimer());
  }
  get dimensions(): FormArray<DimensionForm> { return this.form.controls.dimensions; }
  totalWeight(): number { return this.dimensions.controls.reduce((total, dimension) => total + dimension.controls.weight.value, 0); }
  editDraft(rubric: RubricVersion): void { this.selected.set(rubric); this.saved.set(false); this.saveError.set(''); this.activeDimensionIndex.set(0); this.dimensions.clear(); rubric.dimensions.forEach(dimension => this.dimensions.push(this.createDimension(dimension))); this.form.controls.name.setValue(rubric.name); this.form.markAsPristine(); }
  openEditor(rubric: RubricVersion): void { this.editDraft(rubric); void this.router?.navigateByUrl(`${this.rubricsPath()}/${rubric.id}/edit`).catch(() => undefined); }
  closeEditor(): void { this.clearReturnToListTimer(); this.selected.set(null); this.saved.set(false); this.saveError.set(''); }
  openPublishComparison(rubric: RubricVersion): void { this.comparingRubric.set(rubric); }
  closePublishComparison(): void { this.comparingRubric.set(null); }
  confirmPublish(rubric: RubricVersion): void {
    let failed = false;
    this.http.post<void>(`/api/llm/courses/${this.courseId()}/rubrics/${rubric.id}/publish`, {}).pipe(
      catchError(() => { this.notifyError('No se pudo publicar la versión de la rúbrica.'); failed = true; return of(null); })
    ).subscribe(() => {
      if (!failed) {
        this.toast.success(`Rúbrica v${rubric.version} publicada correctamente.`);
        this.closePublishComparison();
        this.closeEditor();
        this.rubrics.reload();
      }
    });
  }
  createNextVersion(rubric: RubricVersion): void {
    this.http.post<RubricVersion>(`/api/llm/courses/${this.courseId()}/rubrics/${rubric.id}/next-version`, {}).pipe(
      catchError(() => { this.notifyError('No se pudo crear la nueva versión.'); return of(null); })
    ).subscribe(created => {
      if (created) {
        this.toast.success(`Nueva versión borrador v${created.version} creada.`);
        this.rubrics.reload();
        void this.router?.navigateByUrl(`${this.rubricsPath()}/${created.id}/edit`).catch(() => undefined);
      }
    });
  }
  selectTemplate(event: Event): void { this.templateVersionId.set((event.target as HTMLSelectElement).value); }
  setNewRubricName(event: Event): void { this.newRubricName.set((event.target as HTMLInputElement).value); }
  selectDimension(index: number): void { this.activeDimensionIndex.set(index); }
  scrollToCalibrationGuide(event: MouseEvent): void {
    event.preventDefault();
    const guide = this.document.getElementById('guia-calibracion');
    guide?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    guide?.focus({ preventScroll: true });
  }
  createFirstDraft(): void {
    if (!this.templateVersionId()) { this.notifyError('Seleccioná una plantilla institucional publicada.'); return; }
    if (!this.newRubricName().trim()) { this.notifyError('Ingresá un nombre para identificar esta rúbrica.'); return; }
    this.creating.set(true);
    this.http.post<RubricVersion>(`/api/llm/courses/${this.courseId()}/rubrics`, { templateVersionId: this.templateVersionId(), name: this.newRubricName().trim() }).pipe(
      catchError(() => { this.notifyError('No se pudo crear la rúbrica inicial.'); return of(null); }),
      finalize(() => this.creating.set(false)),
    ).subscribe((created) => { if (created) { this.toast.success('Borrador de rúbrica creado.'); this.rubrics.reload(); void this.router?.navigateByUrl(`${this.rubricsPath()}/${created.id}/edit`).catch(() => undefined); } });
  }
  save(): void { const rubric = this.selected(); if (!rubric) return; if (this.form.invalid) { this.form.markAllAsTouched(); this.saveError.set('Completá los campos obligatorios y verificá que los pesos sumen 100 %.'); this.toast.error('No se pudo guardar: revisá los campos obligatorios.'); return; } this.saving.set(true); this.saved.set(false); this.saveError.set(''); this.http.patch<RubricVersion>(`/api/llm/courses/${this.courseId()}/rubrics/${rubric.id}`, this.form.getRawValue(), { headers: { 'If-Match': String(rubric.revision) } }).pipe(catchError(error => { const message = error.status === 409 ? 'El borrador cambió en otro dispositivo. Recargá antes de guardar.' : error.error?.detail ?? 'No se pudo guardar el borrador. Intentá nuevamente.'; this.saveError.set(message); this.toast.error(message); return of(null); }), finalize(() => this.saving.set(false))).subscribe(updated => { if (!updated) return; this.selected.set(updated); this.rubrics.update(page => ({ items: page.items.map(item => item.id === updated.id ? updated : item) })); this.form.markAsPristine(); this.saved.set(true); this.toast.success('Borrador guardado.', 3000); this.scheduleReturnToList(); }); }
  stateLabel(state: string): string { return state === 'DRAFT' ? 'Borrador' : state === 'PUBLISHED' ? 'Publicada' : state === 'SUPERSEDED' ? 'Reemplazada' : state; }
  anchor(level: AnchorLevel, anchor?: Anchor): FormGroup<AnchorControls> { return this.formBuilder.group<AnchorControls>({ behavior: this.formBuilder.control(anchor?.behavior ?? '', Validators.required), referenceScore: this.formBuilder.control(anchor?.referenceScore ?? (level === 'low' ? 25 : level === 'medium' ? 60 : 90), [Validators.required, Validators.min(0), Validators.max(100)]), example: this.formBuilder.control(anchor?.example ?? '', Validators.required) }); }
  private notifyError(message: string): void { this.toast.error(message); }
  private scheduleReturnToList(): void {
    this.clearReturnToListTimer();
    this.returningToRubrics.set(true);
    this.returnToListTimer = setTimeout(() => void this.router?.navigateByUrl(this.rubricsPath()).catch(() => undefined), 2000);
  }
  private clearReturnToListTimer(): void {
    if (this.returnToListTimer !== null) clearTimeout(this.returnToListTimer);
    this.returnToListTimer = null;
    this.returningToRubrics.set(false);
  }
  private createDimension(dimension: DimensionInput): DimensionForm { return this.formBuilder.group<DimensionControls>({ key: this.formBuilder.control(dimension.key), label: this.formBuilder.control(dimension.label), criterion: this.formBuilder.control(dimension.criterion, Validators.required), anchors: this.formBuilder.group({ low: this.anchor('low', dimension.anchors?.low), medium: this.anchor('medium', dimension.anchors?.medium), high: this.anchor('high', dimension.anchors?.high) }, { validators: anchorsOrdered }), weight: this.formBuilder.control(dimension.weight, [Validators.required, Validators.min(0.01), Validators.max(100)]) }); }
}
interface RubricPage { items: RubricVersion[]; }
interface RubricVersion { id: string; familyId: string; name: string; version: number; state: string; revision: number; templateOriginVersionId?: string; dimensions: DimensionInput[]; }
interface DimensionInput { key: string; label: string; criterion: string; anchors: Anchors; weight: number; }
