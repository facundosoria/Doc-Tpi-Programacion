import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { GoldenSetApiService } from './golden-set-api.service';

describe('GoldenSetApiService', () => {
  let api: GoldenSetApiService;
  let http: HttpTestingController;
  const baseUrl = 'http://localhost:8080/api/llm/golden-sets';

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    api = TestBed.inject(GoldenSetApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('lists and loads golden sets from the S1 API', () => {
    let listed: unknown; let loaded: unknown;
    api.list().subscribe(value => listed = value);
    api.get('set-1').subscribe(value => loaded = value);
    http.expectOne(baseUrl).flush([{ id: 'set-1', version: 1 }]);
    http.expectOne(`${baseUrl}/set-1`).flush({ id: 'set-1', version: 1, entries: [] });

    expect(listed).toEqual([{ id: 'set-1', version: 1 }]);
    expect(loaded).toMatchObject({ id: 'set-1', entries: [] });
  });

  it('creates a fixed-rubric golden set with an idempotency key', () => {
    vi.spyOn(crypto, 'randomUUID').mockReturnValue('00000000-0000-4000-8000-000000000001');
    api.create().subscribe();
    const request = http.expectOne(baseUrl);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ rubricVersion: '1.0', language: 'es' });
    expect(request.request.headers.get('Idempotency-Key')).toBe('00000000-0000-4000-8000-000000000001');
    request.flush({ id: 'set-1', version: 1 });
  });

  it('sends transcript and reference scores when adding an entry', () => {
    vi.spyOn(crypto, 'randomUUID').mockReturnValue('00000000-0000-4000-8000-000000000002');
    const transcript = [{ role: 'learner', text: 'hola' }];
    const scores = { autonomy: 80 };
    api.addEntry('set-1', transcript, scores).subscribe();
    const request = http.expectOne(`${baseUrl}/set-1/entries`);

    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ transcript, referenceScores: scores });
    expect(request.request.headers.get('Idempotency-Key')).toBe('00000000-0000-4000-8000-000000000002');
    request.flush({ id: 'entry-1' });
  });
});
