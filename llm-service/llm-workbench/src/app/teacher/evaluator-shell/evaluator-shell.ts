import { HttpErrorResponse, httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { CourseContextService } from '../../course-context.service';
import { ToastService } from '../toast.service';

@Component({
  selector: 'app-evaluator-shell',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './evaluator-shell.component.html',
  styleUrl: './evaluator-shell.component.scss',
})
export class EvaluatorShell {
  readonly courseId = input('');
  private readonly courseContext = inject(CourseContextService);
  private readonly router = inject(Router);
  readonly toast = inject(ToastService);
  readonly courses = toSignal(this.courseContext.courses(), { initialValue: [] });
  readonly course = computed(() => this.courses().find((course) => course.id === this.courseId()));
  readonly currentUrl = toSignal(this.router.events.pipe(filter(event => event instanceof NavigationEnd), map(() => this.router.url)), { initialValue: this.router.url });
  readonly goldenSetBreadcrumb = computed(() => {
    const url = this.currentUrl();
    return url.includes('/golden-set/new') ? 'Nuevo Golden Set' : '';
  });
  readonly activeCalibration = httpResource<ActiveCalibration>(() => {
    const courseId = this.courseId();
    return courseId ? `/api/llm/courses/${courseId}/active-calibration` : undefined;
  });
  readonly assignments = httpResource<AssignmentPage>(() => {
    const courseId = this.courseId();
    return courseId ? `/api/llm/courses/${courseId}/challenge-calibration-assignments` : undefined;
  }, { defaultValue: { items: [] } });
  readonly pendingEvaluations = httpResource<PendingEvaluationPage>(() => {
    const courseId = this.courseId();
    return courseId ? `/api/llm/courses/${courseId}/pending-evaluations` : undefined;
  }, { defaultValue: { items: [] } });
  readonly hasActiveCalibration = computed(() => this.activeCalibration.hasValue());
  readonly hasNoActiveCalibration = computed(() => {
    const error = this.activeCalibration.error();
    return error instanceof HttpErrorResponse && error.status === 404;
  });
  readonly calibrationLabel = computed(() => this.activeCalibration.isLoading() ? 'Consultando calibración…' : this.hasActiveCalibration() ? 'Calibración activa' : this.hasNoActiveCalibration() ? 'Sin calibración activa' : this.activeCalibration.error() ? 'No se pudo consultar la calibración' : 'Sin calibración activa');
  readonly sectionContent = computed(() => {
    const url = this.currentUrl();
    if (url.includes('/rubricas/new')) return { label: 'RÚBRICAS', title: 'Nueva rúbrica', description: 'Partí de una plantilla institucional aprobada y adaptala a las necesidades del curso.' };
    if (/\/rubricas\/[^/]+\/edit/.test(url)) return { label: 'RÚBRICAS', title: 'Editar rúbrica', description: 'Ajustá criterios, ejemplos y pesos antes de publicar una nueva versión.' };
    if (url.includes('/rubricas')) return { label: 'RÚBRICAS', title: 'Rúbricas versionadas', description: 'Administrá los criterios que orientan la evaluación pedagógica del curso.' };
    if (url.includes('/golden-set/new')) return { label: 'GOLDEN SET', title: 'Nuevo Golden Set', description: 'Prepará casos de referencia para validar el evaluador.' };
    if (/\/golden-set\/[^/]+\/edit/.test(url)) return { label: 'GOLDEN SET', title: 'Editar Golden Set', description: 'Actualizá los casos de referencia de esta versión borrador.' };
    if (url.includes('/golden-set')) return { label: 'GOLDEN SET', title: 'Conversaciones de referencia', description: 'Gestioná los casos que calibran el uso pedagógico de IA.' };
    if (url.includes('/calibraciones')) return { label: 'CALIBRACIONES', title: 'Calibración del evaluador', description: 'Validá la concordancia del evaluador contra el estándar PAR-14.' };
    if (url.includes('/llm-api-keys')) return { label: 'ADMINISTRACIÓN LLM', title: 'API keys y evaluador', description: 'Administrá proveedores, modelos candidatos y el evaluador global.' };
    if (url.includes('/asignaciones')) return { label: 'ASIGNACIONES', title: 'Desafíos y evaluaciones', description: 'Consultá la calibración asociada a cada desafío del curso.' };
    if (url.includes('/como-usar')) return { label: 'GUÍA RÁPIDA', title: 'Cómo usar TUP-Golde-Set', description: 'Configurá una evaluación pedagógica trazable en cuatro pasos.' };
    return { label: 'RESUMEN', title: 'Estado del evaluador', description: 'Monitoreá la calibración activa, alertas y accesos a borradores.' };
  });
  currentSection(): string {
    const url = this.currentUrl();
    return ['resumen', 'rubricas', 'golden-set', 'calibraciones', 'llm-api-keys', 'asignaciones', 'como-usar'].find(section => url.includes(`/${section}`)) ?? 'resumen';
  }
  navigateSection(event: Event): void {
    const section = (event.target as HTMLSelectElement).value;
    void this.router.navigate(['/docente', 'cursos', this.courseId(), 'evaluador', section]);
  }
}

interface ActiveCalibration { courseId: string; calibrationRunId: string; activatedAt: string; }
interface AssignmentPage { items: { challengeId: string; calibrationRunId: string; locked: boolean }[]; }
interface PendingEvaluationPage { items: { id: string; attemptId: string; calibrationRunId: string; state: string }[]; }
