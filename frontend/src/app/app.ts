import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { IconComponent } from './ui/icon/icon';
import { NAV_ITEMS, type NavItem } from './core/nav';
import { PROFESSIONAL } from './core/mock';

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