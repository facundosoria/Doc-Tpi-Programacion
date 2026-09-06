/**
 * SCORING ENGINE & CIRCUIT BREAKER (LLMOps)
 * Implementa el Scoring Híbrido Determinístico (RF-IA-15),
 * el cálculo del MAE, el Disyuntor de Deriva (PAR-14) y el muestreo de auditoría (PAR-10).
 */

class ScoringEngine {
    constructor(rubricConfig = null) {
        this.rubric = rubricConfig || (typeof RUBRIC_CONFIG !== 'undefined' ? RUBRIC_CONFIG : {
            tolerancia_par14: 5.0,
            dimensiones: [
                { id: "autonomia", peso: 0.30 },
                { id: "claridad", peso: 0.25 },
                { id: "progresion", peso: 0.20 },
                { id: "cumplimiento", peso: 0.15 },
                { id: "eficiencia", peso: 0.10 }
            ]
        });
    }

    /**
     * Calcula la nota ponderada oficial (RF-IA-15) a partir de los puntajes 0-100 de las 5 dimensiones.
     * La aritmética se ejecuta SIEMPRE en código determinístico, nunca en el LLM.
     */
    calculateWeightedScore(dimensionScores) {
        const d1 = Number(dimensionScores.autonomia || 0);
        const d2 = Number(dimensionScores.claridad || 0);
        const d3 = Number(dimensionScores.progresion || 0);
        const d4 = Number(dimensionScores.cumplimiento || 0);
        const d5 = Number(dimensionScores.eficiencia || 0);

        const total = (d1 * 0.30) + (d2 * 0.25) + (d3 * 0.20) + (d4 * 0.15) + (d5 * 0.10);
        return Math.round(total * 100) / 100;
    }

    /**
     * Calcula métricas objetivas determinísticas basadas en telemetría de IDE y guardarraíles.
     * Entre el 45% y 60% de la rúbrica está anclada a estos valores calculados en código.
     */
    extractDeterministicMetrics(telemetria) {
        const {
            ediciones_antes_primer_mensaje = 0,
            ejecuciones_test_previas = 0,
            tiempo_hasta_primer_mensaje_segundos = 0,
            mensajes_triviales = 0,
            incidentes_jailbreak = 0,
            turnos_totales = 1
        } = telemetria;

        // 1. Scoring Determinístico de Cumplimiento (D4):
        // Base 100, cada jailbreak/inyección detectada por Capa 1/AST descuenta 45 pts.
        let baseCumplimiento = 100 - (incidentes_jailbreak * 45);
        if (baseCumplimiento < 10) baseCumplimiento = 10;

        // 2. Scoring Determinístico de Eficiencia (D5):
        // Penaliza ráfagas de mensajes vacíos o relación señal/ruido pobre.
        let ratioTrivial = mensajes_triviales / Math.max(turnos_totales, 1);
        let baseEficiencia = Math.max(10, Math.round(100 - (ratioTrivial * 80) - (mensajes_triviales * 10)));

        // 3. Indicador de Autonomía Previa (Bonus determinístico para D1):
        let scoreAutonomiaPrevia = 0;
        if (ediciones_antes_primer_mensaje > 0 || ejecuciones_test_previas > 0) {
            scoreAutonomiaPrevia = Math.min(100, (ediciones_antes_primer_mensaje * 5) + (ejecuciones_test_previas * 15));
        }

        return {
            baseCumplimiento,
            baseEficiencia,
            scoreAutonomiaPrevia,
            tiempo_espera_minutos: (tiempo_hasta_primer_mensaje_segundos / 60).toFixed(1),
            ratioTrivial: (ratioTrivial * 100).toFixed(0) + "%"
        };
    }

    /**
     * Calcula la desviación absoluta individual: |Score_IA - Score_Docente|
     */
    calculateDeviation(scoreIA, scoreDocente) {
        const diff = Math.abs(scoreIA - scoreDocente);
        return Math.round(diff * 100) / 100;
    }

    /**
     * Calcula el Error Absoluto Medio (MAE) y estadísticas de calibración del Golden Set.
     */
    evaluateCalibrationRun(items) {
        if (!items || items.length === 0) {
            return {
                total_items: 0,
                mae: 0,
                max_desviacion: 0,
                aprobado: false,
                detalles: []
            };
        }

        let sumaDesvios = 0;
        let maxDesvio = 0;
        let itemsAprobados = 0;

        const detalles = items.map(item => {
            const scoreIA = typeof item.score_ia === 'number' ? item.score_ia : item.score_ia?.total || 0;
            const scoreDocente = item.score_docente?.total || item.score_humano_esperado || 0;
            const desvio = this.calculateDeviation(scoreIA, scoreDocente);

            sumaDesvios += desvio;
            if (desvio > maxDesvio) maxDesvio = desvio;

            const pasaToleranciaIndividual = desvio <= this.rubric.tolerancia_par14;
            if (pasaToleranciaIndividual) itemsAprobados++;

            return {
                id: item.id,
                estudiante: item.estudiante,
                topic_id: item.topic_id,
                score_docente: scoreDocente,
                score_ia: scoreIA,
                desvio: desvio,
                aprobado: pasaToleranciaIndividual,
                dimensiones_docente: item.score_docente,
                dimensiones_ia: item.score_ia_dimensiones || {}
            };
        });

        const mae = Math.round((sumaDesvios / items.length) * 100) / 100;
        const tolerancia = this.rubric.tolerancia_par14;
        const aprobado = mae <= tolerancia;

        return {
            total_items: items.length,
            mae: mae,
            tolerancia_maxima: tolerancia,
            max_desviacion: Math.round(maxDesvio * 100) / 100,
            tasa_aprobacion: Math.round((itemsAprobados / items.length) * 100),
            aprobado: aprobado,
            circuit_breaker_tripped: !aprobado,
            estado_curso: aprobado ? "ACTIVE" : "BLOQUEADO (DRAFT)",
            detalles: detalles
        };
    }

