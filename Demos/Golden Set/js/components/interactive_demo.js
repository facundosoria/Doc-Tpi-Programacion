/**
 * INTERACTIVE DEMO COMPONENT (SECCIÓN 2)
 * Laboratorio Docente de Calibración con IA Real Multi-Proveedor.
 * Soporta Google Gemini, OpenAI, Groq, OpenRouter, Anthropic Claude y Ollama/Local.
 * Calibración Neutral de Cátedra (RF-IA-30b) sobre las 5 Rúbricas Fijas (RF-IA-15).
 */

class InteractiveDemo {
    constructor(containerId, topicsData = null, casesData = null, rubricData = null) {
        this.container = document.getElementById(containerId);
        this.topics = topicsData || (typeof GOLDEN_SET_TOPICS !== 'undefined' ? GOLDEN_SET_TOPICS : []);
        this.cases = casesData || (typeof GOLDEN_SET_CASES !== 'undefined' ? GOLDEN_SET_CASES : []);
        this.rubric = rubricData || (typeof RUBRIC_CONFIG !== 'undefined' ? RUBRIC_CONFIG : {});
        this.presets = typeof CATEDRA_CALIBRATION_PRESETS !== 'undefined' ? CATEDRA_CALIBRATION_PRESETS : {};
        
        this.aiService = new AIService();
        this.scoringEngine = new ScoringEngine();
        
        this.selectedTopicId = "ALL";
        this.selectedCaseId = this.cases[0]?.id || "GS-01";
        this.evaluationsState = {};
        this.isEvaluatingSingle = false;
        this.isEvaluatingBatch = false;
        this.isLoadingModels = false;
        this.availableModels = [];
        
        // Estado de Calibración Docente (RF-IA-30b) con persistencia
        const savedDirectives = localStorage.getItem('catedra_custom_directives');
        if (savedDirectives) {
            try {
                this.currentDirectives = JSON.parse(savedDirectives);
                this.activePresetId = localStorage.getItem('catedra_active_preset') || 'PERSONALIZADO';
            } catch (e) {
                this.activePresetId = "ESTANDAR_UTN";
                this.currentDirectives = { ...this.presets.ESTANDAR_UTN?.directivas };
            }
        } else {
            this.activePresetId = "ESTANDAR_UTN";
            this.currentDirectives = { ...this.presets.ESTANDAR_UTN?.directivas };
        }
        this.isCalibrationPanelOpen = true; // Abierto por defecto para visibilidad docente
    }

    async init() {
        if (!this.container) return;
        this.render();
        this.updateCaseDetail();
        this.renderBatchSummary();
        await this.loadModels(false);
    }

    getSelectedCase() {
        const filtered = this.getFilteredCases();
        const foundInFiltered = filtered.find(c => c.id === this.selectedCaseId);
        if (foundInFiltered) return foundInFiltered;
        if (filtered.length > 0) {
            this.selectedCaseId = filtered[0].id;
            return filtered[0];
        }
        return this.cases.find(c => c.id === this.selectedCaseId) || this.cases[0];
    }

    getFilteredCases() {
        if (this.selectedTopicId === "ALL") return this.cases;
        return this.cases.filter(c => c.topic_id === this.selectedTopicId);
    }

    filterByTopic(topicId) {
        this.selectedTopicId = topicId;

        // 1. Actualizar estilos visuales de botones de filtro
        if (this.container) {
            this.container.querySelectorAll('.topic-filter-btn').forEach(btn => {
                const btnTopic = btn.getAttribute('data-topic');
                if (btnTopic === topicId) {
                    btn.className = 'topic-filter-btn px-3 py-1.5 rounded-xl text-xs font-medium border transition bg-cyan-950 text-cyan-300 border-cyan-500/50 ring-1 ring-cyan-500/30';
                } else {
                    btn.className = 'topic-filter-btn px-3 py-1.5 rounded-xl text-xs font-medium border transition bg-slate-950 text-slate-400 border-slate-800 hover:border-slate-700';
                }
            });
        }

        // 2. Sincronizar caso seleccionado con la lista filtrada
        const filtered = this.getFilteredCases();
        if (!filtered.some(c => c.id === this.selectedCaseId)) {
            this.selectedCaseId = filtered[0]?.id || "GS-01";
        }

        // 3. Actualizar contador de casos en el título
        const countBadge = document.getElementById('cases-count-badge');
        if (countBadge) {
            countBadge.textContent = filtered.length;
        }

        // 4. Actualizar lista de casos y vista de detalle
        this.renderCasesList();
        this.updateCaseDetail();
    }

    setPreset(presetId) {
        if (!this.presets[presetId]) return;
        this.activePresetId = presetId;
        this.currentDirectives = { ...this.presets[presetId].directivas };

        localStorage.setItem('catedra_custom_directives', JSON.stringify(this.currentDirectives));
        localStorage.setItem('catedra_active_preset', presetId);

        // Actualizar directamente textareas
        const d1 = document.getElementById('directive-autonomia');
        const d2 = document.getElementById('directive-claridad');
        const d3 = document.getElementById('directive-progresion');
        const d4 = document.getElementById('directive-cumplimiento');
        const d5 = document.getElementById('directive-eficiencia');

        if (d1) d1.value = this.currentDirectives.autonomia || '';
        if (d2) d2.value = this.currentDirectives.claridad || '';
        if (d3) d3.value = this.currentDirectives.progresion || '';
        if (d4) d4.value = this.currentDirectives.cumplimiento || '';
        if (d5) d5.value = this.currentDirectives.eficiencia || '';

        // Actualizar botones de preset
        if (this.container) {
            this.container.querySelectorAll('.preset-btn').forEach(btn => {
                const pid = btn.getAttribute('data-preset');
                if (pid === presetId) {
                    btn.className = 'preset-btn px-2.5 py-1 rounded-lg transition bg-purple-600 text-white font-bold shadow-md shadow-purple-600/30';
                } else {
                    btn.className = 'preset-btn px-2.5 py-1 rounded-lg transition text-slate-400 hover:text-white';
                }
            });
        }

        // Actualizar etiqueta del perfil activo
        const activeLabel = document.getElementById('active-preset-label');
        if (activeLabel) {
            activeLabel.textContent = this.presets[presetId]?.nombre || 'Personalizado';
        }

        this.updateCaseDetail();
    }

    resetToDefaultPreset() {
        this.setPreset('ESTANDAR_UTN');
        alert("✅ Enfoque de cátedra restablecido al Estándar UTN FRC.");
    }

    toggleCalibrationPanel() {
        this.isCalibrationPanelOpen = !this.isCalibrationPanelOpen;
        const panel = document.getElementById('teacher-calibration-panel');
        const toggleBtn = document.getElementById('toggle-calibration-panel-btn');

        if (panel) {
            if (this.isCalibrationPanelOpen) {
                panel.classList.remove('hidden');
            } else {
                panel.classList.add('hidden');
            }
        }

        if (toggleBtn) {
            toggleBtn.className = `px-3.5 py-2 rounded-xl text-xs font-bold border transition flex items-center gap-2 ${this.isCalibrationPanelOpen ? 'bg-purple-950 text-purple-300 border-purple-500/50 ring-1 ring-purple-500/30' : 'bg-slate-950 text-slate-300 border-slate-800 hover:border-slate-700'}`;
            const chevron = toggleBtn.querySelector('.chevron-icon');
            if (chevron) {
                chevron.setAttribute('data-lucide', this.isCalibrationPanelOpen ? 'chevron-up' : 'chevron-down');
                if (window.lucide) lucide.createIcons();
            }
        }
    }

