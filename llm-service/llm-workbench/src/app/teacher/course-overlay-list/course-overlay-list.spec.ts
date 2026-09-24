import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CourseOverlayList } from './course-overlay-list';
import { challengeUuidV5 } from '../challenge-catalog/challenge-catalog';

const courseId = '22222222-2222-2222-2222-222222222222';

function item(overrides: Partial<{ challengeId: string; id: string; version: number; name: string; state: string; revision: number }> = {}) {
  return {
    challengeId: challengeUuidV5('be-01'),
    id: 'overlay-1',
    version: 1,
    name: 'Overlay Hello',
    state: 'DRAFT',
    revision: 1,
    ...overrides,
  };
}

describe('CourseOverlayList', () => {
  async function createPage(items: unknown[]) {
    await TestBed.configureTestingModule({ imports: [CourseOverlayList], providers: [provideHttpClient(), provideHttpClientTesting()] }).compileComponents();
    const fixture = TestBed.createComponent(CourseOverlayList);
    fixture.componentRef.setInput('courseId', courseId);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`/api/llm/courses/${courseId}/rubric-overlays`).flush({ items });
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  it('renders the overlays with challenge names resolved from the catalog', async () => {
    const fixture = await createPage([item()]);
    expect(fixture.nativeElement.textContent).toContain('Hello Java');
    expect(fixture.nativeElement.textContent).toContain('Overlay Hello');
    expect(fixture.nativeElement.textContent).toContain('Borrador');
    expect(fixture.nativeElement.querySelector('.state.draft')).not.toBeNull();
  });

  it('shows the raw challenge id when it does not match the catalog', async () => {
    const fixture = await createPage([item({ challengeId: '77777777-7777-7777-7777-777777777777' })]);
    expect(fixture.nativeElement.textContent).toContain('77777777…');
  });

  it('paginates showing at most five per page', async () => {
    const items = Array.from({ length: 12 }, (_, index) => item({ id: `ov-${index}`, name: `Overlay ${index}`, version: index + 1 }));
    const fixture = await createPage(items);
    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(5);
    expect(fixture.nativeElement.textContent).toContain('Página 1 de 3');
  });

  it('filters by text', async () => {
    const items = [item(), item({ id: 'ov-2', name: 'Overlay Mundo' })];
    const fixture = await createPage(items);
    const input = fixture.nativeElement.querySelector('input[type="text"]') as HTMLInputElement;
    input.value = 'mundo';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Overlay Mundo');
  });

  it('filters by state', async () => {
    const items = [item({ state: 'DRAFT' }), item({ id: 'ov-2', name: 'Overlay Pub', state: 'PUBLISHED' })];
    const fixture = await createPage(items);
    const select = fixture.nativeElement.querySelector('select') as HTMLSelectElement;
    select.value = 'PUBLISHED';
    select.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(1);
    expect(fixture.nativeElement.textContent).toContain('Publicada');
    expect(fixture.nativeElement.querySelector('.state.published')).not.toBeNull();
  });
});