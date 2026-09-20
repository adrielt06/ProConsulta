import type { IconName } from '../ui/icon/icon-paths';

export interface NavItem {
  key: string;
  label: string;
  route: string;
  icon: IconName;
  description: string;
}

export const NAV_ITEMS: NavItem[] = [
  {
    key: 'metricas',
    label: 'Métricas',
    route: '/metricas',
    icon: 'layout-grid',
    description: 'Resumen del día de tu consultorio: turnos, pacientes y caja.',
  },
  {
    key: 'pacientes',
    label: 'Pacientes',
    route: '/pacientes',
    icon: 'users',
    description: 'Fichas del consultorio, búsqueda y deduplicación por DNI.',
  },
  {
    key: 'agenda',
    label: 'Agenda',
    route: '/agenda',
    icon: 'calendar',
    description: 'Turnos, disponibilidad y estados de la semana.',
  },
  {
    key: 'pagos',
    label: 'Pagos',
    route: '/pagos',
    icon: 'card',
    description: 'Cobros, facturación y arqueo del mes.',
  },
  {
    key: 'servicios',
    label: 'Servicios',
    route: '/servicios',
    icon: 'briefcase',
    description: 'Catálogo de prestaciones con precio y duración.',
  },
  {
    key: 'perfil',
    label: 'Perfil',
    route: '/perfil',
    icon: 'user',
    description: 'Cuenta, ajustes y datos del consultorio.',
  },
];

export interface QuickAction {
  label: string;
  icon: IconName;
  route: string;
}

export const QUICK_ACTIONS: QuickAction[] = [
  { label: 'Nueva consulta', icon: 'plus-circle', route: '/servicios' },
  { label: 'Nuevo paciente', icon: 'user-plus', route: '/pacientes' },
  { label: 'Agendar turno', icon: 'calendar-plus', route: '/agenda' },
  { label: 'Registrar pago', icon: 'wallet', route: '/pagos' },
];