import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface GoldenSetEntry { id: string; transcript: unknown[]; referenceScores: Record<string, number>; createdAt: string; }
export interface GoldenSet { id: string; version: number; rubricVersion: string; language: string; createdAt: string; entries?: GoldenSetEntry[]; }

@Injectable({ providedIn: 'root' })
export class GoldenSetApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/llm/golden-sets';
  list(): Observable<GoldenSet[]> { return this.http.get<GoldenSet[]>(this.baseUrl); }
  get(id: string): Observable<GoldenSet> { return this.http.get<GoldenSet>(`${this.baseUrl}/${id}`); }
  create(): Observable<GoldenSet> {
    return this.http.post<GoldenSet>(this.baseUrl, { rubricVersion: '1.0', language: 'es' }, { headers: { 'Idempotency-Key': crypto.randomUUID() } });
  }
  addEntry(id: string, transcript: unknown[], referenceScores: Record<string, number>): Observable<{ id: string }> {
    return this.http.post<{ id: string }>(`${this.baseUrl}/${id}/entries`, { transcript, referenceScores }, { headers: { 'Idempotency-Key': crypto.randomUUID() } });
  }
}
