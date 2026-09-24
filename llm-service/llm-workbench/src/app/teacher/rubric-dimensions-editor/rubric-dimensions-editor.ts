import { ChangeDetectionStrategy, Component, computed, effect, input, output, signal } from '@angular/core';

export interface EditorAnchor { behavior: string; referenceScore: number | null; example: string; }
export interface EditorAnchors { low: EditorAnchor; medium: EditorAnchor; high: EditorAnchor; }
export interface EditorDimension {
  key: string;
  label: string;
  criterion: string;
  anchors: EditorAnchors;
  weight: number;
}

const ANCHOR_LEVELS: (keyof EditorAnchors)[] = ['low', 'medium', 'high'];

const PRESET_DIMENSIONS: EditorDimension[] = [
  {
    key: 'comprension', label: 'Comprensión del problema', weight: 12,
    criterion: 'Entiende el enunciado y sus restricciones antes de consultar o codificar.',
    anchors: {
      low: { behavior: 'Empieza a codificar o consultar sin identificar qué pide el enunciado.', referenceScore: 25, example: 'Pide la solución directa sin mencionar restricciones ni formato de entrada.' },
      medium: { behavior: 'Identifica el objetivo central pero omite algún caso o restricción.', referenceScore: 60, example: 'Sabe que debe leer entrada estándar, pero no el formato exacto de salida.' },
      high: { behavior: 'Desglosa el enunciado y explicita entradas, salidas y casos límite.', referenceScore: 90, example: 'Describe el formato de entrada/salida y las restricciones antes de escribir código.' },
    },
  },
  {
    key: 'autonomia', label: 'Autonomía', weight: 14,
    criterion: 'Intenta resolver por sí mismo antes de pedir ayuda al asistente.',
    anchors: {
      low: { behavior: 'Copia la sugerencia sin elaborarla ni comprobarla.', referenceScore: 25, example: 'Repite el código sugerido sin explicar qué cambió.' },
      medium: { behavior: 'Aplica una sugerencia y realiza una verificación parcial.', referenceScore: 60, example: 'Prueba la alternativa indicada y comenta brevemente el resultado.' },
      high: { behavior: 'Formula hipótesis, contrasta alternativas y verifica su solución.', referenceScore: 90, example: 'Explica por qué elige una estrategia y valida casos límite.' },
    },
  },
  {
    key: 'claridad', label: 'Claridad de la consulta', weight: 11,
    criterion: 'Formula preguntas precisas, con contexto y evidencia.',
    anchors: {
      low: { behavior: 'Formula pedidos vagos sin contexto del problema.', referenceScore: 25, example: 'Pregunta "no funciona" sin mostrar error ni intento.' },
      medium: { behavior: 'Describe parte del problema y aporta información útil.', referenceScore: 60, example: 'Indica el error y el fragmento donde ocurre.' },
      high: { behavior: 'Formula una consulta precisa con contexto, hipótesis y evidencia.', referenceScore: 90, example: 'Explica el comportamiento esperado, el resultado y qué ya probó.' },
    },
  },
  {
    key: 'progresion', label: 'Progresión', weight: 10,
    criterion: 'Incorpora la ayuda recibida y avanza entre mensajes.',
    anchors: {
      low: { behavior: 'Repite la misma consulta sin aplicar lo respondido.', referenceScore: 25, example: 'Vuelve a preguntar lo mismo sin probar el cambio sugerido.' },
      medium: { behavior: 'Aplica parcialmente la orientación y logra un avance.', referenceScore: 60, example: 'Corrige un error señalado pero necesita una nueva guía para el siguiente.' },
      high: { behavior: 'Itera, aprende de la respuesta y resuelve el siguiente paso con independencia.', referenceScore: 90, example: 'Ajusta su enfoque y explica el avance obtenido.' },
    },
  },
  {
    key: 'sintaxis', label: 'Correctitud del código', weight: 10,
    criterion: 'Escribe sintaxis y estructuras válidas del lenguaje.',
    anchors: {
      low: { behavior: 'Genera código con errores de sintaxis o estructuras inapropiadas.', referenceScore: 25, example: 'Usa tipos o métodos que no existen en la versión del lenguaje.' },
      medium: { behavior: 'Escribe código que compila con advertencias o detalles mejorables.', referenceScore: 60, example: 'Compila, pero deja variables sin usar o imports redundantes.' },
      high: { behavior: 'Produce código correcto y consistente con las convenciones del lenguaje.', referenceScore: 90, example: 'Usa las estructuras esperadas y el estilo del desafío.' },
    },
  },
  {
    key: 'errores', label: 'Manejo de errores', weight: 10,
    criterion: 'Interpreta los mensajes de error y los corrige.',
    anchors: {
      low: { behavior: 'Muestra el error sin intentar interpretarlo o lo ignora.', referenceScore: 25, example: 'Pega el stacktrace y espera la respuesta sin analizarlo.' },
      medium: { behavior: 'Lee el error, identifica la zona del problema e intenta un arreglo.', referenceScore: 60, example: 'Localiza la línea del error y prueba una corrección razonada.' },
      high: { behavior: 'Diagnostica la causa, corrige y valida que el caso vuelva a pasar.', referenceScore: 90, example: 'Explica la causa del fallo y verifica con la entrada original.' },
    },
  },
  {
    key: 'eficiencia', label: 'Eficiencia de la solución', weight: 8,
    criterion: 'Resuelve sin redundancias y con la estructura adecuada.',
    anchors: {
      low: { behavior: 'Resuelve de forma correcta pero con complejidad innecesaria.', referenceScore: 25, example: 'Usa bucles anidados donde una estructura estándar alcanza.' },
      medium: { behavior: 'Elige una solución razonable con algún punto mejorable.', referenceScore: 60, example: 'Funciona, pero repite cálculos que podrían reutilizarse.' },
      high: { behavior: 'Selecciona la estructura y complejidad adecuadas al problema.', referenceScore: 90, example: 'Usa la colección o algoritmo que mejor se ajusta al volumen de datos.' },
    },
  },
  {
    key: 'verificacion-borde', label: 'Verificación de casos borde', weight: 10,
    criterion: 'Prueba la solución con casos normales y de borde.',
    anchors: {
      low: { behavior: 'Entrega sin probar o solo con un caso de ejemplo.', referenceScore: 25, example: 'Prueba únicamente la entrada del enunciado.' },
      medium: { behavior: 'Prueba el caso principal y algún caso extra.', referenceScore: 60, example: 'Verifica además valores límite o entradas vacías.' },
      high: { behavior: 'Diseña casos de borde y valida contra el formato esperado.', referenceScore: 90, example: 'Prueba máximos, negativos, vacíos y compara con la salida exacta.' },
    },
  },
  {
    key: 'limites', label: 'Cumplimiento de límites', weight: 8,
    criterion: 'Respeta las reglas de uso de IA; no pide la solución final.',
    anchors: {
      low: { behavior: 'Intenta obtener la solución completa o hacer que el asistente resuelva por él.', referenceScore: 25, example: 'Pide que le den el código terminado del desafío.' },
      medium: { behavior: 'Respeta los límites tras recibir una aclaración.', referenceScore: 60, example: 'Reformula una solicitud demasiado directa.' },
      high: { behavior: 'Solicita orientación conceptual dentro de las reglas.', referenceScore: 90, example: 'Pide una pista sobre la estructura y desarrolla su propia solución.' },
    },
  },
  {
    key: 'justificacion', label: 'Justificación', weight: 7,
    criterion: 'Justifica sus decisiones técnicas y explica el resultado de su trabajo.',
    anchors: {
      low: { behavior: 'No describe su razonamiento ni el resultado de lo intentado.', referenceScore: 25, example: 'Solo envía el código sin comentar su enfoque.' },
      medium: { behavior: 'Comenta parcialmente sus decisiones o el estado de avance.', referenceScore: 60, example: 'Explica qué probó pero no por qué eligió ese camino.' },
      high: { behavior: 'Relata su proceso: hipótesis, decisiones, resultados y aprendizaje.', referenceScore: 90, example: 'Cuenta qué intentó, qué falló, cómo lo corrigió y qué concluyó.' },
    },
  },
];

