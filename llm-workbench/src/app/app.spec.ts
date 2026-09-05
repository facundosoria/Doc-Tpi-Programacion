import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [App], providers: [provideRouter([])] }).compileComponents();
  });

  it('renders the S1 workbench identity and a router outlet', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const page = fixture.nativeElement as HTMLElement;

    expect(page.querySelector('.brand')?.textContent).toContain('LLM WORKBENCH');
    expect(page.textContent).toContain('Golden sets · S1');
    expect(page.querySelector('router-outlet')).not.toBeNull();
  });
});
