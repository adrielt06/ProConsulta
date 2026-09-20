import { Component, computed, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { IconComponent } from '../../ui/icon/icon';
import { NAV_ITEMS, type NavItem } from '../../core/nav';

@Component({
  selector: 'app-placeholder',
  imports: [RouterLink, IconComponent],
  styleUrl: './placeholder.css',
  templateUrl: './placeholder.html',
})
export class PlaceholderComponent {
  private readonly item = signal<NavItem | null>(null);

  protected readonly label = computed(() => this.item()?.label ?? '');
  protected readonly description = computed(() => this.item()?.description ?? '');
  protected readonly icon = computed(() => this.item()?.icon ?? 'layout-grid');

  constructor(route: ActivatedRoute) {
    const key = route.snapshot.data['navKey'];
    const match = NAV_ITEMS.find((navItem) => navItem.key === key);
    this.item.set(match ?? null);
  }
}