import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ChallengeRubricOverlayPage } from './challenge-rubric-overlay-page';
import { challengeUuidV5 } from '../challenge-catalog/challenge-catalog';

const courseId = '22222222-2222-2222-2222-222222222222';
const baseline = { id: 'baseline-1', version: 2, name: 'Rubrica base', state: 'PUBLISHED' };

describe('ChallengeRubricOverlayPage', () => {
  async function createPage() {
    await TestBed.configureTestingModule({ imports: [ChallengeRubricOverlayPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(ChallengeRubricOverlayPage);
    fixture.componentRef.setInput('courseId', courseId);
    fixture.detectChanges();
    return fixture;
  }

  it('shows the challenge selector before choosing one', async () => {
    const fixture = await createPage();
    expect(fixture.nativeElement.textContent).toContain('Elegí un desafío');
    expect(fixture.nativeElement.querySelector('app-challenge-selector')).toBeTruthy();
  });

  it('selects a challenge and loads its overlay versions', async () => {
    const fixture = await createPage();
    const http = TestBed.inject(HttpTestingController);
    const uuid = challengeUuidV5('be-01');
    fixture.componentInstance.onSelect({ id: 'be-01', type: 'BE', name: '01-hello-java', title: 'Hello Java', tags: ['java'] });
    fixture.detectChanges();
    http.expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [baseline] });
    http.expectOne(`/api/llm/courses/${courseId}/challenges/${uuid}/rubric-overlay`).flush({ items: [] });
    await fixture.whenStable(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Aún no hay overlay para este desafío');
  });

  it('lists existing overlay versions with actions', async () => {
    const fixture = await createPage();
    const http = TestBed.inject(HttpTestingController);
    const uuid = challengeUuidV5('fe-01');
    fixture.componentInstance.onSelect({ id: 'fe-01', type: 'FE', name: 'exercise-adding-even-numbers', title: 'Adding Even Numbers', tags: ['math'] });
    fixture.detectChanges();
    http.expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [baseline] });
    http.expectOne(`/api/llm/courses/${courseId}/challenges/${uuid}/rubric-overlay`)
      .flush({ items: [{ id: 'overlay-1', familyId: 'f', version: 1, name: 'Overlay A', state: 'PUBLISHED', revision: 2, rubricKind: 'MODULAR_CUSTOM', userPrompt: '', customDimensions: [] }] });
    await fixture.whenStable(); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Overlay A');
    expect(fixture.nativeElement.textContent).toContain('Nueva versión');
  });

  it('creates a draft overlay from a published baseline', async () => {
    const fixture = await createPage();
    const http = TestBed.inject(HttpTestingController);
    const uuid = challengeUuidV5('be-02');
    fixture.componentInstance.onSelect({ id: 'be-02', type: 'BE', name: '02-java-stdin-stdout', title: 'Java Stdin Stdout', tags: ['io'] });
    fixture.detectChanges();
    http.expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [baseline] });
    http.expectOne(`/api/llm/courses/${courseId}/challenges/${uuid}/rubric-overlay`).flush({ items: [] });
    await fixture.whenStable(); fixture.detectChanges();
    fixture.componentInstance.newOverlayName.set('Overlay Nuevo');
    fixture.componentInstance.baselineVersionId.set('baseline-1');
    fixture.componentInstance.createDraft();
    const request = http.expectOne(`/api/llm/courses/${courseId}/challenges/${uuid}/rubric-overlay`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body.name).toBe('Overlay Nuevo');
    expect(request.request.body.baselineVersionId).toBe('baseline-1');
    request.flush({ id: 'overlay-2', familyId: 'f', version: 1, name: 'Overlay Nuevo', state: 'DRAFT', revision: 1, rubricKind: 'MODULAR_CUSTOM', userPrompt: '', customDimensions: [], baselineVersionId: 'baseline-1' });
    fixture.detectChanges();
    const reload = http.expectOne(`/api/llm/courses/${courseId}/challenges/${uuid}/rubric-overlay`);
    reload.flush({ items: [{ id: 'overlay-2', familyId: 'f', version: 1, name: 'Overlay Nuevo', state: 'DRAFT', revision: 1, rubricKind: 'MODULAR_CUSTOM', userPrompt: '', customDimensions: [] }] });
  });

  it('saves overlay dimensions through the editor', async () => {
    const fixture = await createPage();
    const http = TestBed.inject(HttpTestingController);
    const uuid = challengeUuidV5('fe-02');
    const version = { id: 'overlay-1', familyId: 'f', version: 1, name: 'Overlay A', state: 'DRAFT', revision: 2, rubricKind: 'MODULAR_CUSTOM', userPrompt: 'Guia', customDimensions: [], baselineVersionId: 'baseline-1' };
    fixture.componentInstance.onSelect({ id: 'fe-02', type: 'FE', name: 'exercise-anagram', title: 'Anagram', tags: ['strings'] });
    fixture.detectChanges();
    http.expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [baseline] });
    http.expectOne(`/api/llm/courses/${courseId}/challenges/${uuid}/rubric-overlay`).flush({ items: [version] });
    await fixture.whenStable(); fixture.detectChanges();
    fixture.componentInstance.edit(version);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-rubric-dimensions-editor')).toBeTruthy();
    fixture.componentInstance.save(version, [{ key: 'a', label: 'A', criterion: 'c', anchors: { low: { behavior: 'b', referenceScore: 25, example: 'e' }, medium: { behavior: 'm', referenceScore: 60, example: 'e' }, high: { behavior: 'h', referenceScore: 90, example: 'e' } }, weight: 100 }]);
    const request = http.expectOne(`/api/llm/courses/${courseId}/challenges/${uuid}/rubric-overlay/overlay-1`);
    expect(request.request.method).toBe('PATCH');
    expect(request.request.headers.get('If-Match')).toBe('2');
    request.flush({ ...version, revision: 3, customDimensions: [{ key: 'a', label: 'A', criterion: 'c', anchors: { low: { behavior: 'b', referenceScore: 25, example: 'e' }, medium: { behavior: 'm', referenceScore: 60, example: 'e' }, high: { behavior: 'h', referenceScore: 90, example: 'e' } }, weight: 100 }] });
    fixture.detectChanges();
    const reload = http.expectOne(`/api/llm/courses/${courseId}/challenges/${uuid}/rubric-overlay`);
    reload.flush({ items: [{ ...version, revision: 3 }] });
  });

  it('loads the preset and saves the draft from the editor UI', async () => {
    const fixture = await createPage();
    const http = TestBed.inject(HttpTestingController);
    const uuid = challengeUuidV5('be-03');
    fixture.componentInstance.onSelect({ id: 'be-03', type: 'BE', name: '03-stdin-stdout-boolean', title: 'Stdin Stdout Boolean', tags: ['io'] });
    fixture.detectChanges();
    http.expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [baseline] });
    http.expectOne(`/api/llm/courses/${courseId}/challenges/${uuid}/rubric-overlay`).flush({ items: [] });
    await fixture.whenStable(); fixture.detectChanges();

    fixture.componentInstance.newOverlayName.set('Overlay UI');
    fixture.componentInstance.baselineVersionId.set('baseline-1');
    fixture.componentInstance.createDraft();
    const post = http.expectOne(`/api/llm/courses/${courseId}/challenges/${uuid}/rubric-overlay`);
    expect(post.request.method).toBe('POST');
    post.flush({ id: 'overlay-ui', familyId: 'f', version: 1, name: 'Overlay UI', state: 'DRAFT', revision: 1, rubricKind: 'MODULAR_CUSTOM', userPrompt: '', customDimensions: [], baselineVersionId: 'baseline-1' });
    fixture.detectChanges();
    const reload = http.expectOne(`/api/llm/courses/${courseId}/challenges/${uuid}/rubric-overlay`);
    reload.flush({ items: [{ id: 'overlay-ui', familyId: 'f', version: 1, name: 'Overlay UI', state: 'DRAFT', revision: 1, rubricKind: 'MODULAR_CUSTOM', userPrompt: '', customDimensions: [] }] });
    await fixture.whenStable(); fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('app-rubric-dimensions-editor')).toBeTruthy();
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    const preset = buttons.find((button) => button.textContent?.includes('Cargar 10 dimensiones'));
    expect(preset).toBeTruthy();
    preset!.click();
    fixture.detectChanges();

    const save = buttons.find((button) => button.textContent?.trim() === 'Guardar borrador')!;
    expect(save.disabled).toBe(false);
    save.click();
    fixture.detectChanges();

    const patch = http.expectOne(`/api/llm/courses/${courseId}/challenges/${uuid}/rubric-overlay/overlay-ui`);
    expect(patch.request.method).toBe('PATCH');
    expect(patch.request.body.customDimensions).toHaveLength(10);
    patch.flush({ id: 'overlay-ui', familyId: 'f', version: 1, name: 'Overlay UI', state: 'DRAFT', revision: 2, rubricKind: 'MODULAR_CUSTOM', userPrompt: '', customDimensions: patch.request.body.customDimensions });
    fixture.detectChanges();
    const reload2 = http.expectOne(`/api/llm/courses/${courseId}/challenges/${uuid}/rubric-overlay`);
    reload2.flush({ items: [{ id: 'overlay-ui', familyId: 'f', version: 1, name: 'Overlay UI', state: 'DRAFT', revision: 2, rubricKind: 'MODULAR_CUSTOM', userPrompt: '', customDimensions: patch.request.body.customDimensions }] });
  });
});