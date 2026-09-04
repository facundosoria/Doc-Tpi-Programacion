import { Component, signal, computed, inject, ElementRef, ViewChild } from '@angular/core';
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
export class App {
  private readonly ragService = inject(RagService);

  @ViewChild('messagesContainer') private messagesContainer!: ElementRef<HTMLDivElement>;

  // Estado con Signals (Angular 21)
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
  readonly hasDocument = computed(() => this.activeDocument() !== null);
  readonly charCount = computed(() => this.queryText.length);
  readonly isCharLimitWarn = computed(() => this.queryText.length > 550);

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
      this.processSelectedFile(e.dataTransfer.files[0]);
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      this.processSelectedFile(input.files[0]);
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
    this.uploadProgressText.set('Extrayendo texto y vectorizando en memoria (TF-IDF)...');

    this.ragService.uploadPdf(file).subscribe({
      next: (doc) => {
        this.onDocumentReady(doc);
        this.isUploading.set(false);
        this.showToast(`Documento '${doc.fileName}' indexado con éxito`, 'success');
      },
      error: (err) => {
        this.isUploading.set(false);
        this.showToast(err.error?.error || 'Error al procesar el archivo PDF', 'error');
      }
    });
  }

  loadSamplePdf(): void {
    this.isUploading.set(true);
    this.uploadProgressText.set('Cargando y vectorizando PDF de prueba...');

    this.ragService.loadSamplePdf().subscribe({
      next: (doc) => {
        this.onDocumentReady(doc);
        this.isUploading.set(false);
        this.showToast('PDF de prueba indexado correctamente en memoria', 'success');
      },
      error: (err) => {
        this.isUploading.set(false);
        this.showToast(err.error?.error || 'Error cargando PDF de prueba', 'error');
      }
    });
  }

  private onDocumentReady(doc: RagDocumentInfo): void {
    this.activeDocument.set(doc);

    // Cargar fragmentos indexados
    this.ragService.getDocumentChunks(doc.documentId).subscribe({
      next: (chunks) => this.chunks.set(chunks),
      error: () => console.warn('No se pudieron cargar los fragmentos')
    });

    // Notificación en chat del tutor
    const welcomeTutorMsg: ChatMessage = {
      id: 'doc_ready_' + Date.now(),
      sender: 'tutor',
      text: `He indexado ${doc.fileName} (${doc.pageCount} páginas, ${doc.chunkCount} fragmentos). Estoy listo para responder tus dudas de manera concisa y pedagógica.`,
      htmlContent: `<p>He indexado el documento <strong>${doc.fileName}</strong> (${doc.pageCount} páginas, ${doc.chunkCount} fragmentos semánticos).</p><p>Estoy listo para responder tus dudas de manera <strong>concisa y pedagógica</strong>. ¿Qué punto del material te gustaría revisar?</p>`,
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
    this.showToast('Documento desvinculado de la sesión', 'info');
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

    // V1. Validación: Documento activo requerido
    if (!this.activeDocument()) {
      this.validationAlert.set('Debes arrastrar o cargar un documento PDF antes de formular preguntas.');
      this.showToast('Carga un PDF en el panel izquierdo primero', 'warn');
      return;
    }

    // V2. Validación: Vacío
    if (!query) {
      this.validationAlert.set('Escribe una consulta válida sobre el documento.');
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

    // Iniciar llamada al Tutor
    this.isThinking.set(true);

    this.ragService.chatWithTutor({
      documentId: this.activeDocument()!.documentId,
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
        if (resp.estado === 'BLOCKED_PROFANITY' || resp.estado === 'BLOCKED_INJECTION') {
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
