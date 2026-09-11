import { inject } from '@angular/core';
import { CanActivateFn, Router, Routes } from '@angular/router';
import { map } from 'rxjs';
import { CourseContextService } from './course-context.service';
import { CourseEmptyComponent } from './course-empty.component';

const firstAuthorizedCourse: CanActivateFn = () => {
  const context = inject(CourseContextService);
  const router = inject(Router);
  return context.courses().pipe(map((courses) => router.createUrlTree(courses.length
    ? ['/docente', 'cursos', courses[0].id, 'evaluador', 'resumen']
    : ['/docente', 'sin-cursos'])));
};

const authorizedCourseGuard: CanActivateFn = (route) => {
  const context = inject(CourseContextService);
  const router = inject(Router);
  const courseId = route.paramMap.get('courseId');
  return context.courses().pipe(map((courses) => courses.some((course) => course.id === courseId)
    ? true
    : router.createUrlTree(courses.length
      ? ['/docente', 'cursos', courses[0].id, 'evaluador', 'resumen']
      : ['/docente', 'sin-cursos'])));
};

const emptyCourseGuard: CanActivateFn = () => {
  const context = inject(CourseContextService);
  const router = inject(Router);
  return context.courses().pipe(map((courses) => courses.length
    ? router.createUrlTree(['/docente', 'cursos', courses[0].id, 'evaluador', 'resumen'])
    : true));
};

export const routes: Routes = [
  { path: 'docente/cursos/:courseId/evaluador', canActivate: [authorizedCourseGuard], loadComponent: () => import('./teacher/evaluator-shell/evaluator-shell').then((m) => m.EvaluatorShell), children: [
    { path: '', pathMatch: 'full', redirectTo: 'resumen' },
    { path: 'resumen', loadComponent: () => import('./teacher/summary-page/summary-page').then((m) => m.SummaryPage) },
    { path: 'rubricas/new', loadComponent: () => import('./teacher/rubrics-page/rubrics-page').then((m) => m.RubricsPage) },
    { path: 'rubricas/:versionId/edit', loadComponent: () => import('./teacher/rubrics-page/rubrics-page').then((m) => m.RubricsPage) },
    { path: 'rubricas', loadComponent: () => import('./teacher/rubrics-page/rubrics-page').then((m) => m.RubricsPage) },
    { path: 'golden-set/new', loadComponent: () => import('./teacher/golden-set-page/golden-set-page').then((m) => m.GoldenSetPage) },
    { path: 'golden-set/:versionId/edit', loadComponent: () => import('./teacher/golden-set-page/golden-set-page').then((m) => m.GoldenSetPage) },
    { path: 'golden-set', loadComponent: () => import('./teacher/golden-set-page/golden-set-page').then((m) => m.GoldenSetPage) },
    { path: 'calibraciones', loadComponent: () => import('./teacher/calibrations-page/calibrations-page').then((m) => m.CalibrationsPage) },
    { path: 'llm-api-keys', loadComponent: () => import('./teacher/llm-settings-page/llm-settings-page').then((m) => m.LlmSettingsPage) },
    { path: 'asignaciones', loadComponent: () => import('./teacher/assignments-page/assignments-page').then((m) => m.AssignmentsPage) },
    { path: 'como-usar', loadComponent: () => import('./teacher/how-to-use-page/how-to-use-page').then((m) => m.HowToUsePage) },
  ] },
  { path: 'docente/sin-cursos', canActivate: [emptyCourseGuard], component: CourseEmptyComponent },
  { path: 'docente', canActivate: [firstAuthorizedCourse], component: CourseEmptyComponent },
  { path: 'golden-sets/new', canActivate: [firstAuthorizedCourse], component: CourseEmptyComponent },
  { path: 'golden-sets/:id', canActivate: [firstAuthorizedCourse], component: CourseEmptyComponent },
  { path: 'golden-sets', canActivate: [firstAuthorizedCourse], component: CourseEmptyComponent },
  { path: '', pathMatch: 'full', canActivate: [firstAuthorizedCourse], component: CourseEmptyComponent },
  { path: '**', canActivate: [firstAuthorizedCourse], component: CourseEmptyComponent },
];
