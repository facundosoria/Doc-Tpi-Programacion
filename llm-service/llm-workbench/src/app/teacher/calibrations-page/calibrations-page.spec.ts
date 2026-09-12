import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { beforeEach, describe, expect, it } from 'vitest';
import { CalibrationsPage } from './calibrations-page';

describe('CalibrationsPage', () => {
  const COURSE_ID = '00000000-0000-0000-0000-000000000010';

  beforeEach(() => {
    TestBed.resetTestingModule();
  });

  it('loads published rubrics, golden sets, models and displays active calibration status', async () => {
    await TestBed.configureTestingModule({
      imports: [CalibrationsPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    const fixture = TestBed.createComponent(CalibrationsPage);
    fixture.componentRef.setInput('courseId', COURSE_ID);
    fixture.detectChanges();

    const http = TestBed.inject(HttpTestingController);

    http.expectOne(`/api/llm/courses/${COURSE_ID}/calibrations`).flush({ items: [] });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/rubrics`).flush({
      items: [
        { id: 'rub-1', name: 'Rúbrica estándar', version: 1, state: 'PUBLISHED' },
        { id: 'rub-draft', name: 'Rúbrica borrador', version: 2, state: 'DRAFT' },
      ],
    });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/golden-sets`).flush({
      items: [
        { id: 'gs-1', name: 'Golden Set inicial', version: 1, state: 'PUBLISHED', cases: [{}, {}] },
        { id: 'gs-draft', name: 'Golden Set borrador', version: 2, state: 'DRAFT', cases: [] },
      ],
    });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/model-deployments`).flush({
      items: [
        { id: 'mod-1', provider: 'openai', modelId: 'gpt-4o-mini', modelVersion: '2024-07-18', state: 'ENABLED' },
      ],
    });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/active-calibration`).flush({
      courseId: COURSE_ID,
      calibrationRunId: 'run-active-01',
      activatedAt: '2026-09-01T10:00:00Z',
    });

    await fixture.whenStable();
    fixture.detectChanges();

    const comp = fixture.componentInstance;
    // Only published items are offered for calibration
    expect(comp.publishedRubrics().length).toBe(1);
    expect(comp.publishedGoldenSets().length).toBe(1);
    expect(comp.modelDeployments().length).toBe(1);
    expect(comp.activeCalibration.value()?.calibrationRunId).toBe('run-active-01');

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Calibración activa del curso');
    expect(text).toContain('run-acti');
  });

  it('displays summary preview (GS-0541) and enqueues calibration run (GS-0540)', async () => {
    await TestBed.configureTestingModule({
      imports: [CalibrationsPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    const fixture = TestBed.createComponent(CalibrationsPage);
    fixture.componentRef.setInput('courseId', COURSE_ID);
    fixture.detectChanges();

    const http = TestBed.inject(HttpTestingController);

    http.expectOne(`/api/llm/courses/${COURSE_ID}/calibrations`).flush({ items: [] });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/rubrics`).flush({
      items: [{ id: 'rub-1', name: 'Rúbrica estándar', version: 1, state: 'PUBLISHED' }],
    });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/golden-sets`).flush({
      items: [{ id: 'gs-1', name: 'Golden Set inicial', version: 1, state: 'PUBLISHED', cases: [{}, {}, {}] }],
    });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/model-deployments`).flush({
      items: [{ id: 'mod-1', provider: 'openai', modelId: 'gpt-4o-mini', modelVersion: '2024-07-18', state: 'ENABLED' }],
    });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/active-calibration`).flush(null);

    await fixture.whenStable();
    fixture.detectChanges();

    const comp = fixture.componentInstance;
    comp.runForm.setValue({
      rubricVersionId: 'rub-1',
      goldenSetVersionId: 'gs-1',
      modelDeploymentId: 'mod-1',
    });
    fixture.detectChanges();

    // GS-0541: Resumen previo visible
    expect(comp.selectedRubric()?.name).toBe('Rúbrica estándar');
    expect(comp.selectedGoldenSet()?.cases.length).toBe(3);
    expect(comp.selectedModel()?.modelId).toBe('gpt-4o-mini');

    const previewText = fixture.nativeElement.querySelector('.preview-box').textContent;
    expect(previewText).toContain('Rúbrica estándar (versión inmutable v1)');
    expect(previewText).toContain('3 casos de referencia humana');
    expect(previewText).toContain('openai / gpt-4o-mini');
    expect(previewText).toContain('Criterio PAR-14');

    // GS-0540: Start calibration
    comp.startCalibration();
    const createReq = http.expectOne(`/api/llm/courses/${COURSE_ID}/calibrations`);
    expect(createReq.request.method).toBe('POST');
    expect(createReq.request.body).toEqual({
      rubricVersionId: 'rub-1',
      goldenSetVersionId: 'gs-1',
      modelDeploymentId: 'mod-1',
    });
    createReq.flush({
      id: 'run-new-1',
      state: 'QUEUED',
      progress: 0,
      rubricVersionId: 'rub-1',
      goldenSetVersionId: 'gs-1',
      modelDeploymentId: 'mod-1',
      reason: 'MANUAL',
      createdAt: '2026-09-07T10:00:00Z',
    });
    fixture.detectChanges();

    http.expectOne(`/api/llm/courses/${COURSE_ID}/calibrations`).flush({
      items: [{
        id: 'run-new-1',
        state: 'QUEUED',
        progress: 0,
        rubricVersionId: 'rub-1',
        goldenSetVersionId: 'gs-1',
        modelDeploymentId: 'mod-1',
        reason: 'MANUAL',
        createdAt: '2026-09-07T10:00:00Z',
      }],
    });
    fixture.detectChanges();

    expect(comp.successBanner()).toContain('Corrida de calibración encolada exitosamente');
  });

  it('displays async progress for running calibrations and shows PAR-14 metrics (GS-0542, GS-0543)', async () => {
    await TestBed.configureTestingModule({
      imports: [CalibrationsPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    const fixture = TestBed.createComponent(CalibrationsPage);
    fixture.componentRef.setInput('courseId', COURSE_ID);
    fixture.detectChanges();

    const http = TestBed.inject(HttpTestingController);

    http.expectOne(`/api/llm/courses/${COURSE_ID}/calibrations`).flush({
      items: [
        {
          id: 'run-running-1',
          state: 'RUNNING',
          progress: 45,
          rubricVersionId: 'rub-1',
          goldenSetVersionId: 'gs-1',
          modelDeploymentId: 'mod-1',
          reason: 'MANUAL',
          createdAt: '2026-09-07T10:10:00Z',
        },
        {
          id: 'run-passed-1',
          state: 'PASSED',
          progress: 100,
          rubricVersionId: 'rub-1',
          goldenSetVersionId: 'gs-1',
          modelDeploymentId: 'mod-1',
          maeFinal: 3.25,
          maxIndividualError: 6,
          reason: 'MANUAL',
          createdAt: '2026-09-07T09:00:00Z',
        },
      ],
    });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/rubrics`).flush({ items: [] });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/golden-sets`).flush({ items: [] });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/model-deployments`).flush({ items: [] });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/active-calibration`).flush(null);

    await fixture.whenStable();
    fixture.detectChanges();

    const comp = fixture.componentInstance;
    // GS-0542: Running runs list with progress
    expect(comp.runningRuns().length).toBe(1);
    expect(comp.runningRuns()[0].progress).toBe(45);

    // GS-0543: PAR-14 Metrics display
    expect(comp.selectedCompletedRun()?.state).toBe('PASSED');
    expect(comp.selectedCompletedRun()?.maeFinal).toBe(3.25);
    expect(comp.selectedCompletedRun()?.maxIndividualError).toBe(6);

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('45% completado');
    expect(text).toContain('APROBADA (PAR-14)');
    expect(text).toContain('3.25');
  });

  it('handles activation preview and two-step confirmation with migrable challenges (GS-0544, GS-0545)', async () => {
    await TestBed.configureTestingModule({
      imports: [CalibrationsPage],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    const fixture = TestBed.createComponent(CalibrationsPage);
    fixture.componentRef.setInput('courseId', COURSE_ID);
    fixture.detectChanges();

    const http = TestBed.inject(HttpTestingController);

    http.expectOne(`/api/llm/courses/${COURSE_ID}/calibrations`).flush({
      items: [{
        id: 'run-passed-1',
        state: 'PASSED',
        progress: 100,
        rubricVersionId: 'rub-1',
        goldenSetVersionId: 'gs-1',
        modelDeploymentId: 'mod-1',
        maeFinal: 2.5,
        maxIndividualError: 5,
        reason: 'MANUAL',
        createdAt: '2026-09-07T09:00:00Z',
      }],
    });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/rubrics`).flush({ items: [] });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/golden-sets`).flush({ items: [] });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/model-deployments`).flush({ items: [] });
    http.expectOne(`/api/llm/courses/${COURSE_ID}/active-calibration`).flush(null);

    await fixture.whenStable();
    fixture.detectChanges();

    const comp = fixture.componentInstance;
    const run = comp.selectedCompletedRun()!;

    // Step 1: Open activation preview
    comp.startActivationPreview(run);
    const prevReq = http.expectOne(`/api/llm/courses/${COURSE_ID}/calibrations/run-passed-1/activate-preview`);
    expect(prevReq.request.method).toBe('POST');
    prevReq.flush({
      previewToken: 'tok-abc-123',
      migrableChallengeIds: ['00000000-0000-0000-0000-0000000000aa'],
      lockedChallengeIds: ['00000000-0000-0000-0000-0000000000bb'],
    });
    fixture.detectChanges();

    expect(comp.activationPreview()?.previewToken).toBe('tok-abc-123');
    expect(comp.selectedMigrableIds()).toContain('00000000-0000-0000-0000-0000000000aa');

    const modalText = fixture.nativeElement.querySelector('.activation-box').textContent;
    expect(modalText).toContain('1 desafío(s) bloqueado(s)');
    expect(modalText).toContain('Desafíos migrables');

    // Step 2: Confirm activation
    comp.confirmActivation(run, comp.activationPreview()!);
    const actReq = http.expectOne(`/api/llm/courses/${COURSE_ID}/calibrations/run-passed-1/activate`);
    expect(actReq.request.method).toBe('POST');
    expect(actReq.request.body).toEqual({
      previewToken: 'tok-abc-123',
      challengeIds: ['00000000-0000-0000-0000-0000000000aa'],
    });
    actReq.flush({
      courseId: COURSE_ID,
      calibrationRunId: 'run-passed-1',
      activatedAt: '2026-09-07T11:00:00Z',
    });
    fixture.detectChanges();

    http.expectOne(`/api/llm/courses/${COURSE_ID}/active-calibration`).flush({
      courseId: COURSE_ID,
      calibrationRunId: 'run-passed-1',
      activatedAt: '2026-09-07T11:00:00Z',
    });
    fixture.detectChanges();

    expect(comp.successBanner()).toContain('activada exitosamente en el curso');
    expect(comp.activationPreview()).toBeNull();
  });
});
