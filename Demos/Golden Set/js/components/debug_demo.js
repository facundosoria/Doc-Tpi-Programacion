/**
 * DEBUG DEMO COMPONENT (SECCIÓN 1)
 * Visualizador paso a paso estilo depuración del Golden Set y Calibración 5D de Cátedra.
 */

class DebugDemo {
    constructor(containerId, casesData = null, scoringEngine = null) {
        this.container = document.getElementById(containerId);
        this.cases = casesData || (typeof GOLDEN_SET_CASES !== 'undefined' ? GOLDEN_SET_CASES : []);
        this.scoringEngine = scoringEngine || new ScoringEngine();
        
        this.currentStep = 1;
        this.totalSteps = 6;
        this.selectedCaseIndex = 0;
        this.simulationMode = 'calibrated';
        this.isPlaying = false;
        this.playbackSpeed = 1500;
        this.playTimer = null;
        
        this.logs = [];
        this.calculatedResults = [];
    }

    init() {
        if (!this.container) return;
        this.recalculateAllCases();
        this.render();
        this.attachEventListeners();
        this.updateStepView();
    }

    recalculateAllCases() {
        this.calculatedResults = this.cases.map(caseItem => {
            const evalResult = this.simulationMode === 'calibrated' 
                ? this.scoringEngine.simulateCalibratedEvaluation(caseItem)
                : this.scoringEngine.simulateDriftEvaluation(caseItem);

            const scoreDocente = caseItem.score_docente.total;
            const scoreIA = evalResult.score_total;
            const desvio = this.scoringEngine.calculateDeviation(scoreIA, scoreDocente);

            return {
                ...caseItem,
                score_ia: evalResult,
                desvio: desvio,
                pasa_tolerancia: desvio <= 5.0
            };
        });
    }

    setSimulationMode(mode) {
        this.simulationMode = mode;
        this.recalculateAllCases();
        this.addLog(`[CONFIG] Modo de simulación cambiado a: ${mode.toUpperCase()}`, 'info');
        this.updateStepView();
    }

    selectCase(index) {
        this.selectedCaseIndex = Math.max(0, Math.min(this.cases.length - 1, index));
        this.updateStepView();
    }

    addLog(message, type = 'info') {
        const timestamp = new Date().toISOString().split('T')[1].slice(0, 8);
        this.logs.push({ timestamp, message, type });
        if (this.logs.length > 50) this.logs.shift();
        this.renderLogs();
    }

    renderLogs() {
        const term = document.getElementById('debug-terminal-logs');
        if (!term) return;
        term.innerHTML = this.logs.map(log => {
            let color = 'text-slate-300';
            if (log.type === 'success') color = 'text-emerald-400';
            if (log.type === 'warn') color = 'text-amber-400';
            if (log.type === 'error') color = 'text-rose-400';
            if (log.type === 'step') color = 'text-cyan-400 font-bold';
            return `<div class="font-mono text-xs leading-relaxed"><span class="text-slate-500">[${log.timestamp}]</span> <span class="${color}">${log.message}</span></div>`;
        }).join('');
        term.scrollTop = term.scrollHeight;
    }

    goToStep(stepNumber) {
        this.currentStep = Math.max(1, Math.min(this.totalSteps, stepNumber));
        this.updateStepView();
    }

    nextStep() {
        if (this.currentStep < this.totalSteps) {
            this.goToStep(this.currentStep + 1);
        } else {
            this.goToStep(1);
        }
    }

    prevStep() {
        if (this.currentStep > 1) {
            this.goToStep(this.currentStep - 1);
        }
    }

    togglePlay() {
        this.isPlaying = !this.isPlaying;
        const playBtn = document.getElementById('debug-play-btn');
        if (playBtn) {
            playBtn.innerHTML = this.isPlaying 
                ? `<i data-lucide="pause" class="w-4 h-4 mr-1 inline"></i> Pausar` 
                : `<i data-lucide="play" class="w-4 h-4 mr-1 inline"></i> Auto-Play`;
            if (window.lucide) lucide.createIcons();
        }

        if (this.isPlaying) {
            this.addLog('[DEBUGGER] Inicio de reproducción automática.', 'info');
            this.playTimer = setInterval(() => {
                if (this.currentStep >= this.totalSteps) {
                    this.goToStep(1);
                } else {
                    this.nextStep();
                }
            }, this.playbackSpeed);
        } else {
            clearInterval(this.playTimer);
            this.playTimer = null;
            this.addLog('[DEBUGGER] Reproducción pausada.', 'info');
        }
    }

    setPlaybackSpeed(speedMs) {
        this.playbackSpeed = speedMs;
        if (this.isPlaying) {
            clearInterval(this.playTimer);
            this.playTimer = setInterval(() => {
                if (this.currentStep >= this.totalSteps) {
                    this.goToStep(1);
                } else {
                    this.nextStep();
                }
            }, this.playbackSpeed);
        }
    }

