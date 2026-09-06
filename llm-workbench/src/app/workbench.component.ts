import { DatePipe, JsonPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { GoldenSet, GoldenSetApiService } from './golden-set-api.service';

@Component({ selector: 'app-workbench', imports: [ReactiveFormsModule, RouterLink, DatePipe, JsonPipe], changeDetection: ChangeDetectionStrategy.OnPush, templateUrl: './workbench.component.html', styleUrl: './workbench.component.scss' })
export class WorkbenchComponent {
  private readonly api = inject(GoldenSetApiService); private readonly route = inject(ActivatedRoute); private readonly router = inject(Router); private readonly fb = inject(FormBuilder);
  readonly sets = signal<GoldenSet[]>([]); readonly selected = signal<GoldenSet | null>(null); readonly loading = signal(false); readonly saving = signal(false); readonly error = signal<string | null>(null); readonly mode = signal<'list' | 'new' | 'detail'>('list');
  readonly scoreFields = [{key:'autonomy',label:'Autonomía',weight:30},{key:'clarity',label:'Claridad',weight:25},{key:'progression',label:'Progresión',weight:20},{key:'compliance',label:'Cumplimiento',weight:15},{key:'efficiency',label:'Eficiencia',weight:10}];
  readonly entryForm = this.fb.nonNullable.group({ transcript: ['', Validators.required], autonomy: [80, [Validators.min(0), Validators.max(100)]], clarity: [70, [Validators.min(0), Validators.max(100)]], progression: [75, [Validators.min(0), Validators.max(100)]], compliance: [100, [Validators.min(0), Validators.max(100)]], efficiency: [85, [Validators.min(0), Validators.max(100)]] });
  readonly canSave = computed(() => this.entryForm.valid && !this.saving());
  constructor() { this.route.url.subscribe(() => this.loadRoute()); }
  loadRoute() { const id = this.route.snapshot.paramMap.get('id'); this.error.set(null); if (id) { this.mode.set('detail'); this.loadSet(id); return; } if (this.route.snapshot.routeConfig?.path === 'golden-sets/new') { this.mode.set('new'); return; } this.mode.set('list'); this.loadList(); }
  loadList() { this.loading.set(true); this.api.list().subscribe({next: sets => { this.sets.set(sets); this.loading.set(false); }, error: error => this.fail(error)}); }
  loadSet(id:string) { this.loading.set(true); this.api.get(id).subscribe({next: set => {this.selected.set(set);this.loading.set(false);},error:error=>this.fail(error)}); }
  createSet() { this.saving.set(true); this.error.set(null); this.api.create().subscribe({next:set=>this.router.navigate(['/golden-sets',set.id]),error:error=>this.fail(error),complete:()=>this.saving.set(false)}); }
  loadSample() { this.entryForm.patchValue({transcript:JSON.stringify([{role:'learner',text:'Intenté resolver el ejercicio y encontré un error al recorrer una lista vacía.'}],null,2)}); }
  addEntry() { const current=this.selected(); if(!current||!this.canSave())return; let transcript:unknown[]; try{transcript=JSON.parse(this.entryForm.controls.transcript.value);if(!Array.isArray(transcript)||transcript.length===0)throw new Error();}catch{this.error.set('La transcripción debe ser un arreglo JSON con al menos un mensaje.');return;} this.saving.set(true);this.error.set(null);const values=this.entryForm.getRawValue();const scores=Object.fromEntries(this.scoreFields.map(field=>[field.key,values[field.key as keyof typeof values] as number]));this.api.addEntry(current.id,transcript,scores).subscribe({next:()=>this.loadSet(current.id),error:error=>this.fail(error),complete:()=>this.saving.set(false)}); }
  private fail(error:any){this.loading.set(false);this.saving.set(false);this.error.set(error?.error?.detail??'No se pudo contactar al backend. Iniciá llm-service con el perfil workbench.');}
}
