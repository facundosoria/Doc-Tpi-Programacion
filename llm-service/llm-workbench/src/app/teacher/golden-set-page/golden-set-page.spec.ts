import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GoldenSetPage, parseTranscriptText } from './golden-set-page';

describe('GoldenSetPage', () => {
  const courseId = '00000000-0000-0000-0000-000000000010';
  const coursesUrl = '/api/courses/me/course-cohorts';

  async function setup(listError = false) {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage);
    fixture.componentRef.setInput('courseId', courseId);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    const list = http.expectOne(`/api/llm/courses/${courseId}/golden-sets`);
    if (listError) list.flush({ detail: 'servicio temporalmente no disponible' }, { status: 503, statusText: 'Service Unavailable' });
    else list.flush({ items: [] });
    http.expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [] });
    http.expectOne(coursesUrl).flush([{ id: courseId, name: 'Taller de Estructuras' }]);
    await fixture.whenStable();
    fixture.detectChanges();
    return { fixture, http };
  }

  async function setupWithDraft() {
    const { fixture, http } = await setup();
    fixture.componentInstance.draft.set({ id: 'set-1', name: 'Golden Set', version: 1, state: 'DRAFT', cases: [] });
    fixture.detectChanges();
    return { fixture, http };
  }

  it('parses a complete teacher-friendly conversation', () => {
    expect(parseTranscriptText('Estudiante: Tengo una duda\nTutor IA: ¿Qué probaste?').error).toBeNull();
    expect(parseTranscriptText('Texto sin rol').error).toContain('Empezá');
  });

  it('shows three initial tabs and saves the PRD-minimal payload', async () => {
    const { fixture, http } = await setup();
    fixture.componentInstance.draft.set({ id: 'set-1', name: 'Golden Set', version: 1, state: 'DRAFT', cases: [] });
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('[role="tab"]').length).toBe(3);
    const page = fixture.componentInstance;
    page.form.patchValue({ transcriptText: 'Estudiante: Probé una pila\nTutor IA: ¿Qué condición falta?', AUTONOMY: 80, CLARITY: 75, PROGRESSION: 70, COMPLIANCE: 100, EFFICIENCY: 65 });
    page.saveCase();
    const request = http.expectOne(`/api/llm/courses/${courseId}/golden-sets/set-1/cases`);
    expect(request.request.body).toEqual({ transcript: [{ role: 'STUDENT', content: 'Probé una pila', position: 0 }, { role: 'TUTOR', content: '¿Qué condición falta?', position: 1 }], referenceScores: { AUTONOMY: 80, CLARITY: 75, PROGRESSION: 70, COMPLIANCE: 100, EFFICIENCY: 65 } });
  });

  it('sends an idempotency key when creating a draft', async () => {
    const { fixture, http } = await setup();
    fixture.componentInstance.createDraft();
    const request = http.expectOne(`/api/llm/courses/${courseId}/golden-sets`);
    expect(request.request.method).toBe('POST');
    expect(request.request.headers.get('Idempotency-Key')).toBeTruthy();
    request.flush({ id: 'set-1', name: 'Golden Set', version: 1, state: 'DRAFT' });
    await Promise.resolve();
    fixture.detectChanges();
    expect(fixture.componentInstance.message()).toContain('Borrador creado');
  });

  it('keeps publishing unavailable until there are three cases', async () => {
    const { fixture } = await setup();
    fixture.componentInstance.draft.set({ id: 'set-1', name: 'Golden Set', version: 1, state: 'DRAFT', cases: [] });
    fixture.componentInstance.publish();
    expect(fixture.componentInstance.error()).toContain('entre tres y cinco');
  });

  it('shows the authorized-teacher panel with the course in context', async () => {
    const { fixture } = await setup();
    const panel: HTMLElement | null = fixture.nativeElement.querySelector('.operating-as');
    expect(panel).not.toBeNull();
    expect(panel?.getAttribute('role')).toBe('status');
    expect(panel?.textContent).toContain('Operando como:');
    expect(panel?.textContent).toContain('Taller de Estructuras');
  });

  it('warns that the transcript is reference material, not instructions', async () => {
    const { fixture } = await setupWithDraft();
    const note: HTMLElement | null = fixture.nativeElement.querySelector('.field-note');
    expect(note).not.toBeNull();
    expect(note?.textContent).toContain('no se interpreta como instrucciones');
  });

  it('shows an explicit not-authorized message when the service answers 403', async () => {
    const { fixture, http } = await setup();
    fixture.componentInstance.loadDraft('set-1');
    http.expectOne(`/api/llm/courses/${courseId}/golden-sets/set-1`).flush({ detail: 'no sos docente del curso' }, { status: 403, statusText: 'Forbidden' });
    expect(fixture.componentInstance.error()).toContain('No autorizado');
    expect(fixture.componentInstance.error()).toContain('no sos docente del curso');
    expect(fixture.componentInstance.draft()).toBeNull();
  });

  it('shows a retryable alert when the list answers 5xx', async () => {
    const { fixture } = await setup(true);
    const alert: HTMLElement | null = fixture.nativeElement.querySelector('[role="alert"]');
    expect(alert?.textContent).toContain('No se pudo cargar el Golden Set');
    expect(fixture.nativeElement.querySelector('button')?.textContent).toContain('Reintentar');
    expect(fixture.nativeElement.querySelector('.start-card')).toBeNull();
  });

  it('navigates the case tabs with arrow keys and keeps aria-selected in sync', async () => {
    const { fixture } = await setupWithDraft();
    const tablist: HTMLElement | null = fixture.nativeElement.querySelector('[role="tablist"]');
    expect(tablist).not.toBeNull();
    const initialSelected = fixture.nativeElement.querySelector('[role="tab"][aria-selected="true"]');
    expect(initialSelected?.getAttribute('id')).toBe('case-tab-0');
    tablist!.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowRight', bubbles: true }));
    fixture.detectChanges();
    const afterSelected = fixture.nativeElement.querySelector('[role="tab"][aria-selected="true"]');
    expect(afterSelected?.getAttribute('id')).toBe('case-tab-1');
    expect(afterSelected?.getAttribute('tabindex')).toBe('0');
    expect(fixture.nativeElement.querySelector('[role="tab"]')?.getAttribute('tabindex')).toBe('-1');
  });

  it('associates every control with a label and announces the weighted score', async () => {
    const { fixture } = await setupWithDraft();
    const root = fixture.nativeElement as HTMLElement;
    const labels = Array.from(root.querySelectorAll('[for]'), (el) => el.getAttribute('for'));
    const inputs = Array.from(root.querySelectorAll('textarea, input[type="number"]'));
    for (const input of inputs) {
      const id = input.getAttribute('id');
      expect(id).toBeTruthy();
      expect(labels).toContain(id);
    }
    expect(root.querySelector('.weighted')?.getAttribute('role')).toBe('status');
    expect(root.querySelectorAll('fieldset legend').length).toBe(2);
  });

  it('marks publishing complete when five cases are loaded', async () => {
    const { fixture, http } = await setup();
    const cases = Array.from({ length: 5 }, (_, index) => ({ id: `case-${index}`, order: index + 1, transcript: [{ role: 'STUDENT' as const, content: 'Mensaje A', position: 0 }, { role: 'TUTOR' as const, content: 'Mensaje B', position: 1 }], referenceScores: { AUTONOMY: 80, CLARITY: 75, PROGRESSION: 70, COMPLIANCE: 100, EFFICIENCY: 65 } }));
    fixture.componentInstance.draft.set({ id: 'set-1', name: 'Golden Set', version: 1, state: 'DRAFT', cases });
    fixture.componentInstance.publish();
    http.expectOne(`/api/llm/courses/${courseId}/golden-sets/set-1/publish`).flush({ state: 'PUBLISHED' });
    expect(fixture.componentInstance.error()).toBe('');
    expect(fixture.componentInstance.message()).toContain('publicado correctamente');
  });
});