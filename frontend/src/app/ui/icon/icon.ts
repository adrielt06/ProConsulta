import { Component, computed, inject, input } from '@angular/core';
import { DomSanitizer, type SafeHtml } from '@angular/platform-browser';
import { ICON_PATHS, type IconName } from './icon-paths';

@Component({
  selector: 'app-icon',
  template: `
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="2"
      stroke-linecap="round"
      stroke-linejoin="round"
      aria-hidden="true"
      [attr.width]="size()"
      [attr.height]="size()"
      [innerHTML]="paths()"
    ></svg>
  `,
  styles: [
    `
      :host {
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }

      svg {
        flex-shrink: 0;
      }
    `,
  ],
})
export class IconComponent {
  readonly name = input<IconName>('layout-grid');
  readonly size = input<number>(18);

  // Los paths son marcado nuestro, constante y sin input de usuario. Sin el
  // bypass, el sanitizador de Angular elimina los elementos SVG de innerHTML
  // (su lista de elementos seguros es HTML-only), y los íconos no renderizan.
  private readonly sanitizer = inject(DomSanitizer);

  protected readonly paths = computed<SafeHtml>(() =>
    this.sanitizer.bypassSecurityTrustHtml(ICON_PATHS[this.name()]),
  );
}