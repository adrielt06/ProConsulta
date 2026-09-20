# Design — ProConsulta

Fuente de verdad de los tokens y convenciones visuales del frontend. Si algo no está documentado acá, no es una pieza del sistema de diseño.

Estado: v1. Tema claro, estilo SaaS tipo Linear/Stripe/Notion.

## Principios

- Calma y confianza: nada grita, los acentos son puntuales.
- Jerarquía clara: lo que el profesional necesita durante el día va primero.
- Azul como acento primario; verde/ámbar/rojo sólo para estados que significan algo.
- Glassmorphism únicamente en la barra de navegación. Nada de exceso de gradientes ni efectos.

## Color

Los tokens viven en `src/styles.css` como variables CSS. Los componentes no hardcodean colores.

### Acentos y neutros
| Token | Valor | Uso |
| --- | --- | --- |
| `--color-primary-50` | `#eff6ff` | fondos de énfasis, estado activo de nav |
| `--color-primary-100` | `#dbeafe` | avatares, contenedores de icono |
| `--color-primary-600` | `#2563eb` | botones primarios, enlaces, bordes activos |
| `--color-primary-700` | `#1d4ed8` | hover de primario |
| `--color-bg` | `#f7f9fc` | fondo de la página |
| `--color-surface` | `#ffffff` | tarjetas y paneles |
| `--color-border` | `#e7eaf0` | bordes suaves de tarjetas y controles |
| `--text-primary` | `#101828` | títulos y texto principal |
| `--text-secondary` | `#475467` | subtítulos, descripciones |
| `--text-muted` | `#98a2b3` | metadatos, placeholders |

### Estados (sólo para semáforos)
| Tone | Fondo | Texto | Uso |
| --- | --- | --- | --- |
| success | `#ecfdf3` | `#027a48` | Confirmado, ingresos al alza |
| warning | `#fffaeb` | `#b54708` | Pendiente |
| info | `#eff6ff` | `#1d4ed8` | Atendido |
| error | `#fef3f2` | `#b42318` | No asistió |
| neutral | `#f2f4f7` | `#475467` | Cancelado |

## Tipografía

- Stack del sistema: `Inter, ui-sans-serif, system-ui, "Segoe UI", Roboto, Arial, sans-serif`.
- Escala: título de página `22–26px / 700`, tarjeta `16px / 600`, cuerpo `14px`, metadato `12–13px`.
- Números tabulares en horarios, turnos y montos (`font-variant-numeric: tabular-nums`).
- `letter-spacing: -0.01em` en títulos.

## Espaciado, radio y sombra

- Espaciado de 4px: `4 / 8 / 12 / 16 / 20 / 24 / 32`.
- Radio: `--radius-sm 8px` (inputs, botones), `--radius-md 12px` (tarjetas), `--radius-lg 16px` (topbar).
- Sombras: tarjetas `0 1px 2px rgba(16,24,40,.04)`, `0 1px 3px rgba(16,24,40,.06)`; flotante (topbar, menús) `0 12px 32px -16px rgba(16,24,40,.16)`.

## Glassmorphism (sólo topbar)

```css
background: rgba(255, 255, 255, .78);
backdrop-filter: blur(14px) saturate(1.4);
border: 1px solid rgba(16, 24, 40, .07);
```

Píldora flotante: `position: sticky; top: 12px`, márgenes laterales que la separan del viewport, radio `16px`.

## Componentes

### Tarjeta
Fondo `--color-surface`, borde `1px solid var(--color-border)`, radio `--radius-md`, sombra de tarjeta. Encabezado: título `16px/600` + acción a la derecha.

### Badge
Píldora `12px/500`, `padding: 2px 10px`, radio `999px`, con la paleta de tonos de estados. Sólo representa estados (nunca decoración).

### Botones
- `btn--primary`: fondo `--color-primary-600`, texto blanco, hover `--color-primary-700`.
- `btn--secondary`: fondo blanco, borde `--color-border`, hover `--color-bg`.
- `btn--ghost`: sin fondo ni borde, hover `--color-bg`.
- Tamaño base `13–14px/500`, `padding: 9px 14px`, radio `--radius-sm`, gap icono `8px`, alto mínimo `36–40px`.
- `:focus-visible` con anillo `2px solid var(--color-primary-600)`.

### Avatar
Iniciales en cuadro `10px`: fondo `--color-primary-100`, texto `--color-primary-700` `600`. Tamaños `32` (sm), `40` (md).

### Icono
SVG `24×24`, stroke `currentColor`, `stroke-width: 2`, puntas redondeadas (estilo Lucide). `aria-hidden="true"`. El componente `app-icon` es la única forma de usarlos.

## Estado de turnos

| Modelo | Etiqueta | Badge |
| --- | --- | --- |
| `CONFIRMED` | Confirmado | `badge--success` |
| `PENDING` | Pendiente | `badge--warning` |
| `ATTENDED` | Atendido | `badge--info` |
| `NO_SHOW` | No asistió | `badge--error` |
| `CANCELLED` | Cancelado | `badge--neutral` |

## Layout

- Desktop-first. Contenido hasta `1200px`, centrado, padding lateral `20px`.
- Topbar: logo + nav (Métricas, Pacientes, Agenda, Pagos, Servicios, Perfil) + acciones (barra actual + avatar). Nav con `routerLinkActive` (exacto).
- Breakpoints: `≤920px` nav colapsa a menú desplegable glass (hamburguesa con `aria-expanded`); grids de métricas y acciones pasan de `4` → `2` → `1` columna.

## Accesibilidad

- Landmarks: `<header>`, `<nav aria-label>`, `<main>`, secciones con `aria-label`.
- Contraste AA sobre blanco y `--color-bg`.
- `prefers-reduced-motion`: desactivar transiciones.