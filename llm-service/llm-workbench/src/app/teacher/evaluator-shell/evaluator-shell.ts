import { HttpErrorResponse, httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, input } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { filter, map } from 'rxjs';
import { CourseContextService } from '../../course-context.service';

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
  readonly calibrationDescription = computed(() => this.activeCalibration.isLoading() ? 'Actualizando el estado del curso.' : this.hasActiveCalibration() ? 'Las nuevas evaluaciones usan una versión trazable.' : this.hasNoActiveCalibration() ? 'Hacé una calibración aprobada antes de habilitar desafíos.' : this.activeCalibration.error() ? 'Reintentá al recuperar conexión con el servicio.' : 'Hacé una calibración aprobada antes de habilitar desafíos.');
  currentSection(): string {
    const url = this.currentUrl();
    return ['resumen', 'rubricas', 'golden-set', 'calibraciones', 'asignaciones', 'como-usar'].find(section => url.includes(`/${section}`)) ?? 'resumen';
  }
  navigateSection(event: Event): void {
    const section = (event.target as HTMLSelectElement).value;
    void this.router.navigate(['/docente', 'cursos', this.courseId(), 'evaluador', section]);
  }
}

interface ActiveCalibration { courseId: string; calibrationRunId: string; activatedAt: string; }
interface AssignmentPage { items: { challengeId: string; calibrationRunId: string; locked: boolean }[]; }
interface PendingEvaluationPage { items: { id: string; attemptId: string; calibrationRunId: string; state: string }[]; }
