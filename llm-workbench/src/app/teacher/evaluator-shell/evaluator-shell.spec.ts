import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { EvaluatorShell } from './evaluator-shell';

describe('EvaluatorShell', () => {
  it('orients the teacher within a course and exposes all evaluator sections as links', async () => {
    await TestBed.configureTestingModule({
      imports: [EvaluatorShell],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();
    const fixture = TestBed.createComponent(EvaluatorShell);
    fixture.componentRef.setInput('courseId', '00000000-0000-0000-0000-000000000010');
    fixture.detectChanges();
    TestBed.inject(HttpTestingController)
      .expectOne('/api/llm/courses')
      .flush({ items: [{ id: '00000000-0000-0000-0000-000000000010', name: 'Programación III' }] });
    TestBed.inject(HttpTestingController)
      .expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/active-calibration')
      .flush(null, { status: 404, statusText: 'Not Found' });
    TestBed.inject(HttpTestingController)
      .expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/challenge-calibration-assignments')
      .flush({ items: [] });
    TestBed.inject(HttpTestingController)
      .expectOne('/api/llm/courses/00000000-0000-0000-0000-000000000010/pending-evaluations')
      .flush({ items: [{ id: 'pending-1', attemptId: 'attempt-1', calibrationRunId: 'run-1', state: 'QUEUED' }] });
    await fixture.whenStable();
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;

    expect(page.querySelector('nav[aria-label="Ruta de navegación"]')).not.toBeNull();
    expect(page.textContent).toContain('Programación III');
    expect(page.textContent).toContain('Sin calibración activa');
    expect(page.querySelector('[role="alert"]')?.textContent).toContain('Hay 1 evaluación(es) de uso de IA en cola');
    expect([...page.querySelectorAll('.course-nav a')].map((link) => link.textContent?.trim()))
      .toEqual(['⌂Resumen', '▤Rúbricas', '✦Golden Set', '◌Calibraciones', '⌘Asignaciones', '?Cómo usar']);
  });
});
