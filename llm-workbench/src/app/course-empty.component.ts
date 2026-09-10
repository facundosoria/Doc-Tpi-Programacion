import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-course-empty',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<section class="empty-state" aria-labelledby="empty-title"><p class="eyebrow">EVALUADOR DE USO PEDAGÓGICO DE IA</p><h1 id="empty-title">No tenés cursos autorizados</h1><p>Cuando recibas acceso a un curso, vas a poder administrar sus rúbricas, Golden Sets y calibraciones desde este espacio.</p></section>`,
  styles: [`.empty-state{max-width:44rem;margin:5rem auto;padding:2.5rem;border:1px solid #d0d7de;border-radius:.75rem;background:#fff}.eyebrow{font-size:.75rem;font-weight:700;letter-spacing:.08em;color:#57606a}.empty-state h1{margin:.5rem 0 1rem}`],
})
export class CourseEmptyComponent {}
