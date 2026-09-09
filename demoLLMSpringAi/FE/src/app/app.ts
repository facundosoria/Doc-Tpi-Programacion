import { Component, signal, computed, inject, ElementRef, ViewChild, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { marked } from 'marked';
import { RagService, RagDocumentInfo, DocumentChunk, RagFuenteDto } from './services/rag.service';

export interface ChatMessage {
  id: string;
  sender: 'student' | 'tutor';
  text: string;
  htmlContent: string;
  status?: string;
  fuentes?: RagFuenteDto[];
  cached?: boolean;
  tokensGastados?: number;
  rolTutor?: string;
  timestamp: Date;
  showSources?: boolean;
}

export interface ToastItem {
  id: number;
  message: string;
  type: 'success' | 'error' | 'warn' | 'info';
}

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  private readonly ragService = inject(RagService);

  @ViewChild('messagesContainer') private messagesContainer!: ElementRef<HTMLDivElement>;

  // Estado con Signals (Angular 21) - Multi-fuente NotebookLM
  readonly sources = signal<RagDocumentInfo[]>([]);
  readonly selectedSourceIds = signal<Set<string>>(new Set());
  readonly activeDocument = signal<RagDocumentInfo | null>(null);
  readonly conversacionId = signal<string | null>(null);
  readonly tokensSaved = signal<number>(0);
  readonly isUploading = signal<boolean>(false);
  readonly uploadProgressText = signal<string>('Procesando PDF...');
  readonly isThinking = signal<boolean>(false);
  readonly isDragOver = signal<boolean>(false);
  readonly showChunks = signal<boolean>(false);
  readonly chunks = signal<DocumentChunk[]>([]);
  readonly validationAlert = signal<string | null>(null);
  readonly toasts = signal<ToastItem[]>([]);

  // Input del chat
  queryText = '';
  private lastSubmitTime = 0;
  private readonly cooldownMs = 1200;

  // Tutor info
  readonly tutorRole = signal<string>('Profesor Tutor Pedagógico');

  // Historial de mensajes
  readonly messages = signal<ChatMessage[]>([
    {
      id: 'welcome',
      sender: 'tutor',
      text: '¡Hola! Soy tu Profesor Tutor Pedagógico. Mi misión es ayudarte a comprender a fondo el material que cargues.',
      htmlContent: `<p>¡Hola! Soy tu <strong>Profesor Tutor Pedagógico</strong>. Mi misión es ayudarte a comprender a fondo el material que cargues.</p>
      <p>Mis respuestas serán <strong>cortas, concisas y directas a tu duda</strong>, explicándote el concepto y citando las páginas del PDF.</p>
      <div class="welcome-guide">
        <strong><i class="fa-solid fa-circle-info"></i> ¿Cómo empezar?</strong>
        <ol>
          <li>Arrastra un archivo PDF en el panel izquierdo (o presiona "Cargar PDF de Prueba").</li>
          <li>Escribe tu duda puntual en el chat.</li>
          <li>Recibirás una respuesta directa y concisa con las páginas citadas del PDF.</li>
        </ol>
      </div>`,
      rolTutor: 'Profesor Tutor Pedagógico',
      timestamp: new Date()
    }
  ]);

  // Computed signals
  readonly hasDocument = computed(() => this.sources().length > 0);
  readonly selectedSourcesCount = computed(() => this.selectedSourceIds().size);
  readonly isAllSelected = computed(() => this.sources().length > 0 && this.selectedSourceIds().size === this.sources().length);
  readonly charCount = computed(() => this.queryText.length);
  readonly isCharLimitWarn = computed(() => this.queryText.length > 550);

  // ==========================================
  // INICIALIZACIÓN Y GESTIÓN DE FUENTES
  // ==========================================

  ngOnInit(): void {
    this.loadDocuments();
  }

  loadDocuments(): void {
    this.ragService.getDocuments().subscribe({
      next: (docs) => {
        this.sources.set(docs);
        if (docs.length > 0) {
          // Por defecto, seleccionar todas las fuentes disponibles
          this.selectedSourceIds.set(new Set(docs.map((d) => d.documentId)));
          if (!this.activeDocument()) {
            this.activeDocument.set(docs[0]);
          }
        }
      },
      error: (err) => {
        console.warn('No se pudieron listar los documentos de la base de datos:', err);
      }
    });
  }

  toggleSource(documentId: string): void {
    this.selectedSourceIds.update((set) => {
      const next = new Set(set);
      if (next.has(documentId)) {
        next.delete(documentId);
      } else {
        next.add(documentId);
      }
      return next;
    });
  }

  isSourceSelected(documentId: string): boolean {
    return this.selectedSourceIds().has(documentId);
  }

  selectAllSources(): void {
    this.selectedSourceIds.set(new Set(this.sources().map((s) => s.documentId)));
  }

  deselectAllSources(): void {
    this.selectedSourceIds.set(new Set());
  }

  deleteSource(docId: string, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    this.ragService.deleteDocument(docId).subscribe({
      next: () => {
        this.sources.update((list) => list.filter((d) => d.documentId !== docId));
        this.selectedSourceIds.update((set) => {
          const next = new Set(set);
          next.delete(docId);
          return next;
        });
        if (this.activeDocument()?.documentId === docId) {
          const remaining = this.sources();
          this.activeDocument.set(remaining.length > 0 ? remaining[0] : null);
          this.chunks.set([]);
          this.showChunks.set(false);
        }
        this.showToast('Fuente eliminada de la base de datos', 'info');
      },
      error: (err) => {
        this.showToast(err.error?.error || 'Error al eliminar la fuente', 'error');
      }
    });
  }

  inspectChunks(doc: RagDocumentInfo, event?: Event): void {
    if (event) {
      event.stopPropagation();
    }
    if (this.activeDocument()?.documentId === doc.documentId && this.showChunks()) {
      this.showChunks.set(false);
      return;
    }
    this.activeDocument.set(doc);
    this.ragService.getDocumentChunks(doc.documentId).subscribe({
      next: (chunks) => {
        this.chunks.set(chunks);
        this.showChunks.set(true);
      },
      error: () => this.showToast('No se pudieron obtener los fragmentos', 'warn')
    });
  }

  // ==========================================
  // DRAG & DROP Y CARGA DE PDF
  // ==========================================

  onDragOver(e: DragEvent): void {
    e.preventDefault();
    e.stopPropagation();
    this.isDragOver.set(true);
  }

  onDragLeave(e: DragEvent): void {
    e.preventDefault();
    e.stopPropagation();
    this.isDragOver.set(false);
  }

  onDrop(e: DragEvent): void {
    e.preventDefault();
    e.stopPropagation();
    this.isDragOver.set(false);

    if (e.dataTransfer?.files && e.dataTransfer.files.length > 0) {
      Array.from(e.dataTransfer.files).forEach((file) => this.processSelectedFile(file));
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      Array.from(input.files).forEach((file) => this.processSelectedFile(file));
      input.value = '';
    }
  }

  private processSelectedFile(file: File): void {
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      this.showToast('Solo se admiten documentos en formato PDF (.pdf)', 'warn');
      return;
    }

    if (file.size > 25 * 1024 * 1024) {
      this.showToast('El archivo supera el límite de 25 MB', 'warn');
      return;
    }

    this.isUploading.set(true);
    this.uploadProgressText.set(`Indexando "${file.name}" con PDFBox y pgvector...`);

    this.ragService.uploadPdf(file).subscribe({
      next: (doc) => {
        this.onDocumentReady(doc);
        this.isUploading.set(false);
        this.showToast(`Documento '${doc.fileName}' indexado con éxito`, 'success');
      },
      error: (err) => {
        this.isUploading.set(false);
        this.showToast(err.error?.error || `Error al procesar ${file.name}`, 'error');
      }
    });
  }

  loadSamplePdf(): void {
    this.isUploading.set(true);
    this.uploadProgressText.set('Cargando y vectorizando PDF de prueba con pgvector...');

    this.ragService.loadSamplePdf().subscribe({
      next: (doc) => {
        this.onDocumentReady(doc);
        this.isUploading.set(false);
        this.showToast('PDF de prueba indexado correctamente en memoria/pgvector', 'success');
      },
      error: (err) => {
        this.isUploading.set(false);
        this.showToast(err.error?.error || 'Error cargando PDF de prueba', 'error');
      }
    });
  }

  private onDocumentReady(doc: RagDocumentInfo): void {
    this.activeDocument.set(doc);

    // Actualizar lista de fuentes y seleccionarla
    this.sources.update((prev) => {
      const filtered = prev.filter((d) => d.documentId !== doc.documentId);
      return [doc, ...filtered];
    });
    this.selectedSourceIds.update((set) => {
      const next = new Set(set);
      next.add(doc.documentId);
      return next;
    });

    // Cargar fragmentos indexados
    this.ragService.getDocumentChunks(doc.documentId).subscribe({
      next: (chunks) => this.chunks.set(chunks),
      error: () => console.warn('No se pudieron cargar los fragmentos')
    });

    // Notificación en chat del tutor
    const welcomeTutorMsg: ChatMessage = {
      id: 'doc_ready_' + Date.now(),
      sender: 'tutor',
      text: `He indexado "${doc.fileName}" (${doc.pageCount} páginas, ${doc.chunkCount} fragmentos semánticos). Añadido al panel de fuentes (${this.selectedSourceIds().size} activa${this.selectedSourceIds().size > 1 ? 's' : ''}).`,
      htmlContent: `<p>He indexado la fuente <strong>${doc.fileName}</strong> (${doc.pageCount} páginas, ${doc.chunkCount} fragmentos semánticos con embeddings).</p><p>Está seleccionada y lista para consultas multi-fuente estilo NotebookLM.</p>`,
      rolTutor: 'Profesor Tutor Pedagógico',
      timestamp: new Date()
    };
    this.messages.update((msgs) => [...msgs, welcomeTutorMsg]);
    this.scrollToBottom();
  }

  removeDocument(): void {
    this.activeDocument.set(null);
    this.chunks.set([]);
    this.showChunks.set(false);
    this.showToast('Vista de fragmentos cerrada', 'info');
  }

  toggleChunksView(): void {
    this.showChunks.update((v) => !v);
  }

  // ==========================================
  // CHAT & VALIDACIONES
  // ==========================================

  onInputChange(): void {
    this.validationAlert.set(null);
  }

  onKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.sendQuery();
    }
  }

  useQuickPrompt(promptText: string): void {
    this.queryText = promptText;
    this.sendQuery();
  }

  sendQuery(): void {
    const query = this.queryText.trim();
    const selectedIds = Array.from(this.selectedSourceIds());

    // V1. Validación: Al menos una fuente seleccionada
    if (selectedIds.length === 0) {
      this.validationAlert.set('Debes seleccionar al menos una fuente en el panel de fuentes antes de formular preguntas.');
      this.showToast('Selecciona al menos una fuente en el panel izquierdo', 'warn');
      return;
    }

    // V2. Validación: Vacío
    if (!query) {
      this.validationAlert.set('Escribe una consulta válida sobre las fuentes seleccionadas.');
      return;
    }

    // V3. Validación: Longitud mínima y máxima
    if (query.length < 4) {
      this.validationAlert.set('Tu pregunta es muy corta (mínimo 4 caracteres).');
      return;
    }

    if (query.length > 600) {
      this.validationAlert.set('Tu pregunta excede los 600 caracteres. Sé más conciso para optimizar tokens.');
      return;
    }

    // V4. Validación: Cooldown anti-flood
    const now = Date.now();
    if (now - this.lastSubmitTime < this.cooldownMs) {
      this.validationAlert.set('Vas muy rápido. Espera un segundo antes de enviar otra consulta.');
      return;
    }
    this.lastSubmitTime = now;

    // Agregar mensaje del estudiante
    const studentMsg: ChatMessage = {
      id: 'student_' + Date.now(),
      sender: 'student',
      text: query,
      htmlContent: `<p>${this.escapeHtml(query)}</p>`,
      timestamp: new Date()
    };
    this.messages.update((msgs) => [...msgs, studentMsg]);
    this.queryText = '';
    this.scrollToBottom();

    // Iniciar llamada al Tutor con multi-fuente
    this.isThinking.set(true);

    this.ragService.chatWithTutor({
      documentIds: selectedIds,
      documentId: selectedIds[0],
      pregunta: query,
      conversacionId: this.conversacionId()
    }).subscribe({
      next: (resp) => {
        this.isThinking.set(false);

        if (resp.conversacionId) {
          this.conversacionId.set(resp.conversacionId);
        }
        if (resp.rolTutor) {
          this.tutorRole.set(resp.rolTutor);
        }

        // Actualizar métrica de tokens ahorrados si fue bloqueado o provino de caché
        if (resp.estado === 'BLOCKED_PROFANITY' || resp.estado === 'BLOCKED_INJECTION' || resp.estado === 'BLOCKED_NO_SOURCE') {
          this.tokensSaved.update((t) => t + 350);
        } else if (resp.cached) {
          this.tokensSaved.update((t) => t + 250);
        }

        const tutorMsg: ChatMessage = {
          id: 'tutor_' + Date.now(),
          sender: 'tutor',
          text: resp.respuesta,
          htmlContent: this.renderMarkdown(resp.respuesta),
          status: resp.estado,
          fuentes: resp.fuentes || [],
          cached: resp.cached,
          tokensGastados: resp.tokensGastados,
          rolTutor: resp.rolTutor || this.tutorRole(),
          timestamp: new Date(),
          showSources: false
        };

        this.messages.update((msgs) => [...msgs, tutorMsg]);
        this.scrollToBottom();
      },
      error: (err) => {
        this.isThinking.set(false);
        const errMsg: ChatMessage = {
          id: 'error_' + Date.now(),
          sender: 'tutor',
          text: 'Error de conexión: ' + (err.error?.error || err.message),
          htmlContent: `<p>Error de conexión con el backend: ${err.error?.error || err.message}</p>`,
          status: 'ERROR',
          timestamp: new Date()
        };
        this.messages.update((msgs) => [...msgs, errMsg]);
        this.scrollToBottom();
      }
    });
  }

  toggleMessageSources(msg: ChatMessage): void {
    msg.showSources = !msg.showSources;
  }

  clearChat(): void {
    this.messages.set([
      {
        id: 'reset_' + Date.now(),
        sender: 'tutor',
        text: 'Conversación reiniciada. Puedes formular nuevas consultas sobre el PDF.',
        htmlContent: '<p>Conversación reiniciada. Puedes formular nuevas consultas sobre el material activo.</p>',
        rolTutor: this.tutorRole(),
        timestamp: new Date()
      }
    ]);
    this.conversacionId.set(null);
    this.showToast('Historial de chat reiniciado', 'info');
  }

  // ==========================================
  // HELPERS
  // ==========================================

  renderMarkdown(content: string): string {
    if (!content) return '';
    try {
      return marked.parse(content) as string;
    } catch {
      return `<p>${this.escapeHtml(content)}</p>`;
    }
  }

  formatBytes(bytes?: number): string {
    if (!bytes) return '0 KB';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  private escapeHtml(str: string): string {
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  private scrollToBottom(): void {
    setTimeout(() => {
      if (this.messagesContainer) {
        this.messagesContainer.nativeElement.scrollTop = this.messagesContainer.nativeElement.scrollHeight;
      }
    }, 100);
  }

  showToast(message: string, type: ToastItem['type']): void {
    const toast: ToastItem = { id: Date.now(), message, type };
    this.toasts.update((t) => [...t, toast]);
    setTimeout(() => {
      this.toasts.update((t) => t.filter((item) => item.id !== toast.id));
    }, 3500);
  }
}