    /**
     * Determina si una evaluación requiere auditoría humana según PAR-10
     * (Confianza < 0.70 o muestreo estadístico del 10%).
     */
    requiresHumanAudit(confidenceScore, forceSamplingRate = 0.10) {
        if (confidenceScore < 0.70) {
            return {
                requiere_auditoria: true,
                motivo: "Baja Confianza del Modelo (< 0.70)"
            };
        }
        const isSampled = Math.random() < forceSamplingRate;
        if (isSampled) {
            return {
                requiere_auditoria: true,
                motivo: "Muestreo Estadístico Aleatorio del 10% (PAR-10)"
            };
        }
        return {
            requiere_auditoria: false,
            motivo: "Aprobación Automática (Confianza >= 0.70)"
        };
    }

    /**
     * Motor de simulación para calibración calibrada (MAE <= 2.5 pts).
     * Útil para demostraciones automáticas o cuando no se dispone de API Key.
     */
    simulateCalibratedEvaluation(caseItem) {
        const docente = caseItem.score_docente;
        
        // Simula leves fluctuaciones naturales del LLM (T=0) dentro del margen de ±2 pts
        const jitter = () => (Math.sin(caseItem.id.charCodeAt(3) || 1) * 1.8);

        const d1 = Math.min(100, Math.max(0, Math.round(docente.autonomia + (jitter() * 0.8))));
        const d2 = Math.min(100, Math.max(0, Math.round(docente.claridad + (jitter() * -0.6))));
        const d3 = Math.min(100, Math.max(0, Math.round(docente.progresion + (jitter() * 0.5))));
        const d4 = Math.min(100, Math.max(0, Math.round(docente.cumplimiento + (jitter() * -0.4))));
        const d5 = Math.min(100, Math.max(0, Math.round(docente.eficiencia + (jitter() * 0.7))));

        const total = this.calculateWeightedScore({
            autonomia: d1,
            claridad: d2,
            progresion: d3,
            cumplimiento: d4,
            eficiencia: d5
        });

        return {
            score_total: total,
            dimensiones: {
                autonomia: { puntaje: d1, justificacion: `Evaluación semántica de autonomía coherente con el benchmark docente (${docente.autonomia} pts).` },
                claridad: { puntaje: d2, justificacion: `Evaluación de especificidad y formulación del problema alineada con ancla de cátedra.` },
                progresion: { puntaje: d3, justificacion: `Verificación del progreso iterativo tras pistas socráticas del tutor.` },
                cumplimiento: { puntaje: d4, justificacion: `Análisis de apego a guardarraíles y detección de pedidos de solución.` },
                eficiencia: { puntaje: d5, justificacion: `Métrica de densidad informativa por turno de consulta.` }
            },
            confidence_score: 0.94,
            senales_de_manipulacion: caseItem.telemetria.incidentes_jailbreak > 0,
            modelo_usado: "Claude Haiku 4.5 / Gemini 2.5 Flash (Calibrado)",
            tiempo_ms: 380
        };
    }

    /**
     * Motor de simulación para model drift severo (MAE > 7.5 pts).
     * Demuestra cómo el Circuit Breaker de PAR-14 bloquea automáticamente la publicación del curso.
     */
    simulateDriftEvaluation(caseItem) {
        const docente = caseItem.score_docente;
        // Modelo con deriva: sobrecalifica casos débiles (+12 pts) y castiga casos avanzados (-10 pts)
        const bias = docente.total < 50 ? 14 : -9;

        const d1 = Math.min(100, Math.max(0, Math.round(docente.autonomia + bias + (Math.random() * 4 - 2))));
        const d2 = Math.min(100, Math.max(0, Math.round(docente.claridad + bias)));
        const d3 = Math.min(100, Math.max(0, Math.round(docente.progresion + bias * 0.8)));
        const d4 = Math.min(100, Math.max(0, Math.round(docente.cumplimiento + (bias > 0 ? 10 : -8))));
        const d5 = Math.min(100, Math.max(0, Math.round(docente.eficiencia + bias)));

        const total = this.calculateWeightedScore({
            autonomia: d1,
            claridad: d2,
            progresion: d3,
            cumplimiento: d4,
            eficiencia: d5
        });

        return {
            score_total: total,
            dimensiones: {
                autonomia: { puntaje: d1, justificacion: `[DERIVA DETECTADA] Sesgo de severidad alterado en los pesos del modelo.` },
                claridad: { puntaje: d2, justificacion: `[DERIVA DETECTADA] Discrepancia con las directivas de rúbrica oficiales.` },
                progresion: { puntaje: d3, justificacion: `[DERIVA DETECTADA] Juicio semántico descalibrado.` },
                cumplimiento: { puntaje: d4, justificacion: `[DERIVA DETECTADA] Evaluación distorsionada de incidentes.` },
                eficiencia: { puntaje: d5, justificacion: `[DERIVA DETECTADA] Error acumulativo en métrica de turnos.` }
            },
            confidence_score: 0.62,
            senales_de_manipulacion: false,
            modelo_usado: "Modelo No Calibrado / Versión con Drift",
            tiempo_ms: 320
        };
    }
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = ScoringEngine;
}
