import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconComponent } from '../../ui/icon/icon';
import { QUICK_ACTIONS } from '../../core/nav';
import {
  METRICS,
  NEXT_APPOINTMENT,
  PROFESSIONAL,
  RECENT_PATIENTS,
  TODAY_APPOINTMENTS,
  type Appointment,
  type RecentPatient,
} from '../../core/mock';
import { STATUS_META } from '../../core/status';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, IconComponent],
  styleUrl: './dashboard.css',
  templateUrl: './dashboard.html',
})
export class DashboardComponent {
  protected readonly professional = PROFESSIONAL;
  protected readonly metrics = METRICS;
  protected readonly next = NEXT_APPOINTMENT;
  protected readonly appointments = TODAY_APPOINTMENTS;
  protected readonly patients = RECENT_PATIENTS;
  protected readonly quickActions = QUICK_ACTIONS;

  protected readonly greeting = (() => {
    const hour = new Date().getHours();
    if (hour < 12) return 'Buenos días';
    if (hour < 19) return 'Buenas tardes';
    return 'Buenas noches';
  })();

  protected readonly todayLabel = (() => {
    const parts = new Date().toLocaleDateString('es-AR', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    });
    return parts.charAt(0).toUpperCase() + parts.slice(1);
  })();

  protected badgeClass(status: Appointment['status']): string {
    return 'badge ' + STATUS_META[status].cls;
  }

  protected statusLabel(status: Appointment['status']): string {
    return STATUS_META[status].label;
  }

  protected initials(name: string): string {
    return name
      .split(' ')
      .map((word) => word.charAt(0))
      .slice(0, 2)
      .join('')
      .toUpperCase();
  }

  protected trackById(_index: number, item: { id: string }): string {
    return item.id;
  }

  protected trackPatient(_index: number, item: RecentPatient): string {
    return item.id;
  }

  protected trackMetric(_index: number, item: { label: string }): string {
    return item.label;
  }

  protected trackAction(_index: number, item: { label: string }): string {
    return item.label;
  }
}