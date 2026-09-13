import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AssignmentsPage } from './assignments-page';

describe('AssignmentsPage', () => {
  it('lists locked assignments and queued AI-use evaluations from the course API', async () => {
    await TestBed.configureTestingModule({ imports: [AssignmentsPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(AssignmentsPage);
    fixture.componentRef.setInput('courseId', '00000000-0000-0000-0000-000000000010');
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/challenge-calibration-assignments').flush({ items: [{ challengeId: 'challenge-0001', calibrationRunId: 'run-0001', locked: true }] });
    http.expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/pending-evaluations').flush({ items: [{ id: 'pending-1', attemptId: 'attempt-0001', calibrationRunId: 'run-0001', state: 'QUEUED' }] });
    await fixture.whenStable(); fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    expect(page.textContent).toContain('Bloqueada por intento');
    expect(page.textContent).toContain('1 evaluación(es) esperan una calibración válida');
    expect(page.textContent).toContain('no decide la aprobación académica');
  });
});