@Component({
  selector: 'app-rubric-dimensions-editor',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './rubric-dimensions-editor.component.html',
  styleUrl: './rubric-dimensions-editor.component.scss',
})
export class RubricDimensionsEditor {
  readonly dimensions = input<EditorDimension[]>([]);
  readonly save = output<EditorDimension[]>();
  readonly cancel = output<void>();

  readonly anchorLevels: (keyof EditorAnchors)[] = ANCHOR_LEVELS;

  readonly local = signal<EditorDimension[]>([]);
  readonly collapsedKeys = signal<Set<string>>(new Set());

  readonly totalWeight = computed(() =>
    this.local().reduce((total, dimension) => total + Number(dimension.weight || 0), 0));

  readonly valid = computed(() => {
    const dimensions = this.local();
    return dimensions.length > 0
      && this.totalWeight() === 100
      && dimensions.every(isCompleteDimension);
  });

  readonly validationMessage = computed(() => {
    const dimensions = this.local();
    if (dimensions.length === 0) return 'Agregá al menos una dimensión para poder guardar el borrador.';
    if (this.totalWeight() !== 100) return `Los pesos deben sumar 100 % (actual: ${this.totalWeight()} %).`;
    const missing = dimensions.filter((dimension) => !isCompleteDimension(dimension));
    if (missing.length > 0) {
      const names = missing.slice(0, 3).map((dimension) => dimension.label || dimension.key).join(', ');
      const rest = missing.length > 3 ? ` y ${missing.length - 3} más` : '';
      return `Completá el criterio y las tres anclas de: ${names}${rest}.`;
    }
    return '';
  });

