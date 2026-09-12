import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterOutlet } from '@angular/router';
import { filter, map } from 'rxjs';
import { CourseContextService } from './course-context.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  private readonly router = inject(Router);
  private readonly courseContext = inject(CourseContextService);
  private readonly url = toSignal(this.router.events.pipe(filter((event) => event instanceof NavigationEnd), map(() => this.router.url)), { initialValue: this.router.url });
  private readonly courses = toSignal(this.courseContext.courses(), { initialValue: [] });
  readonly currentCourse = computed(() => {
    const courseId = this.url().match(/\/docente\/cursos\/([^/]+)/)?.[1];
    return this.courses().find((course) => course.id === courseId);
  });
}
