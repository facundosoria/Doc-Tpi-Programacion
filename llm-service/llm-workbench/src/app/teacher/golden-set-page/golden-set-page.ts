import { DatePipe } from '@angular/common';
import { httpResource, HttpClient, HttpErrorResponse } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, input, signal } from '@angular/core';
import { AbstractControl, NonNullableFormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { EMPTY, catchError, finalize, of } from 'rxjs';
import { createIdempotencyKey } from '../../course-api.service';

const SCORE_KEYS = ['AUTONOMY', 'CLARITY', 'PROGRESSION', 'COMPLIANCE', 'EFFICIENCY'] as const;
type ScoreKey = (typeof SCORE_KEYS)[number];
type JustificationKey = `${ScoreKey}_justification`;

export interface StagedRowPayload {
  transcript: { role: string; content: string; position?: number }[];
  challengeContext: { externalChallengeId: string; statement: string };
  metadata?: Record<string, unknown>;
  author: string;
  referenceScores: Record<string, number>;
  scoreJustifications?: Record<string, string>;
}

export interface StagedRow {
  rowNumber: number;
  payload: StagedRowPayload;
  errors: string[];
}

export interface TranscriptMessage {
  role: 'STUDENT' | 'TUTOR';
  content: string;
}

export interface TranscriptParseResult {
  messages: TranscriptMessage[];
  error: string | null;
}

/** Converts the teacher-friendly transcript into the API's ordered role-aware format. */
export function parseTranscriptText(text: string): TranscriptParseResult {
  const messages: TranscriptMessage[] = [];
  let current: TranscriptMessage | null = null;
  const labels: Record<string, TranscriptMessage['role']> = {
    estudiante: 'STUDENT', alumno: 'STUDENT', student: 'STUDENT',
    tutor: 'TUTOR', 'tutor ia': 'TUTOR', ia: 'TUTOR', assistant: 'TUTOR', asistente: 'TUTOR',
  };

  for (const line of text.replace(/\r/g, '').split('\n')) {
    const match = line.match(/^\s*(estudiante|alumno|student|tutor(?:\s+ia)?|ia|assistant|asistente)\s*:\s*(.*)$/i);
    if (match) {
      const role = labels[match[1].toLocaleLowerCase()];
      current = { role, content: match[2].trim() };
      messages.push(current);
    } else if (line.trim()) {
      if (!current) {
        return { messages: [], error: 'Empezá cada bloque con “Estudiante:” o “Tutor IA:”.' };
      }
      current.content = `${current.content}${current.content ? '\n' : ''}${line.trim()}`;
    }
  }

  const nonEmpty = messages.filter(message => message.content.trim());
  if (!nonEmpty.length) {
    return { messages: [], error: 'Pegá una conversación con mensajes de Estudiante y Tutor IA.' };
  }
  if (!nonEmpty.some(message => message.role === 'STUDENT') || !nonEmpty.some(message => message.role === 'TUTOR')) {
    return { messages: nonEmpty, error: 'La conversación debe incluir al menos un mensaje del estudiante y una respuesta del Tutor IA.' };
  }
  return { messages: nonEmpty, error: null };
}

function transcriptValidator(control: AbstractControl<string>): ValidationErrors | null {
  return parseTranscriptText(control.value).error ? { transcriptFormat: true } : null;
}

export function validateRow(payload: StagedRowPayload): string[] {
  const errs: string[] = [];
  if (!Array.isArray(payload.transcript) || payload.transcript.length === 0) {
    errs.push('Falta transcript / mensajes');
  } else {
    for (const msg of payload.transcript) {
      if (!msg.content || !msg.content.trim()) {
        errs.push('Contenido de mensaje vacío');
        break;
      }
      if (msg.role !== 'STUDENT' && msg.role !== 'TUTOR') {
        errs.push('Rol de mensaje inválido');
        break;
      }
    }
  }
  if (!payload.challengeContext?.statement?.trim()) errs.push('Falta enunciado del desafío');
  if (!payload.author?.trim()) errs.push('Falta autor');
  for (const k of SCORE_KEYS) {
    const s = payload.referenceScores?.[k];
    if (typeof s !== 'number' || !Number.isInteger(s) || s < 0 || s > 100) {
      errs.push(`Puntuación ${k} debe ser entero entre 0 y 100`);
    }
  }
  return errs;
}

export function parseImportText(text: string, formatHint: 'JSON' | 'CSV' | 'AUTO' = 'AUTO'): { format: 'JSON' | 'CSV'; rows: StagedRow[] } {
  const trimmed = text.trim();
  const isJson = formatHint === 'JSON' || (formatHint === 'AUTO' && (trimmed.startsWith('[') || trimmed.startsWith('{')));
  if (isJson) {
    try {
      const parsed = JSON.parse(trimmed);
      const items: any[] = Array.isArray(parsed) ? parsed : (parsed.cases || parsed.items || parsed.rows || [parsed]);
      const rows: StagedRow[] = items.map((item, idx) => {
        let transcript = item.transcript || item.messages || [];
        if (!transcript.length && (item.student_message || item.tutor_message)) {
          transcript = [
            ...(item.student_message ? [{ role: 'STUDENT', content: item.student_message, position: 0 }] : []),
            ...(item.tutor_message ? [{ role: 'TUTOR', content: item.tutor_message, position: 1 }] : []),
          ];
        }
        const payload: StagedRowPayload = {
          transcript: transcript.map((m: any, p: number) => ({ role: m.role || 'STUDENT', content: m.content || '', position: m.position ?? p })),
          challengeContext: {
            externalChallengeId: item.challengeContext?.externalChallengeId || item.externalChallengeId || item.external_challenge_id || '',
            statement: item.challengeContext?.statement || item.statement || '',
          },
          metadata: item.metadata && typeof item.metadata === 'object' ? item.metadata : (item.notes ? { notes: item.notes } : {}),
          author: item.author || item.autor || '',
          referenceScores: {
            AUTONOMY: Number(item.referenceScores?.AUTONOMY ?? item.autonomy ?? item.AUTONOMY ?? 0),
            CLARITY: Number(item.referenceScores?.CLARITY ?? item.clarity ?? item.CLARITY ?? 0),
            PROGRESSION: Number(item.referenceScores?.PROGRESSION ?? item.progression ?? item.PROGRESSION ?? 0),
            COMPLIANCE: Number(item.referenceScores?.COMPLIANCE ?? item.compliance ?? item.COMPLIANCE ?? 0),
            EFFICIENCY: Number(item.referenceScores?.EFFICIENCY ?? item.efficiency ?? item.EFFICIENCY ?? 0),
          },
          scoreJustifications: item.scoreJustifications || item.justifications || {},
        };
        return { rowNumber: idx + 1, payload, errors: validateRow(payload) };
      });
      return { format: 'JSON', rows };
    } catch {
      return { format: 'JSON', rows: [] };
    }
  }

  // CSV parsing
  const lines = trimmed.split(/\r?\n/).filter(line => line.trim().length > 0);
  if (lines.length < 2) return { format: 'CSV', rows: [] };
  const delimiter = lines[0].includes(';') ? ';' : ',';
  const headers = lines[0].split(delimiter).map(h => h.trim().replace(/^["']|["']$/g, '').toLowerCase());
  const rows: StagedRow[] = [];
  for (let i = 1; i < lines.length; i++) {
    const values: string[] = [];
    let current = '';
    let inQuotes = false;
    const line = lines[i];
    for (let c = 0; c < line.length; c++) {
      const char = line[c];
      if (char === '"' || char === "'") {
        inQuotes = !inQuotes;
      } else if (char === delimiter && !inQuotes) {
        values.push(current.trim().replace(/^["']|["']$/g, ''));
        current = '';
      } else {
        current += char;
      }
    }
    values.push(current.trim().replace(/^["']|["']$/g, ''));
    const rec: Record<string, string> = {};
    headers.forEach((hdr, idx) => { rec[hdr] = values[idx] ?? ''; });

    const studentMsg = rec['student_message'] || rec['estudiante'] || rec['alumno'] || '';
    const tutorMsg = rec['tutor_message'] || rec['tutor'] || '';
    const transcript: { role: string; content: string; position?: number }[] = [];
    if (studentMsg) transcript.push({ role: 'STUDENT', content: studentMsg, position: 0 });
    if (tutorMsg) transcript.push({ role: 'TUTOR', content: tutorMsg, position: transcript.length });

    const payload: StagedRowPayload = {
      transcript,
      challengeContext: {
        externalChallengeId: rec['externalchallengeid'] || rec['challenge_id'] || rec['desafio_id'] || '',
        statement: rec['statement'] || rec['enunciado'] || rec['contexto'] || '',
      },
      metadata: rec['metadata'] || rec['notas'] ? { notes: rec['metadata'] || rec['notas'] } : {},
      author: rec['author'] || rec['autor'] || '',
      referenceScores: {
        AUTONOMY: parseInt(rec['autonomy'] || rec['autonomia'] || '0', 10),
        CLARITY: parseInt(rec['clarity'] || rec['claridad'] || '0', 10),
        PROGRESSION: parseInt(rec['progression'] || rec['progresion'] || '0', 10),
        COMPLIANCE: parseInt(rec['compliance'] || rec['cumplimiento'] || '0', 10),
        EFFICIENCY: parseInt(rec['efficiency'] || rec['eficiencia'] || '0', 10),
      },
      scoreJustifications: {},
    };
    rows.push({ rowNumber: i, payload, errors: validateRow(payload) });
  }
  return { format: 'CSV', rows };
}

export interface EligibleInteractionItem {
  id: string;
  preview: string;
  externalChallengeId: string;
  statement: string;
}

export type ActivityType = 'CHALLENGE' | 'EXAM';

export interface DemoActivity {
  id: string;
  type: ActivityType;
  title: string;
  statement: string;
  createdAt: string;
}

// Temporary local catalog until Challenge and Exams expose their course APIs.
const DEMO_ACTIVITIES: readonly DemoActivity[] = [
  { id: 'exam-parcial-02', type: 'EXAM', title: 'Parcial 2 · Estructuras dinámicas', statement: 'Resolvé los ejercicios sobre listas enlazadas, pilas y colas.', createdAt: '2026-09-06T10:00:00-03:00' },
  { id: 'desafio-grafos-01', type: 'CHALLENGE', title: 'Grafos · Recorrido en profundidad', statement: 'Implementá un recorrido DFS para un grafo representado mediante listas de adyacencia.', createdAt: '2026-09-04T09:30:00-03:00' },
  { id: 'desafio-pilas-02', type: 'CHALLENGE', title: 'Pilas · Evaluador de expresiones', statement: 'Usá una pila para verificar el balanceo de paréntesis en una expresión.', createdAt: '2026-08-28T14:00:00-03:00' },
  { id: 'exam-parcial-01', type: 'EXAM', title: 'Parcial 1 · Colecciones', statement: 'Completá las operaciones solicitadas usando arreglos y listas.', createdAt: '2026-08-21T08:00:00-03:00' },
  { id: 'desafio-arreglos-01', type: 'CHALLENGE', title: 'Arreglos · Búsqueda del máximo', statement: 'Encontrá el valor máximo de un arreglo de enteros sin ordenar.', createdAt: '2026-08-14T11:00:00-03:00' },
];

@Component({
  selector: 'app-golden-set-page',
  imports: [ReactiveFormsModule, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './golden-set-page.component.html',
  styleUrl: './golden-set-page.component.scss',
})
export class GoldenSetPage {
  private readonly http = inject(HttpClient);
  private readonly fb = inject(NonNullableFormBuilder);
  private readonly route = inject(ActivatedRoute, { optional: true });
  private readonly router = inject(Router, { optional: true });

  readonly courseId = input('');
  readonly editorRoutePath = this.route?.snapshot.routeConfig?.path ?? 'golden-set';
  readonly editorRoute = this.editorRoutePath !== 'golden-set';
  readonly creatingGoldenSetRoute = this.editorRoutePath === 'golden-set/new';
  readonly editingVersionId = this.route?.snapshot.paramMap.get('versionId') ?? null;
  readonly goldenSetListPath = computed(() => `/docente/cursos/${this.courseId()}/evaluador/golden-set`);
  readonly goldenSets = httpResource<GoldenSetPageResponse>(
    () => this.courseId() ? `/api/llm/courses/${this.courseId()}/golden-sets` : undefined,
    { defaultValue: { items: [] } }
  );

  readonly draft = signal<GoldenSetVersion | null>(null);
  readonly saving = signal(false);
  readonly creatingDraft = signal(false);
  readonly saved = signal(false);
  readonly saveError = signal('');
  readonly scoreKeys = SCORE_KEYS;
  readonly activityTypeFilter = signal<'ALL' | ActivityType>('ALL');
  readonly activities = [...DEMO_ACTIVITIES].sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  readonly filteredActivities = computed(() => {
    const type = this.activityTypeFilter();
    return this.activities.filter(activity => type === 'ALL' || activity.type === type);
  });

  // Staging / Import state (GS-0533)
  readonly importDraft = signal<GoldenSetVersion | null>(null);
  readonly rawInput = signal('');
  readonly detectedFormat = signal<'JSON' | 'CSV'>('JSON');
  readonly stagingRows = signal<StagedRow[]>([]);
  readonly activeBatch = signal<{ id: string; state: string } | null>(null);
  readonly importing = signal(false);
  readonly importError = signal('');
  readonly importSuccess = signal('');
  readonly editingRow = signal<StagedRow | null>(null);

  // Real interaction selection & anonymization preview state (GS-0534)
  readonly realInteractionDraft = signal<GoldenSetVersion | null>(null);
  readonly loadingEligible = signal(false);
  readonly eligibleList = signal<EligibleInteractionItem[]>([]);
  readonly anonymizedPreview = signal<any | null>(null);
  readonly savingReal = signal(false);
  readonly realCaseError = signal('');

  // Synthetic cases generation & mandatory teacher review state (GS-0535)
  readonly syntheticDraft = signal<GoldenSetVersion | null>(null);
  readonly generatingSynthetic = signal(false);
  readonly syntheticProposals = signal<any[]>([]);
  readonly selectedSyntheticCase = signal<any | null>(null);
  readonly savingSynthetic = signal(false);
  readonly syntheticError = signal('');

  readonly validCount = computed(() => this.stagingRows().filter(r => r.errors.length === 0).length);
  readonly errorCount = computed(() => this.stagingRows().filter(r => r.errors.length > 0).length);
  readonly latestPublished = computed(() => this.goldenSets.value().items
    .filter(set => set.state === 'PUBLISHED')
    .sort((a, b) => b.version - a.version)[0] ?? null);
  readonly editedGoldenSet = computed(() => this.editingVersionId
    ? this.goldenSets.value().items.find(set => set.id === this.editingVersionId) ?? null
    : null);

  readonly form = this.fb.group({
    transcriptText: ['', [Validators.required, transcriptValidator]],
    externalChallengeId: ['', Validators.required],
    statement: ['', Validators.required],
    metadata: [''],
    author: ['', Validators.required],
    AUTONOMY: [0, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    CLARITY: [0, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    PROGRESSION: [0, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    COMPLIANCE: [0, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    EFFICIENCY: [0, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    AUTONOMY_justification: [''],
    CLARITY_justification: [''],
    PROGRESSION_justification: [''],
    COMPLIANCE_justification: [''],
    EFFICIENCY_justification: [''],
  });

  readonly realCaseForm = this.fb.group({
    author: ['Docente', Validators.required],
    AUTONOMY: [80, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    CLARITY: [80, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    PROGRESSION: [80, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    COMPLIANCE: [100, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    EFFICIENCY: [80, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    AUTONOMY_justification: [''],
    CLARITY_justification: [''],
    PROGRESSION_justification: [''],
    COMPLIANCE_justification: [''],
    EFFICIENCY_justification: [''],
  });

  readonly syntheticCaseForm = this.fb.group({
    author: ['Docente evaluador', Validators.required],
    AUTONOMY: [70, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    CLARITY: [75, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    PROGRESSION: [80, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    COMPLIANCE: [90, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    EFFICIENCY: [75, [Validators.required, Validators.min(0), Validators.max(100), Validators.pattern(/^\d+$/)]],
    AUTONOMY_justification: [''],
    CLARITY_justification: [''],
    PROGRESSION_justification: [''],
    COMPLIANCE_justification: [''],
    EFFICIENCY_justification: [''],
  });

  constructor() {
    effect(() => {
      const set = this.editedGoldenSet();
      if (this.editorRoute && set?.state === 'DRAFT' && !this.draft()) this.startCase(set);
    });
  }

  createDraft() {
    this.creatingDraft.set(true);
    this.saveError.set('');
    const name = this.newDraftName();
    this.http.post<GoldenSetVersion>(`/api/llm/courses/${this.courseId()}/golden-sets`, {
      name,
    }, { headers: { 'Idempotency-Key': createIdempotencyKey() } }).pipe(
      catchError((error: HttpErrorResponse) => { this.saveError.set(this.describeDraftError(error)); return of(null); }),
      finalize(() => this.creatingDraft.set(false)),
    ).subscribe((created) => { if (created) { this.goldenSets.reload(); this.startCase({ ...created, name, cases: [] }); } });
  }

  /** A course cannot have two Golden Set families with the same name, including a logically deleted one. */
  private newDraftName(): string {
    return `Golden Set de práctica · ${new Date().toISOString().replace('T', ' ').slice(0, 19)} UTC`;
  }

  startCase(set: GoldenSetVersion) {
    this.importDraft.set(null);
    this.realInteractionDraft.set(null);
    this.draft.set(set);
    this.form.reset({ transcriptText: '', AUTONOMY: 0, CLARITY: 0, PROGRESSION: 0, COMPLIANCE: 0, EFFICIENCY: 0 });
    this.saved.set(false);
    this.saveError.set('');
  }

  cancelCase() {
    this.draft.set(null);
    this.saved.set(false);
    this.saveError.set('');
  }

  scoreLabel(key: ScoreKey): string {
    return ({ AUTONOMY: 'Autonomía', CLARITY: 'Claridad', PROGRESSION: 'Progresión', COMPLIANCE: 'Cumplimiento', EFFICIENCY: 'Eficiencia' })[key];
  }

  justificationControl(key: ScoreKey): JustificationKey { return `${key}_justification`; }
  scoreInvalid(key: ScoreKey): boolean { const control = this.form.controls[key]; return control.invalid && control.touched; }
  transcriptStatus(): TranscriptParseResult { return parseTranscriptText(this.form.controls.transcriptText.value); }

  setActivityTypeFilter(event: Event) {
    this.activityTypeFilter.set((event.target as HTMLSelectElement).value as 'ALL' | ActivityType);
  }

  selectActivity(event: Event) {
    const id = (event.target as HTMLSelectElement).value;
    const activity = this.activities.find(item => item.id === id);
    this.form.patchValue({ externalChallengeId: id, ...(activity ? { statement: activity.statement } : {}) });
  }

  saveCase() {
    const set = this.draft();
    if (!set) return;
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    const transcript = parseTranscriptText(value.transcriptText).messages;
    const referenceScores = Object.fromEntries(SCORE_KEYS.map(key => [key, value[key]]));
    const scoreJustifications = Object.fromEntries(
      SCORE_KEYS.filter(key => value[this.justificationControl(key)].trim()).map(key => [key, value[this.justificationControl(key)].trim()])
    );
    this.saving.set(true);
    this.saved.set(false);
    this.saveError.set('');
    this.http.post(`/api/llm/courses/${this.courseId()}/golden-sets/${set.id}/cases`, {
      transcript: transcript.map((message, position) => ({ ...message, position })),
      challengeContext: { externalChallengeId: value.externalChallengeId, statement: value.statement },
      metadata: value.metadata.trim() ? { notes: value.metadata.trim() } : {},
      author: value.author,
      referenceScores,
      scoreJustifications,
    }).pipe(
      catchError((error: HttpErrorResponse) => {
        this.saveError.set(this.describeSaveError(error));
        return of(null);
      }),
      finalize(() => this.saving.set(false))
    ).subscribe(created => {
      if (!created) return;
      this.form.reset({ transcriptText: '', AUTONOMY: 0, CLARITY: 0, PROGRESSION: 0, COMPLIANCE: 0, EFFICIENCY: 0 });
      this.saved.set(true);
      this.goldenSets.reload();
    });
  }

  private describeSaveError(error: HttpErrorResponse): string {
    const detail = typeof error.error?.detail === 'string' ? error.error.detail : '';
    if (error.status === 422) return detail || 'Revisá los puntajes: se requieren cinco enteros entre 0 y 100, junto con conversación, contexto y autor.';
    if (error.status === 403) return 'No tenés permiso para modificar este curso.';
    if (error.status === 409) return detail || 'Este Golden Set ya no es un borrador. Creá una nueva versión para agregar casos.';
    if (error.status === 0) return 'No se pudo conectar con el servidor. Verificá que el workbench y el servicio estén en ejecución.';
    return detail || `No se pudo guardar el caso (error ${error.status || 'desconocido'}). Intentá nuevamente.`;
  }

  private describeDraftError(error: HttpErrorResponse): string {
    const detail = typeof error.error?.detail === 'string' ? error.error.detail : '';
    if (error.status === 422) return detail || 'El servidor rechazó el borrador. El nombre enviado no es válido.';
    if (error.status === 403) return 'No tenés permiso para crear un Golden Set en este curso.';
    if (error.status === 0) return 'No se pudo conectar con el servicio. Verificá que el workbench esté en ejecución.';
    return detail || `No se pudo crear el borrador (error ${error.status || 'desconocido'}). Intentá nuevamente.`;
  }

  // Import Staging methods (GS-0533)
  startImport(set: GoldenSetVersion) {
    this.draft.set(null);
    this.realInteractionDraft.set(null);
    this.importDraft.set(set);
    this.rawInput.set('');
    this.stagingRows.set([]);
    this.activeBatch.set(null);
    this.importError.set('');
    this.importSuccess.set('');
    this.editingRow.set(null);
  }

  cancelImport() {
    this.importDraft.set(null);
    this.editingRow.set(null);
    this.stagingRows.set([]);
    this.activeBatch.set(null);
  }

  onRawInput(event: Event) {
    this.rawInput.set((event.target as HTMLTextAreaElement).value);
  }

  onFileSelected(event: Event) {
    const inputEl = event.target as HTMLInputElement;
    if (!inputEl.files || inputEl.files.length === 0) return;
    const file = inputEl.files[0];
    const reader = new FileReader();
    reader.onload = () => {
      const text = String(reader.result ?? '');
      this.rawInput.set(text);
      this.processRawInput();
    };
    reader.readAsText(file);
  }

  processRawInput() {
    const text = this.rawInput();
    if (!text.trim()) {
      this.stagingRows.set([]);
      this.activeBatch.set(null);
      return;
    }
    const { format, rows } = parseImportText(text, 'AUTO');
    this.detectedFormat.set(format);
    this.stagingRows.set(rows);
    this.activeBatch.set(null);
    this.importError.set('');
  }

  startEditRow(rowNumber: number) {
    const found = this.stagingRows().find(r => r.rowNumber === rowNumber);
    if (found) {
      this.editingRow.set(JSON.parse(JSON.stringify(found)));
    }
  }

  cancelRowEdit() {
    this.editingRow.set(null);
  }

  updateEditField(field: 'author' | 'statement' | 'externalChallengeId', event: Event) {
    const current = this.editingRow();
    if (!current) return;
    const val = (event.target as HTMLInputElement | HTMLSelectElement).value;
    if (field === 'author') current.payload.author = val;
    else if (field === 'statement') current.payload.challengeContext.statement = val;
    else if (field === 'externalChallengeId') current.payload.challengeContext.externalChallengeId = val;
    this.editingRow.set({ ...current });
  }

  updateEditScore(key: ScoreKey, event: Event) {
    const current = this.editingRow();
    if (!current) return;
    const num = parseInt((event.target as HTMLInputElement).value, 10);
    current.payload.referenceScores[key] = isNaN(num) ? 0 : num;
    this.editingRow.set({ ...current });
  }

  saveRowEdit() {
    const current = this.editingRow();
    if (!current) return;
    const updatedErrors = validateRow(current.payload);
    const updatedRow: StagedRow = { ...current, errors: updatedErrors };
    const newRows = this.stagingRows().map(r => r.rowNumber === updatedRow.rowNumber ? updatedRow : r);
    this.stagingRows.set(newRows);

    const batch = this.activeBatch();
    if (batch) {
      this.http.patch(
        `/api/llm/courses/${this.courseId()}/golden-set-imports/${batch.id}/rows/${updatedRow.rowNumber}`,
        updatedRow.payload
      ).pipe(catchError(() => of(null))).subscribe(() => {
        this.validateBatch(batch.id);
      });
    }
    this.editingRow.set(null);
  }

  uploadAndValidateBatch() {
    const set = this.importDraft();
    if (!set || this.stagingRows().length === 0) return;
    this.importing.set(true);
    this.importError.set('');

    const body = {
      goldenSetVersionId: set.id,
      format: this.detectedFormat(),
      rows: this.stagingRows().map(r => r.payload),
    };

    this.http.post<{ id: string; state: string }>(
      `/api/llm/courses/${this.courseId()}/golden-set-imports`,
      body,
      { headers: { 'Idempotency-Key': createIdempotencyKey() } }
    ).pipe(
      catchError(() => {
        this.importError.set('No se pudo crear el lote de importación en el servidor.');
        return of(null);
      })
    ).subscribe(created => {
      if (!created) {
        this.importing.set(false);
        return;
      }
      this.validateBatch(created.id);
    });
  }

  validateBatch(batchId: string) {
    this.http.post<{ id: string; state: string }>(
      `/api/llm/courses/${this.courseId()}/golden-set-imports/${batchId}/validate`,
      {},
      { headers: { 'Idempotency-Key': createIdempotencyKey() } }
    ).pipe(
      catchError(() => {
        this.importError.set('No se pudo validar el lote en el servidor.');
        return of(null);
      }),
      finalize(() => this.importing.set(false))
    ).subscribe(validated => {
      if (validated) {
        this.activeBatch.set(validated);
      }
    });
  }

  commitBatch() {
    const set = this.importDraft();
    const batch = this.activeBatch();
    if (!set || !batch || batch.state !== 'READY' || this.errorCount() > 0) return;

    this.importing.set(true);
    this.importError.set('');

    this.http.post(
      `/api/llm/courses/${this.courseId()}/golden-set-imports/${batch.id}/commit`,
      {},
      { headers: { 'Idempotency-Key': createIdempotencyKey() } }
    ).pipe(
      catchError(() => {
        this.importError.set('No se pudo confirmar el lote en el servidor.');
        return of(null);
      }),
      finalize(() => this.importing.set(false))
    ).subscribe(() => {
      const count = this.stagingRows().length;
      this.cancelImport();
      this.importSuccess.set(`Lote confirmado exitosamente: ${count} caso(s) incorporados al Golden Set.`);
      this.goldenSets.reload();
    });
  }

  // Real Interaction selection & anonymization preview (GS-0534)
  startRealInteraction(set: GoldenSetVersion) {
    this.draft.set(null);
    this.importDraft.set(null);
    this.realInteractionDraft.set(set);
    this.anonymizedPreview.set(null);
    this.realCaseError.set('');
    this.loadingEligible.set(true);
    this.http.get<{ items: EligibleInteractionItem[] }>(
      `/api/llm/courses/${this.courseId()}/eligible-interactions`
    ).pipe(
      catchError(() => of({ items: [] })),
      finalize(() => this.loadingEligible.set(false))
    ).subscribe(res => {
      this.eligibleList.set(res.items);
    });
  }

  cancelRealInteraction() {
    this.realInteractionDraft.set(null);
    this.anonymizedPreview.set(null);
    this.realCaseError.set('');
  }

  previewAnonymization(set: GoldenSetVersion, item: EligibleInteractionItem) {
    this.http.post<any>(
      `/api/llm/courses/${this.courseId()}/eligible-interactions/${item.id}/anonymize-preview`,
      {},
      { headers: { 'Idempotency-Key': createIdempotencyKey() } }
    ).pipe(
      catchError(() => {
        this.realCaseError.set('No se pudo generar la vista previa anonimizada.');
        return of(null);
      })
    ).subscribe(preview => {
      if (preview) {
        this.anonymizedPreview.set(preview);
      }
    });
  }

  saveRealCase(set: GoldenSetVersion) {
    const preview = this.anonymizedPreview();
    if (!preview || this.realCaseForm.invalid) {
      this.realCaseForm.markAllAsTouched();
      return;
    }
    const val = this.realCaseForm.getRawValue();
    const referenceScores = Object.fromEntries(SCORE_KEYS.map(k => [k, val[k]]));
    const scoreJustifications = Object.fromEntries(
      SCORE_KEYS.filter(k => val[this.justificationControl(k)].trim()).map(k => [k, val[this.justificationControl(k)].trim()])
    );

    this.savingReal.set(true);
    this.realCaseError.set('');

    this.http.post(
      `/api/llm/courses/${this.courseId()}/golden-sets/${set.id}/cases`,
      {
        transcript: preview.transcript,
        challengeContext: preview.challengeContext,
        metadata: preview.metadata || {},
        author: val.author,
        referenceScores,
        scoreJustifications,
      },
      { headers: { 'Idempotency-Key': createIdempotencyKey() } }
    ).pipe(
      catchError(() => {
        this.realCaseError.set('No se pudo incorporar el caso real.');
        return of(null);
      }),
      finalize(() => this.savingReal.set(false))
    ).subscribe(created => {
      if (created) {
        this.cancelRealInteraction();
        this.importSuccess.set('Interacción real anonimizada incorporada con éxito al Golden Set.');
        this.goldenSets.reload();
      }
    });
  }

  // Synthetic cases generation & mandatory teacher review (GS-0535)
  startSyntheticGeneration(set: GoldenSetVersion) {
    this.draft.set(null);
    this.importDraft.set(null);
    this.realInteractionDraft.set(null);
    this.syntheticDraft.set(set);
    this.selectedSyntheticCase.set(null);
    this.syntheticProposals.set([]);
    this.syntheticError.set('');
    this.generatingSynthetic.set(true);

    this.http.post<{ id: string; state: string; items: any[] }>(
      `/api/llm/courses/${this.courseId()}/synthetic-golden-set-cases`,
      { count: 2 },
      { headers: { 'Idempotency-Key': createIdempotencyKey() } }
    ).pipe(
      catchError(() => {
        this.syntheticError.set('No se pudieron generar propuestas de casos sintéticos.');
        return of(null);
      }),
      finalize(() => this.generatingSynthetic.set(false))
    ).subscribe(res => {
      if (res && res.items) {
        this.syntheticProposals.set(res.items);
        if (res.items.length > 0) {
          this.selectSyntheticCase(res.items[0]);
        }
      }
    });
  }

  selectSyntheticCase(item: any) {
    this.selectedSyntheticCase.set(item);
    this.syntheticCaseForm.patchValue({
      author: 'Docente evaluador',
      AUTONOMY: 70,
      CLARITY: 75,
      PROGRESSION: 80,
      COMPLIANCE: 90,
      EFFICIENCY: 75,
    });
  }

  cancelSyntheticGeneration() {
    this.syntheticDraft.set(null);
    this.syntheticProposals.set([]);
    this.selectedSyntheticCase.set(null);
    this.syntheticError.set('');
  }

  saveSyntheticCase(set: GoldenSetVersion) {
    const item = this.selectedSyntheticCase();
    if (!item || this.syntheticCaseForm.invalid) return;

    const val = this.syntheticCaseForm.getRawValue();
    const referenceScores: Record<string, number> = {
      AUTONOMY: Number(val.AUTONOMY),
      CLARITY: Number(val.CLARITY),
      PROGRESSION: Number(val.PROGRESSION),
      COMPLIANCE: Number(val.COMPLIANCE),
      EFFICIENCY: Number(val.EFFICIENCY),
    };
    const scoreJustifications: Record<string, string> = {};
    if (val.AUTONOMY_justification) scoreJustifications['AUTONOMY'] = val.AUTONOMY_justification;
    if (val.CLARITY_justification) scoreJustifications['CLARITY'] = val.CLARITY_justification;
    if (val.PROGRESSION_justification) scoreJustifications['PROGRESSION'] = val.PROGRESSION_justification;
    if (val.COMPLIANCE_justification) scoreJustifications['COMPLIANCE'] = val.COMPLIANCE_justification;
    if (val.EFFICIENCY_justification) scoreJustifications['EFFICIENCY'] = val.EFFICIENCY_justification;

    this.savingSynthetic.set(true);
    this.syntheticError.set('');

    this.http.post(
      `/api/llm/courses/${this.courseId()}/golden-sets/${set.id}/cases`,
      {
        transcript: item.transcript,
        challengeContext: item.challengeContext,
        metadata: item.metadata || {},
        author: val.author,
        referenceScores,
        scoreJustifications,
      },
      { headers: { 'Idempotency-Key': createIdempotencyKey() } }
    ).pipe(
      catchError(() => {
        this.syntheticError.set('No se pudo incorporar el caso sintético revisado.');
        return of(null);
      }),
      finalize(() => this.savingSynthetic.set(false))
    ).subscribe(created => {
      if (created) {
        this.importSuccess.set('Caso sintético evaluado y revisado por docente incorporado con éxito al Golden Set.');
        this.cancelSyntheticGeneration();
        this.goldenSets.reload();
      }
    });
  }

  // Publication & cloning lifecycle (GS-0536)
  publishVersion(set: GoldenSetVersion) {
    let failed = false;
    this.http.post(
      `/api/llm/courses/${this.courseId()}/golden-sets/${set.id}/publish`,
      {},
      { headers: { 'Idempotency-Key': createIdempotencyKey() } }
    ).pipe(
      catchError(() => {
        this.importError.set('No se pudo publicar la versión.');
        failed = true;
        return of(null);
      })
    ).subscribe(() => {
      if (!failed) {
        this.importSuccess.set(`Versión ${set.version} publicada exitosamente (inmutable).`);
        this.goldenSets.reload();
      }
    });
  }

  createNextVersion(set: GoldenSetVersion) {
    this.http.post<GoldenSetVersion>(
      `/api/llm/courses/${this.courseId()}/golden-sets/${set.id}/next-version`,
      {},
      { headers: { 'Idempotency-Key': createIdempotencyKey() } }
    ).pipe(
      catchError((error: HttpErrorResponse) => {
        this.importError.set(this.describeNextVersionError(error));
        return of(null);
      })
    ).subscribe(created => {
      if (created) {
        this.importSuccess.set(`Nueva versión borrador (v${created.version}) creada exitosamente.`);
        this.goldenSets.reload();
        // Creating a version is the beginning of an editing task, not an endpoint.
        void this.router?.navigateByUrl(this.editorPath(created.id)).catch(() => undefined);
      }
    });
  }

  createNextVersionFromNavigation() {
    const published = this.latestPublished();
    if (!published) {
      this.importError.set('Para crear una versión nueva necesitás una versión publicada como base.');
      return;
    }
    this.createNextVersion(published);
  }

  editorPath(versionId: string): string {
    return `${this.goldenSetListPath()}/${versionId}/edit`;
  }

  deleteUnusedDraft(set: GoldenSetVersion) {
    if (!confirm(`¿Eliminar lógicamente “${set.name}” v${set.version}? Esta acción sólo se permite si nunca fue publicado ni usado.`)) return;
    this.importError.set('');
    this.http.delete(`/api/llm/courses/${this.courseId()}/golden-sets/${set.id}`).pipe(
      catchError((error: HttpErrorResponse) => {
        const detail = typeof error.error?.detail === 'string' ? error.error.detail : '';
        this.importError.set(error.status === 409 ? detail || 'No se puede eliminar porque este Golden Set tiene uso registrado.' : detail || 'No se pudo eliminar el Golden Set.');
        return EMPTY;
      })
    ).subscribe(() => {
      // DELETE returns 204 without a response body; completion still means success.
      this.importSuccess.set(`Golden Set v${set.version} eliminado lógicamente.`);
      this.goldenSets.reload();
    });
  }

  private describeNextVersionError(error: HttpErrorResponse): string {
    const detail = typeof error.error?.detail === 'string' ? error.error.detail : '';
    if (error.status === 422) return detail || 'No se pudo copiar un caso de la versión publicada porque sus datos de referencia no son válidos.';
    if (error.status === 409) return detail || 'Esta versión ya no está publicada. Actualizá la lista e intentá nuevamente.';
    if (error.status === 403) return 'No tenés permiso para crear una versión en este curso.';
    if (error.status === 0) return 'No se pudo conectar con el servicio. Verificá que el workbench esté en ejecución.';
    return detail || 'No se pudo crear la nueva versión. Intentá nuevamente.';
  }

  stateLabel(state: string): string { return state === 'DRAFT' ? 'Borrador' : state === 'PUBLISHED' ? 'Publicada' : 'Reemplazada'; }
  reviewLabel(state: string): string { return state === 'REVIEWED' ? 'Revisado' : 'Pendiente'; }
}

interface GoldenSetPageResponse { items: GoldenSetVersion[]; }
interface GoldenSetVersion { id: string; name: string; version: number; state: string; cases: GoldenSetCase[]; }
interface GoldenSetCase { id: string; order: number; author: string; reviewState: string; }
