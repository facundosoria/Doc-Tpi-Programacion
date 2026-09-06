/**
 * MAIN APP CONTROLLER
 * Coordina la navegación entre pestañas, modales de arquitectura y ciclo de vida de la demo.
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. Inicializar Motores y Componentes
    const scoringEngine = new ScoringEngine(RUBRIC_CONFIG);
    const debugDemo = new DebugDemo('debug-demo-container', GOLDEN_SET_CASES, scoringEngine);
    const interactiveDemo = new InteractiveDemo('interactive-demo-container', GOLDEN_SET_TOPICS, GOLDEN_SET_CASES, RUBRIC_CONFIG);

    // Exponer instancias globales para callbacks en el DOM
    window.debugDemoInstance = debugDemo;
    window.interactiveDemoInstance = interactiveDemo;

    // 2. Inicializar Vistas
    debugDemo.init();
    interactiveDemo.init();

    // 3. Manejo de Pestañas Principales
    const tabDebugBtn = document.getElementById('tab-debug-btn');
    const tabInteractiveBtn = document.getElementById('tab-interactive-btn');
    const tabRubricBtn = document.getElementById('tab-rubric-btn');

    const debugContainer = document.getElementById('debug-demo-container');
    const interactiveContainer = document.getElementById('interactive-demo-container');
    const rubricContainer = document.getElementById('rubric-spec-container');

    function switchTab(activeTab) {
        // Ocultar todos
        debugContainer.classList.add('hidden');
        interactiveContainer.classList.add('hidden');
        rubricContainer.classList.add('hidden');

        // Resetear estilos de tabs
        [tabDebugBtn, tabInteractiveBtn, tabRubricBtn].forEach(btn => {
            btn.className = 'nav-tab-btn px-4 py-2 rounded-xl text-xs font-semibold text-slate-400 hover:text-white hover:bg-slate-800/60 transition flex items-center gap-2';
        });

        if (activeTab === 'debug') {
            debugContainer.classList.remove('hidden');
            tabDebugBtn.className = 'nav-tab-btn px-4 py-2 rounded-xl text-xs font-bold bg-cyan-950/70 text-cyan-400 border border-cyan-500/40 shadow-lg shadow-cyan-500/10 flex items-center gap-2';
            debugDemo.updateStepView();
        } else if (activeTab === 'interactive') {
            interactiveContainer.classList.remove('hidden');
            tabInteractiveBtn.className = 'nav-tab-btn px-4 py-2 rounded-xl text-xs font-bold bg-emerald-950/70 text-emerald-400 border border-emerald-500/40 shadow-lg shadow-emerald-500/10 flex items-center gap-2';
            interactiveDemo.updateCaseDetail();
        } else if (activeTab === 'rubric') {
            rubricContainer.classList.remove('hidden');
            tabRubricBtn.className = 'nav-tab-btn px-4 py-2 rounded-xl text-xs font-bold bg-purple-950/70 text-purple-400 border border-purple-500/40 shadow-lg shadow-purple-500/10 flex items-center gap-2';
        }

        if (window.lucide) lucide.createIcons();
    }

    if (tabDebugBtn) tabDebugBtn.addEventListener('click', () => switchTab('debug'));
    if (tabInteractiveBtn) tabInteractiveBtn.addEventListener('click', () => switchTab('interactive'));
    if (tabRubricBtn) tabRubricBtn.addEventListener('click', () => switchTab('rubric'));

    // 4. Modal de Ayuda & Arquitectura
    const helpModalBtn = document.getElementById('open-help-modal-btn');
    const helpModal = document.getElementById('help-modal');
    const closeHelpModalBtn = document.getElementById('close-help-modal-btn');

    if (helpModalBtn && helpModal) {
        helpModalBtn.addEventListener('click', () => helpModal.classList.remove('hidden'));
    }
    if (closeHelpModalBtn && helpModal) {
        closeHelpModalBtn.addEventListener('click', () => helpModal.classList.add('hidden'));
    }
    if (helpModal) {
        helpModal.addEventListener('click', (e) => {
            if (e.target === helpModal) helpModal.classList.add('hidden');
        });
    }

    // 5. Atajos de Teclado
    document.addEventListener('keydown', (e) => {
        // Si el foco está en un input de texto, ignorar atajos
        if (['INPUT', 'TEXTAREA', 'SELECT'].includes(document.activeElement.tagName)) return;

        if (e.key === '1') switchTab('debug');
        if (e.key === '2') switchTab('interactive');
        if (e.key === '3') switchTab('rubric');
        if (e.key === ' ' && !debugContainer.classList.contains('hidden')) {
            e.preventDefault();
            debugDemo.togglePlay();
        }
        if (e.key === 'ArrowRight' && !debugContainer.classList.contains('hidden')) {
            debugDemo.nextStep();
        }
        if (e.key === 'ArrowLeft' && !debugContainer.classList.contains('hidden')) {
            debugDemo.prevStep();
        }
        if (e.key === 'Escape' && helpModal && !helpModal.classList.contains('hidden')) {
            helpModal.classList.add('hidden');
        }
    });

    // Renderizado inicial de iconos de Lucide
    if (window.lucide) lucide.createIcons();
});