    render() {
        this.container.innerHTML = `
            <div class="space-y-6">
                <!-- Header de Control de la Barra de Depuración -->
                <div class="bg-slate-900/90 border border-slate-800 rounded-2xl p-5 shadow-xl backdrop-blur-md">
                    <div class="flex flex-wrap items-center justify-between gap-4 pb-4 border-b border-slate-800/80">
                        <div>
                            <div class="flex items-center gap-2">
                                <span class="px-2.5 py-0.5 rounded-full text-xs font-mono font-semibold bg-cyan-500/20 text-cyan-400 border border-cyan-500/30">
                                    PIPELINE LLMOps DEBUGGER
                                </span>
                                <span class="text-xs text-slate-400">Paso <strong class="text-white">${this.currentStep}</strong> de ${this.totalSteps}</span>
                            </div>
                            <h2 class="text-lg font-bold text-white mt-1 flex items-center gap-2">
                                <i data-lucide="activity" class="w-5 h-5 text-cyan-400"></i>
                                Trazabilidad y Calibración en 5 Rúbricas Fijas
                            </h2>
                        </div>

                        <!-- Controles de Reproducción -->
                        <div class="flex items-center flex-wrap gap-2">
                            <div class="flex items-center bg-slate-950 p-1 rounded-xl border border-slate-800">
                                <button id="debug-prev-btn" class="p-2 hover:bg-slate-800 rounded-lg text-slate-300 hover:text-white transition" title="Paso Anterior">
                                    <i data-lucide="chevron-left" class="w-4 h-4"></i>
                                </button>
                                <button id="debug-play-btn" class="px-3 py-1.5 bg-cyan-600 hover:bg-cyan-500 text-white rounded-lg text-xs font-semibold flex items-center transition shadow-lg shadow-cyan-600/20">
                                    <i data-lucide="play" class="w-4 h-4 mr-1"></i> Auto-Play
                                </button>
                                <button id="debug-next-btn" class="p-2 hover:bg-slate-800 rounded-lg text-slate-300 hover:text-white transition" title="Paso Siguiente">
                                    <i data-lucide="chevron-right" class="w-4 h-4"></i>
                                </button>
                            </div>

                            <select id="debug-speed-select" class="bg-slate-950 text-slate-300 text-xs rounded-xl px-2.5 py-2 border border-slate-800 focus:outline-none focus:border-cyan-500">
                                <option value="2500">0.5x (Lento)</option>
                                <option value="1500" selected>1.0x (Normal)</option>
                                <option value="800">2.0x (Rápido)</option>
                                <option value="400">4.0x (Turbo)</option>
                            </select>

                            <div class="flex rounded-xl p-1 bg-slate-950 border border-slate-800 text-xs">
                                <button id="mode-calibrated-btn" class="px-3 py-1.5 rounded-lg font-medium transition ${this.simulationMode === 'calibrated' ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'text-slate-400 hover:text-white'}">
                                    <i data-lucide="check-circle" class="w-3.5 h-3.5 inline mr-1"></i> Calibrado (Pasa)
                                </button>
                                <button id="mode-drift-btn" class="px-3 py-1.5 rounded-lg font-medium transition ${this.simulationMode === 'drift' ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30' : 'text-slate-400 hover:text-white'}">
                                    <i data-lucide="alert-triangle" class="w-3.5 h-3.5 inline mr-1"></i> Deriva (Dispara Disyuntor)
                                </button>
                            </div>
                        </div>
                    </div>

                    <!-- Stepper Visual Interactivo -->
                    <div class="grid grid-cols-2 md:grid-cols-6 gap-2 pt-4">
                        ${[
                            { step: 1, title: "1. Ingesta Golden Set", icon: "database" },
                            { step: 2, title: "2. Telemetría en Código", icon: "binary" },
                            { step: 3, title: "3. Inferencia Semántica", icon: "sparkles" },
                            { step: 4, title: "4. Fusión Ponderada 5D", icon: "calculator" },
                            { step: 5, title: "5. Error MAE & Delta", icon: "trending-up" },
                            { step: 6, title: "6. Disyuntor PAR-14", icon: "shield-alert" }
                        ].map(s => `
                            <button class="step-nav-btn p-2.5 rounded-xl text-left border transition-all ${this.currentStep === s.step ? 'bg-cyan-950/40 border-cyan-500/50 shadow-lg shadow-cyan-500/10' : 'bg-slate-950/40 border-slate-800/80 hover:border-slate-700'}" data-step="${s.step}">
                                <div class="flex items-center gap-1.5 text-xs ${this.currentStep === s.step ? 'text-cyan-400 font-bold' : 'text-slate-400'}">
                                    <i data-lucide="${s.icon}" class="w-3.5 h-3.5"></i>
                                    <span>Paso ${s.step}</span>
                                </div>
                                <div class="text-[11px] font-medium text-slate-200 truncate mt-0.5">${s.title.substring(3)}</div>
                            </button>
                        `).join('')}
                    </div>
                </div>

                <!-- Panel Principal -->
                <div id="debug-step-content" class="grid grid-cols-1 lg:grid-cols-12 gap-6"></div>

                <!-- Consola Terminal Inferior -->
                <div class="bg-slate-950 border border-slate-800 rounded-2xl overflow-hidden shadow-2xl">
                    <div class="bg-slate-900/80 px-4 py-2.5 border-b border-slate-800 flex items-center justify-between">
                        <div class="flex items-center gap-2">
                            <span class="w-3 h-3 rounded-full bg-rose-500 inline-block"></span>
                            <span class="w-3 h-3 rounded-full bg-amber-500 inline-block"></span>
                            <span class="w-3 h-3 rounded-full bg-emerald-500 inline-block"></span>
                            <span class="text-xs font-mono text-slate-400 ml-2">Console Telemetry Log & Auditoría LLMOps</span>
                        </div>
                        <button id="clear-logs-btn" class="text-[11px] font-mono text-slate-400 hover:text-white transition">Limpiar</button>
                    </div>
                    <div id="debug-terminal-logs" class="p-4 h-36 overflow-y-auto space-y-1 font-mono text-xs bg-slate-950/90 select-text"></div>
                </div>
            </div>
        `;

        if (window.lucide) lucide.createIcons();
    }

