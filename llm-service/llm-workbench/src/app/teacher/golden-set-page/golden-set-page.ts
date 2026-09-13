import { httpResource, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { catchError, finalize, of } from 'rxjs';
import { createIdempotencyKey } from '../../course-api.service';

const SCORE_KEYS = ['AUTONOMY', 'CLARITY', 'PROGRESSION', 'COMPLIANCE', 'EFFICIENCY'] as const;
type ScoreKey = typeof SCORE_KEYS[number];
export interface TranscriptMessage { role: 'STUDENT' | 'TUTOR'; content: string; position?: number; }
export interface TranscriptParseResult { messages: TranscriptMessage[]; error: string | null; }

export function parseTranscriptText(text: string): TranscriptParseResult {
  const messages: TranscriptMessage[] = []; let current: TranscriptMessage | null = null;
  const roles: Record<string, TranscriptMessage['role']> = { estudiante: 'STUDENT', alumno: 'STUDENT', student: 'STUDENT', tutor: 'TUTOR', 'tutor ia': 'TUTOR', ia: 'TUTOR', assistant: 'TUTOR', asistente: 'TUTOR' };
  for (const line of text.replace(/\r/g, '').split('\n')) {
    const match = line.match(/^\s*(estudiante|alumno|student|tutor(?:\s+ia)?|ia|assistant|asistente)\s*:\s*(.*)$/i);
    if (match) { current = { role: roles[match[1].toLocaleLowerCase()], content: match[2].trim() }; messages.push(current); }
    else if (line.trim()) { if (!current) return { messages: [], error: 'Empezá cada mensaje con “Estudiante:” o “Tutor IA:”.' }; current.content = `${current.content}${current.content ? '\n' : ''}${line.trim()}`; }
  }
  const valid = messages.filter(message => message.content.trim());
  if (!valid.length) return { messages: [], error: 'Pegá una conversación con mensajes de Estudiante y Tutor IA.' };
  if (!valid.some(message => message.role === 'STUDENT') || !valid.some(message => message.role === 'TUTOR')) return { messages: valid, error: 'La conversación debe incluir al estudiante y al Tutor IA.' };
  return { messages: valid, error: null };
}

@Component({ selector: 'app-golden-set-page', imports: [ReactiveFormsModule], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './golden-set-page.component.html', styleUrl: './golden-set-page.component.scss' })
export class GoldenSetPage {
  private readonly http = inject(HttpClient); private readonly fb = inject(NonNullableFormBuilder); private readonly route = inject(ActivatedRoute, { optional: true });
  readonly courseId = input(''); readonly scoreKeys = SCORE_KEYS;
  readonly routePath = this.route?.snapshot.routeConfig?.path ?? 'golden-set'; readonly creating = this.routePath === 'golden-set/new'; readonly versionId = this.route?.snapshot.paramMap.get('versionId') ?? null;
  readonly listPath = computed(() => `/docente/cursos/${this.courseId()}/evaluador/golden-set`);
  readonly goldenSets = httpResource<GoldenSetPageResponse>(() => this.courseId() ? `/api/llm/courses/${this.courseId()}/golden-sets` : undefined, { defaultValue: { items: [] } });
  readonly rubrics = httpResource<RubricPage>(() => this.courseId() ? `/api/llm/courses/${this.courseId()}/rubrics` : undefined, { defaultValue: { items: [] } });
  readonly draft = signal<GoldenSetDetail | null>(null); readonly activeIndex = signal(0); readonly saving = signal(false); readonly creatingDraft = signal(false); readonly message = signal(''); readonly error = signal(''); readonly editingCaseId = signal<string | null>(null);
  readonly form = this.fb.group({ transcriptText: ['', Validators.required], AUTONOMY: [0, [Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]], CLARITY: [0, [Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]], PROGRESSION: [0, [Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]], COMPLIANCE: [0, [Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]], EFFICIENCY: [0, [Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]] });
  readonly caseCount = computed(() => this.draft()?.cases.length ?? 0); readonly completeCount = computed(() => this.caseCount()); readonly visibleSlots = computed(() => this.caseCount() < 3 ? 3 : Math.min(5, this.caseCount() + 1)); readonly slots = computed(() => Array.from({ length: this.visibleSlots() }, (_, index) => index)); readonly canAddOptional = computed(() => this.caseCount() >= 3 && this.caseCount() < 5); readonly canPublish = computed(() => this.caseCount() >= 3 && this.caseCount() <= 5);
  readonly progressPercent = computed(() => Math.min(100, this.completeCount() / 3 * 100));
  readonly weightedScore = computed(() => { const value = this.form.getRawValue(); return Math.round((value.AUTONOMY * .30 + value.CLARITY * .25 + value.PROGRESSION * .20 + value.COMPLIANCE * .15 + value.EFFICIENCY * .10) * 10) / 10; });
  readonly rubric = computed(() => this.rubrics.value().items.find(item => item.state === 'PUBLISHED') ?? this.rubrics.value().items[0] ?? null);
  constructor() { effect(() => { const id = this.versionId; if (id && this.courseId() && this.draft()?.id !== id) this.loadDraft(id); }); }
  createDraft(): void { this.creatingDraft.set(true); this.error.set(''); this.http.post<GoldenSetDetail>(`/api/llm/courses/${this.courseId()}/golden-sets`, { name: `Golden Set · ${new Date().toISOString().slice(0, 10)}` }, { headers: { 'Idempotency-Key': createIdempotencyKey() } }).pipe(catchError(error => { this.error.set(this.describeError(error, 'No se pudo crear el borrador.')); return of(null); }), finalize(() => this.creatingDraft.set(false))).subscribe(created => { if (created) { this.draft.set({ ...created, cases: [] }); this.goldenSets.reload(); this.message.set('Borrador creado. Cargá el primer caso.'); } }); }
  loadDraft(id: string): void { this.http.get<GoldenSetDetail>(`/api/llm/courses/${this.courseId()}/golden-sets/${id}`).pipe(catchError(error => { this.error.set(this.describeError(error, 'No se pudo recuperar el borrador.')); return of(null); })).subscribe(detail => { if (detail) { this.draft.set(detail); this.selectSlot(0, true); } }); }
  selectSlot(index: number, force = false): void { if (!force && this.form.dirty && !confirm('Hay cambios sin guardar en este caso. ¿Querés descartarlos?')) return; this.activeIndex.set(index); this.error.set(''); this.message.set(''); const current = this.draft()?.cases[index]; if (current) { this.editingCaseId.set(current.id); this.form.reset({ transcriptText: this.formatTranscript(current.transcript), ...current.referenceScores }); } else { this.editingCaseId.set(null); this.form.reset({ transcriptText: '', AUTONOMY: 0, CLARITY: 0, PROGRESSION: 0, COMPLIANCE: 0, EFFICIENCY: 0 }); } this.form.markAsPristine(); }
  onTabKeydown(event: KeyboardEvent): void { const count = this.visibleSlots(); let next = this.activeIndex(); if (event.key === 'ArrowRight') next = (next + 1) % count; else if (event.key === 'ArrowLeft') next = (next - 1 + count) % count; else if (event.key === 'Home') next = 0; else if (event.key === 'End') next = count - 1; else return; event.preventDefault(); this.selectSlot(next); }
  saveAndNext(): void { this.saveCase(true); }
  saveCase(next = false): void { const set = this.draft(); const transcript = parseTranscriptText(this.form.controls.transcriptText.value); if (!set || this.form.invalid || transcript.error) { this.form.markAllAsTouched(); this.error.set(transcript.error ?? 'Completá los cinco puntajes con enteros entre 0 y 100.'); return; } const value = this.form.getRawValue(); const body = { transcript: transcript.messages.map((message, position) => ({ ...message, position })), referenceScores: Object.fromEntries(SCORE_KEYS.map(key => [key, value[key]])) }; const caseId = this.editingCaseId(); const request = caseId ? this.http.patch(`/api/llm/courses/${this.courseId()}/golden-sets/${set.id}/cases/${caseId}`, body) : this.http.post(`/api/llm/courses/${this.courseId()}/golden-sets/${set.id}/cases`, body, { headers: { 'Idempotency-Key': createIdempotencyKey() } }); this.saving.set(true); this.error.set(''); request.pipe(catchError(error => { this.error.set(this.describeError(error, 'No se pudo guardar el caso.')); return of(null); }), finalize(() => this.saving.set(false))).subscribe(saved => { if (!saved) return; this.message.set('Caso guardado en el borrador.'); this.goldenSets.reload(); this.http.get<GoldenSetDetail>(`/api/llm/courses/${this.courseId()}/golden-sets/${set.id}`).subscribe(detail => { this.draft.set(detail); this.selectSlot(next ? Math.min(this.activeIndex() + 1, this.visibleSlots() - 1) : this.activeIndex(), true); }); }); }
  addOptionalCase(): void { if (this.canAddOptional()) this.selectSlot(this.caseCount()); }
  publish(): void { const set = this.draft(); if (!set) return; if (!this.canPublish()) { this.error.set('Completá entre tres y cinco casos antes de publicar.'); return; } this.saving.set(true); this.error.set(''); this.http.post(`/api/llm/courses/${this.courseId()}/golden-sets/${set.id}/publish`, {}).pipe(catchError(error => { this.error.set(this.describeError(error, 'No se pudo publicar el Golden Set.')); return of(null); }), finalize(() => this.saving.set(false))).subscribe(result => { if (result !== null) { this.message.set('Golden Set publicado correctamente.'); this.goldenSets.reload(); this.loadDraft(set.id); } }); }
  scoreLabel(key: ScoreKey): string { return ({ AUTONOMY: 'Autonomía', CLARITY: 'Claridad', PROGRESSION: 'Progresión', COMPLIANCE: 'Cumplimiento', EFFICIENCY: 'Eficiencia' })[key]; }
  criterion(key: ScoreKey): string { return this.rubric()?.dimensions.find(dimension => dimension.key === key)?.criterion ?? 'Consultá la rúbrica vigente del curso.'; }
  private formatTranscript(messages: TranscriptMessage[]): string { return messages.map(message => `${message.role === 'STUDENT' ? 'Estudiante' : 'Tutor IA'}: ${message.content}`).join('\n'); }
  private describeError(error: HttpErrorResponse, fallback: string): string { return error.error?.detail ?? fallback; }
}
interface GoldenSetPageResponse { items: GoldenSetListItem[]; } interface GoldenSetListItem { id: string; name: string; version: number; state: string; cases: { id: string }[]; } interface GoldenSetDetail { id: string; name: string; version: number; state: string; cases: GoldenSetCase[]; } interface GoldenSetCase { id: string; order: number; transcript: TranscriptMessage[]; referenceScores: Record<ScoreKey, number>; } interface RubricPage { items: RubricVersion[]; } interface RubricVersion { state: string; dimensions: { key: ScoreKey; criterion: string }[]; }
