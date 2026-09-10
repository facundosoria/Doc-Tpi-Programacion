import { httpResource, HttpClient } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { AbstractControl, FormArray, FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';

const defaultDimensions = [
  ['AUTONOMY', 'Autonomía'], ['CLARITY', 'Claridad'], ['PROGRESSION', 'Progresión'], ['COMPLIANCE', 'Cumplimiento'], ['EFFICIENCY', 'Eficiencia'],
].map(([key, label]) => ({ key, label, criterion: `Criterio inicial de ${label.toLowerCase()}.`, anchors: '{"low":"Inicial","medium":"Adecuado","high":"Avanzado"}', evaluatorPrompt: `Evaluá ${label.toLowerCase()} en la interacción.`, weight: 20 }));

type DimensionControls = { key: FormControl<string>; label: FormControl<string>; criterion: FormControl<string>; anchors: FormControl<string>; evaluatorPrompt: FormControl<string>; weight: FormControl<number>; };
type DimensionForm = FormGroup<DimensionControls>;

const anchorsJson: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  try {
    const anchors = JSON.parse(control.value as string) as Record<string, unknown>;
    return ['low', 'medium', 'high'].every(key => typeof anchors[key] === 'string' && anchors[key].trim()) ? null : { anchors: true };
  } catch { return { anchors: true }; }
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
  private readonly route = inject(ActivatedRoute, { optional: true }); private readonly router = inject(Router, { optional: true });
  readonly courseId = input(''); readonly rubrics = httpResource<RubricPage>(() => this.courseId() ? `/api/llm/courses/${this.courseId()}/rubrics` : undefined, { defaultValue: { items: [] } });
  readonly editorRoutePath = this.route?.snapshot.routeConfig?.path ?? 'rubricas';
  readonly editorRoute = this.editorRoutePath !== 'rubricas';
  readonly creatingRoute = this.editorRoutePath === 'rubricas/new';
  readonly editingVersionId = this.route?.snapshot.paramMap.get('versionId') ?? null;
  readonly rubricsPath = computed(() => `/docente/cursos/${this.courseId()}/evaluador/rubricas`);
  readonly selected = signal<RubricVersion | null>(null); readonly saving = signal(false); readonly saved = signal(false); readonly saveError = signal('');
  readonly creating = signal(false);
  readonly comparingRubric = signal<RubricVersion | null>(null); readonly publishSuccess = signal(''); readonly publishError = signal('');
  readonly form = this.formBuilder.group({ name: ['', Validators.required], dimensions: this.formBuilder.array<DimensionForm>([], { validators: weightsTotal100 }) });
  constructor() { effect(() => { const id = this.editingVersionId; const rubric = this.rubrics.value().items.find(item => item.id === id); if (id && rubric && this.selected()?.id !== id) this.editDraft(rubric); }); }
  get dimensions(): FormArray<DimensionForm> { return this.form.controls.dimensions; }
  totalWeight(): number { return this.dimensions.controls.reduce((total, dimension) => total + dimension.controls.weight.value, 0); }
  editDraft(rubric: RubricVersion): void { this.selected.set(rubric); this.saved.set(false); this.saveError.set(''); this.dimensions.clear(); rubric.dimensions.forEach(dimension => this.dimensions.push(this.createDimension(dimension))); this.form.controls.name.setValue(rubric.name); this.form.markAsPristine(); }
  openEditor(rubric: RubricVersion): void { this.editDraft(rubric); void this.router?.navigateByUrl(`${this.rubricsPath()}/${rubric.id}/edit`).catch(() => undefined); }
  closeEditor(): void { this.selected.set(null); this.saved.set(false); this.saveError.set(''); }
  openPublishComparison(rubric: RubricVersion): void { this.comparingRubric.set(rubric); this.publishError.set(''); this.publishSuccess.set(''); }
  closePublishComparison(): void { this.comparingRubric.set(null); this.publishError.set(''); }
  confirmPublish(rubric: RubricVersion): void {
    let failed = false;
    this.http.post<void>(`/api/llm/courses/${this.courseId()}/rubrics/${rubric.id}/publish`, {}).pipe(
      catchError(() => { this.publishError.set('No se pudo publicar la versión de la rúbrica.'); failed = true; return of(null); })
    ).subscribe(() => {
      if (!failed) {
        this.publishSuccess.set(`Rúbrica v${rubric.version} publicada exitosamente (inmutable).`);
        this.closePublishComparison();
        this.closeEditor();
        this.rubrics.reload();
      }
    });
  }
  createNextVersion(rubric: RubricVersion): void {
    this.http.post<RubricVersion>(`/api/llm/courses/${this.courseId()}/rubrics/${rubric.id}/next-version`, {}).pipe(
      catchError(() => { this.publishError.set('No se pudo crear la nueva versión.'); return of(null); })
    ).subscribe(created => {
      if (created) {
        this.publishSuccess.set(`Nueva versión borrador (v${created.version}) creada exitosamente.`);
        this.rubrics.reload();
        void this.router?.navigateByUrl(`${this.rubricsPath()}/${created.id}/edit`).catch(() => undefined);
      }
    });
  }
  createFirstDraft(): void {
    this.creating.set(true); this.publishError.set('');
    this.http.post<RubricVersion>(`/api/llm/courses/${this.courseId()}/rubrics`, { name: 'Rúbrica de práctica', dimensions: defaultDimensions }).pipe(
      catchError(() => { this.publishError.set('No se pudo crear la rúbrica inicial.'); return of(null); }),
      finalize(() => this.creating.set(false)),
    ).subscribe((created) => { if (created) { this.rubrics.reload(); void this.router?.navigateByUrl(`${this.rubricsPath()}/${created.id}/edit`).catch(() => undefined); } });
  }
  save(): void { const rubric = this.selected(); if (!rubric) return; if (this.form.invalid) { this.form.markAllAsTouched(); return; } this.saving.set(true); this.saved.set(false); this.saveError.set(''); this.http.patch<RubricVersion>(`/api/llm/courses/${this.courseId()}/rubrics/${rubric.id}`, this.form.getRawValue(), { headers: { 'If-Match': String(rubric.revision) } }).pipe(catchError(error => { this.saveError.set(error.status === 409 ? 'El borrador cambió en otro dispositivo. Recargá antes de guardar.' : 'No se pudo guardar el borrador. Intentá nuevamente.'); return of(null); }), finalize(() => this.saving.set(false))).subscribe(updated => { if (!updated) return; this.selected.set(updated); this.rubrics.update(page => ({ items: page.items.map(item => item.id === updated.id ? updated : item) })); this.form.markAsPristine(); this.saved.set(true); }); }
  stateLabel(state: string): string { return state === 'DRAFT' ? 'Borrador' : state === 'PUBLISHED' ? 'Publicada' : state === 'SUPERSEDED' ? 'Reemplazada' : state; }
  private createDimension(dimension: DimensionInput): DimensionForm { return this.formBuilder.group<DimensionControls>({ key: this.formBuilder.control(dimension.key), label: this.formBuilder.control(dimension.label), criterion: this.formBuilder.control(dimension.criterion, Validators.required), anchors: this.formBuilder.control(dimension.anchors, [Validators.required, anchorsJson]), evaluatorPrompt: this.formBuilder.control(dimension.evaluatorPrompt, Validators.required), weight: this.formBuilder.control(dimension.weight, [Validators.required, Validators.min(0), Validators.max(100)]) }); }
}
interface RubricPage { items: RubricVersion[]; }
interface RubricVersion { id: string; familyId: string; name: string; version: number; state: string; revision: number; dimensions: DimensionInput[]; }
interface DimensionInput { key: string; label: string; criterion: string; anchors: string; evaluatorPrompt: string; weight: number; }
