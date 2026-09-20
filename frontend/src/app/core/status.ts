export type AppointmentStatus = 'CONFIRMED' | 'PENDING' | 'ATTENDED' | 'NO_SHOW' | 'CANCELLED';

export const STATUS_META: Record<AppointmentStatus, { label: string; cls: string }> = {
  CONFIRMED: { label: 'Confirmado', cls: 'badge--success' },
  PENDING: { label: 'Pendiente', cls: 'badge--warning' },
  ATTENDED: { label: 'Atendido', cls: 'badge--info' },
  NO_SHOW: { label: 'No asistió', cls: 'badge--error' },
  CANCELLED: { label: 'Cancelado', cls: 'badge--neutral' },
};