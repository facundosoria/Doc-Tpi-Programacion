import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { RubricDimensionsEditor, EditorDimension } from './rubric-dimensions-editor';

function dimension(overrides: Partial<EditorDimension> = {}): EditorDimension {
  return {
    key: 'algoritmos', label: 'Algoritmos', criterion: 'Evalúa el diseño',
    anchors: {
      low: { behavior: 'b', referenceScore: 25, example: 'e' },
      medium: { behavior: 'm', referenceScore: 60, example: 'e' },
      high: { behavior: 'h', referenceScore: 90, example: 'e' },
    },
    weight: 100,
    ...overrides,
  };
}

@Component({ selector: 'app-host', imports: [RubricDimensionsEditor], changeDetection: ChangeDetectionStrategy.OnPush, template: '<app-rubric-dimensions-editor [dimensions]="dims()" (save)="saved.set($event)" (cancel)="cancelled.set(true)" />' })
class HostComponent {
  readonly dims = signal<EditorDimension[]>([dimension()]);
  readonly saved = signal<EditorDimension[] | null>(null);
  readonly cancelled = signal(false);
}

describe('RubricDimensionsEditor', () => {
  async function create() {
    await TestBed.configureTestingModule({ imports: [HostComponent] }).compileComponents();
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('renders the dimensions and the total weight', async () => {
    const fixture = await create();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Algoritmos');
    expect(text).toContain('100 %');
  });

  it('adds a new dimension locally without emitting save', async () => {
    const fixture = await create();
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    const add = buttons.find((button) => button.textContent === 'Agregar dimensión')!;
    add.click();
    fixture.detectChanges();
    expect(fixture.componentInstance.saved()).toBeNull();
    expect(fixture.nativeElement.querySelectorAll('fieldset.dimension-panel').length).toBe(2);
  });

  it('removes a dimension locally without emitting save', async () => {
    const fixture = await create();
    const remove = fixture.nativeElement.querySelector('button[aria-label="Quitar dimensión"]') as HTMLButtonElement;
    remove.click();
    fixture.detectChanges();
    expect(fixture.componentInstance.saved()).toBeNull();
    expect(fixture.nativeElement.querySelectorAll('fieldset.dimension-panel').length).toBe(0);
  });

  it('emits the save event with the current dimensions', async () => {
    const fixture = await create();
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    const save = buttons.find((button) => button.textContent === 'Guardar borrador')!;
    expect((save as HTMLButtonElement).disabled).toBe(false);
    save.click();
    fixture.detectChanges();
    expect(fixture.componentInstance.saved()!.length).toBe(1);
  });

  it('disables save while a dimension has an empty criterion', async () => {
    const fixture = await create();
    fixture.componentInstance.dims.set([dimension({ criterion: '' })]);
    fixture.detectChanges();
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    const save = buttons.find((button) => button.textContent === 'Guardar borrador')!;
    expect((save as HTMLButtonElement).disabled).toBe(true);
  });

  it('emits the cancel event', async () => {
    const fixture = await create();
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    const cancel = buttons.find((button) => button.textContent === 'Cerrar editor')!;
    cancel.click();
    fixture.detectChanges();
    expect(fixture.componentInstance.cancelled()).toBe(true);
  });

  it('flags weights that do not total 100', async () => {
    const fixture = await create();
    fixture.componentInstance.dims.set([dimension({ weight: 60 }), dimension({ key: 'pruebas', weight: 30 })]);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Los pesos deben sumar 100 %.');
  });

  it('disables save while an anchor is empty', async () => {
    const fixture = await create();
    const incomplete = dimension();
    incomplete.anchors.low.behavior = '';
    fixture.componentInstance.dims.set([incomplete]);
    fixture.detectChanges();
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    const save = buttons.find((button) => button.textContent === 'Guardar borrador')!;
    expect((save as HTMLButtonElement).disabled).toBe(true);
  });

  it('loads the 10-dimension preset collapsed', async () => {
    const fixture = await create();
    const buttons = Array.from(fixture.nativeElement.querySelectorAll('button')) as HTMLButtonElement[];
    const preset = buttons.find((button) => button.textContent === 'Cargar 10 dimensiones de ejemplo')!;
    preset.click();
    fixture.detectChanges();
    const panels = fixture.nativeElement.querySelectorAll('fieldset.dimension-panel');
    expect(panels.length).toBe(10);
    expect(fixture.nativeElement.textContent).toContain('Autonomía');
    expect(fixture.nativeElement.textContent).toContain('Justificación');
    expect(fixture.nativeElement.querySelectorAll('fieldset.dimension-panel.collapsed').length).toBe(10);
    expect(fixture.nativeElement.querySelector('.anchors-editor')).toBeNull();
    const save = buttons.find((button) => button.textContent === 'Guardar borrador')!;
    expect((save as HTMLButtonElement).disabled).toBe(false);
  });

  it('collapses and expands a dimension to hide the anchors', async () => {
    const fixture = await create();
    expect(fixture.nativeElement.querySelector('.anchors-editor')).not.toBeNull();
    const toggle = fixture.nativeElement.querySelector('button.collapse-toggle') as HTMLButtonElement;
    toggle.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.anchors-editor')).toBeNull();
    toggle.click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.anchors-editor')).not.toBeNull();
  });
});