    async loadModels(showFeedback = true) {
        const modelSelect = document.getElementById('llm-model-select');
        const refreshBtn = document.getElementById('refresh-models-btn');
        const statusBadge = document.getElementById('api-status-badge');
        const currentProvider = this.aiService.getProvider();
        const effectiveProvider = this.aiService.getEffectiveProvider();
        const provName = this.aiService.getProviderDisplayName(effectiveProvider);

        this.isLoadingModels = true;
        if (refreshBtn) {
            refreshBtn.innerHTML = `<i data-lucide="loader-2" class="w-3.5 h-3.5 animate-spin text-cyan-400"></i>`;
            if (window.lucide) lucide.createIcons();
        }

        if (modelSelect) {
            modelSelect.disabled = true;
            modelSelect.innerHTML = `<option value="">⏳ Cargando modelos disponibles de ${provName}...</option>`;
        }

        if (statusBadge) {
            statusBadge.className = "text-xs font-mono px-2.5 py-1 rounded-full bg-cyan-500/10 text-cyan-300 border border-cyan-500/30";
            statusBadge.innerHTML = `<i data-lucide="loader-2" class="w-3 h-3 inline mr-1 animate-spin"></i> Consultando modelos (${provName})...`;
            if (window.lucide) lucide.createIcons();
        }

        try {
            const res = await this.aiService.fetchAvailableModels();
            this.availableModels = res.models || [];
            
            if (modelSelect) {
                modelSelect.disabled = false;
                if (this.availableModels.length === 0) {
                    modelSelect.innerHTML = `<option value="">No se encontraron modelos</option>`;
                } else {
                    const currentSelected = this.aiService.getModel();
                    const optionsHtml = this.availableModels.map(m => {
                        const isSelected = m.id === currentSelected;
                        const star = m.isRecommended ? '⭐ ' : '';
                        return `<option value="${m.id}" ${isSelected ? 'selected' : ''}>${star}${m.name} (${m.id})</option>`;
                    }).join('');
                    modelSelect.innerHTML = optionsHtml;

                    // Si el modelo guardado no está en la lista, seleccionar el primer recomendado o el primero
                    if (!this.availableModels.some(m => m.id === currentSelected)) {
                        const rec = this.availableModels.find(m => m.isRecommended) || this.availableModels[0];
                        if (rec) {
                            this.aiService.setModel(rec.id);
                            modelSelect.value = rec.id;
                        }
                    }
                }
            }

            const hasKey = this.aiService.hasApiKey();
            if (statusBadge) {
                if (hasKey) {
                    statusBadge.className = "text-xs font-mono px-2.5 py-1 rounded-full bg-emerald-500/15 text-emerald-300 border border-emerald-500/30";
                    statusBadge.innerHTML = `<i data-lucide="check-circle" class="w-3 h-3 inline mr-1 text-emerald-400"></i> ${provName} · <strong>${this.availableModels.length}</strong> modelos listos`;
                } else {
                    statusBadge.className = "text-xs font-mono px-2.5 py-1 rounded-full bg-amber-500/15 text-amber-300 border border-amber-500/30";
                    statusBadge.innerHTML = `<i data-lucide="key" class="w-3 h-3 inline mr-1 text-amber-400"></i> Ingrese API Key para ${provName}`;
                }
                if (window.lucide) lucide.createIcons();
            }

        } catch (err) {
            console.error("Error al cargar modelos:", err);
            if (statusBadge) {
                statusBadge.className = "text-xs font-mono px-2.5 py-1 rounded-full bg-rose-500/15 text-rose-300 border border-rose-500/30";
                statusBadge.innerHTML = `<i data-lucide="alert-triangle" class="w-3 h-3 inline mr-1 text-rose-400"></i> Error al consultar API de ${provName}`;
                if (window.lucide) lucide.createIcons();
            }
        } finally {
            this.isLoadingModels = false;
            if (refreshBtn) {
                refreshBtn.innerHTML = `<i data-lucide="refresh-cw" class="w-3.5 h-3.5"></i>`;
                if (window.lucide) lucide.createIcons();
            }
        }
    }

    async onProviderSelected(prov) {
        this.aiService.setProvider(prov);
        const baseUrlContainer = document.getElementById('llm-base-url-container');
        if (baseUrlContainer) {
            if (prov === 'ollama' || prov === 'custom') {
                baseUrlContainer.classList.remove('hidden');
            } else {
                baseUrlContainer.classList.add('hidden');
            }
        }
        await this.loadModels(true);
    }

    async onApiKeyInput(key) {
        this.aiService.setApiKey(key);
        const provSelect = document.getElementById('llm-provider-select');
        
        // Si está en modo Auto-detectar, reflejar el proveedor detectado
        if (this.aiService.getProvider() === 'auto') {
            const detected = this.aiService.detectProvider(key);
            const provName = this.aiService.getProviderDisplayName(detected);
            const detectedLabel = document.getElementById('detected-provider-label');
            if (detectedLabel) {
                detectedLabel.textContent = `(Detectado: ${provName})`;
            }
        }
        
        await this.loadModels(false);
    }

    saveApiKey(key) {
        this.aiService.setApiKey(key);
        const effProv = this.aiService.getEffectiveProvider();
        const provName = this.aiService.getProviderDisplayName(effProv);
        alert(key ? `✅ API Key guardada para ${provName}. Modelos sincronizados.` : "API Key borrada.");
        this.loadModels(true);
    }

    applyDirectives() {
        const d1 = document.getElementById('directive-autonomia')?.value.trim();
        const d2 = document.getElementById('directive-claridad')?.value.trim();
        const d3 = document.getElementById('directive-progresion')?.value.trim();
        const d4 = document.getElementById('directive-cumplimiento')?.value.trim();
        const d5 = document.getElementById('directive-eficiencia')?.value.trim();

        this.currentDirectives = {
            autonomia: d1 || this.currentDirectives.autonomia,
            claridad: d2 || this.currentDirectives.claridad,
            progresion: d3 || this.currentDirectives.progresion,
            cumplimiento: d4 || this.currentDirectives.cumplimiento,
            eficiencia: d5 || this.currentDirectives.eficiencia
        };

        this.activePresetId = 'PERSONALIZADO';
        localStorage.setItem('catedra_custom_directives', JSON.stringify(this.currentDirectives));
        localStorage.setItem('catedra_active_preset', 'PERSONALIZADO');

        const activeLabel = document.getElementById('active-preset-label');
        if (activeLabel) {
            activeLabel.textContent = 'Personalizado';
        }

        if (this.container) {
            this.container.querySelectorAll('.preset-btn').forEach(btn => {
                btn.className = 'preset-btn px-2.5 py-1 rounded-lg transition text-slate-400 hover:text-white';
            });
        }

        alert("✅ Enfoque de Cátedra guardado y aplicado. El Evaluador y Tutor IA utilizarán estas directivas en cada rúbrica.");
        this.updateCaseDetail();
    }

