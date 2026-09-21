import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { IconComponent } from './ui/icon/icon';
import { NAV_ITEMS, type NavItem } from './core/nav';
import { PROFESSIONAL } from './core/mock';

const THEME_KEY = 'proconsulta-theme';

export type AppTheme = 'light' | 'dark';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent],
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly navItems = NAV_ITEMS;
  protected readonly professional = PROFESSIONAL;
  protected readonly menuOpen = signal(false);
  protected readonly dark = signal(false);

  constructor() {
    this.applyTheme(this.readInitialTheme());
  }

  protected toggleTheme(): void {
    this.applyTheme(this.dark() ? 'light' : 'dark');
  }

  private readInitialTheme(): AppTheme {
    let stored: string | null = null;
    try {
      stored = localStorage.getItem(THEME_KEY);
    } catch {
      stored = null;
    }
    if (stored === 'light' || stored === 'dark') return stored;

    const systemDark =
      typeof window.matchMedia === 'function' &&
      window.matchMedia('(prefers-color-scheme: dark)').matches;
    return systemDark ? 'dark' : 'light';
  }

  private applyTheme(theme: AppTheme): void {
    this.dark.set(theme === 'dark');
    document.documentElement.dataset['theme'] = theme;

    const meta = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
    if (meta) meta.content = theme === 'dark' ? '#0a1120' : '#f7f9fc';

    try {
      localStorage.setItem(THEME_KEY, theme);
    } catch {
      /* almacenamiento no disponible */
    }
  }

  protected toggleMenu(): void {
    this.menuOpen.update((open) => !open);
  }

  protected closeMenu(): void {
    this.menuOpen.set(false);
  }

  protected trackNav(index: number, item: NavItem): string {
    return item.route;
  }
}