/**
 * CHARTS & VISUALIZATIONS
 * Renderiza gráficos interactivos usando HTML5 Canvas nativo para visualización 5D,
 * métricas de desvío y medidor de Disyuntor PAR-14.
 */

class GoldenSetCharts {
    /**
     * Dibuja un Radar Chart (Gráfico Radial) de las 5 dimensiones comparando Docente vs IA.
     */
    static drawRadarChart(canvasId, docenteScores, iaScores = null) {
        const canvas = document.getElementById(canvasId);
        if (!canvas) return;
        const ctx = canvas.getContext('2d');
        const width = canvas.width;
        const height = canvas.height;
        const centerX = width / 2;
        const centerY = height / 2;
        const radius = Math.min(centerX, centerY) - 40;

        ctx.clearRect(0, 0, width, height);

        const dimensions = [
            { key: 'autonomia', label: 'Autonomía (30%)', color: '#10b981' },
            { key: 'claridad', label: 'Claridad (25%)', color: '#38bdf8' },
            { key: 'progresion', label: 'Progresión (20%)', color: '#818cf8' },
            { key: 'cumplimiento', label: 'Límites (15%)', color: '#f59e0b' },
            { key: 'eficiencia', label: 'Eficiencia (10%)', color: '#ec4899' }
        ];

        const totalDims = dimensions.length;
        const angleStep = (Math.PI * 2) / totalDims;
        const startAngle = -Math.PI / 2;

        // 1. Dibujar círculos concéntricos de referencia (20, 40, 60, 80, 100)
        const levels = [20, 40, 60, 80, 100];
        ctx.strokeStyle = 'rgba(148, 163, 184, 0.15)';
        ctx.lineWidth = 1;

        levels.forEach(level => {
            const r = (level / 100) * radius;
            ctx.beginPath();
            for (let i = 0; i <= totalDims; i++) {
                const angle = startAngle + i * angleStep;
                const x = centerX + r * Math.cos(angle);
                const y = centerY + r * Math.sin(angle);
                if (i === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            }
            ctx.closePath();
            ctx.stroke();

            // Etiqueta de nivel
            ctx.fillStyle = 'rgba(148, 163, 184, 0.35)';
            ctx.font = '10px monospace';
            ctx.fillText(`${level}`, centerX + 5, centerY - r + 10);
        });

        // 2. Dibujar ejes radiales y etiquetas de dimensiones
        dimensions.forEach((dim, i) => {
            const angle = startAngle + i * angleStep;
            const x = centerX + radius * Math.cos(angle);
            const y = centerY + radius * Math.sin(angle);

            ctx.beginPath();
            ctx.strokeStyle = 'rgba(148, 163, 184, 0.25)';
            ctx.moveTo(centerX, centerY);
            ctx.lineTo(x, y);
            ctx.stroke();

            // Posición de etiqueta
            const labelDist = radius + 22;
            const lx = centerX + labelDist * Math.cos(angle);
            const ly = centerY + labelDist * Math.sin(angle);

            ctx.fillStyle = '#cbd5e1';
            ctx.font = 'bold 11px system-ui, sans-serif';
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(dim.label, lx, ly);
        });

        // Función auxiliar para obtener coordenadas de polígono
        const getPoints = (scoreObj) => {
            return dimensions.map((dim, i) => {
                const angle = startAngle + i * angleStep;
                let val = 0;
                if (scoreObj) {
                    if (typeof scoreObj[dim.key] === 'number') {
                        val = scoreObj[dim.key];
                    } else if (scoreObj[dim.key]?.puntaje) {
                        val = scoreObj[dim.key].puntaje;
                    }
                }
                const r = (Math.max(0, Math.min(100, val)) / 100) * radius;
                return {
                    x: centerX + r * Math.cos(angle),
                    y: centerY + r * Math.sin(angle),
                    val: val
                };
            });
        };

        // 3. Polígono DOCENTE (Patrón Golden Set) - Color Cyan / Azul
        if (docenteScores) {
            const ptsDocente = getPoints(docenteScores);
            ctx.beginPath();
            ptsDocente.forEach((p, i) => {
                if (i === 0) ctx.moveTo(p.x, p.y);
                else ctx.lineTo(p.x, p.y);
            });
            ctx.closePath();
            ctx.fillStyle = 'rgba(56, 189, 248, 0.22)';
            ctx.fill();
            ctx.strokeStyle = '#38bdf8';
            ctx.lineWidth = 2.5;
            ctx.stroke();

            // Puntos
            ptsDocente.forEach(p => {
                ctx.beginPath();
                ctx.arc(p.x, p.y, 4, 0, Math.PI * 2);
                ctx.fillStyle = '#38bdf8';
                ctx.fill();
            });
        }

        // 4. Polígono IA (Evaluador LLM) - Color Violeta / Magenta
        if (iaScores) {
            const ptsIA = getPoints(iaScores);
            ctx.beginPath();
            ptsIA.forEach((p, i) => {
                if (i === 0) ctx.moveTo(p.x, p.y);
                else ctx.lineTo(p.x, p.y);
            });
            ctx.closePath();
            ctx.fillStyle = 'rgba(168, 85, 247, 0.25)';
            ctx.fill();
            ctx.strokeStyle = '#a855f7';
            ctx.lineWidth = 2.5;
            ctx.stroke();

            // Puntos
            ptsIA.forEach(p => {
                ctx.beginPath();
                ctx.arc(p.x, p.y, 4.5, 0, Math.PI * 2);
                ctx.fillStyle = '#ec4899';
                ctx.fill();
                ctx.strokeStyle = '#ffffff';
                ctx.lineWidth = 1.5;
                ctx.stroke();
            });
        }
    }

    /**
     * Dibuja una barra de progreso animada de calibración y error MAE.
     */
    static drawMaeGauge(elementId, currentMae, tolerance = 5.0) {
        const container = document.getElementById(elementId);
        if (!container) return;

        const maxScale = 15.0;
        const percentage = Math.min(100, (currentMae / maxScale) * 100);
        const isOk = currentMae <= tolerance;
        const color = isOk ? 'emerald' : 'rose';

        container.innerHTML = `
            <div class="flex items-center justify-between text-xs font-mono mb-1.5">
                <span class="text-slate-400">MAE Actual: <strong class="text-${color}-400 text-sm font-bold">${currentMae.toFixed(2)} pts</strong></span>
                <span class="text-slate-400">Umbral PAR-14: <strong class="text-amber-400">±${tolerance.toFixed(1)} pts</strong></span>
            </div>
            <div class="relative w-full h-3.5 bg-slate-800 rounded-full overflow-hidden border border-slate-700">
                <!-- Marca del umbral 5.0 -->
                <div class="absolute top-0 bottom-0 w-0.5 bg-amber-400 z-10" style="left: ${(tolerance / maxScale) * 100}%" title="Límite PAR-14 (5.0 pts)"></div>
                <!-- Barra de valor -->
                <div class="h-full bg-gradient-to-r from-${isOk ? 'cyan-500 to-emerald-500' : 'amber-500 to-rose-500'} transition-all duration-500" style="width: ${percentage}%"></div>
            </div>
            <div class="flex justify-between text-[10px] font-mono text-slate-500 mt-1">
                <span>0.0</span>
                <span>5.0 (Límite)</span>
                <span>10.0</span>
                <span>15.0+</span>
            </div>
        `;
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = GoldenSetCharts;
}
