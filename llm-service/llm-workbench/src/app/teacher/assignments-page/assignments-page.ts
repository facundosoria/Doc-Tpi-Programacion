import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-assignments-page',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './assignments-page.component.html',
  styleUrl: './assignments-page.component.scss',
})
export class AssignmentsPage {
  readonly courseId = input('');
  readonly assignments = httpResource<AssignmentPage>(() => this.courseId() ? `/api/llm/courses/${this.courseId()}/challenge-calibration-assignments` : undefined, { defaultValue: { items: [] } });
  readonly pending = httpResource<PendingPage>(() => this.courseId() ? `/api/llm/courses/${this.courseId()}/pending-evaluations` : undefined, { defaultValue: { items: [] } });
  readonly queuedCount = computed(() => this.pending.value().items.length);
  reload(): void { this.assignments.reload(); this.pending.reload(); }
  shortId(id: string): string { return id.length > 12 ? `${id.slice(0, 8)}…${id.slice(-4)}` : id; }
}

interface AssignmentPage { items: { challengeId: string; calibrationRunId: string; locked: boolean }[]; }
interface PendingPage { items: { id: string; attemptId: string; calibrationRunId: string; state: string }[]; }
