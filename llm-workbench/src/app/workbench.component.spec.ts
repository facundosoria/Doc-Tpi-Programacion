import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { GoldenSet, GoldenSetApiService } from './golden-set-api.service';
import { WorkbenchComponent } from './workbench.component';

describe('WorkbenchComponent', () => {
  const routeUrl = new BehaviorSubject([]);
  const route: any = { url: routeUrl, snapshot: { paramMap: { get: (_key: string): string | null => null }, routeConfig: { path: 'golden-sets' } } };
  const router = { navigate: vi.fn() };
  const api = { list: vi.fn(), get: vi.fn(), create: vi.fn(), addEntry: vi.fn() };
  let fixture: ComponentFixture<WorkbenchComponent>;
  let component: WorkbenchComponent;
  const set: GoldenSet = { id: 'set-1', version: 1, rubricVersion: '1.0', language: 'es', createdAt: '2026-01-01T00:00:00Z', entries: [] };

  beforeEach(async () => {
    route.snapshot.paramMap.get = (_key: string): string | null => null;
    route.snapshot.routeConfig.path = 'golden-sets';
    api.list.mockReturnValue(of([])); api.get.mockReturnValue(of(set)); api.create.mockReturnValue(of(set)); api.addEntry.mockReturnValue(of({ id: 'entry-1' }));
    await TestBed.configureTestingModule({ imports: [WorkbenchComponent], providers: [
      { provide: GoldenSetApiService, useValue: api }, { provide: ActivatedRoute, useValue: route }, { provide: Router, useValue: router },
    ] }).compileComponents();
    fixture = TestBed.createComponent(WorkbenchComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => { TestBed.resetTestingModule(); vi.clearAllMocks(); });

  it('loads and shows the golden-set list for the list route', () => {
    api.list.mockReturnValue(of([set]));
    component.loadRoute(); fixture.detectChanges();

    expect(component.mode()).toBe('list');
    expect(component.sets()).toEqual([set]);
    expect(fixture.nativeElement.textContent).toContain('Golden sets disponibles');
  });

  it('loads the selected golden set for a detail route', () => {
    route.snapshot.paramMap.get = (_key: string): string | null => 'set-1';
    component.loadRoute();

    expect(component.mode()).toBe('detail');
    expect(api.get).toHaveBeenCalledWith('set-1');
    expect(component.selected()).toEqual(set);
  });

  it('adds the provided sample transcript to the form', () => {
    component.loadSample();
    expect(component.entryForm.controls.transcript.value).toContain('lista vacía');
  });

  it('rejects a transcript that is not a non-empty JSON array before calling the API', () => {
    component.selected.set(set);
    component.entryForm.controls.transcript.setValue('{"role":"learner"}');
    component.addEntry();

    expect(component.error()).toContain('arreglo JSON');
    expect(api.addEntry).not.toHaveBeenCalled();
  });

  it('sends valid scores and refreshes the selected golden set after adding an entry', () => {
    component.selected.set(set);
    component.entryForm.controls.transcript.setValue('[{"role":"learner","text":"hola"}]');
    component.addEntry();

    expect(api.addEntry).toHaveBeenCalledWith('set-1', [{ role: 'learner', text: 'hola' }], expect.objectContaining({ autonomy: 80, efficiency: 85 }));
    expect(api.get).toHaveBeenCalledWith('set-1');
  });

  it('displays the backend problem detail when loading fails', () => {
    api.list.mockReturnValue(throwError(() => ({ error: { detail: 'Servicio no disponible' } })));
    component.loadList();

    expect(component.loading()).toBe(false);
    expect(component.error()).toBe('Servicio no disponible');
  });

  it('navigates to the new golden set after creation', () => {
    component.createSet();
    expect(router.navigate).toHaveBeenCalledWith(['/golden-sets', 'set-1']);
  });
});
