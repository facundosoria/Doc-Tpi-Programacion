import { ChangeDetectionStrategy, Component, computed, input, output, signal } from '@angular/core';
import { ChallengeExercise, CHALLENGES } from '../challenge-catalog/challenge-catalog';

@Component({
  selector: 'app-challenge-selector',
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './challenge-selector.component.html',
  styleUrl: './challenge-selector.component.scss',
})
export class ChallengeSelector {
  readonly placeholder = input('Buscar un desafío…');
  readonly selected = output<ChallengeExercise>();

  readonly query = signal('');
  readonly activeIndex = signal(0);
  readonly open = signal(true);

  readonly filtered = computed(() => {
    const q = this.query().trim().toLowerCase();
    if (!q) return CHALLENGES;
    return CHALLENGES.filter((exercise) =>
      [exercise.name, exercise.title, exercise.id, ...exercise.tags]
        .join(' ')
        .toLowerCase()
        .includes(q));
  });

  onQuery(event: Event): void {
    this.query.set((event.target as HTMLInputElement).value);
    this.activeIndex.set(0);
    this.open.set(true);
  }

  onKeydown(event: KeyboardEvent): void {
    const list = this.filtered();
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      this.activeIndex.set((this.activeIndex() + 1) % Math.max(list.length, 1));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      this.activeIndex.set((this.activeIndex() - 1 + Math.max(list.length, 1)) % Math.max(list.length, 1));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      const choice = list[this.activeIndex()] ?? list[0];
      if (choice) this.pick(choice);
    } else if (event.key === 'Escape') {
      this.open.set(false);
    }
  }

  pick(exercise: ChallengeExercise): void {
    this.selected.emit(exercise);
    this.query.set(`${exercise.title} (${exercise.id})`);
    this.open.set(false);
  }
}