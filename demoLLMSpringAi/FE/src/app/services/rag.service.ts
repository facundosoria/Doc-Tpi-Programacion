import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface RagDocumentInfo {
  documentId: string;
  fileName: string;
  fileSizeBytes: number;
  pageCount: number;
  chunkCount: number;
  uploadedAt: string;
  previewText: string;
}

export interface DocumentChunk {
  id: string;
  documentId: string;
  documentName: string;
  pageNumber: number;
  chunkIndex: number;
  content: string;
  similarityScore: number;
}

export interface RagFuenteDto {
  pageNumber: number;
  chunkIndex: number;
  score: number;
  textoExtracto: string;
}

export interface RagChatRequest {
  documentId: string;
  pregunta: string;
  conversacionId?: string | null;
}

export interface RagChatResponse {
  respuesta: string;
  estado: string; // "OK", "BLOCKED_PROFANITY", "BLOCKED_INJECTION", "BLOCKED_VALIDATION", etc.
  mensajeValidacion?: string;
  tokensGastados: number;
  cached: boolean;
  rolTutor: string;
  fuentes: RagFuenteDto[];
  conversacionId?: string;
}

@Injectable({
  providedIn: 'root'
})
export class RagService {
  private readonly http = inject(HttpClient);
  // Conexión dinámica con el backend Spring Boot
  private readonly baseUrl = 'http://localhost:8080/api/rag';

  uploadPdf(file: File): Observable<RagDocumentInfo> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<RagDocumentInfo>(`${this.baseUrl}/upload`, formData);
  }

  loadSamplePdf(): Observable<RagDocumentInfo> {
    return this.http.post<RagDocumentInfo>(`${this.baseUrl}/sample-pdf`, {});
  }

  chatWithTutor(request: RagChatRequest): Observable<RagChatResponse> {
    return this.http.post<RagChatResponse>(`${this.baseUrl}/chat`, request);
  }

  getDocumentChunks(documentId: string): Observable<DocumentChunk[]> {
    return this.http.get<DocumentChunk[]>(`${this.baseUrl}/documento/${documentId}/chunks`);
  }
}
