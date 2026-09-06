import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

export interface GoldenSetEntry { id: string; transcript: unknown[]; referenceScores: Record<string, number>; createdAt: string; }
export interface GoldenSet { id: string; version: number; rubricVersion: string; language: string; createdAt: string; entries?: GoldenSetEntry[]; }

/** UUID válido para la cabecera de idempotencia, también en navegadores sin randomUUID(). */
export function createIdempotencyKey(cryptoApi: Crypto | undefined = globalThis.crypto): string {
  if (typeof cryptoApi?.randomUUID === "function") return cryptoApi.randomUUID();

  const bytes = new Uint8Array(16);
  if (typeof cryptoApi?.getRandomValues === "function") {
    cryptoApi.getRandomValues(bytes);
  } else {
    // La clave no autentica: sólo impide que un reintento duplique el POST.
    for (let index = 0; index < bytes.length; index += 1) bytes[index] = Math.floor(Math.random() * 256);
  }
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  const hex = Array.from(bytes, value => value.toString(16).padStart(2, "0")).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

@Injectable({ providedIn: 'root' })
export class GoldenSetApiService {
  private readonly http = inject(HttpClient);
  // El navegador sólo conoce su mismo origen. El host que lo sirve decide si /api
  // llega al backend demo o al API Gateway de la plataforma.
  private readonly baseUrl = '/api/llm/golden-sets';
  list(): Observable<GoldenSet[]> { return this.http.get<GoldenSet[]>(this.baseUrl); }
  get(id: string): Observable<GoldenSet> { return this.http.get<GoldenSet>(`${this.baseUrl}/${id}`); }
  create(): Observable<GoldenSet> {
    return this.http.post<GoldenSet>(this.baseUrl, { rubricVersion: '1.0', language: 'es' }, { headers: { 'Idempotency-Key': createIdempotencyKey() } });
  }
  addEntry(id: string, transcript: unknown[], referenceScores: Record<string, number>): Observable<{ id: string }> {
    return this.http.post<{ id: string }>(`${this.baseUrl}/${id}/entries`, { transcript, referenceScores }, { headers: { 'Idempotency-Key': createIdempotencyKey() } });
  }
}
