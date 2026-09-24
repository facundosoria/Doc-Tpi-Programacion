import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';
import { CHALLENGES, challengeUuidV5 } from '../challenge-catalog/challenge-catalog';

@Component({
  selector: 'app-course-overlay-list',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './course-overlay-list.component.html',
  styleUrl: './course-overlay-list.component.scss',
})
export class CourseOverlayList {
  readonly courseId = input('');

  readonly overlays = httpResource<OverlayPage>(() => this.courseId() ? `/api/llm/courses/${this.courseId()}/rubric-overlays` : undefined, { defaultValue: { items: [] } });

  readonly query = signal('');
  readonly stateFilter = signal('');
  readonly page = signal(1);
  readonly pageSize = 5;

  private readonly challengeNames = new Map<string, string>(
    CHALLENGES.map((challenge) => [challengeUuidV5(challenge.id), challenge.title]));

  readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    const state = this.stateFilter();
    return this.overlays.value().items.filter((item) => {
      const challengeName = this.challengeName(item.challengeId);
      const matchesQuery = !q || item.name.toLowerCase().includes(q) || challengeName.toLowerCase().includes(q);
      const matchesState = !state || item.state === state;
      return matchesQuery && matchesState;
    });
  });

  readonly pageCount = computed(() => Math.max(1, Math.ceil(this.filtered().length / this.pageSize)));

  readonly pages = computed(() => Array.from({ length: this.pageCount() }, (_, index) => index + 1));

  readonly pageItems = computed(() => {
    const page = Math.min(this.page(), this.pageCount());
    const start = (page - 1) * this.pageSize;
    return this.filtered().slice(start, start + this.pageSize);
  });

  challengeName(challengeId: string): string {
    return this.challengeNames.get(challengeId) ?? `${challengeId.slice(0, 8)}…`;
  }

  onQuery(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
    this.page.set(1);
  }

  onState(event: Event): void {
    this.stateFilter.set((event.target as HTMLSelectElement).value);
    this.page.set(1);
  }

  setPage(page: number): void {
    this.page.set(Math.min(Math.max(page, 1), this.pageCount()));
  }

  stateLabel(state: string): string {
    return state === 'DRAFT' ? 'Borrador' : state === 'PUBLISHED' ? 'Publicada' : state;
  }
}

interface OverlayPage { items: OverlayItem[]; }
interface OverlayItem {
  challengeId: string;
  id: string;
  version: number;
  name: string;
  state: string;
  revision: number;
}