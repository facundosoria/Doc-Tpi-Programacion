import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GoldenSetPage, parseTranscriptText } from './golden-set-page';

describe('GoldenSetPage', () => {
  const courseId = '00000000-0000-0000-0000-000000000010';

  it('parses a complete teacher-friendly conversation', () => {
    expect(parseTranscriptText('Estudiante: Tengo una duda\nTutor IA: ¿Qué probaste?').error).toBeNull();
    expect(parseTranscriptText('Texto sin rol').error).toContain('Empezá');
  });

  it('shows three initial tabs and saves the PRD-minimal payload', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage); fixture.componentRef.setInput('courseId', courseId); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`/api/llm/courses/${courseId}/golden-sets`).flush({ items: [] });
    http.expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [] });
    fixture.componentInstance.createDraft();
    http.expectOne(`/api/llm/courses/${courseId}/golden-sets`).flush({ id: 'set-1', name: 'Golden Set', version: 1, state: 'DRAFT' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('[role="tab"]').length).toBe(3);
    const page = fixture.componentInstance;
    page.form.patchValue({ transcriptText: 'Estudiante: Probé una pila\nTutor IA: ¿Qué condición falta?', AUTONOMY: 80, CLARITY: 75, PROGRESSION: 70, COMPLIANCE: 100, EFFICIENCY: 65 });
    page.saveCase();
    const request = http.expectOne(`/api/llm/courses/${courseId}/golden-sets/set-1/cases`);
    expect(request.request.body).toEqual({ transcript: [{ role: 'STUDENT', content: 'Probé una pila', position: 0 }, { role: 'TUTOR', content: '¿Qué condición falta?', position: 1 }], referenceScores: { AUTONOMY: 80, CLARITY: 75, PROGRESSION: 70, COMPLIANCE: 100, EFFICIENCY: 65 } });
  });

  it('keeps publishing unavailable until there are three cases', async () => {
    await TestBed.configureTestingModule({ imports: [GoldenSetPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(GoldenSetPage); fixture.componentRef.setInput('courseId', courseId); fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`/api/llm/courses/${courseId}/golden-sets`).flush({ items: [] }); http.expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [] });
    fixture.componentInstance.draft.set({ id: 'set-1', name: 'Golden Set', version: 1, state: 'DRAFT', cases: [] });
    fixture.componentInstance.publish();
    expect(fixture.componentInstance.error()).toContain('entre tres y cinco');
  });
});