    render() {
        const currentProvider = this.aiService.getProvider();
        const effectiveProvider = this.aiService.getEffectiveProvider();
        const hasKey = this.aiService.hasApiKey();
        const isCustomOrLocal = currentProvider === 'ollama' || currentProvider === 'custom';

        this.container.innerHTML = `
            <div class="space-y-6">
                <!-- Barra de Configuración de Proveedor LLM Universal, API Key y Modelos Dinámicos -->
                <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-5 shadow-xl backdrop-blur-md space-y-4">
                    <div class="flex flex-wrap items-center justify-between gap-4">
                        <div>
                            <div class="flex items-center gap-2">
                                <span class="px-2.5 py-0.5 rounded-full text-xs font-mono font-semibold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                                    CALIBRADOR MULTI-LLM UNIVERSAL
                                </span>
                                <span id="api-status-badge" class="text-xs font-mono px-2.5 py-0.5 rounded-full ${hasKey ? 'bg-emerald-500/15 text-emerald-300 border border-emerald-500/30' : 'bg-amber-500/15 text-amber-300 border border-amber-500/30'}">
                                    <i data-lucide="${hasKey ? 'check' : 'key'}" class="w-3 h-3 inline mr-1"></i>
                                    ${hasKey ? 'API Key Configurada' : 'Modo Manual / Ingrese API Key'}
                                </span>
                            </div>
                            <h2 class="text-lg font-bold text-white mt-1 flex items-center gap-2">
                                <i data-lucide="cpu" class="w-5 h-5 text-emerald-400"></i>
                                Laboratorio Docente de Calibración Multi-Modelo - Programación III
                            </h2>
                        </div>

                        <!-- Botón de Calibración Lote Completo -->
                        <button id="run-batch-calibration-btn" class="px-4 py-2 bg-gradient-to-r from-cyan-600 to-indigo-600 hover:from-cyan-500 hover:to-indigo-500 text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5 shadow-lg shadow-indigo-500/20">
                            <i data-lucide="play-circle" class="w-4 h-4"></i>
                            Calibrar Lote Completo Golden Set (Batch)
                        </button>
                    </div>

                    <!-- Controles de Configuración Universal de LLM -->
                    <div class="grid grid-cols-1 md:grid-cols-12 gap-3 pt-2 border-t border-slate-800/80 items-center">
                        
                        <!-- 1. Selector de Proveedor (3 cols) -->
                        <div class="md:col-span-3">
                            <label class="block text-[11px] font-mono text-slate-400 mb-1 flex items-center justify-between">
                                <span>Proveedor de IA:</span>
                                <span id="detected-provider-label" class="text-cyan-400 text-[10px]"></span>
                            </label>
                            <select id="llm-provider-select" class="w-full bg-slate-950 border border-slate-800 text-xs font-mono text-cyan-300 px-3 py-2 rounded-xl focus:outline-none focus:border-cyan-500 transition cursor-pointer">
                                <option value="auto" ${currentProvider === 'auto' ? 'selected' : ''}>⚡ Auto-detectar por API Key</option>
                                <option value="gemini" ${currentProvider === 'gemini' ? 'selected' : ''}>Google Gemini (Oficial)</option>
                                <option value="openai" ${currentProvider === 'openai' ? 'selected' : ''}>OpenAI (GPT-4o, o1, o3)</option>
                                <option value="groq" ${currentProvider === 'groq' ? 'selected' : ''}>Groq (Llama 3.3, Mixtral - Ultra Rápido)</option>
                                <option value="openrouter" ${currentProvider === 'openrouter' ? 'selected' : ''}>OpenRouter (Universal Router)</option>
                                <option value="anthropic" ${currentProvider === 'anthropic' ? 'selected' : ''}>Anthropic Claude (3.7 / 3.5 Sonnet)</option>
                                <option value="ollama" ${currentProvider === 'ollama' ? 'selected' : ''}>Ollama / Local (localhost:11434)</option>
                                <option value="custom" ${currentProvider === 'custom' ? 'selected' : ''}>Endpoint Personalizado (Custom OpenAI)</option>
                            </select>
                        </div>

                        <!-- 2. Input de API Key (4 cols) -->
                        <div class="md:col-span-4">
                            <label class="block text-[11px] font-mono text-slate-400 mb-1">API Key / Token:</label>
                            <div class="flex items-center gap-1.5">
                                <input id="llm-api-key-input" type="password" placeholder="Pegar cualquier API Key (AIzaSy..., sk-..., gsk_...)..." 
                                    value="${this.aiService.getApiKey()}"
                                    class="w-full bg-slate-950 border border-slate-800 text-xs font-mono text-white px-3 py-2 rounded-xl focus:outline-none focus:border-emerald-500 transition" />
                                <button id="save-api-key-btn" title="Guardar Key" class="px-2.5 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-semibold transition shrink-0 shadow-lg shadow-emerald-600/20">
                                    <i data-lucide="save" class="w-3.5 h-3.5"></i>
                                </button>
                            </div>
                        </div>

                        <!-- 3. Selector Dinámico de Modelos (4 cols) -->
                        <div class="md:col-span-4">
                            <label class="block text-[11px] font-mono text-slate-400 mb-1 flex items-center justify-between">
                                <span>Modelo Disponible en Tiempo Real:</span>
                                <span class="text-[10px] text-slate-500 font-mono">⭐ Recomendados</span>
                            </label>
                            <div class="flex items-center gap-1.5">
                                <select id="llm-model-select" class="w-full bg-slate-950 border border-slate-800 text-xs font-mono text-emerald-300 px-3 py-2 rounded-xl focus:outline-none focus:border-emerald-500 transition cursor-pointer truncate">
                                    <option value="">Cargando modelos...</option>
                                </select>
                                <button id="refresh-models-btn" title="Consultar modelos disponibles en tiempo real" class="px-2.5 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-medium transition shrink-0">
                                    <i data-lucide="refresh-cw" class="w-3.5 h-3.5"></i>
                                </button>
                            </div>
                        </div>

                        <!-- 4. Botón Probar Conexión (1 col) -->
                        <div class="md:col-span-1 flex flex-col justify-end">
                            <label class="block text-[11px] font-mono text-transparent mb-1">.</label>
                            <button id="test-api-btn" title="Probar conexión con el modelo seleccionado" class="w-full py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded-xl text-xs font-medium transition flex items-center justify-center gap-1">
                                <i data-lucide="zap" class="w-3.5 h-3.5 text-amber-400"></i>
                            </button>
                        </div>
                    </div>

                    <!-- Campo Adicional para Endpoint Personalizado / Base URL (Ollama / Local) -->
                    <div id="llm-base-url-container" class="${isCustomOrLocal ? '' : 'hidden'} pt-2 border-t border-slate-800/60 flex items-center gap-3">
                        <label class="text-xs font-mono text-slate-400 shrink-0">URL Base / Endpoint:</label>
                        <input id="llm-base-url-input" type="text" placeholder="http://localhost:11434/v1" 
                            value="${this.aiService.getBaseUrl()}"
                            class="w-full bg-slate-950 border border-slate-800 text-xs font-mono text-cyan-300 px-3 py-1.5 rounded-xl focus:outline-none focus:border-cyan-500" />
                    </div>

                    <!-- Botón para Abrir/Cerrar Panel de Anclaje Docente -->
                    <div class="pt-2 border-t border-slate-800/80 flex flex-wrap items-center justify-between gap-3">
                        <div class="flex items-center gap-3">
                            <button id="toggle-calibration-panel-btn" class="px-3.5 py-2 rounded-xl text-xs font-bold border transition flex items-center gap-2 ${this.isCalibrationPanelOpen ? 'bg-purple-950 text-purple-300 border-purple-500/50 ring-1 ring-purple-500/30' : 'bg-slate-950 text-slate-300 border-slate-800 hover:border-slate-700'}">
                                <i data-lucide="wrench" class="w-4 h-4 text-purple-400"></i>
                                <span>Configurar Enfoque Personalizado por Rúbrica (RF-IA-30b)</span>
                                <i data-lucide="${this.isCalibrationPanelOpen ? 'chevron-up' : 'chevron-down'}" class="chevron-icon w-3.5 h-3.5 ml-1"></i>
                            </button>
                            <span class="text-xs font-mono text-slate-400">
                                Perfil Activo: <strong id="active-preset-label" class="text-purple-300">${this.presets[this.activePresetId]?.nombre || (this.activePresetId === 'PERSONALIZADO' ? 'Personalizado' : 'Estándar UTN FRC')}</strong>
                            </span>
                        </div>
                    </div>
                </div>

                <!-- PANEL DE CALIBRACIÓN DOCENTE Y CUADROS DE ENFOQUE POR RÚBRICA (RF-IA-30b) -->
                <div id="teacher-calibration-panel" class="${this.isCalibrationPanelOpen ? '' : 'hidden'} bg-slate-900 border border-purple-500/30 rounded-2xl p-5 shadow-2xl space-y-4">
                    <div class="flex flex-wrap items-center justify-between gap-4 pb-3 border-b border-slate-800">
                        <div>
                            <div class="flex items-center gap-2">
                                <span class="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-purple-500/20 text-purple-300 border border-purple-500/30">
                                    RF-IA-15 / RF-IA-30b / RF-IA-36
                                </span>
                                <h3 class="text-sm font-bold text-white">Enfoque y Criterio Pedagógico de Cátedra por Rúbrica</h3>
                            </div>
                            <p class="text-xs text-slate-400 mt-1">
                                🔒 <strong>Pesos fijos por diseño de plataforma (30/25/20/15/10%).</strong> Utilizá los cuadros de texto para indicarle al agente tutor/evaluador qué aspectos técnicos exigir, premiar o penalizar en tu materia.
                            </p>
                        </div>

                        <!-- Presets de Cátedra -->
                        <div class="flex items-center gap-2 bg-slate-950 p-1.5 rounded-xl border border-slate-800 text-xs">
                            <span class="text-slate-400 font-mono px-2">Presets Rápidos:</span>
                            <button class="preset-btn px-2.5 py-1 rounded-lg transition ${this.activePresetId === 'ESTANDAR_UTN' ? 'bg-purple-600 text-white font-bold shadow-md shadow-purple-600/30' : 'text-slate-400 hover:text-white'}" data-preset="ESTANDAR_UTN">
                                Estándar UTN
                            </button>
                            <button class="preset-btn px-2.5 py-1 rounded-lg transition ${this.activePresetId === 'ALTA_EXIGENCIA' ? 'bg-purple-600 text-white font-bold shadow-md shadow-purple-600/30' : 'text-slate-400 hover:text-white'}" data-preset="ALTA_EXIGENCIA">
                                Alta Exigencia
                            </button>
                            <button class="preset-btn px-2.5 py-1 rounded-lg transition ${this.activePresetId === 'FORMATIVO' ? 'bg-purple-600 text-white font-bold shadow-md shadow-purple-600/30' : 'text-slate-400 hover:text-white'}" data-preset="FORMATIVO">
                                Formativo
                            </button>
                        </div>
                    </div>

                    <!-- Grilla de los 5 Cuadros de Texto de Enfoque por Rúbrica -->
                    <div class="grid grid-cols-1 md:grid-cols-5 gap-3.5">
                        <!-- Rúbrica 1: Autonomía -->
                        <div class="p-3.5 bg-slate-950 rounded-xl border border-slate-800 flex flex-col justify-between space-y-2.5">
                            <div>
                                <div class="flex items-center justify-between text-xs font-mono mb-1">
                                    <span class="text-emerald-400 font-bold">1. Autonomía</span>
                                    <span class="text-[10px] px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-300 font-bold border border-emerald-500/30" title="Peso fijo inalterable por plataforma">🔒 30%</span>
                                </div>
                                <p class="text-[10px] text-slate-400 leading-tight mb-2">
                                    Mide si el alumno investiga antes de consultar, cuestiona y no delega pasivamente la solución.
                                </p>
                                <label class="text-[11px] font-semibold text-purple-300 flex items-center gap-1 mb-1">
                                    <i data-lucide="edit-3" class="w-3 h-3 text-purple-400"></i> Enfoque Personalizado:
                                </label>
                            </div>
                            <textarea id="directive-autonomia" placeholder="Ej: Exigir formulación de hipótesis técnicas y trade-offs de concurrencia..." class="w-full h-28 bg-slate-900 border border-slate-800 text-[11px] text-slate-200 p-2.5 rounded-lg focus:outline-none focus:border-purple-500 font-sans resize-none leading-relaxed">${this.currentDirectives.autonomia || ''}</textarea>
                        </div>

                        <!-- Rúbrica 2: Claridad -->
                        <div class="p-3.5 bg-slate-950 rounded-xl border border-slate-800 flex flex-col justify-between space-y-2.5">
                            <div>
                                <div class="flex items-center justify-between text-xs font-mono mb-1">
                                    <span class="text-sky-400 font-bold">2. Claridad</span>
                                    <span class="text-[10px] px-2 py-0.5 rounded bg-sky-500/20 text-sky-300 font-bold border border-sky-500/30" title="Peso fijo inalterable por plataforma">🔒 25%</span>
                                </div>
                                <p class="text-[10px] text-slate-400 leading-tight mb-2">
                                    Formulación de preguntas precisas con código, stack traces y síntomas concretos.
                                </p>
                                <label class="text-[11px] font-semibold text-purple-300 flex items-center gap-1 mb-1">
                                    <i data-lucide="edit-3" class="w-3 h-3 text-purple-400"></i> Enfoque Personalizado:
                                </label>
                            </div>
                            <textarea id="directive-claridad" placeholder="Ej: Exigir que adjunte fragmento de código y stack trace exacto..." class="w-full h-28 bg-slate-900 border border-slate-800 text-[11px] text-slate-200 p-2.5 rounded-lg focus:outline-none focus:border-purple-500 font-sans resize-none leading-relaxed">${this.currentDirectives.claridad || ''}</textarea>
                        </div>

                        <!-- Rúbrica 3: Progresión -->
                        <div class="p-3.5 bg-slate-950 rounded-xl border border-slate-800 flex flex-col justify-between space-y-2.5">
                            <div>
                                <div class="flex items-center justify-between text-xs font-mono mb-1">
                                    <span class="text-indigo-400 font-bold">3. Progresión</span>
                                    <span class="text-[10px] px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 font-bold border border-indigo-500/30" title="Peso fijo inalterable por plataforma">🔒 20%</span>
                                </div>
                                <p class="text-[10px] text-slate-400 leading-tight mb-2">
                                    Construcción acumulativa sobre las pistas socráticas e iteración lógica entre turnos.
                                </p>
                                <label class="text-[11px] font-semibold text-purple-300 flex items-center gap-1 mb-1">
                                    <i data-lucide="edit-3" class="w-3 h-3 text-purple-400"></i> Enfoque Personalizado:
                                </label>
                            </div>
                            <textarea id="directive-progresion" placeholder="Ej: Exigir reporte de qué ocurrió al probar la pista socrática..." class="w-full h-28 bg-slate-900 border border-slate-800 text-[11px] text-slate-200 p-2.5 rounded-lg focus:outline-none focus:border-purple-500 font-sans resize-none leading-relaxed">${this.currentDirectives.progresion || ''}</textarea>
                        </div>

                        <!-- Rúbrica 4: Cumplimiento -->
                        <div class="p-3.5 bg-slate-950 rounded-xl border border-slate-800 flex flex-col justify-between space-y-2.5">
                            <div>
                                <div class="flex items-center justify-between text-xs font-mono mb-1">
                                    <span class="text-amber-400 font-bold">4. Ética y Límites</span>
                                    <span class="text-[10px] px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 font-bold border border-amber-500/30" title="Peso fijo inalterable por plataforma">🔒 15%</span>
                                </div>
                                <p class="text-[10px] text-slate-400 leading-tight mb-2">
                                    Respeto a las reglas pedagógicas; penaliza pedidos directos de código o manipulaciones.
                                </p>
                                <label class="text-[11px] font-semibold text-purple-300 flex items-center gap-1 mb-1">
                                    <i data-lucide="edit-3" class="w-3 h-3 text-purple-400"></i> Enfoque Personalizado:
                                </label>
                            </div>
                            <textarea id="directive-cumplimiento" placeholder="Ej: Cero tolerancia ante pedidos de código resuelto o evasiones..." class="w-full h-28 bg-slate-900 border border-slate-800 text-[11px] text-slate-200 p-2.5 rounded-lg focus:outline-none focus:border-purple-500 font-sans resize-none leading-relaxed">${this.currentDirectives.cumplimiento || ''}</textarea>
                        </div>

                        <!-- Rúbrica 5: Eficiencia -->
                        <div class="p-3.5 bg-slate-950 rounded-xl border border-slate-800 flex flex-col justify-between space-y-2.5">
                            <div>
                                <div class="flex items-center justify-between text-xs font-mono mb-1">
                                    <span class="text-rose-400 font-bold">5. Eficiencia</span>
                                    <span class="text-[10px] px-2 py-0.5 rounded bg-rose-500/20 text-rose-300 font-bold border border-rose-500/30" title="Peso fijo inalterable por plataforma">🔒 10%</span>
                                </div>
                                <p class="text-[10px] text-slate-400 leading-tight mb-2">
                                    Densidad técnica por turno. Penaliza ráfagas de mensajes vacíos o triviales.
                                </p>
                                <label class="text-[11px] font-semibold text-purple-300 flex items-center gap-1 mb-1">
                                    <i data-lucide="edit-3" class="w-3 h-3 text-purple-400"></i> Enfoque Personalizado:
                                </label>
                            </div>
                            <textarea id="directive-eficiencia" placeholder="Ej: Priorizar densidad técnica; penalizar ráfagas monosilábicas..." class="w-full h-28 bg-slate-900 border border-slate-800 text-[11px] text-slate-200 p-2.5 rounded-lg focus:outline-none focus:border-purple-500 font-sans resize-none leading-relaxed">${this.currentDirectives.eficiencia || ''}</textarea>
                        </div>
                    </div>

                    <!-- Botones de Acción del Panel de Enfoque -->
                    <div class="flex flex-wrap items-center justify-between gap-3 pt-3 border-t border-slate-800/80">
                        <span class="text-[11px] text-slate-500 font-mono">
                            <i data-lucide="info" class="w-3.5 h-3.5 inline text-purple-400 mr-1"></i>
                            El enfoque modula el juicio del LLM manteniendo idénticas las ponderaciones de plataforma.
                        </span>
                        <div class="flex items-center gap-2">
                            <button id="reset-directives-btn" class="px-3 py-2 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-xl text-xs font-semibold transition flex items-center gap-1.5">
                                <i data-lucide="rotate-ccw" class="w-3.5 h-3.5"></i>
                                Restablecer al Estándar UTN
                            </button>
                            <button id="apply-directives-btn" class="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5 shadow-lg shadow-purple-600/20">
                                <i data-lucide="check" class="w-4 h-4"></i>
                                Guardar y Aplicar Enfoque de Cátedra
                            </button>
                        </div>
                    </div>
                </div>

                <!-- Filtro de Temas de Programación 3 -->
                <div class="flex items-center gap-2 flex-wrap bg-slate-900/60 p-3 rounded-2xl border border-slate-800/80">
                    <span class="text-xs font-mono text-slate-400 mr-1">Filtrar por Tema:</span>
                    <button class="topic-filter-btn px-3 py-1.5 rounded-xl text-xs font-medium border transition ${this.selectedTopicId === 'ALL' ? 'bg-cyan-950 text-cyan-300 border-cyan-500/50 ring-1 ring-cyan-500/30' : 'bg-slate-950 text-slate-400 border-slate-800 hover:border-slate-700'}" data-topic="ALL">
                        Todos los Casos (10)
                    </button>
                    ${this.topics.map(t => `
                        <button class="topic-filter-btn px-3 py-1.5 rounded-xl text-xs font-medium border transition ${this.selectedTopicId === t.id ? 'bg-cyan-950 text-cyan-300 border-cyan-500/50 ring-1 ring-cyan-500/30' : 'bg-slate-950 text-slate-400 border-slate-800 hover:border-slate-700'}" data-topic="${t.id}">
                            <i data-lucide="${t.icon || 'book'}" class="w-3.5 h-3.5 inline mr-1"></i>
                            ${t.id}: ${t.nombre.split(':')[0]}
                        </button>
                    `).join('')}
                </div>

                <!-- Grid Principal: Lista de Casos y Detalle de Evaluación -->
                <div class="grid grid-cols-1 lg:grid-cols-12 gap-6">
                    <!-- Columna Izquierda: Casos de Estudiantes (4 cols) -->
                    <div class="lg:col-span-4 space-y-4">
                        <div class="bg-slate-900 border border-slate-800 rounded-2xl p-4">
                            <div class="flex items-center justify-between mb-3">
                                <h3 class="text-xs font-bold text-slate-300 uppercase tracking-wider font-mono">
                                    Muestrario de Exámenes (<span id="cases-count-badge">${this.getFilteredCases().length}</span>)
                                </h3>
                                <span class="text-[10px] text-slate-500 font-mono">Seleccione uno</span>
                            </div>

                            <div id="interactive-cases-list" class="space-y-2.5 max-h-[580px] overflow-y-auto pr-1">
                                <!-- Inyectado dinámicamente -->
                            </div>
                        </div>
                    </div>

                    <!-- Columna Derecha: Detalle del Caso, Transcripción e Inferencia IA (8 cols) -->
                    <div class="lg:col-span-8 space-y-6">
                        <div id="case-detail-container">
                            <!-- Detalle inyectado dinámicamente -->
                        </div>
                    </div>
                </div>

                <!-- Panel Inferior: Resumen Estadístico de Calibración Batch y Disyuntor PAR-14 -->
                <div id="batch-summary-panel" class="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-2xl">
                    <!-- Inyectado dinámicamente -->
                </div>
            </div>
        `;

        if (window.lucide) lucide.createIcons();
        this.renderCasesList();
        this.attachEventListeners();
    }

