import { Directive, ElementRef, OnDestroy, afterNextRender, inject, input, output } from '@angular/core';

/**
 * Apparition en fondu vers le haut quand l'élément entre à l'écran (une seule fois).
 * Émet `visible` à ce moment-là (sert à lancer les compteurs). Sans IntersectionObserver : visible tout de suite.
 */
@Directive({ selector: '[appReveal]', host: { class: 'reveal' } })
export class Reveal implements OnDestroy {
  private readonly el = inject<ElementRef<HTMLElement>>(ElementRef);
  private obs?: IntersectionObserver;
  readonly delai = input(0, { alias: 'appReveal', transform: (v: number | string) => Number(v) || 0 });
  readonly visible = output<void>();

  constructor() {
    afterNextRender(() => {
      const el = this.el.nativeElement;
      el.style.transitionDelay = `${this.delai()}s`;
      if (typeof IntersectionObserver === 'undefined') return this.montrer();
      this.obs = new IntersectionObserver((entrees) => entrees.some((e) => e.isIntersecting) && this.montrer(), { threshold: 0.15 });
      this.obs.observe(el);
    });
  }

  private montrer(): void {
    this.el.nativeElement.classList.add('in');
    this.obs?.disconnect();
    this.visible.emit();
  }

  ngOnDestroy(): void {
    this.obs?.disconnect();
  }
}