  constructor() {
    effect(() => this.local.set(cloneDimensions(this.dimensions())));
  }

  isCollapsed(key: string): boolean {
    return this.collapsedKeys().has(key);
  }

  toggleCollapse(key: string): void {
    const next = new Set(this.collapsedKeys());
    if (next.has(key)) { next.delete(key); } else { next.add(key); }
    this.collapsedKeys.set(next);
  }

  loadPreset(): void {
    this.local.set(cloneDimensions(PRESET_DIMENSIONS));
    this.collapsedKeys.set(new Set(PRESET_DIMENSIONS.map((dimension) => dimension.key)));
  }

  onWeight(dimension: EditorDimension, event: Event): void {
    dimension.weight = Number((event.target as HTMLInputElement).value);
    this.refresh();
  }

  onCriterion(dimension: EditorDimension, event: Event): void {
    dimension.criterion = (event.target as HTMLTextAreaElement).value;
    this.refresh();
  }

  onAnchorBehavior(dimension: EditorDimension, level: keyof EditorAnchors, event: Event): void {
    dimension.anchors[level].behavior = (event.target as HTMLTextAreaElement).value;
    this.refresh();
  }

  onAnchorScore(dimension: EditorDimension, level: keyof EditorAnchors, event: Event): void {
    const raw = (event.target as HTMLInputElement).value;
    dimension.anchors[level].referenceScore = raw.trim() === '' ? null : Number(raw);
    this.refresh();
  }

  onAnchorExample(dimension: EditorDimension, level: keyof EditorAnchors, event: Event): void {
    dimension.anchors[level].example = (event.target as HTMLTextAreaElement).value;
    this.refresh();
  }

  remove(dimension: EditorDimension): void {
    const nextCollapsed = new Set(this.collapsedKeys());
    nextCollapsed.delete(dimension.key);
    this.collapsedKeys.set(nextCollapsed);
    this.local.set(this.local().filter((item) => item !== dimension));
  }

  add(): void {
    const index = this.local().length + 1;
    this.local.set([
      ...this.local(),
      {
        key: `dim-${index}`,
        label: `Dimensión ${index}`,
        criterion: '',
        anchors: {
          low: { behavior: '', referenceScore: 25, example: '' },
          medium: { behavior: '', referenceScore: 60, example: '' },
          high: { behavior: '', referenceScore: 90, example: '' },
        },
        weight: 100,
      },
    ]);
  }

  saveLocal(): void {
    this.save.emit(this.local());
  }

  trackByKey(_index: number, dimension: EditorDimension): string {
    return dimension.key;
  }

  private refresh(): void {
    this.local.set([...this.local()]);
  }
}

function isCompleteDimension(dimension: EditorDimension): boolean {
  if (!dimension.key?.trim() || !dimension.label?.trim() || !dimension.criterion?.trim()) return false;
  return ANCHOR_LEVELS.every((level) => {
    const anchor = dimension.anchors[level];
    return anchor != null
      && Boolean(anchor.behavior?.trim())
      && Boolean(anchor.example?.trim())
      && anchor.referenceScore != null
      && Number.isFinite(anchor.referenceScore);
  });
}

function cloneDimensions(dimensions: EditorDimension[]): EditorDimension[] {
  return dimensions.map((dimension) => ({
    key: dimension.key,
    label: dimension.label,
    criterion: dimension.criterion,
    anchors: {
      low: { ...dimension.anchors.low },
      medium: { ...dimension.anchors.medium },
      high: { ...dimension.anchors.high },
    },
    weight: dimension.weight,
  }));
}