    renderCasesList() {
        const list = document.getElementById('interactive-cases-list');
        if (!list) return;

        const filtered = this.getFilteredCases();
        if (filtered.length === 0) {
            list.innerHTML = `<div class="text-xs text-slate-500 font-mono text-center py-6">No hay exámenes en este tema.</div>`;
            return;
        }

        list.innerHTML = filtered.map(c => {
            const isSelected = c.id === this.selectedCaseId;
            const evalResult = this.evaluationsState[c.id];
            const hasEval = Boolean(evalResult);

            let statusBadge = `<span class="px-1.5 py-0.5 rounded text-[10px] font-mono bg-slate-800 text-slate-400">Sin evaluar</span>`;
            if (hasEval) {
                const desvio = this.scoringEngine.calculateDeviation(evalResult.score_total, c.score_docente.total);
                const isOk = desvio <= 5.0;
                statusBadge = `<span class="px-1.5 py-0.5 rounded text-[10px] font-mono font-bold ${isOk ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/20 text-rose-400 border border-rose-500/30'}">Δ ${desvio.toFixed(1)} pts</span>`;
            }

            return `
                <button class="case-item-btn w-full text-left p-3 rounded-xl border transition flex flex-col gap-1.5 ${isSelected ? 'bg-cyan-950/60 border-cyan-500/50 shadow-lg shadow-cyan-500/10 ring-1 ring-cyan-500/30' : 'bg-slate-950/70 border-slate-800/80 hover:border-slate-700'}" data-case-id="${c.id}">
                    <div class="flex items-center justify-between">
                        <span class="font-bold text-xs ${isSelected ? 'text-cyan-300' : 'text-white'} font-mono">${c.id}: ${c.estudiante.split('(')[0]}</span>
                        ${statusBadge}
                    </div>
                    <div class="text-[11px] text-slate-400 truncate">${c.perfil}</div>
                    <div class="flex items-center justify-between pt-1 mt-0.5 border-t border-slate-800/60 text-[10px] font-mono">
                        <span class="text-slate-500">${c.topic_id}</span>
                        <span class="text-cyan-400 font-bold">Doc: ${c.score_docente.total} pts</span>
                    </div>
                </button>
            `;
        }).join('');

        list.querySelectorAll('.case-item-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                this.selectedCaseId = btn.getAttribute('data-case-id');
                this.renderCasesList();
                this.updateCaseDetail();
            });
        });
    }

    updateCaseDetail() {
        const container = document.getElementById('case-detail-container');
        if (!container) return;

        const activeCase = this.getSelectedCase();
        if (!activeCase) {
            container.innerHTML = `<div class="text-xs text-slate-500 font-mono text-center py-12">Selecciona un examen para visualizar su detalle.</div>`;
            return;
        }

        const evalResult = this.evaluationsState[activeCase.id];
        const hasEval = Boolean(evalResult);
        const currentModel = this.aiService.getModel();
        const effectiveProv = this.aiService.getEffectiveProvider();
        const provName = this.aiService.getProviderDisplayName(effectiveProv);

        container.innerHTML = `
            <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl space-y-5">
                <!-- Header del Caso -->
                <div class="flex flex-wrap items-start justify-between gap-4 pb-4 border-b border-slate-800">
                    <div>
                        <div class="flex items-center gap-2 mb-1">
                            <span class="text-xs font-mono font-bold px-2.5 py-0.5 rounded bg-cyan-500/20 text-cyan-400 border border-cyan-500/30">
                                ${activeCase.id}
                            </span>
                            <span class="text-xs text-slate-400 font-mono">${activeCase.topic_id}</span>
                        </div>
                        <h3 class="text-base font-bold text-white">${activeCase.estudiante} - <span class="text-slate-400 font-normal text-xs">${activeCase.perfil}</span></h3>
                        <p class="text-xs text-slate-400 mt-0.5">${activeCase.consigna}</p>
                    </div>

                    <!-- Botones de Acción -->
                    <div class="flex items-center gap-2">
                        <button id="evaluate-single-case-btn" class="px-4 py-2 bg-cyan-600 hover:bg-cyan-500 disabled:opacity-50 text-white rounded-xl text-xs font-bold transition flex items-center gap-1.5 shadow-lg shadow-cyan-600/20" ${this.isEvaluatingSingle ? 'disabled' : ''}>
                            <i data-lucide="${this.isEvaluatingSingle ? 'loader-2' : 'sparkles'}" class="w-4 h-4 ${this.isEvaluatingSingle ? 'animate-spin' : ''}"></i>
                            ${this.isEvaluatingSingle ? `Evaluando con ${currentModel}...` : `Evaluar con ${provName}`}
                        </button>
                    </div>
                </div>

                <!-- Pestañas de Inspección: Transcripción / Código / Rúbrica Docente -->
                <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <!-- Transcripción Forense -->
                    <div class="space-y-2">
                        <div class="text-xs font-semibold text-slate-300 flex items-center justify-between">
                            <span class="flex items-center gap-1.5"><i data-lucide="message-square" class="w-3.5 h-3.5 text-cyan-400"></i> Diálogo Alumno ↔ Tutor IA</span>
                            <span class="text-[10px] font-mono text-slate-500">${activeCase.transcripcion.length} mensajes</span>
                        </div>
                        <div class="p-3 bg-slate-950 rounded-xl border border-slate-800 max-h-56 overflow-y-auto space-y-2.5 text-xs">
                            ${activeCase.transcripcion.map(t => `
                                <div class="p-2.5 rounded-lg ${t.emisor === 'ALUMNO' ? 'bg-slate-900 border border-slate-800 text-slate-200' : 'bg-cyan-950/30 border border-cyan-900/40 text-cyan-200'}">
                                    <div class="flex items-center justify-between text-[10px] font-mono font-bold ${t.emisor === 'ALUMNO' ? 'text-slate-400' : 'text-cyan-400'} mb-1">
                                        <span>${t.emisor}</span>
                                    </div>
                                    <div class="leading-relaxed select-text">${t.mensaje}</div>
                                </div>
                            `).join('')}
                        </div>
                    </div>

                    <!-- Código Fuente y Telemetría -->
                    <div class="space-y-2">
                        <div class="text-xs font-semibold text-slate-300 flex items-center justify-between">
                            <span class="flex items-center gap-1.5"><i data-lucide="code" class="w-3.5 h-3.5 text-emerald-400"></i> Código Fuente Java / Telemetría</span>
                            <span class="text-[10px] font-mono text-slate-500">${activeCase.telemetria.ediciones_antes_primer_mensaje} ediciones previas</span>
                        </div>
                        <div class="p-3 bg-slate-950 rounded-xl border border-slate-800 max-h-56 overflow-y-auto font-mono text-[11px] text-slate-300 select-text">
                            <pre class="text-emerald-400/90 leading-tight"><code>${activeCase.codigo_alumno}</code></pre>
                        </div>
                    </div>
                </div>

                <!-- Comparativa 5D y Radar Chart -->
                <div class="bg-slate-950 p-4 rounded-xl border border-slate-800 space-y-4">
                    <div class="flex flex-wrap items-center justify-between gap-2 border-b border-slate-800/80 pb-3">
                        <h4 class="text-xs font-bold text-slate-200 uppercase font-mono flex items-center gap-2">
                            <i data-lucide="radar" class="w-4 h-4 text-purple-400"></i>
                            Desglose de Calificación en 5 Rúbricas Fijas (RF-IA-15)
                        </h4>
                        <div class="flex items-center gap-4 text-xs font-mono">
                            <span class="flex items-center gap-1.5"><span class="w-3 h-3 rounded bg-sky-400 inline-block"></span> Patrón Docente (${activeCase.score_docente.total} pts)</span>
                            <span class="flex items-center gap-1.5"><span class="w-3 h-3 rounded bg-purple-400 inline-block"></span> Evaluador IA (${hasEval ? evalResult.score_total + ' pts' : 'Pendiente'})</span>
                        </div>
                    </div>

                    <div class="grid grid-cols-1 md:grid-cols-12 gap-6 items-center">
                        <!-- Canvas Radar Chart (5 cols) -->
                        <div class="md:col-span-5 flex flex-col items-center justify-center">
                            <canvas id="interactive-radar-canvas" width="280" height="240" class="max-w-full"></canvas>
                        </div>

                        <!-- Barras de Dimensiones y Justificaciones (7 cols) -->
                        <div class="md:col-span-7 space-y-2.5">
                            ${this.rubric.dimensiones.map(dim => {
                                const docVal = activeCase.score_docente[dim.id] || 0;
                                const iaVal = hasEval ? evalResult.dimensiones[dim.id]?.puntaje : null;
                                const iaJust = hasEval ? evalResult.dimensiones[dim.id]?.justificacion : '';
                                const customFocus = this.currentDirectives[dim.id] || '';

                                return `
                                    <div class="p-2.5 bg-slate-900 rounded-lg border border-slate-800 text-xs">
                                        <div class="flex items-center justify-between font-mono mb-1">
                                            <span class="text-slate-300 font-semibold">${dim.nombre} (${dim.porcentaje})</span>
                                            <div class="space-x-2">
                                                <span class="text-sky-400">Doc: <strong>${docVal}</strong></span>
                                                <span class="${hasEval ? 'text-purple-400 font-bold' : 'text-slate-600'}">IA: <strong>${hasEval ? iaVal : '-'}</strong></span>
                                            </div>
                                        </div>
                                        <div class="relative w-full h-1.5 bg-slate-800 rounded-full overflow-hidden">
                                            <div class="absolute top-0 bottom-0 bg-sky-500 opacity-60 rounded-full" style="width: ${docVal}%"></div>
                                            ${hasEval ? `<div class="absolute top-0 bottom-0 bg-purple-500 rounded-full" style="width: ${iaVal}%"></div>` : ''}
                                        </div>
                                        ${hasEval ? `<p class="text-[10px] text-slate-400 mt-1 italic">${iaJust}</p>` : ''}
                                        <div class="text-[10px] text-purple-300/80 mt-1.5 pt-1 border-t border-slate-800/60 flex items-start gap-1 font-mono">
                                            <i data-lucide="compass" class="w-3 h-3 text-purple-400 shrink-0 mt-0.5"></i>
                                            <span class="truncate"><strong>Enfoque Cátedra:</strong> "${customFocus}"</span>
                                        </div>
                                    </div>
                                `;
                            }).join('')}
                        </div>
                    </div>

                    <!-- Diagnóstico de Desviación Individual -->
                    ${hasEval ? `
                        <div class="mt-4 pt-3 border-t border-slate-800/80 flex flex-wrap items-center justify-between text-xs font-mono">
                            <div class="flex items-center gap-2">
                                <span class="text-slate-400">Desvío Individual (Δ):</span>
                                <strong class="text-sm ${this.scoringEngine.calculateDeviation(evalResult.score_total, activeCase.score_docente.total) <= 5.0 ? 'text-emerald-400' : 'text-rose-400'}">
                                    ${this.scoringEngine.calculateDeviation(evalResult.score_total, activeCase.score_docente.total).toFixed(2)} pts
                                </strong>
                                <span class="px-2 py-0.5 rounded text-[10px] ${this.scoringEngine.calculateDeviation(evalResult.score_total, activeCase.score_docente.total) <= 5.0 ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/20 text-rose-400 border border-rose-500/30'}">
                                    ${this.scoringEngine.calculateDeviation(evalResult.score_total, activeCase.score_docente.total) <= 5.0 ? 'Dentro de Tolerancia (<= 5.0)' : 'Fuera de Tolerancia (> 5.0)'}
                                </span>
                            </div>

                            <div class="flex items-center gap-3">
                                <span class="text-slate-400">Modelo: <strong class="text-cyan-300">${evalResult.modelo_usado || currentModel}</strong></span>
                                <span class="text-slate-400">Confianza: <strong class="text-white">${(evalResult.confidence_score * 100).toFixed(0)}%</strong></span>
                                <span class="text-slate-400">Latencia: <strong class="text-cyan-400">${evalResult.tiempo_ms} ms</strong></span>
                                ${evalResult.requiere_auditoria_humana ? `<span class="px-2 py-0.5 rounded text-[10px] bg-amber-500/20 text-amber-400 border border-amber-500/30">Auditoría Requerida (PAR-10)</span>` : ''}
                            </div>
                        </div>
                    ` : `
                        <div class="text-center py-2 text-xs font-mono text-slate-500">
                            Presiona "Evaluar con ${provName}" para obtener el juicio del LLM calibrado con tus directivas de cátedra.
                        </div>
                    `}
                </div>
            </div>
        `;

        if (window.lucide) lucide.createIcons();

        GoldenSetCharts.drawRadarChart(
            'interactive-radar-canvas',
            activeCase.score_docente,
            evalResult?.dimensiones || null
        );

        const evalBtn = document.getElementById('evaluate-single-case-btn');
        if (evalBtn) {
            evalBtn.addEventListener('click', () => this.evaluateSingleCase(activeCase));
        }
    }

    async evaluateSingleCase(caseItem) {
        this.isEvaluatingSingle = true;
        this.updateCaseDetail();

        try {
            let result;
            if (this.aiService.hasApiKey()) {
                result = await this.aiService.evaluateCase(caseItem, this.currentDirectives);
            } else {
                await new Promise(r => setTimeout(r, 600));
                result = this.scoringEngine.simulateCalibratedEvaluation(caseItem);
            }

            this.evaluationsState[caseItem.id] = result;
            this.isEvaluatingSingle = false;
            this.renderCasesList();
            this.updateCaseDetail();
            this.renderBatchSummary();
        } catch (error) {
            this.isEvaluatingSingle = false;
            this.updateCaseDetail();
            alert(`Error al evaluar el caso con el modelo de IA:\n${error.message}`);
        }
    }

    async runBatchCalibration() {
        const targetCases = this.getFilteredCases();
        if (targetCases.length === 0) return;

        this.isEvaluatingBatch = true;
        const batchBtn = document.getElementById('run-batch-calibration-btn');
        const currentModel = this.aiService.getModel();
        
        if (batchBtn) {
            batchBtn.innerHTML = `<i data-lucide="loader-2" class="w-4 h-4 animate-spin inline mr-1"></i> Calibrando Lote (0/${targetCases.length})...`;
            if (window.lucide) lucide.createIcons();
        }

        for (let i = 0; i < targetCases.length; i++) {
            const c = targetCases[i];
            if (batchBtn) {
                batchBtn.innerHTML = `<i data-lucide="loader-2" class="w-4 h-4 animate-spin inline mr-1"></i> Calibrando ${i + 1}/${targetCases.length} (${c.id})...`;
                if (window.lucide) lucide.createIcons();
            }

            try {
                let evalRes;
                if (this.aiService.hasApiKey()) {
                    evalRes = await this.aiService.evaluateCase(c, this.currentDirectives);
                } else {
                    await new Promise(r => setTimeout(r, 200));
                    evalRes = this.scoringEngine.simulateCalibratedEvaluation(c);
                }
                this.evaluationsState[c.id] = evalRes;
            } catch (err) {
                console.error(`Error en caso ${c.id}:`, err);
            }
        }

        this.isEvaluatingBatch = false;
        if (batchBtn) {
            batchBtn.innerHTML = `<i data-lucide="play-circle" class="w-4 h-4 inline mr-1"></i> Calibrar Lote Completo Golden Set (Batch)`;
            if (window.lucide) lucide.createIcons();
        }

        this.renderCasesList();
        this.updateCaseDetail();
        this.renderBatchSummary();
    }

    renderBatchSummary() {
        const panel = document.getElementById('batch-summary-panel');
        if (!panel) return;

        const evaluatedItems = this.cases.filter(c => this.evaluationsState[c.id]).map(c => ({
            ...c,
            score_ia: this.evaluationsState[c.id].score_total,
            score_ia_dimensiones: this.evaluationsState[c.id].dimensiones
        }));

        const totalEvaluated = evaluatedItems.length;
        const calibResult = this.scoringEngine.evaluateCalibrationRun(evaluatedItems);

        panel.innerHTML = `
            <div class="flex flex-wrap items-center justify-between gap-4 mb-4">
                <div>
                    <h3 class="text-sm font-bold text-white flex items-center gap-2">
                        <i data-lucide="check-circle" class="w-4 h-4 text-emerald-400"></i>
                        Resultado de Calibración de Cátedra (PAR-14)
                    </h3>
                    <p class="text-xs text-slate-400">Progreso del Lote: <strong>${totalEvaluated} de ${this.cases.length} exámenes evaluados</strong> con <strong>${this.aiService.getModel()}</strong>.</p>
                </div>

                <div class="flex items-center gap-3">
                    <div class="text-right">
                        <div class="text-[10px] font-mono text-slate-400 uppercase">Error Absoluto Medio (MAE)</div>
                        <div class="text-xl font-mono font-bold ${calibResult.aprobado ? 'text-emerald-400' : 'text-rose-400'}">
                            ${totalEvaluated > 0 ? calibResult.mae.toFixed(2) + ' pts' : 'N/A'}
                        </div>
                    </div>

                    <span class="px-3 py-1.5 rounded-xl text-xs font-bold font-mono ${calibResult.aprobado ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/20 text-rose-400 border border-rose-500/30'}">
                        ${totalEvaluated === 0 ? 'Sin Datos' : (calibResult.aprobado ? '✅ CALIBRACIÓN APROBADA (CURSO ACTIVE)' : '🚨 CIRCUIT BREAKER TRIPPED (CURSO BLOQUEADO)')}
                    </span>
                </div>
            </div>

            <!-- Gauge de Tolerancia PAR-14 -->
            <div id="interactive-mae-gauge" class="mb-4">
                <!-- Inyectado por GoldenSetCharts.drawMaeGauge -->
            </div>

            <div class="grid grid-cols-1 md:grid-cols-4 gap-3 text-xs font-mono pt-2">
                <div class="p-3 rounded-xl bg-slate-950 border border-slate-800">
                    <div class="text-slate-500 text-[10px]">TOLERANCIA MÁXIMA (PAR-14)</div>
                    <div class="text-amber-400 font-bold text-sm mt-0.5">±5.00 puntos</div>
                </div>
                <div class="p-3 rounded-xl bg-slate-950 border border-slate-800">
                    <div class="text-slate-500 text-[10px]">DESVIACIÓN MÁXIMA EN LOTE</div>
                    <div class="${calibResult.max_desviacion <= 5.0 ? 'text-emerald-400' : 'text-rose-400'} font-bold text-sm mt-0.5">${totalEvaluated > 0 ? calibResult.max_desviacion.toFixed(2) + ' pts' : '-'}</div>
                </div>
                <div class="p-3 rounded-xl bg-slate-950 border border-slate-800">
                    <div class="text-slate-500 text-[10px]">TASA DE CASOS EN TOLERANCIA</div>
                    <div class="text-cyan-400 font-bold text-sm mt-0.5">${totalEvaluated > 0 ? calibResult.tasa_aprobacion + '%' : '-'}</div>
                </div>
                <div class="p-3 rounded-xl bg-slate-950 border border-slate-800">
                    <div class="text-slate-500 text-[10px]">ESTADO DE PUBLICACIÓN</div>
                    <div class="${calibResult.aprobado ? 'text-emerald-400' : 'text-rose-400'} font-bold text-sm mt-0.5">${totalEvaluated > 0 ? calibResult.estado_curso : 'PENDIENTE'}</div>
                </div>
            </div>
        `;

        if (window.lucide) lucide.createIcons();

        if (totalEvaluated > 0) {
            GoldenSetCharts.drawMaeGauge('interactive-mae-gauge', calibResult.mae, 5.0);
        }
    }

    attachEventListeners() {
        const providerSelect = document.getElementById('llm-provider-select');
        const apiKeyInput = document.getElementById('llm-api-key-input');
        const saveKeyBtn = document.getElementById('save-api-key-btn');
        const testApiBtn = document.getElementById('test-api-btn');
        const modelSelect = document.getElementById('llm-model-select');
        const refreshModelsBtn = document.getElementById('refresh-models-btn');
        const baseUrlInput = document.getElementById('llm-base-url-input');
        const batchBtn = document.getElementById('run-batch-calibration-btn');
        const toggleCalibBtn = document.getElementById('toggle-calibration-panel-btn');
        const applyDirectivesBtn = document.getElementById('apply-directives-btn');
        const resetDirectivesBtn = document.getElementById('reset-directives-btn');

        if (providerSelect) {
            providerSelect.addEventListener('change', (e) => {
                this.onProviderSelected(e.target.value);
            });
        }

        if (modelSelect) {
            modelSelect.addEventListener('change', (e) => {
                this.aiService.setModel(e.target.value);
                this.updateCaseDetail();
            });
        }

        if (refreshModelsBtn) {
            refreshModelsBtn.addEventListener('click', () => {
                this.loadModels(true);
            });
        }

        if (baseUrlInput) {
            baseUrlInput.addEventListener('change', (e) => {
                this.aiService.setBaseUrl(e.target.value);
                this.loadModels(true);
            });
        }

        let debounceTimer = null;
        if (apiKeyInput) {
            apiKeyInput.addEventListener('input', (e) => {
                clearTimeout(debounceTimer);
                debounceTimer = setTimeout(() => {
                    this.onApiKeyInput(e.target.value.trim());
                }, 400);
            });
        }

        if (saveKeyBtn && apiKeyInput) {
            saveKeyBtn.addEventListener('click', () => {
                this.saveApiKey(apiKeyInput.value.trim());
            });
        }

        if (testApiBtn) {
            testApiBtn.addEventListener('click', async () => {
                testApiBtn.innerHTML = `<i data-lucide="loader-2" class="w-3.5 h-3.5 animate-spin"></i>`;
                if (window.lucide) lucide.createIcons();
                
                const testRes = await this.aiService.testConnection();
                if (testRes.ok) {
                    alert(`✅ Conexión exitosa con el modelo ${testRes.model} (${this.aiService.getProviderDisplayName(testRes.provider)})!`);
                } else {
                    alert(`❌ Error al conectar con el modelo:\n${testRes.error}`);
                }

                testApiBtn.innerHTML = `<i data-lucide="zap" class="w-3.5 h-3.5 text-amber-400"></i>`;
                if (window.lucide) lucide.createIcons();
            });
        }

        if (batchBtn) {
            batchBtn.addEventListener('click', () => this.runBatchCalibration());
        }

        if (toggleCalibBtn) {
            toggleCalibBtn.addEventListener('click', () => this.toggleCalibrationPanel());
        }

        if (applyDirectivesBtn) {
            applyDirectivesBtn.addEventListener('click', () => this.applyDirectives());
        }

        if (resetDirectivesBtn) {
            resetDirectivesBtn.addEventListener('click', () => this.resetToDefaultPreset());
        }

        // Preset buttons
        if (this.container) {
            this.container.querySelectorAll('.preset-btn').forEach(btn => {
                btn.addEventListener('click', () => {
                    const presetId = btn.getAttribute('data-preset');
                    this.setPreset(presetId);
                });
            });

            // Topic filter buttons
            this.container.querySelectorAll('.topic-filter-btn').forEach(btn => {
                btn.addEventListener('click', () => {
                    const topicId = btn.getAttribute('data-topic');
                    this.filterByTopic(topicId);
                });
            });
        }
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = InteractiveDemo;
}
