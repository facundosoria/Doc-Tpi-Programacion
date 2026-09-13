/**
 * Tutor IA Pedagógico — Cliente RAG con Spring AI
 */

document.addEventListener('DOMContentLoaded', () => {
  // Estado de la aplicación
  const API_BASE = window.location.port === '8080' ? '' : 'http://localhost:8080';

  const state = {
    activeDocument: null,
    conversacionId: null,
    tokensSaved: 0,
    isProcessing: false,
    lastSubmitTime: 0,
    cooldownMs: 1200
  };

  // Elementos del DOM
  const dropZone = document.getElementById('dropZone');
  const fileInput = document.getElementById('fileInput');
  const btnLoadSample = document.getElementById('btnLoadSample');
  const docCard = document.getElementById('docCard');
  const docStatusBadge = document.getElementById('docStatusBadge');
  const docStatusText = document.getElementById('docStatusText');
  const docName = document.getElementById('docName');
  const docSize = document.getElementById('docSize');
  const docPages = document.getElementById('docPages');
  const docChunks = document.getElementById('docChunks');
  const docPreviewText = document.getElementById('docPreviewText');
  const btnRemoveDoc = document.getElementById('btnRemoveDoc');
  const btnToggleChunks = document.getElementById('btnToggleChunks');
  const chunksAccordion = document.getElementById('chunksAccordion');
  const chunksList = document.getElementById('chunksList');
  const chunksCountBadge = document.getElementById('chunksCountBadge');

  const chatMessages = document.getElementById('chatMessages');
  const chatForm = document.getElementById('chatForm');
  const messageInput = document.getElementById('messageInput');
  const btnSend = document.getElementById('btnSend');
  const charCounter = document.getElementById('charCounter');
  const validationAlert = document.getElementById('validationAlert');
  const validationAlertText = document.getElementById('validationAlertText');
  const quickPrompts = document.getElementById('quickPrompts');
  const tokenSavingsCount = document.getElementById('tokenSavingsCount');
  const tutorTitle = document.getElementById('tutorTitle');
  const tutorRoleDescription = document.getElementById('tutorRoleDescription');
  const btnClearChat = document.getElementById('btnClearChat');

  // ============================================================
  // GESTIÓN DE DRAG & DROP Y CARGA DE PDF
  // ============================================================

  ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
    dropZone.addEventListener(eventName, preventDefaults, false);
    document.body.addEventListener(eventName, preventDefaults, false);
  });

  function preventDefaults(e) {
    e.preventDefault();
    e.stopPropagation();
  }

  ['dragenter', 'dragover'].forEach(eventName => {
    dropZone.addEventListener(eventName, () => dropZone.classList.add('dragover'), false);
  });

  ['dragleave', 'drop'].forEach(eventName => {
    dropZone.addEventListener(eventName, () => dropZone.classList.remove('dragover'), false);
  });

  dropZone.addEventListener('drop', (e) => {
    const files = e.dataTransfer.files;
    if (files.length > 0) {
      handlePdfUpload(files[0]);
    }
  });

  dropZone.addEventListener('click', () => {
    fileInput.click();
  });

  fileInput.addEventListener('change', () => {
    if (fileInput.files.length > 0) {
      handlePdfUpload(fileInput.files[0]);
    }
  });

  btnLoadSample.addEventListener('click', async () => {
    try {
      setLoadingDropzone(true, 'Indexando PDF de prueba...');
      const response = await fetch(`${API_BASE}/api/rag/sample-pdf`, { method: 'POST' });
      const data = await response.json();

      if (!response.ok) {
        throw new Error(data.error || 'Error al cargar PDF de muestra');
      }

      onDocumentLoaded(data);
      showToast('PDF de muestra cargado e indexado en memoria con éxito', 'success');
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setLoadingDropzone(false);
    }
  });

  btnRemoveDoc.addEventListener('click', () => {
    state.activeDocument = null;
    docCard.style.display = 'none';
    dropZone.style.display = 'block';
    quickPrompts.style.display = 'none';
    docStatusBadge.classList.remove('active');
    docStatusText.textContent = 'Sin documento';
    fileInput.value = '';
    showToast('Documento desvinculado de la sesión', 'info');
  });

  btnToggleChunks.addEventListener('click', () => {
    const isVisible = chunksAccordion.style.display === 'block';
    chunksAccordion.style.display = isVisible ? 'none' : 'block';
  });

  async function handlePdfUpload(file) {
    // Validaciones de archivo en frontend
    if (!file.name.toLowerCase().endsWith('.pdf')) {
      showToast('Solo se admiten documentos en formato PDF (.pdf)', 'warn');
      return;
    }

    if (file.size > 25 * 1024 * 1024) {
      showToast('El archivo supera el límite de 25 MB', 'warn');
      return;
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
      setLoadingDropzone(true, 'Extrayendo texto e indexando vectores TF-IDF...');
      const response = await fetch(`${API_BASE}/api/rag/upload`, {
        method: 'POST',
        body: formData
      });

      const data = await response.json();
      if (!response.ok) {
        throw new Error(data.error || 'Error al procesar el archivo');
      }

      onDocumentLoaded(data);
      showToast(`Documento '${data.fileName}' indexado con éxito`, 'success');
    } catch (err) {
      showToast(err.message, 'error');
    } finally {
      setLoadingDropzone(false);
    }
  }

  function onDocumentLoaded(doc) {
    state.activeDocument = doc;

    // Actualizar Card
    docName.textContent = doc.fileName;
    docName.title = doc.fileName;
    docSize.textContent = formatBytes(doc.fileSizeBytes);
    docPages.textContent = doc.pageCount;
    docChunks.textContent = doc.chunkCount;
    chunksCountBadge.textContent = doc.chunkCount;
    docPreviewText.textContent = doc.previewText || 'Texto listo para consulta RAG.';

    docCard.style.display = 'flex';
    dropZone.style.display = 'none';
    quickPrompts.style.display = 'flex';

    // Status Indicator
    docStatusBadge.classList.add('active');
    docStatusText.textContent = `Listo: ${doc.fileName.substring(0, 18)}...`;

    // Cargar fragmentos para auditoría
    loadDocumentChunks(doc.documentId);

    // Mensaje del tutor en el chat
    appendTutorMessage({
      respuesta: `He leído e indexado el documento **${doc.fileName}** (${doc.pageCount} páginas, ${doc.chunkCount} fragmentos semánticos). \n\nEstoy listo para responder tus dudas de forma **pedagógica y concisa**. ¿Qué parte te gustaría explorar?`,
      estado: 'OK',
      tokensGastados: 0,
      fuentes: [],
      rolTutor: 'Profesor Tutor Pedagógico'
    });
  }

  async function loadDocumentChunks(docId) {
    try {
      const res = await fetch(`${API_BASE}/api/rag/documento/${docId}/chunks`);
      if (res.ok) {
        const chunks = await res.json();
        renderChunksList(chunks);
      }
    } catch (e) {
      console.warn('No se pudieron cargar los fragmentos:', e);
    }
  }

  function renderChunksList(chunks) {
    chunksList.innerHTML = '';
    chunks.forEach((c) => {
      const item = document.createElement('div');
      item.className = 'chunk-item';
      item.innerHTML = `
        <div class="chunk-item-header">
          <span>Página ${c.pageNumber} • Chunk #${c.chunkIndex + 1}</span>
          <span>${c.content.length} chars</span>
        </div>
        <div class="chunk-item-body">${escapeHtml(c.content.substring(0, 160))}...</div>
      `;
      chunksList.appendChild(item);
    });
  }

  function setLoadingDropzone(isLoading, text) {
    if (isLoading) {
      dropZone.style.pointerEvents = 'none';
      dropZone.innerHTML = `
        <div class="drop-content">
          <div class="drop-icon"><i class="fa-solid fa-spinner fa-spin"></i></div>
          <h3>${text || 'Procesando PDF...'}</h3>
          <p>Extracción de páginas, tokenización y vectorización en memoria</p>
        </div>
      `;
    } else {
      dropZone.style.pointerEvents = 'auto';
      dropZone.innerHTML = `
        <input type="file" id="fileInput" accept=".pdf" class="file-input-hidden" />
        <div class="drop-content">
          <div class="drop-icon"><i class="fa-solid fa-cloud-arrow-up"></i></div>
          <h3>Arrastra y suelta tu archivo PDF aquí</h3>
          <p>O haz clic para seleccionarlo de tu equipo</p>
          <div class="drop-specs">
            <span><i class="fa-regular fa-file"></i> Solo archivos .pdf</span>
            <span><i class="fa-solid fa-shield-halved"></i> Máximo 25 MB</span>
          </div>
        </div>
      `;
      // Reasociar listener
      const newFileInput = document.getElementById('fileInput');
      newFileInput.addEventListener('change', () => {
        if (newFileInput.files.length > 0) handlePdfUpload(newFileInput.files[0]);
      });
    }
  }

  // ============================================================
  // VALIDACIONES EN TIEMPO REAL & CHAT INPUT
  // ============================================================

  messageInput.addEventListener('input', () => {
    // Auto-resize
    messageInput.style.height = 'auto';
    messageInput.style.height = Math.min(messageInput.scrollHeight, 120) + 'px';

    // Contador de caracteres
    const len = messageInput.value.length;
    charCounter.textContent = `${len} / 600`;

    if (len > 550) {
      charCounter.classList.add('limit-warn');
    } else {
      charCounter.classList.remove('limit-warn');
    }

    hideValidationAlert();
  });

  messageInput.addEventListener('keydown', (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      chatForm.dispatchEvent(new Event('submit'));
    }
  });

  // Sugerencias rápidas (chips)
  document.querySelectorAll('.chip-btn').forEach(btn => {
    btn.addEventListener('click', () => {
      messageInput.value = btn.dataset.query;
      messageInput.dispatchEvent(new Event('input'));
      chatForm.dispatchEvent(new Event('submit'));
    });
  });

  // Envío del Formulario
  chatForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const query = messageInput.value.trim();

    // 1. Validación: Documento cargado
    if (!state.activeDocument) {
      showValidationAlert('Debes arrastrar o cargar un documento PDF antes de formular preguntas.');
      showToast('Carga un PDF en el panel izquierdo primero', 'warn');
      return;
    }

    // 2. Validación: Vacío
    if (!query) {
      showValidationAlert('Por favor escribe una duda o pregunta sobre el documento.');
      return;
    }

    // 3. Validación: Longitud mínima y máxima
    if (query.length < 4) {
      showValidationAlert('Tu pregunta es demasiado corta (mínimo 4 caracteres).');
      return;
    }

    if (query.length > 600) {
      showValidationAlert('Tu pregunta excede los 600 caracteres. Sé más conciso para optimizar la respuesta.');
      return;
    }

    // 4. Validación: Cooldown anti-flood
    const now = Date.now();
    if (now - state.lastSubmitTime < state.cooldownMs) {
      showValidationAlert('Vas muy rápido. Espera un segundo antes de enviar otra consulta.');
      return;
    }
    state.lastSubmitTime = now;

    // Ejecutar envío
    await sendStudentQuestion(query);
  });

  async function sendStudentQuestion(pregunta) {
    if (state.isProcessing) return;
    state.isProcessing = true;
    btnSend.disabled = true;
    hideValidationAlert();

    // 1. Mostrar mensaje del alumno en la UI
    appendStudentMessage(pregunta);
    messageInput.value = '';
    messageInput.style.height = 'auto';
    charCounter.textContent = '0 / 600';

    // 2. Mostrar indicador de escritura del tutor
    const typingId = showTypingIndicator();

    try {
      const payload = {
        documentId: state.activeDocument.documentId,
        pregunta: pregunta,
        conversacionId: state.conversacionId
      };

      const res = await fetch(`${API_BASE}/api/rag/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const data = await res.json();
      removeTypingIndicator(typingId);

      if (!res.ok) {
        throw new Error(data.error || 'Error al comunicarse con el tutor IA.');
      }

      // 3. Procesar respuesta y Guardrails
      handleTutorResponse(data);

    } catch (err) {
      removeTypingIndicator(typingId);
      appendErrorMessage('No se pudo conectar con el servicio: ' + err.message);
    } finally {
      state.isProcessing = false;
      btnSend.disabled = false;
      messageInput.focus();
    }
  }

  function handleTutorResponse(data) {
    if (data.conversacionId) {
      state.conversacionId = data.conversacionId;
    }

    if (data.rolTutor) {
      tutorTitle.textContent = data.rolTutor;
      tutorRoleDescription.textContent = `Especialista en ${state.activeDocument?.fileName || 'el material'}`;
    }

    // Si fue bloqueado por malas palabras o inyecciones -> Sumar tokens ahorrados
    if (data.estado === 'BLOCKED_PROFANITY' || data.estado === 'BLOCKED_INJECTION') {
      state.tokensSaved += 350; // Estimación de tokens ahorrados por consulta no enviada al LLM
      updateTokenSavings();
    } else if (data.cached) {
      state.tokensSaved += (data.tokensGastados || 250);
      updateTokenSavings();
    }

    appendTutorMessage(data);
  }

  function updateTokenSavings() {
    tokenSavingsCount.textContent = state.tokensSaved.toLocaleString();
  }

  function showValidationAlert(msg) {
    validationAlertText.textContent = msg;
    validationAlert.style.display = 'flex';
  }

  function hideValidationAlert() {
    validationAlert.style.display = 'none';
  }

  // ============================================================
  // RENDERIZADO DE MENSAJES EN EL CHAT
  // ============================================================

  function appendStudentMessage(text) {
    const msgDiv = document.createElement('div');
    msgDiv.className = 'message student-message';
    msgDiv.innerHTML = `
      <div class="msg-avatar"><i class="fa-solid fa-user-graduate"></i></div>
      <div class="msg-body">
        <div class="msg-header" style="justify-content: flex-end;">
          <span class="msg-author">Estudiante</span>
        </div>
        <div class="msg-content">
          <p>${escapeHtml(text)}</p>
        </div>
      </div>
    `;
    chatMessages.appendChild(msgDiv);
    scrollToBottom();
  }

  function appendTutorMessage(data) {
    const isBlockedProfanity = data.estado === 'BLOCKED_PROFANITY';
    const isBlockedInjection = data.estado === 'BLOCKED_INJECTION';
    const isCached = data.cached === true;

    let badgeHtml = '';
    let extraClass = '';

    if (isBlockedProfanity) {
      badgeHtml = `<div class="blocked-badge"><i class="fa-solid fa-hand"></i> Lenguaje Inapropiado Interceptado (0 Tokens)</div>`;
      extraClass = 'guardrail-blocked';
    } else if (isBlockedInjection) {
      badgeHtml = `<div class="blocked-badge" style="background:rgba(139,92,246,0.2); color:#c4b5fd; border-color:rgba(139,92,246,0.4);">
        <i class="fa-solid fa-shield-halved"></i> Intento de Manipulación Neutralizado (0 Tokens)
      </div>`;
      extraClass = 'guardrail-injection';
    } else if (isCached) {
      badgeHtml = `<div class="blocked-badge" style="background:rgba(16,185,129,0.2); color:#6ee7b7; border-color:rgba(16,185,129,0.4);">
        <i class="fa-solid fa-bolt"></i> Respuesta de Caché en Memoria (0 Tokens)
      </div>`;
    }

    // Renderizado Markdown de la respuesta
    const parsedContent = (typeof marked !== 'undefined' && marked.parse)
      ? marked.parse(data.respuesta || '')
      : `<p>${escapeHtml(data.respuesta || '')}</p>`;

    // Fuentes citadas
    let sourcesHtml = '';
    if (data.fuentes && data.fuentes.length > 0) {
      const sourceItems = data.fuentes.map((f, i) => `
        <div class="source-item">
          <div class="source-badge">Página ${f.pageNumber} • Relevancia: ${Math.round(f.score * 100)}%</div>
          <div class="source-text">"${escapeHtml(f.textoExtracto)}"</div>
        </div>
      `).join('');

      sourcesHtml = `
        <div class="msg-sources-box">
          <button class="sources-toggle-btn" onclick="toggleSources(this)">
            <i class="fa-solid fa-book-bookmark"></i> Fuentes consultadas del PDF (${data.fuentes.length})
            <i class="fa-solid fa-chevron-down" style="font-size:0.7em;"></i>
          </button>
          <div class="sources-content" style="display: none;">
            ${sourceItems}
          </div>
        </div>
      `;
    }

    const msgDiv = document.createElement('div');
    msgDiv.className = 'message tutor-message';
    msgDiv.innerHTML = `
      <div class="msg-avatar"><i class="fa-solid fa-chalkboard-user"></i></div>
      <div class="msg-body">
        <div class="msg-header">
          <span class="msg-author">${data.rolTutor || 'Profesor Tutor IA'}</span>
          <span class="badge-role">Pedagógico</span>
        </div>
        <div class="msg-content ${extraClass}">
          ${badgeHtml}
          ${parsedContent}
          ${sourcesHtml}
        </div>
      </div>
    `;

    chatMessages.appendChild(msgDiv);
    scrollToBottom();
  }

  function appendErrorMessage(text) {
    const msgDiv = document.createElement('div');
    msgDiv.className = 'message tutor-message';
    msgDiv.innerHTML = `
      <div class="msg-avatar" style="background:#dc2626;"><i class="fa-solid fa-triangle-exclamation"></i></div>
      <div class="msg-body">
        <div class="msg-header"><span class="msg-author">Error del Sistema</span></div>
        <div class="msg-content guardrail-blocked">
          <p>${escapeHtml(text)}</p>
        </div>
      </div>
    `;
    chatMessages.appendChild(msgDiv);
    scrollToBottom();
  }

  function showTypingIndicator() {
    const id = 'typing_' + Date.now();
    const div = document.createElement('div');
    div.id = id;
    div.className = 'message tutor-message';
    div.innerHTML = `
      <div class="msg-avatar"><i class="fa-solid fa-chalkboard-user"></i></div>
      <div class="msg-body">
        <div class="msg-header"><span class="msg-author">Profesor Tutor IA</span></div>
        <div class="msg-content">
          <div class="typing-dots">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
          </div>
        </div>
      </div>
    `;
    chatMessages.appendChild(div);
    scrollToBottom();
    return id;
  }

  function removeTypingIndicator(id) {
    const elem = document.getElementById(id);
    if (elem) elem.remove();
  }

  btnClearChat.addEventListener('click', () => {
    chatMessages.innerHTML = `
      <div class="message tutor-message">
        <div class="msg-avatar"><i class="fa-solid fa-chalkboard-user"></i></div>
        <div class="msg-body">
          <div class="msg-header">
            <span class="msg-author">Profesor Tutor IA</span>
            <span class="badge-role">Pedagógico</span>
          </div>
          <div class="msg-content">
            <p>Conversación reiniciada. Puedes formular nuevas preguntas sobre el documento actual.</p>
          </div>
        </div>
      </div>
    `;
    state.conversacionId = null;
    showToast('Historial de chat reiniciado', 'info');
  });

  // Utilidades
  function scrollToBottom() {
    chatMessages.scrollTop = chatMessages.scrollHeight;
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function formatBytes(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
  }

  function showToast(msg, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    const icon = type === 'success' ? 'circle-check' : type === 'error' ? 'circle-exclamation' : 'circle-info';
    toast.innerHTML = `<i class="fa-solid fa-${icon}"></i> <span>${escapeHtml(msg)}</span>`;
    container.appendChild(toast);
    setTimeout(() => {
      toast.style.opacity = '0';
      setTimeout(() => toast.remove(), 300);
    }, 4000);
  }

  // Hacer toggleSources global para el evento onclick en HTML generado
  window.toggleSources = function(btn) {
    const content = btn.nextElementSibling;
    if (!content) return;
    const isVisible = content.style.display === 'flex';
    content.style.display = isVisible ? 'none' : 'flex';
    const icon = btn.querySelector('.fa-chevron-down, .fa-chevron-up');
    if (icon) {
      icon.className = isVisible ? 'fa-solid fa-chevron-down' : 'fa-solid fa-chevron-up';
    }
  };
});
