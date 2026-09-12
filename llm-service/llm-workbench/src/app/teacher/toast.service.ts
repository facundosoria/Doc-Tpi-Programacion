import { Injectable, signal } from '@angular/core';

export type ToastKind = 'success' | 'error';

export interface ToastMessage {
  readonly id: number;
  readonly kind: ToastKind;
  readonly text: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  readonly message = signal<ToastMessage | null>(null);
  private timeoutId: ReturnType<typeof setTimeout> | null = null;
  private nextId = 0;

  success(text: string, durationMs = 5000): void { this.show('success', text, durationMs); }
  error(text: string, durationMs = 7000): void { this.show('error', text, durationMs); }

  dismiss(): void {
    if (this.timeoutId !== null) clearTimeout(this.timeoutId);
    this.timeoutId = null;
    this.message.set(null);
  }

  private show(kind: ToastKind, text: string, durationMs: number): void {
    this.dismiss();
    this.message.set({ id: ++this.nextId, kind, text });
    this.timeoutId = setTimeout(() => this.dismiss(), durationMs);
  }
}
