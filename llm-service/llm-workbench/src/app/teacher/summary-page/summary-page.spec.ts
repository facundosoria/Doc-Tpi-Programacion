import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it } from 'vitest';
import { SummaryPage } from './summary-page';

describe('SummaryPage', () => {
  const COURSE_ID = '00000000-0000-0000-0000-000000000010';

  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  it('renders operational state with active calibration, quick access to drafts and no alerts (GS-0510, GS-0512)', async () => {
    await TestBed.configureTestingModule({
      imports: [SummaryPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(SummaryPage);
    fixture.componentRef.setInput('courseId', COURSE_ID);
    fixture.detectChanges();

    const http = TestBed.inject(HttpTestingController);

    http.expectOne(`/api/llm/courses/${COURSE_ID}/active-calibration`).flush({
      courseId: COURSE_ID,
      calibrationRunId: 'cal-run-00001',
      activatedAt: '2026-09-01T12:00:00Z',
    });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/pending-evaluations`).flush({ items: [] });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/base-update-proposals`).flush({ items: [] });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/rubrics`).flush({
      items: [
        { id: 'rub-1', name: 'Rúbrica de Tutoría', version: 1, state: 'DRAFT' },
      ],
    });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/golden-sets`).flush({
      items: [
        { id: 'gs-1', name: 'Golden Set v1', version: 1, state: 'DRAFT', cases: [{}, {}] },
      ],
    });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/calibrations`).flush({
      items: [
        { id: 'cal-run-00001', state: 'PASSED', progress: 100, createdAt: '2026-09-01T11:50:00Z' },
      ],
    });

    await fixture.whenStable();
    fixture.detectChanges();

    const comp = fixture.componentInstance;
    expect(comp.hasActiveCalibration()).toBe(true);
    expect(comp.pendingCount()).toBe(0);
    expect(comp.draftRubrics().length).toBe(1);
    expect(comp.draftGoldenSets().length).toBe(1);
    expect(comp.recentCalibrations().length).toBe(1);

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Evaluador calibrado y operativo');
    expect(text).toContain('cal-run-');
    expect(text).toContain('Rúbrica de Tutoría');
    expect(text).toContain('Golden Set v1');
    expect(text).toContain('Todo en orden');
  });

  it('renders actionable alerts when calibration is missing, evaluations are pending, and base proposal exists (GS-0511)', async () => {
    await TestBed.configureTestingModule({
      imports: [SummaryPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    }).compileComponents();

    const fixture = TestBed.createComponent(SummaryPage);
    fixture.componentRef.setInput('courseId', COURSE_ID);
    fixture.detectChanges();

    const http = TestBed.inject(HttpTestingController);

    http.expectOne(`/api/llm/courses/${COURSE_ID}/active-calibration`).flush(null);
    http.expectOne(`/api/llm/courses/${COURSE_ID}/pending-evaluations`).flush({
      items: [{ id: 'p-1', attemptId: 'att-1' }, { id: 'p-2', attemptId: 'att-2' }],
    });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/base-update-proposals`).flush({
      items: [{ id: 'prop-1', baseVersion: 2 }],
    });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/rubrics`).flush({ items: [] });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/golden-sets`).flush({ items: [] });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/calibrations`).flush({
      items: [
        { id: 'cal-failed-01', state: 'FAILED', progress: 100, createdAt: '2026-09-07T10:00:00Z' },
      ],
    });

    await fixture.whenStable();
    fixture.detectChanges();

    const comp = fixture.componentInstance;
    expect(comp.hasActiveCalibration()).toBe(false);
    expect(comp.pendingCount()).toBe(2);
    expect(comp.hasBaseProposal()).toBe(true);
    expect(comp.hasRecentFailedCalibration()).toBe(true);

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Servicio suspendido');
    expect(text).toContain('Sin calibración activa para el curso');
    expect(text).toContain('2 evaluación(es) en cola');
    expect(text).toContain('Nueva versión de Golden Set base disponible');
    expect(text).toContain('Última calibración rechazada');
  });
});
