import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { RubricsPage } from './rubrics-page';

const courseId = '00000000-0000-0000-0000-000000000010';
const dimensions = ['AUTONOMY', 'CLARITY', 'PROGRESSION', 'COMPLIANCE', 'EFFICIENCY'].map(key => ({ key, label: key, criterion: `Criterio ${key}`, anchors: '{"low":"bajo","medium":"medio","high":"alto"}', evaluatorPrompt: `Evalúa ${key}`, weight: 20 }));

describe('RubricsPage', () => {
  async function createPage() {
    await TestBed.configureTestingModule({ imports: [RubricsPage], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(RubricsPage); fixture.componentRef.setInput('courseId', courseId); fixture.detectChanges();
    return fixture;
  }

  it('renders course-scoped versions with their publication state', async () => {
    const fixture = await createPage();
    TestBed.inject(HttpTestingController).expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [{ id: 'rubric-1', familyId: 'family-1', name: 'Uso responsable', version: 2, state: 'PUBLISHED', revision: 3, dimensions }] });
    await fixture.whenStable(); fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    expect(page.textContent).toContain('Uso responsable'); expect(page.textContent).toContain('v2'); expect(page.textContent).toContain('Publicada'); expect(page.textContent).toContain('Inmutable');
  });

  it('edits a draft and sends its revision in If-Match', async () => {
    const fixture = await createPage(); const http = TestBed.inject(HttpTestingController);
    http.expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [{ id: 'rubric-1', familyId: 'family-1', name: 'Uso responsable', version: 1, state: 'DRAFT', revision: 3, dimensions }] });
    await fixture.whenStable(); fixture.detectChanges();
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.edit')!.click(); fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Dimensión obligatoria: AUTONOMY');
    fixture.componentInstance.form.controls.name.setValue('Uso responsable actualizado'); fixture.componentInstance.save();
    const request = http.expectOne(`/api/llm/courses/${courseId}/rubrics/rubric-1`);
    expect(request.request.method).toBe('PATCH'); expect(request.request.headers.get('If-Match')).toBe('3'); expect(request.request.body.name).toBe('Uso responsable actualizado');
    request.flush({ id: 'rubric-1', familyId: 'family-1', name: 'Uso responsable actualizado', version: 1, state: 'DRAFT', revision: 4, dimensions });
    fixture.detectChanges(); expect((fixture.nativeElement as HTMLElement).textContent).toContain('Borrador guardado. Revisión 4.');
  });

  it('rejects invalid anchors and weights that do not total 100%', async () => {
    const fixture = await createPage(); const http = TestBed.inject(HttpTestingController);
    http.expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [{ id: 'rubric-1', familyId: 'family-1', name: 'Uso responsable', version: 1, state: 'DRAFT', revision: 3, dimensions }] });
    await fixture.whenStable(); fixture.detectChanges(); fixture.componentInstance.editDraft({ id: 'rubric-1', familyId: 'family-1', name: 'Uso responsable', version: 1, state: 'DRAFT', revision: 3, dimensions });
    fixture.componentInstance.dimensions.at(0).controls.weight.setValue(19); fixture.componentInstance.dimensions.at(1).controls.anchors.setValue('{}'); fixture.componentInstance.save(); fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Los cinco pesos deben sumar exactamente 100 %.'); expect((fixture.nativeElement as HTMLElement).textContent).toContain('Las anclas deben ser un JSON con low, medium y high.');
    http.expectNone(`/api/llm/courses/${courseId}/rubrics/rubric-1`);
  });

  it('publishes a draft with comparison confirmation and reloads versions', async () => {
    const fixture = await createPage(); const http = TestBed.inject(HttpTestingController);
    http.expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [{ id: 'rubric-1', familyId: 'family-1', name: 'Uso pedagógico', version: 1, state: 'DRAFT', revision: 2, dimensions }] });
    await fixture.whenStable(); fixture.detectChanges();

    const page = fixture.nativeElement as HTMLElement;
    const publishBtn = Array.from(page.querySelectorAll<HTMLButtonElement>('button')).find(b => b.textContent?.includes('Publicar'))!;
    publishBtn.click(); fixture.detectChanges();

    expect(page.textContent).toContain('CONFIRMACIÓN DE PUBLICACIÓN');
    expect(page.textContent).toContain('Inmutabilidad garantizada');

    const confirmBtn = Array.from(page.querySelectorAll<HTMLButtonElement>('button')).find(b => b.textContent?.includes('Confirmar publicación inmutable'))!;
    confirmBtn.click(); fixture.detectChanges();

    const publishReq = http.expectOne(`/api/llm/courses/${courseId}/rubrics/rubric-1/publish`);
    expect(publishReq.request.method).toBe('POST');
    publishReq.flush({});
    fixture.detectChanges();
    http.expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [{ id: 'rubric-1', familyId: 'family-1', name: 'Uso pedagógico', version: 1, state: 'PUBLISHED', revision: 2, dimensions }] });
    fixture.detectChanges();

    expect(page.textContent).toContain('Rúbrica v1 publicada exitosamente (inmutable).');
  });

  it('creates next version from a published rubric and reloads versions', async () => {
    const fixture = await createPage(); const http = TestBed.inject(HttpTestingController);
    http.expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [{ id: 'rubric-1', familyId: 'family-1', name: 'Uso pedagógico', version: 1, state: 'PUBLISHED', revision: 2, dimensions }] });
    await fixture.whenStable(); fixture.detectChanges();

    const page = fixture.nativeElement as HTMLElement;
    const nextBtn = Array.from(page.querySelectorAll<HTMLButtonElement>('button')).find(b => b.textContent?.includes('Nueva versión'))!;
    nextBtn.click(); fixture.detectChanges();

    const nextReq = http.expectOne(`/api/llm/courses/${courseId}/rubrics/rubric-1/next-version`);
    expect(nextReq.request.method).toBe('POST');
    nextReq.flush({ id: 'rubric-2', familyId: 'family-1', name: 'Uso pedagógico', version: 2, state: 'DRAFT', revision: 0, dimensions });
    fixture.detectChanges();
    http.expectOne(`/api/llm/courses/${courseId}/rubrics`).flush({ items: [
      { id: 'rubric-1', familyId: 'family-1', name: 'Uso pedagógico', version: 1, state: 'PUBLISHED', revision: 2, dimensions },
      { id: 'rubric-2', familyId: 'family-1', name: 'Uso pedagógico', version: 2, state: 'DRAFT', revision: 0, dimensions }
    ] });
    fixture.detectChanges();

    expect(page.textContent).toContain('Nueva versión borrador (v2) creada exitosamente.');
  });
});
