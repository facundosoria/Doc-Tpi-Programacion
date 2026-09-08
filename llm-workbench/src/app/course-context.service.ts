import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, map, of, shareReplay } from 'rxjs';

export interface AuthorizedCourse {
  id: string;
  name: string;
  subtitle?: string;
}

/** Single source of course context. Business data never comes from browser storage. */
@Injectable({ providedIn: 'root' })
export class CourseContextService {
  private readonly http = inject(HttpClient);
  private readonly authorizedCourses$ = this.http
    .get<AuthorizedCourse[] | { items: AuthorizedCourse[] }>('/api/llm/courses')
    .pipe(
      map((response) => Array.isArray(response) ? response : response.items ?? []),
      catchError(() => of([])),
      shareReplay({ bufferSize: 1, refCount: true }),
    );

  courses(): Observable<AuthorizedCourse[]> { return this.authorizedCourses$; }
  course(courseId: string): Observable<AuthorizedCourse | undefined> {
    return this.courses().pipe(map((courses) => courses.find((course) => course.id === courseId)));
  }
}