    attachEventListeners() {
        const prevBtn = document.getElementById('debug-prev-btn');
        const nextBtn = document.getElementById('debug-next-btn');
        const playBtn = document.getElementById('debug-play-btn');
        const speedSelect = document.getElementById('debug-speed-select');
        const modeCalibrated = document.getElementById('mode-calibrated-btn');
        const modeDrift = document.getElementById('mode-drift-btn');
        const clearLogs = document.getElementById('clear-logs-btn');

        if (prevBtn) prevBtn.addEventListener('click', () => this.prevStep());
        if (nextBtn) nextBtn.addEventListener('click', () => this.nextStep());
        if (playBtn) playBtn.addEventListener('click', () => this.togglePlay());
        if (speedSelect) speedSelect.addEventListener('change', (e) => this.setPlaybackSpeed(parseInt(e.target.value, 10)));
        if (modeCalibrated) modeCalibrated.addEventListener('click', () => this.setSimulationMode('calibrated'));
        if (modeDrift) modeDrift.addEventListener('click', () => this.setSimulationMode('drift'));
        if (clearLogs) clearLogs.addEventListener('click', () => { this.logs = []; this.renderLogs(); });

        this.container.querySelectorAll('.step-nav-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const step = parseInt(btn.getAttribute('data-step'), 10);
                this.goToStep(step);
            });
        });
    }

    updateStepView() {
        this.container.querySelectorAll('.step-nav-btn').forEach(btn => {
            const step = parseInt(btn.getAttribute('data-step'), 10);
            if (step === this.currentStep) {
                btn.className = 'step-nav-btn p-2.5 rounded-xl text-left border transition-all bg-cyan-950/40 border-cyan-500/50 shadow-lg shadow-cyan-500/10 ring-1 ring-cyan-500/30';
            } else {
                btn.className = 'step-nav-btn p-2.5 rounded-xl text-left border transition-all bg-slate-950/40 border-slate-800/80 hover:border-slate-700';
            }
        });

        const contentContainer = document.getElementById('debug-step-content');
        if (!contentContainer) return;

        const activeCase = this.calculatedResults[this.selectedCaseIndex] || this.calculatedResults[0];

        switch (this.currentStep) {
            case 1:
                this.renderStep1(contentContainer, activeCase);
                this.addLog(`[PASO 1] Ingesta del Golden Set: 10 exámenes de muestra de Programación 3 con ground-truth docente.`, 'step');
                break;
            case 2:
                this.renderStep2(contentContainer, activeCase);
                this.addLog(`[PASO 2] Motor Determinístico en Código: Extracción de métricas de telemetría (ediciones previas, AST diffs, detección de inyecciones).`, 'step');
                break;
            case 3:
                this.renderStep3(contentContainer, activeCase);
                this.addLog(`[PASO 3] Inferencia Semántica LLM: T=0.00, Seed=42 con Directivas de Cátedra (RF-IA-30b).`, 'step');
                break;
            case 4:
                this.renderStep4(contentContainer, activeCase);
                this.addLog(`[PASO 4] Fusión Ponderada Oficial RF-IA-15: (D1*0.30) + (D2*0.25) + (D3*0.20) + (D4*0.15) + (D5*0.10) = ${activeCase.score_ia.score_total} pts.`, 'step');
                break;
            case 5:
                this.renderStep5(contentContainer);
                const mae = this.scoringEngine.evaluateCalibrationRun(this.calculatedResults).mae;
                this.addLog(`[PASO 5] Análisis de Desviación Global: Error Absoluto Medio (MAE) = ${mae.toFixed(2)} pts sobre los 10 exámenes patrón.`, 'step');
                break;
            case 6:
                this.renderStep6(contentContainer);
                const calibResult = this.scoringEngine.evaluateCalibrationRun(this.calculatedResults);
                if (calibResult.aprobado) {
                    this.addLog(`[PASO 6] ✅ PAR-14 APROBADO: MAE ${calibResult.mae} <= 5.0 pts. Curso promovido a estado ACTIVE.`, 'success');
                } else {
                    this.addLog(`[PASO 6] 🚨 PAR-14 CIRCUIT BREAKER: MAE ${calibResult.mae} > 5.0 pts. Model Drift detectado. Curso BLOQUEADO en DRAFT con HTTP 503.`, 'error');
                }
                break;
        }

        if (window.lucide) lucide.createIcons();
    }

    // PASO 1: INGESTA DEL LOTE
    renderStep1(container, activeCase) {
        container.innerHTML = `
            <div class="lg:col-span-5 space-y-4">
                <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5">
                    <h3 class="text-sm font-bold text-white flex items-center gap-2 mb-3">
                        <i data-lucide="database" class="w-4 h-4 text-cyan-400"></i>
                        Lote Golden Set de Referencia (10 Exámenes)
                    </h3>
                    <p class="text-xs text-slate-400 mb-4 leading-relaxed">
                        Conjunto inmutable de correcciones y transcripciones de muestra de *Programación III* calificadas por docentes como estándar de referencia (RF-IA-30).
                    </p>
                    <div class="space-y-2 max-h-[360px] overflow-y-auto pr-1">
                        ${this.calculatedResults.map((c, idx) => `
                            <button class="w-full text-left p-2.5 rounded-xl border transition flex items-center justify-between text-xs ${idx === this.selectedCaseIndex ? 'bg-cyan-950/50 border-cyan-500/50 ring-1 ring-cyan-500/30' : 'bg-slate-950/60 border-slate-800/80 hover:border-slate-700'}" onclick="window.debugDemoInstance.selectCase(${idx})">
                                <div>
                                    <div class="font-semibold text-white">${c.id}: ${c.estudiante.split('(')[0]}</div>
                                    <div class="text-[10px] text-slate-400 truncate">${c.perfil}</div>
                                </div>
                                <span class="px-2 py-0.5 rounded text-[10px] font-mono font-bold bg-slate-800 text-cyan-400 border border-slate-700">
                                    Docente: ${c.score_docente.total}
                                </span>
                            </button>
                        `).join('')}
                    </div>
                </div>
            </div>

            <div class="lg:col-span-7 space-y-4">
                <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5">
                    <div class="flex items-center justify-between mb-4">
                        <span class="text-xs font-mono text-cyan-400">CASO SELECCIONADO: ${activeCase.id}</span>
                        <span class="px-2 py-0.5 rounded text-xs font-mono bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                            ${activeCase.topic_id}
                        </span>
                    </div>

                    <h4 class="text-sm font-semibold text-white mb-1">${activeCase.estudiante} - <span class="text-slate-400 font-normal">${activeCase.perfil}</span></h4>
                    <p class="text-xs text-slate-400 mb-4">${activeCase.consigna}</p>

                    <!-- Puntuación Patrón Docente -->
                    <div class="bg-slate-950 p-4 rounded-xl border border-slate-800 mb-4">
                        <div class="text-xs font-semibold text-slate-300 mb-3 flex items-center justify-between">
                            <span>Rúbrica Patrón Cátedra (Ground Truth):</span>
                            <span class="text-cyan-400 font-mono text-sm font-bold">${activeCase.score_docente.total} / 100</span>
                        </div>
                        <div class="grid grid-cols-5 gap-2 text-center font-mono text-xs">
                            <div class="p-2 rounded bg-slate-900 border border-slate-800"><div class="text-[10px] text-slate-400">D1 (30%)</div><strong class="text-emerald-400">${activeCase.score_docente.autonomia}</strong></div>
                            <div class="p-2 rounded bg-slate-900 border border-slate-800"><div class="text-[10px] text-slate-400">D2 (25%)</div><strong class="text-cyan-400">${activeCase.score_docente.claridad}</strong></div>
                            <div class="p-2 rounded bg-slate-900 border border-slate-800"><div class="text-[10px] text-slate-400">D3 (20%)</div><strong class="text-indigo-400">${activeCase.score_docente.progresion}</strong></div>
                            <div class="p-2 rounded bg-slate-900 border border-slate-800"><div class="text-[10px] text-slate-400">D4 (15%)</div><strong class="text-amber-400">${activeCase.score_docente.cumplimiento}</strong></div>
                            <div class="p-2 rounded bg-slate-900 border border-slate-800"><div class="text-[10px] text-slate-400">D5 (10%)</div><strong class="text-rose-400">${activeCase.score_docente.eficiencia}</strong></div>
                        </div>
                        <div class="text-[11px] text-slate-400 mt-3 italic border-t border-slate-800/80 pt-2">
                            "${activeCase.score_docente.justificacion_catedra}"
                        </div>
                    </div>

                    <div class="text-xs font-mono text-slate-400">
                        Estado del Pipeline: <span class="text-emerald-400 font-bold">Lote verificado con Hash SHA-256 inmutable</span>
                    </div>
                </div>
            </div>
        `;
    }

    // PASO 2: EXTRACCIÓN DETERMINÍSTICA
    renderStep2(container, activeCase) {
        const metrics = this.scoringEngine.extractDeterministicMetrics(activeCase.telemetria);
        container.innerHTML = `
            <div class="lg:col-span-6 space-y-4">
                <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5">
                    <h3 class="text-sm font-bold text-white flex items-center gap-2 mb-3">
                        <i data-lucide="binary" class="w-4 h-4 text-emerald-400"></i>
                        Motor Determinístico en Código (Sin LLM)
                    </h3>
                    <p class="text-xs text-slate-400 mb-4 leading-relaxed">
                        Entre el <strong>45% y 60% de la rúbrica</strong> se calcula matemáticamente en código para eliminar la variabilidad y reducir el costo de inferencia (RF-IA-15).
                    </p>
                    
                    <div class="space-y-3">
                        <div class="p-3 bg-slate-950 rounded-xl border border-slate-800 flex items-center justify-between">
                            <div>
                                <div class="text-xs font-semibold text-white">Cumplimiento / Ética (D4)</div>
                                <div class="text-[11px] text-slate-400">Penalización automática por intentos de inyección o pedidos de solución</div>
                            </div>
                            <div class="text-right font-mono">
                                <span class="text-xs px-2 py-0.5 rounded ${activeCase.telemetria.incidentes_jailbreak > 0 ? 'bg-rose-500/20 text-rose-400 border border-rose-500/30' : 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'}">
                                    ${metrics.baseCumplimiento} pts
                                </span>
                            </div>
                        </div>

                        <div class="p-3 bg-slate-950 rounded-xl border border-slate-800 flex items-center justify-between">
                            <div>
                                <div class="text-xs font-semibold text-white">Eficiencia de Interacción (D5)</div>
                                <div class="text-[11px] text-slate-400">Relación de mensajes triviales vs turnos útiles (${metrics.ratioTrivial} triviales)</div>
                            </div>
                            <div class="text-right font-mono">
                                <span class="text-xs px-2 py-0.5 rounded bg-cyan-500/20 text-cyan-400 border border-cyan-500/30">
                                    ${metrics.baseEficiencia} pts
                                </span>
                            </div>
                        </div>

                        <div class="p-3 bg-slate-950 rounded-xl border border-slate-800 flex items-center justify-between">
                            <div>
                                <div class="text-xs font-semibold text-white">Autonomía Previa (Bonus D1)</div>
                                <div class="text-[11px] text-slate-400">Ediciones (${activeCase.telemetria.ediciones_antes_primer_mensaje}) y Tests (${activeCase.telemetria.ejecuciones_test_previas}) antes del 1º mensaje</div>
                            </div>
                            <div class="text-right font-mono">
                                <span class="text-xs px-2 py-0.5 rounded bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
                                    ${metrics.scoreAutonomiaPrevia} / 100
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="lg:col-span-6 space-y-4">
                <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5">
                    <h3 class="text-sm font-bold text-white flex items-center gap-2 mb-3">
                        <i data-lucide="file-code" class="w-4 h-4 text-cyan-400"></i>
                        Evidencia Objetiva Inyectada en el Prompt
                    </h3>
                    <div class="bg-slate-950 p-3.5 rounded-xl border border-slate-800 font-mono text-xs text-slate-300 space-y-1.5">
                        <div class="text-cyan-400 font-bold">&lt;consigna_desafio&gt;</div>
                        <div class="pl-3 text-slate-400">${activeCase.consigna}</div>
                        <div class="text-cyan-400 font-bold">&lt;/consigna_desafio&gt;</div>
                        <div class="text-cyan-400 font-bold mt-2">&lt;evidencia_objetiva_telemetria&gt;</div>
                        <div class="pl-3 text-slate-400">- ediciones antes del primer mensaje: <span class="text-white">${activeCase.telemetria.ediciones_antes_primer_mensaje}</span></div>
                        <div class="pl-3 text-slate-400">- ejecuciones test previas: <span class="text-white">${activeCase.telemetria.ejecuciones_test_previas}</span></div>
                        <div class="pl-3 text-slate-400">- tiempo hasta 1º consulta: <span class="text-white">${activeCase.telemetria.tiempo_hasta_primer_mensaje_segundos}s (${metrics.tiempo_espera_minutos} min)</span></div>
                        <div class="pl-3 text-slate-400">- incidentes detectados: <span class="${activeCase.telemetria.incidentes_jailbreak > 0 ? 'text-rose-400 font-bold' : 'text-emerald-400'}">${activeCase.telemetria.incidentes_jailbreak}</span></div>
                        <div class="text-cyan-400 font-bold">&lt;/evidencia_objetiva_telemetria&gt;</div>
                    </div>
                    <p class="text-[11px] text-slate-400 mt-3">
                        El evaluador recibe datos fácticos antes de la transcripción para evitar alucinaciones.
                    </p>
                </div>
            </div>
        `;
    }

    // PASO 3: INFERENCIA SEMÁNTICA LLM
    renderStep3(container, activeCase) {
        const evalData = activeCase.score_ia;
        const dims = evalData.dimensiones;
        container.innerHTML = `
            <div class="lg:col-span-6 space-y-4">
                <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5">
                    <div class="flex items-center justify-between mb-3">
                        <h3 class="text-sm font-bold text-white flex items-center gap-2">
                            <i data-lucide="sparkles" class="w-4 h-4 text-purple-400"></i>
                            Inferencia Semántica LLM ($T=0.00$)
                        </h3>
                        <span class="text-xs font-mono px-2 py-0.5 rounded bg-purple-500/20 text-purple-300 border border-purple-500/30">
                            Seed: 42 | ${evalData.modelo_usado}
                        </span>
                    </div>

                    <div class="space-y-2.5">
                        ${Object.keys(dims).map(key => {
                            const d = dims[key];
                            return `
                                <div class="p-3 bg-slate-950 rounded-xl border border-slate-800">
                                    <div class="flex items-center justify-between mb-1">
                                        <span class="text-xs font-semibold text-slate-200 uppercase">${key}</span>
                                        <span class="text-xs font-mono font-bold text-purple-400">${d.puntaje} / 100</span>
                                    </div>
                                    <p class="text-[11px] text-slate-400 leading-snug">${d.justificacion}</p>
                                </div>
                            `;
                        }).join('')}
                    </div>
                </div>
            </div>

            <div class="lg:col-span-6 space-y-4">
                <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5">
                    <h3 class="text-sm font-bold text-white flex items-center gap-2 mb-3">
                        <i data-lucide="check-check" class="w-4 h-4 text-cyan-400"></i>
                        Validación de Salida JSON con Pydantic v2
                    </h3>
                    <div class="bg-slate-950 p-3.5 rounded-xl border border-slate-800 font-mono text-xs text-slate-300 overflow-x-auto">
                        <pre class="text-[11px] leading-relaxed text-emerald-300">
{
  "dimensiones": {
    "autonomia": { "puntaje": ${dims.autonomia.puntaje} },
    "claridad": { "puntaje": ${dims.claridad.puntaje} },
    "progresion": { "puntaje": ${dims.progresion.puntaje} },
    "cumplimiento": { "puntaje": ${dims.cumplimiento.puntaje} },
    "eficiencia": { "puntaje": ${dims.eficiencia.puntaje} }
  },
  "confidence_score": ${evalData.confidence_score},
  "senales_de_manipulacion": ${evalData.senales_de_manipulacion}
}</pre>
                    </div>

                    <div class="mt-4 p-3 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between text-xs">
                        <span class="text-slate-400">Nivel de Certeza (Confidence):</span>
                        <span class="font-mono font-bold ${evalData.confidence_score >= 0.7 ? 'text-emerald-400' : 'text-amber-400'}">
                            ${(evalData.confidence_score * 100).toFixed(0)}%
                        </span>
                    </div>
                </div>
            </div>
        `;
    }

    // PASO 4: FUSIÓN PONDERADA 5D
    renderStep4(container, activeCase) {
        const dims = activeCase.score_ia.dimensiones;
        const total = activeCase.score_ia.score_total;
        container.innerHTML = `
            <div class="lg:col-span-7 space-y-4">
                <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5">
                    <h3 class="text-sm font-bold text-white flex items-center gap-2 mb-3">
                        <i data-lucide="calculator" class="w-4 h-4 text-cyan-400"></i>
                        Fórmula Ponderada Oficial RF-IA-15
                    </h3>
                    <div class="p-4 bg-slate-950 rounded-xl border border-slate-800 font-mono text-xs mb-4 text-center">
                        <span class="text-slate-300">Score Final = </span>
                        <span class="text-emerald-400">(D1 × 0.30)</span> + 
                        <span class="text-cyan-400">(D2 × 0.25)</span> + 
                        <span class="text-indigo-400">(D3 × 0.20)</span> + 
                        <span class="text-amber-400">(D4 × 0.15)</span> + 
                        <span class="text-rose-400">(D5 × 0.10)</span>
                    </div>

                    <div class="space-y-2 text-xs font-mono">
                        <div class="flex items-center justify-between p-2.5 bg-slate-950/60 rounded-lg border border-slate-800">
                            <span class="text-emerald-400">1. Autonomía (30%):</span>
                            <span>${dims.autonomia.puntaje} × 0.30 = <strong class="text-white">${(dims.autonomia.puntaje * 0.3).toFixed(2)} pts</strong></span>
                        </div>
                        <div class="flex items-center justify-between p-2.5 bg-slate-950/60 rounded-lg border border-slate-800">
                            <span class="text-cyan-400">2. Claridad de Prompts (25%):</span>
                            <span>${dims.claridad.puntaje} × 0.25 = <strong class="text-white">${(dims.claridad.puntaje * 0.25).toFixed(2)} pts</strong></span>
                        </div>
                        <div class="flex items-center justify-between p-2.5 bg-slate-950/60 rounded-lg border border-slate-800">
                            <span class="text-indigo-400">3. Progresión Lógica (20%):</span>
                            <span>${dims.progresion.puntaje} × 0.20 = <strong class="text-white">${(dims.progresion.puntaje * 0.2).toFixed(2)} pts</strong></span>
                        </div>
                        <div class="flex items-center justify-between p-2.5 bg-slate-950/60 rounded-lg border border-slate-800">
                            <span class="text-amber-400">4. Cumplimiento Límites (15%):</span>
                            <span>${dims.cumplimiento.puntaje} × 0.15 = <strong class="text-white">${(dims.cumplimiento.puntaje * 0.15).toFixed(2)} pts</strong></span>
                        </div>
                        <div class="flex items-center justify-between p-2.5 bg-slate-950/60 rounded-lg border border-slate-800">
                            <span class="text-rose-400">5. Eficiencia de Turnos (10%):</span>
                            <span>${dims.eficiencia.puntaje} × 0.10 = <strong class="text-white">${(dims.eficiencia.puntaje * 0.1).toFixed(2)} pts</strong></span>
                        </div>
                    </div>
                </div>
            </div>

            <div class="lg:col-span-5 space-y-4">
                <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5 text-center flex flex-col justify-center h-full">
                    <div class="text-xs font-mono text-slate-400 uppercase tracking-wider mb-2">Score Final Calculado en Código</div>
                    <div class="text-5xl font-extrabold text-cyan-400 font-mono my-2">${total.toFixed(2)}</div>
                    <div class="text-xs text-slate-400">sobre 100 puntos posibles</div>

                    <div class="mt-6 pt-4 border-t border-slate-800 text-left space-y-2 text-xs">
                        <div class="flex justify-between">
                            <span class="text-slate-400">Nota Patrón Cátedra:</span>
                            <span class="text-white font-mono font-bold">${activeCase.score_docente.total.toFixed(2)} pts</span>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-slate-400">Desviación Individual (Δ):</span>
                            <span class="font-mono font-bold ${activeCase.desvio <= 5.0 ? 'text-emerald-400' : 'text-rose-400'}">
                                ${activeCase.desvio.toFixed(2)} pts ${activeCase.desvio <= 5.0 ? '✓' : '⚠️'}
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }

    // PASO 5: ERROR MAE & DELTA
    renderStep5(container) {
        const calib = this.scoringEngine.evaluateCalibrationRun(this.calculatedResults);
        container.innerHTML = `
            <div class="lg:col-span-12 space-y-4">
                <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5">
                    <div class="flex flex-wrap items-center justify-between gap-4 mb-4">
                        <div>
                            <h3 class="text-sm font-bold text-white flex items-center gap-2">
                                <i data-lucide="trending-up" class="w-4 h-4 text-cyan-400"></i>
                                Comparación Vectorial del Lote Golden Set (10 Exámenes)
                            </h3>
                            <p class="text-xs text-slate-400">Evaluación del Error Absoluto Medio (MAE) contra las notas patrón de cátedra.</p>
                        </div>
                        <div class="flex items-center gap-3">
                            <div class="text-right">
                                <div class="text-[10px] font-mono text-slate-400 uppercase">Error Absoluto Medio (MAE)</div>
                                <div class="text-xl font-mono font-bold ${calib.aprobado ? 'text-emerald-400' : 'text-rose-400'}">
                                    ${calib.mae.toFixed(2)} pts
                                </div>
                            </div>
                            <span class="px-3 py-1 rounded-full text-xs font-semibold ${calib.aprobado ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/20 text-rose-400 border border-rose-500/30'}">
                                ${calib.aprobado ? 'Dentro de Tolerancia (<= 5.0)' : 'Desviación Crítica (> 5.0)'}
                            </span>
                        </div>
                    </div>

                    <div class="overflow-x-auto">
                        <table class="w-full text-left text-xs font-mono">
                            <thead>
                                <tr class="bg-slate-950 text-slate-400 border-b border-slate-800">
                                    <th class="p-2.5">ID</th>
                                    <th class="p-2.5">Estudiante</th>
                                    <th class="p-2.5 text-center">Score Docente</th>
                                    <th class="p-2.5 text-center">Score IA</th>
                                    <th class="p-2.5 text-center">Delta (Δ)</th>
                                    <th class="p-2.5 text-center">Tolerancia PAR-14</th>
                                </tr>
                            </thead>
                            <tbody class="divide-y divide-slate-800/60">
                                ${this.calculatedResults.map(item => `
                                    <tr class="hover:bg-slate-950/40 transition">
                                        <td class="p-2.5 font-bold text-slate-300">${item.id}</td>
                                        <td class="p-2.5 text-white font-sans">${item.estudiante.split('(')[0]} <span class="text-[10px] text-slate-500">(${item.perfil})</span></td>
                                        <td class="p-2.5 text-center text-cyan-400 font-bold">${item.score_docente.total}</td>
                                        <td class="p-2.5 text-center text-purple-400 font-bold">${item.score_ia.score_total}</td>
                                        <td class="p-2.5 text-center ${item.desvio <= 5.0 ? 'text-emerald-400' : 'text-rose-400 font-bold'}">
                                            ${item.desvio.toFixed(2)} pts
                                        </td>
                                        <td class="p-2.5 text-center">
                                            <span class="px-2 py-0.5 rounded text-[10px] font-bold ${item.desvio <= 5.0 ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30' : 'bg-rose-500/20 text-rose-400 border border-rose-500/30'}">
                                                ${item.desvio <= 5.0 ? 'OK (<= 5.0)' : 'FALLÓ (> 5.0)'}
                                            </span>
                                        </td>
                                    </tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                </div>
            </div>
        `;
    }

    // PASO 6: DISYUNTOR PAR-14 & ESTADO DEL CURSO
    renderStep6(container) {
        const calib = this.scoringEngine.evaluateCalibrationRun(this.calculatedResults);
        container.innerHTML = `
            <div class="lg:col-span-7 space-y-4">
                <div class="bg-slate-900 border ${calib.aprobado ? 'border-emerald-500/40 shadow-emerald-500/10' : 'border-rose-500/40 shadow-rose-500/10'} rounded-2xl p-6 shadow-2xl">
                    <div class="flex items-center gap-3 mb-4">
                        <div class="w-12 h-12 rounded-2xl flex items-center justify-center ${calib.aprobado ? 'bg-emerald-500/20 text-emerald-400' : 'bg-rose-500/20 text-rose-400 animate-pulse'}">
                            <i data-lucide="${calib.aprobado ? 'shield-check' : 'shield-alert'}" class="w-6 h-6"></i>
                        </div>
                        <div>
                            <span class="text-xs font-mono font-bold uppercase tracking-wider ${calib.aprobado ? 'text-emerald-400' : 'text-rose-400'}">
                                ${calib.aprobado ? 'ESTADO: CALIBRACIÓN APROBADA (PAR-14)' : 'ESTADO: CIRCUIT BREAKER DISPARADO (PAR-14)'}
                            </span>
                            <h3 class="text-lg font-bold text-white">
                                ${calib.aprobado ? 'Modelo Homologado para Evaluación de Alumnos' : 'Deriva Crítica Detectada - Curso Bloqueado'}
                            </h3>
                        </div>
                    </div>

                    <p class="text-xs text-slate-300 mb-5 leading-relaxed">
                        ${calib.aprobado 
                            ? 'El Error Absoluto Medio (MAE) de <strong>' + calib.mae.toFixed(2) + ' pts</strong> está por debajo de la tolerancia oficial de ±5.0 puntos. El curso de Programación 3 ha sido promovido al estado <strong>ACTIVE</strong>.' 
                            : 'El MAE de <strong>' + calib.mae.toFixed(2) + ' pts</strong> supera el umbral máximo de tolerancia (±5.0 pts). El evaluador emite <strong>HTTP 503 Service Unavailable</strong> y el curso permanece bloqueado en <strong>DRAFT</strong> para proteger a los estudiantes de notas sesgadas.'
                        }
                    </p>

                    <div class="p-4 rounded-xl bg-slate-950 border border-slate-800 space-y-2 text-xs font-mono">
                        <div class="flex justify-between">
                            <span class="text-slate-400">Estado de Publicación del Curso:</span>
                            <strong class="${calib.aprobado ? 'text-emerald-400' : 'text-rose-400'}">${calib.estado_curso}</strong>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-slate-400">Tasa de Aprobación de Casos:</span>
                            <strong class="text-white">${calib.tasa_aprobacion}% (${this.calculatedResults.filter(c => c.desvio <= 5.0).length}/10)</strong>
                        </div>
                        <div class="flex justify-between">
                            <span class="text-slate-400">Desviación Máxima Registrada:</span>
                            <strong class="${calib.max_desviacion <= 5.0 ? 'text-emerald-400' : 'text-rose-400'}">${calib.max_desviacion.toFixed(2)} pts</strong>
                        </div>
                    </div>
                </div>
            </div>

            <div class="lg:col-span-5 space-y-4">
                <div class="bg-slate-900 border border-slate-800 rounded-2xl p-5 flex flex-col justify-between">
                    <div>
                        <h4 class="text-sm font-bold text-white mb-2 flex items-center gap-2">
                            <i data-lucide="lock" class="w-4 h-4 text-amber-400"></i>
                            Regla de Inmutabilidad y No-Bypass
                        </h4>
                        <p class="text-xs text-slate-400 leading-relaxed">
                            Según el requerimiento <strong>RF-IA-36b</strong>, ni el Administrador ni el Profesor pueden sobreescribir manualmente un fallo de calibración del Golden Set.
                        </p>
                    </div>

                    <div class="mt-4 p-3 bg-slate-950 rounded-xl border border-slate-800 text-[11px] font-mono text-slate-400">
                        <div class="text-cyan-400 font-bold mb-1">Hash de Auditoría Criptográfica:</div>
                        <div class="truncate text-slate-500">SHA256: 7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069</div>
                    </div>
                </div>
            </div>
        `;
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = DebugDemo;
}
