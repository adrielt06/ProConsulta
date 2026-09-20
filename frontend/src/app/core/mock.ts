import type { AppointmentStatus } from './status';
import type { IconName } from '../ui/icon/icon-paths';

export interface Appointment {
  id: string;
  patient: string;
  service: string;
  startsAt: string;
  endsAt: string;
  durationMinutes: number;
  status: AppointmentStatus;
}

export interface RecentPatient {
  id: string;
  name: string;
  subtitle: string;
}

export type MetricTone = 'success' | 'warning' | 'info' | 'neutral';

export interface Metric {
  label: string;
  value: string;
  note: string;
  noteTone: MetricTone;
  icon: IconName;
}

export const PROFESSIONAL = {
  displayName: 'Lic. Ana Pérez',
  initials: 'AP',
  practice: 'Nutrición',
};

export const TODAY_APPOINTMENTS: Appointment[] = [
  { id: 'a1', patient: 'Carlos Gómez', service: 'Consulta de nutrición', startsAt: '09:00', endsAt: '09:45', durationMinutes: 45, status: 'CONFIRMED' },
  { id: 'a2', patient: 'María Fernández', service: 'Plan de alimentación', startsAt: '10:30', endsAt: '11:30', durationMinutes: 60, status: 'CONFIRMED' },
  { id: 'a3', patient: 'Lucía Torres', service: 'Seguimiento', startsAt: '12:15', endsAt: '12:45', durationMinutes: 30, status: 'ATTENDED' },
  { id: 'a4', patient: 'Pedro Ramírez', service: 'Consulta de nutrición', startsAt: '14:00', endsAt: '14:45', durationMinutes: 45, status: 'PENDING' },
  { id: 'a5', patient: 'Sofía Álvarez', service: 'Plan de alimentación', startsAt: '15:30', endsAt: '16:30', durationMinutes: 60, status: 'CONFIRMED' },
  { id: 'a6', patient: 'Ramiro Díaz', service: 'Consulta exprés', startsAt: '16:15', endsAt: '16:35', durationMinutes: 20, status: 'PENDING' },
  { id: 'a7', patient: 'Juan Pérez', service: 'Seguimiento', startsAt: '17:30', endsAt: '18:00', durationMinutes: 30, status: 'NO_SHOW' },
  { id: 'a8', patient: 'Valeria Suárez', service: 'Consulta de nutrición', startsAt: '19:00', endsAt: '19:45', durationMinutes: 45, status: 'CONFIRMED' },
];

export const NEXT_APPOINTMENT: Appointment = {
  id: 'a2',
  patient: 'María Fernández',
  service: 'Plan de alimentación · 60 min',
  startsAt: '10:30',
  endsAt: '11:30',
  durationMinutes: 60,
  status: 'CONFIRMED',
};

export const METRICS: Metric[] = [
  { label: 'Turnos de hoy', value: '8', note: '3 pendientes', noteTone: 'warning', icon: 'calendar' },
  { label: 'Próximo turno', value: '10:30', note: 'María Fernández · 60 min', noteTone: 'info', icon: 'clock' },
  { label: 'Pacientes activos', value: '142', note: '+4 esta semana', noteTone: 'success', icon: 'users' },
  { label: 'Ingresos del mes', value: '$1.240.500', note: '+12 % vs agosto', noteTone: 'success', icon: 'wallet' },
];

export const RECENT_PATIENTS: RecentPatient[] = [
  { id: 'p1', name: 'María Fernández', subtitle: 'Turno hoy · 10:30' },
  { id: 'p2', name: 'Carlos Gómez', subtitle: 'Turno hoy · 09:00' },
  { id: 'p3', name: 'Lucía Torres', subtitle: 'Turno hoy · 12:15' },
  { id: 'p4', name: 'Sofía Álvarez', subtitle: 'Última consulta · 15 sep' },
  { id: 'p5', name: 'Pedro Ramírez', subtitle: 'Última consulta · 08 sep' },
];