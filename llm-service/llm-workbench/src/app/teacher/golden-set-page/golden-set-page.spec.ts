import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GoldenSetPage, parseTranscriptText } from './golden-set-page';

describe('GoldenSetPage', () => {
  it('lists versions with author and review state', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage); fixture.componentRef.setInput('courseId', '00000000-0000-0000-0000-000000000010'); fixture.detectChanges();
    TestBed.inject(HttpTestingController).expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({ items: [{ id: 'set-1', name: 'Tutoría inicial', version: 2, state: 'PUBLISHED', cases: [{ id: 'case-1', order: 0, author: 'Docente', reviewState: 'REVIEWED' }] }] });
    await fixture.whenStable(); fixture.detectChanges(); const page = fixture.nativeElement as HTMLElement;
    expect(page.textContent).toContain('Tutoría inicial'); expect(page.textContent).toContain('Docente'); expect(page.textContent).toContain('Revisado');
  });

  it('validates and persists a complete human reference case', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage); fixture.componentRef.setInput('courseId', '00000000-0000-0000-0000-000000000010'); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({ items: [{ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] }] });
    await fixture.whenStable(); fixture.detectChanges();
    fixture.componentInstance.startCase({ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] });
    fixture.componentInstance.saveCase();
    expect(fixture.componentInstance.form.invalid).toBe(true);
    fixture.componentInstance.form.patchValue({ transcriptText: 'Estudiante: Intenté usar un arreglo.\nTutor IA: ¿Qué operación necesitás resolver?', externalChallengeId: 'challenge-1', statement: 'Implementar una pila.', author: 'Docente', metadata: 'pilas; básico', AUTONOMY: 90, CLARITY: 80, PROGRESSION: 70, COMPLIANCE: 100, EFFICIENCY: 60, AUTONOMY_justification: 'Explica su intento.' });
    fixture.componentInstance.saveCase();
    const request = http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets/set-1/cases');
    expect(request.request.method).toBe('POST');
    expect(request.request.body.challengeContext).toEqual({ externalChallengeId: 'challenge-1', statement: 'Implementar una pila.' });
    expect(request.request.body.referenceScores).toEqual({ AUTONOMY: 90, CLARITY: 80, PROGRESSION: 70, COMPLIANCE: 100, EFFICIENCY: 60 });
    expect(request.request.body.transcript).toEqual([{ role: 'STUDENT', content: 'Intenté usar un arreglo.', position: 0 }, { role: 'TUTOR', content: '¿Qué operación necesitás resolver?', position: 1 }]);
    expect(request.request.body.scoreJustifications).toEqual({ AUTONOMY: 'Explica su intento.' });
    request.flush({ id: 'case-1', order: 0, author: 'Docente', reviewState: 'DRAFT' });
    fixture.detectChanges();
    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({ items: [{ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [{ id: 'case-1', order: 0, author: 'Docente', reviewState: 'DRAFT' }] }] });
  });

  it('creates a draft with the name required by the Golden Set API', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage);
    fixture.componentRef.setInput('courseId', '00000000-0000-0000-0000-000000000010');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const listUrl = '/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets';
    http.expectOne(listUrl).flush({ items: [] });

    fixture.componentInstance.createDraft();
    const request = http.expectOne(listUrl);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.name).toMatch(/^Golden Set de práctica · \d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2} UTC$/);
    request.flush({ id: 'set-1', familyId: 'family-1', version: 1, state: 'DRAFT', basedOnVersionId: null });
  });

  it('parses a whole conversation while keeping the roles required by the API', () => {
    expect(parseTranscriptText('Estudiante: Necesito recorrer la lista.\nTutor IA: ¿Qué índice representa el último elemento?\nEstudiante: length - 1.')).toEqual({
      messages: [
        { role: 'STUDENT', content: 'Necesito recorrer la lista.' },
        { role: 'TUTOR', content: '¿Qué índice representa el último elemento?' },
        { role: 'STUDENT', content: 'length - 1.' },
      ],
      error: null,
    });
    expect(parseTranscriptText('Esto no tiene etiquetas.').error).toContain('Estudiante');
  });

  it('filters the local challenge and exam catalog by type and fills the selected context', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage);
    const comp = fixture.componentInstance;

    expect(comp.activities[0].id).toBe('exam-parcial-02');
    comp.activityTypeFilter.set('EXAM');
    expect(comp.filteredActivities().map(activity => activity.id)).toEqual(['exam-parcial-02', 'exam-parcial-01']);

    comp.selectActivity({ target: { value: 'desafio-grafos-01' } } as unknown as Event);
    expect(comp.form.controls.externalChallengeId.value).toBe('desafio-grafos-01');
    expect(comp.form.controls.statement.value).toContain('DFS');
  });

  it('renders focusable help tooltips and full-width field controls in the case editor', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage);
    fixture.componentRef.setInput('courseId', '00000000-0000-0000-0000-000000000010');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({
      items: [{ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] }],
    });
    await fixture.whenStable();
    fixture.componentInstance.startCase({ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] });
    fixture.detectChanges();

    const page = fixture.nativeElement as HTMLElement;
    const triggers = Array.from(page.querySelectorAll<HTMLButtonElement>('.field-help-trigger'));
    const tooltips = page.querySelectorAll<HTMLElement>('.field-help-tooltip[role="tooltip"]');
    const textareas = Array.from(page.querySelectorAll<HTMLTextAreaElement>('.field-control > textarea'));

    expect(triggers.length).toBeGreaterThan(0);
    expect(triggers.every(trigger => trigger.type === 'button' && trigger.getAttribute('aria-label'))).toBe(true);
    expect(tooltips.length).toBe(triggers.length);
    expect(textareas.length).toBeGreaterThanOrEqual(3);
    expect(textareas.slice(0, 3).every(textarea => textarea.classList.contains('full-width-textarea') && !textarea.hasAttribute('style'))).toBe(true);
  });

  it('opens import staging, parses JSON/CSV with preview and per-row validation', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage);
    fixture.componentRef.setInput('courseId', '00000000-0000-0000-0000-000000000010');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({
      items: [{ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] }],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    const comp = fixture.componentInstance;
    comp.startImport({ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] });
    fixture.detectChanges();

    const jsonInput = JSON.stringify([
      {
        transcript: [{ role: 'STUDENT', content: '¿Cómo ordeno?' }, { role: 'TUTOR', content: 'Probá quicksort.' }],
        challengeContext: { externalChallengeId: 'ch-1', statement: 'Ordenar lista' },
        author: 'Docente Juan',
        referenceScores: { AUTONOMY: 80, CLARITY: 85, PROGRESSION: 90, COMPLIANCE: 100, EFFICIENCY: 75 },
      },
      {
        transcript: [{ role: 'STUDENT', content: '¿Qué es un árbol?' }],
        challengeContext: { externalChallengeId: 'ch-2', statement: 'Árboles binarios' },
        author: '',
        referenceScores: { AUTONOMY: 50, CLARITY: 50, PROGRESSION: 50, COMPLIANCE: 50, EFFICIENCY: 50 },
      },
    ]);

    comp.rawInput.set(jsonInput);
    comp.processRawInput();
    fixture.detectChanges();

    expect(comp.stagingRows().length).toBe(2);
    expect(comp.validCount()).toBe(1);
    expect(comp.errorCount()).toBe(1);
    expect(comp.stagingRows()[1].errors).toContain('Falta autor');

    const page = fixture.nativeElement as HTMLElement;
    expect(page.textContent).toContain('Total filas: 2');
    expect(page.textContent).toContain('Con errores (1)');
  });

  it('allows editing an invalid row in staging, uploading batch to backend and committing atomically when READY', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage);
    fixture.componentRef.setInput('courseId', '00000000-0000-0000-0000-000000000010');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({
      items: [{ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] }],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    const comp = fixture.componentInstance;
    comp.startImport({ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] });

    const csvContent = `statement,externalChallengeId,student_message,tutor_message,author,autonomy,clarity,progression,compliance,efficiency\n` +
      `Grafos DFS,ch-10,¿Cómo hago el recorrido?,Empezá marcando visitados.,Docente Ana,120,90,80,100,70`;

    comp.rawInput.set(csvContent);
    comp.processRawInput();
    fixture.detectChanges();

    expect(comp.stagingRows().length).toBe(1);
    expect(comp.errorCount()).toBe(1);

    comp.startEditRow(1);
    comp.updateEditScore('AUTONOMY', { target: { value: '95' } } as any);
    comp.saveRowEdit();
    fixture.detectChanges();

    expect(comp.errorCount()).toBe(0);
    expect(comp.validCount()).toBe(1);

    comp.uploadAndValidateBatch();
    const createReq = http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-set-imports');
    expect(createReq.request.method).toBe('POST');
    createReq.flush({ id: 'batch-99', state: 'DRAFT' });

    const validateReq = http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-set-imports/batch-99/validate');
    expect(validateReq.request.method).toBe('POST');
    validateReq.flush({ id: 'batch-99', state: 'READY' });
    fixture.detectChanges();

    expect(comp.activeBatch()?.state).toBe('READY');

    comp.commitBatch();
    const commitReq = http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-set-imports/batch-99/commit');
    expect(commitReq.request.method).toBe('POST');
    commitReq.flush(null, { status: 201, statusText: 'Created' });
    fixture.detectChanges();

    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({
      items: [{
        id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT',
        cases: [{ id: 'case-new', order: 0, author: 'Docente Ana', reviewState: 'REVIEWED' }],
      }],
    });
    fixture.detectChanges();

    expect(comp.importSuccess()).toContain('Lote confirmado exitosamente');
  });

  it('loads eligible interactions, previews irreversible anonymization and incorporates as REAL', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage);
    fixture.componentRef.setInput('courseId', '00000000-0000-0000-0000-000000000010');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({
      items: [{ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] }],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    const comp = fixture.componentInstance;
    comp.startRealInteraction({ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] });
    const eligibleReq = http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/eligible-interactions');
    expect(eligibleReq.request.method).toBe('GET');
    eligibleReq.flush({
      items: [{ id: 'int-1', preview: 'Consulta de alumno', externalChallengeId: 'ch-pila-01', statement: 'Pilas' }],
    });
    fixture.detectChanges();

    expect(comp.eligibleList().length).toBe(1);

    // Preview anonymization
    comp.previewAnonymization(
      { id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] },
      { id: 'int-1', preview: 'Consulta de alumno', externalChallengeId: 'ch-pila-01', statement: 'Pilas' }
    );
    const prevReq = http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/eligible-interactions/int-1/anonymize-preview');
    expect(prevReq.request.method).toBe('POST');
    prevReq.flush({
      id: 'int-1',
      challengeContext: { externalChallengeId: 'ch-pila-01', statement: 'Pilas' },
      transcript: [{ role: 'STUDENT', content: 'Duda con [REDACTED_EMAIL]' }],
    });
    fixture.detectChanges();

    expect(comp.anonymizedPreview()?.transcript.length).toBe(1);

    // Save real case with ratings
    comp.saveRealCase({ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] });
    const saveReq = http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets/set-1/cases');
    expect(saveReq.request.method).toBe('POST');
    expect(saveReq.request.body).not.toHaveProperty('provenance');
    expect(saveReq.request.body.referenceScores.AUTONOMY).toBe(80);
    saveReq.flush({ id: 'case-real-1', order: 0, author: 'Docente', reviewState: 'REVIEWED' });
    fixture.detectChanges();

    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({
      items: [{ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [{ id: 'case-real-1', order: 0, author: 'Docente', reviewState: 'REVIEWED' }] }],
    });
    fixture.detectChanges();

    expect(comp.importSuccess()).toContain('Interacción real anonimizada incorporada con éxito');
    expect(comp.realInteractionDraft()).toBeNull();
  });

  it('publishes a draft version making it immutable', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage);
    fixture.componentRef.setInput('courseId', '00000000-0000-0000-0000-000000000010');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({
      items: [{
        id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT',
        cases: [{ id: 'c1', order: 0, author: 'Docente', reviewState: 'REVIEWED' }],
      }],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    const comp = fixture.componentInstance;

    // Publish version
    comp.publishVersion({
      id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT',
      cases: [{ id: 'c1', order: 0, author: 'Docente', reviewState: 'REVIEWED' }],
    });
    const pubReq = http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets/set-1/publish');
    expect(pubReq.request.method).toBe('POST');
    pubReq.flush(null);
    fixture.detectChanges();

    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({
      items: [{
        id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'PUBLISHED',
        cases: [{ id: 'c1', order: 0, author: 'Docente', reviewState: 'REVIEWED' }],
      }],
    });
    fixture.detectChanges();

    expect(comp.importSuccess()).toContain('Versión 1 publicada exitosamente (inmutable)');
  });

  it('clones a new version from a published set', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage);
    fixture.componentRef.setInput('courseId', '00000000-0000-0000-0000-000000000010');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({
      items: [{
        id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'PUBLISHED',
        cases: [{ id: 'c1', order: 0, author: 'Docente', reviewState: 'REVIEWED' }],
      }],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    const comp = fixture.componentInstance;

    // Create next version
    comp.createNextVersion({
      id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'PUBLISHED',
      cases: [{ id: 'c1', order: 0, author: 'Docente', reviewState: 'REVIEWED' }],
    });
    const nextReq = http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets/set-1/next-version');
    expect(nextReq.request.method).toBe('POST');
    nextReq.flush({
      id: 'set-2', familyId: 'fam-1', version: 2, state: 'DRAFT', basedOnVersionId: 'set-1',
    });
    fixture.detectChanges();

    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({
      items: [
        { id: 'set-2', name: 'Tutoría inicial', version: 2, state: 'DRAFT', cases: [{ id: 'c1', order: 0, author: 'Docente', reviewState: 'REVIEWED' }] },
        { id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'PUBLISHED', cases: [{ id: 'c1', order: 0, author: 'Docente', reviewState: 'REVIEWED' }] },
      ],
    });
    fixture.detectChanges();

    expect(comp.importSuccess()).toContain('Nueva versión borrador (v2) creada exitosamente');
  });

  it('shows the backend detail when cloning a version is rejected', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage);
    fixture.componentRef.setInput('courseId', '00000000-0000-0000-0000-000000000010');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({ items: [] });
    await fixture.whenStable();

    fixture.componentInstance.createNextVersion({ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'PUBLISHED', cases: [] });
    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets/set-1/next-version')
      .flush({ detail: 'El caso copiado no cumple la regla de puntajes.' }, { status: 422, statusText: 'Unprocessable Entity' });

    expect(fixture.componentInstance.importError()).toBe('El caso copiado no cumple la regla de puntajes.');
  });

  it('refreshes the visible list after a successful bodyless DELETE response', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage);
    fixture.componentRef.setInput('courseId', '00000000-0000-0000-0000-000000000010');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({
      items: [{ id: 'set-1', name: 'Borrador', version: 1, state: 'DRAFT', cases: [] }],
    });
    await fixture.whenStable();

    const originalConfirm = globalThis.confirm;
    globalThis.confirm = () => true;
    try {
      fixture.componentInstance.deleteUnusedDraft({ id: 'set-1', name: 'Borrador', version: 1, state: 'DRAFT', cases: [] });
      http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets/set-1').flush(null, { status: 204, statusText: 'No Content' });
      fixture.detectChanges();
      http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({ items: [] });
      expect(fixture.componentInstance.importSuccess()).toContain('eliminado lógicamente');
    } finally {
      globalThis.confirm = originalConfirm;
    }
  });

  it('requests synthetic cases proposal and allows teacher to review, score and incorporate into golden set', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage);
    fixture.componentRef.setInput('courseId', '00000000-0000-0000-0000-000000000010');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({
      items: [{ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] }],
    });
    await fixture.whenStable();
    fixture.detectChanges();

    const comp = fixture.componentInstance;

    // Start synthetic proposal
    comp.startSyntheticGeneration({ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] });
    const propReq = http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/synthetic-golden-set-cases');
    expect(propReq.request.method).toBe('POST');
    propReq.flush({
      id: 'job-1',
      state: 'COMPLETED',
      items: [{
        id: 'prop-1',
        transcript: [{ role: 'STUDENT', content: '¿Cómo recursiono?' }, { role: 'TUTOR', content: 'Caso base' }],
        challengeContext: { externalChallengeId: 'ch-rec-01', statement: 'Recursión' },
        author: 'Generador Asistido / LLM',
        reviewState: 'DRAFT',
      }],
    });
    fixture.detectChanges();

    expect(comp.syntheticProposals().length).toBe(1);
    expect(comp.selectedSyntheticCase()?.author).toBe('Generador Asistido / LLM');

    // Save reviewed synthetic case
    comp.saveSyntheticCase({ id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT', cases: [] });
    const saveReq = http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets/set-1/cases');
    expect(saveReq.request.method).toBe('POST');
    expect(saveReq.request.body).not.toHaveProperty('provenance');
    expect(saveReq.request.body.referenceScores.AUTONOMY).toBe(70);
    saveReq.flush({ id: 'case-synth-1', order: 0, author: 'Docente evaluador', reviewState: 'REVIEWED' });
    fixture.detectChanges();

    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/golden-sets').flush({
      items: [{
        id: 'set-1', name: 'Tutoría inicial', version: 1, state: 'DRAFT',
        cases: [{ id: 'case-synth-1', order: 0, author: 'Docente evaluador', reviewState: 'REVIEWED' }],
      }],
    });
    fixture.detectChanges();

    expect(comp.importSuccess()).toContain('Caso sintético evaluado y revisado por docente incorporado con éxito al Golden Set');
    expect(comp.syntheticDraft()).toBeNull();
  });
});
