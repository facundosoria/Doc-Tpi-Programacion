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
  documentId?: string;
  documentName?: string;
  pageNumber: number;
  chunkIndex: number;
  score: number;
  textoExtracto: string;
}

export interface RagChatRequest {
  documentId?: string;
  documentIds?: string[];
  pregunta: string;
  conversacionId?: string | null;
}

export interface RagChatResponse {
  respuesta: string;
  estado: string; // "OK", "BLOCKED_PROFANITY", "BLOCKED_INJECTION", "BLOCKED_VALIDATION", "BLOCKED_NO_SOURCE", etc.
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
  private readonly baseUrl = 'http://localhost:8080/api/rag';

  getDocuments(): Observable<RagDocumentInfo[]> {
    return this.http.get<RagDocumentInfo[]>(`${this.baseUrl}/documentos`);
  }

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

  deleteDocument(documentId: string): Observable<{ message: string; documentId: string }> {
    return this.http.delete<{ message: string; documentId: string }>(`${this.baseUrl}/documento/${documentId}`);
  }
}
