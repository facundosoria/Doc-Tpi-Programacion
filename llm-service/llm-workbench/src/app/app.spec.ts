import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [App], providers: [provideRouter([])] }).compileComponents();
  });

  it('renders the V2 evaluator identity and a router outlet', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const page = fixture.nativeElement as HTMLElement;

    expect(page.querySelector('.brand')?.textContent).toContain('TUP');
    expect(page.querySelector('.brand')?.textContent).toContain('Golde-Set');
    expect(page.textContent).toContain('Panel docente');
    expect(page.querySelector('router-outlet')).not.toBeNull();
  });
});
