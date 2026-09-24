import { ChangeDetectionStrategy, Component, output, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ChallengeSelector } from './challenge-selector';
import { CHALLENGES } from '../challenge-catalog/challenge-catalog';

@Component({ selector: 'app-host', imports: [ChallengeSelector], changeDetection: ChangeDetectionStrategy.OnPush, template: '<app-challenge-selector (selected)="chosen.set($event)" />' })
class HostComponent { readonly chosen = signal<unknown>(null); }

describe('ChallengeSelector', () => {
  async function create() {
    await TestBed.configureTestingModule({ imports: [HostComponent] }).compileComponents();
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('shows the full catalog when the query is empty', async () => {
    const fixture = await create();
    const items = fixture.nativeElement.querySelectorAll('li[role="option"]');
    expect(items.length).toBe(CHALLENGES.length);
  });

  it('filters the list as the user types', async () => {
    const fixture = await create();
    const input = (fixture.nativeElement as HTMLElement).querySelector('input[type="text"]') as HTMLInputElement;
    input.value = 'hello';
    input.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('li[role="option"]');
    expect(items.length).toBeGreaterThan(0);
    expect(items.length).toBeLessThan(CHALLENGES.length);
  });

  it('selects the highlighted option on Enter', async () => {
    const fixture = await create();
    const input = (fixture.nativeElement as HTMLElement).querySelector('input[type="text"]') as HTMLInputElement;
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.chosen()).toEqual(CHALLENGES[0]);
  });

  it('moves the active index with ArrowDown and selects on Enter', async () => {
    const fixture = await create();
    const input = (fixture.nativeElement as HTMLElement).querySelector('input[type="text"]') as HTMLInputElement;
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'ArrowDown', bubbles: true }));
    input.dispatchEvent(new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.chosen()).toEqual(CHALLENGES[1]);
  });

  it('emits the selected exercise on mouse pick', async () => {
    const fixture = await create();
    const first = fixture.debugElement.queryAll(By.css('li[role="option"]'))[0];
    first.nativeElement.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    fixture.detectChanges();
    expect(fixture.componentInstance.chosen()).toEqual(CHALLENGES[0]);
  });
});