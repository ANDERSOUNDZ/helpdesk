import { Component, inject, OnInit, OnDestroy, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subscription, catchError, forkJoin, interval, of } from 'rxjs';
import { HelpdeskService } from '../services/helpdesk.service';
import { AuthService } from '../auth/auth.service';
import { ResumenEncuestas, Usuario } from '../models/helpdesk.model';
import { CategoriaMetrica, MetricasService, TicketMetrica } from './metricas.service';

interface Barra {
  etiqueta: string;
  cantidad: number;
  /** Porcentaje del total (texto). */
  pct: number;
  /** Ancho de la barra relativo a la mayor del grupo. */
  ancho: number;
  clase: string;
}

interface CargaTecnico {
  id: number;
  nombre: string;
  asignados: number;
  enProgreso: number;
  resueltos: number;
}

interface DiaTendencia {
  etiqueta: string;
  creados: number;
  resueltos: number;
  altoCreados: number;
  altoResueltos: number;
}

const DIAS_TENDENCIA = 14;
const HORA_MS = 3_600_000;

@Component({
  selector: 'app-dashboard',
  standalone: true,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatTableModule,
    MatChipsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatPaginatorModule
  ],
})
export class DashboardComponent implements OnInit, OnDestroy {
  private helpdeskService = inject(HelpdeskService);
  private metricas = inject(MetricasService);
  readonly auth = inject(AuthService);
  private snackBar = inject(MatSnackBar);
  private autoRefreshSub?: Subscription;

  // --- DATOS CRUDOS (se repintan solo si cambian) ---
  readonly tickets = signal<TicketMetrica[]>([]);
  readonly usuarios = signal<Usuario[]>([]);
  readonly categorias = signal<CategoriaMetrica[]>([]);
  readonly encuestas = signal<ResumenEncuestas | null>(null);
  readonly ticketsDisponibles = signal(true);
  readonly cargado = signal(false);
  readonly exportando = signal(false);

  private firmaTickets = '';
  private firmaEncuestas = '';
  private firmaUsuarios = '';
  private firmaCategorias = '';

  // --- FILTROS, ORDEN Y PAGINACION DE LA TABLA DE REPORTES ---
  readonly texto = signal('');
  readonly filtroEstado = signal('');
  readonly filtroPrioridad = signal('');
  readonly filtroTecnico = signal<number | ''>('');
  readonly filtroCategoria = signal<number | ''>('');
  readonly soloVencidos = signal(false);
  readonly pagina = signal(0);
  readonly tamano = signal(10);
  readonly opcionesPagina = [5, 10, 25];

  columnasReporte: string[] = ['id', 'titulo', 'solicitante', 'categoria', 'prioridad', 'estado', 'tecnico', 'creado', 'sla'];
  readonly columnasCarga: string[] = ['tecnico', 'asignados', 'enProgreso', 'resueltos'];

  // --- KPIs ---
  readonly kpis = computed(() => {
    const lista = this.tickets();
    const ahora = Date.now();
    const abiertos = lista.filter(t => t.estado === 'ABIERTO').length;
    const enProgreso = lista.filter(t => t.estado === 'EN_PROGRESO').length;
    const resueltos = lista.filter(t => t.estado === 'RESUELTO').length;
    const sinAsignar = lista.filter(t => !t.tecnicoId && t.estado !== 'RESUELTO').length;
    const vencidos = lista.filter(t => this.estaVencido(t, ahora)).length;

    const conLimite = lista.filter(t => !!t.fechaLimite);
    const enPlazo = conLimite.filter(t => {
      if (t.estado === 'RESUELTO') {
        return !t.resueltoEn || Date.parse(t.resueltoEn) <= Date.parse(t.fechaLimite!);
      }
      return !this.estaVencido(t, ahora);
    }).length;
    const cumplimientoSla = conLimite.length ? (enPlazo / conLimite.length) * 100 : null;

    const medibles = lista.filter(t => t.estado === 'RESUELTO' && t.creadoEn && t.resueltoEn);
    const horas = medibles.length
      ? medibles.reduce((suma, t) => suma + (Date.parse(t.resueltoEn!) - Date.parse(t.creadoEn!)), 0) / medibles.length / HORA_MS
      : null;

    return {
      total: lista.length,
      abiertos,
      enProgreso,
      resueltos,
      sinAsignar,
      vencidos,
      cumplimientoSla,
      conLimite: conLimite.length,
      tiempoMedioHoras: horas,
      medibles: medibles.length
    };
  });

  // --- DISTRIBUCIONES ---
  readonly porEstado = computed<Barra[]>(() => {
    const k = this.kpis();
    return this.barras([
      { etiqueta: 'ABIERTO', cantidad: k.abiertos, clase: 'barra-abierto' },
      { etiqueta: 'EN_PROGRESO', cantidad: k.enProgreso, clase: 'barra-progreso' },
      { etiqueta: 'RESUELTO', cantidad: k.resueltos, clase: 'barra-resuelto' }
    ]);
  });

  readonly porPrioridad = computed<Barra[]>(() => {
    const lista = this.tickets();
    return this.barras([
      { etiqueta: 'ALTA', cantidad: lista.filter(t => t.prioridad === 'ALTA').length, clase: 'barra-alta' },
      { etiqueta: 'MEDIA', cantidad: lista.filter(t => t.prioridad === 'MEDIA').length, clase: 'barra-media' },
      { etiqueta: 'BAJA', cantidad: lista.filter(t => t.prioridad === 'BAJA').length, clase: 'barra-baja' }
    ]);
  });

  readonly porCategoria = computed<Barra[]>(() => {
    const conteo = new Map<string, number>();
    for (const t of this.tickets()) {
      const nombre = this.nombreCategoria(t.categoriaId);
      conteo.set(nombre, (conteo.get(nombre) ?? 0) + 1);
    }
    const items = [...conteo.entries()]
      .sort((a, b) => b[1] - a[1])
      .map(([etiqueta, cantidad]) => ({ etiqueta, cantidad, clase: 'barra-categoria' }));
    return this.barras(items);
  });

  readonly cargaTecnicos = computed<CargaTecnico[]>(() => {
    const mapa = new Map<number, CargaTecnico>();
    for (const t of this.tickets()) {
      if (!t.tecnicoId) continue;
      const fila = mapa.get(t.tecnicoId) ?? {
        id: t.tecnicoId,
        nombre: this.getNombreUsuario(t.tecnicoId),
        asignados: 0,
        enProgreso: 0,
        resueltos: 0
      };
      fila.asignados++;
      if (t.estado === 'EN_PROGRESO') fila.enProgreso++;
      if (t.estado === 'RESUELTO') fila.resueltos++;
      mapa.set(t.tecnicoId, fila);
    }
    return [...mapa.values()].sort((a, b) => b.asignados - a.asignados);
  });

  // --- SATISFACCION DEL CLIENTE ---
  readonly csat = computed(() => {
    const e = this.encuestas();
    if (!e) return null;
    const respondidas = e.respondidas ?? 0;
    const items = [5, 4, 3, 2, 1].map(nota => {
      const cantidad = Number((e.distribucion ?? {})[nota] ?? 0);
      return { nota, cantidad, ancho: respondidas ? (cantidad / respondidas) * 100 : 0 };
    });
    return {
      promedio: e.promedio ?? 0,
      respondidas,
      pendientes: e.pendientes ?? 0,
      total: e.totalEncuestas ?? 0,
      tasaRespuesta: e.totalEncuestas ? (respondidas / e.totalEncuestas) * 100 : null,
      items
    };
  });

  // --- TENDENCIA (ULTIMOS 14 DIAS) ---
  readonly tendencia = computed(() => {
    const lista = this.tickets();
    const hoy = new Date();
    hoy.setHours(0, 0, 0, 0);

    const dias: { clave: string; etiqueta: string }[] = [];
    for (let i = DIAS_TENDENCIA - 1; i >= 0; i--) {
      const d = new Date(hoy);
      d.setDate(hoy.getDate() - i);
      dias.push({ clave: this.claveDia(d), etiqueta: `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}` });
    }
    const creados = new Map<string, number>();
    const resueltos = new Map<string, number>();
    let sinFecha = 0;
    for (const t of lista) {
      if (t.creadoEn) {
        const clave = this.claveDia(new Date(t.creadoEn));
        creados.set(clave, (creados.get(clave) ?? 0) + 1);
      } else {
        sinFecha++;
      }
      if (t.resueltoEn) {
        const clave = this.claveDia(new Date(t.resueltoEn));
        resueltos.set(clave, (resueltos.get(clave) ?? 0) + 1);
      }
    }
    const maximo = Math.max(1, ...dias.map(d => Math.max(creados.get(d.clave) ?? 0, resueltos.get(d.clave) ?? 0)));
    const serie: DiaTendencia[] = dias.map(d => {
      const c = creados.get(d.clave) ?? 0;
      const r = resueltos.get(d.clave) ?? 0;
      return {
        etiqueta: d.etiqueta,
        creados: c,
        resueltos: r,
        altoCreados: (c / maximo) * 100,
        altoResueltos: (r / maximo) * 100
      };
    });
    return {
      serie,
      totalCreados: serie.reduce((s, d) => s + d.creados, 0),
      totalResueltos: serie.reduce((s, d) => s + d.resueltos, 0),
      sinFecha
    };
  });

  // --- TABLA DE REPORTES ---
  readonly tecnicosParaFiltro = computed(() =>
    this.usuarios().filter(u => u.rol === 'TECNICO' || u.rol === 'ADMINISTRADOR')
  );

  readonly categoriasParaFiltro = computed(() => {
    const ids = new Set<number>();
    this.categorias().forEach(c => ids.add(c.id));
    this.tickets().forEach(t => t.categoriaId && ids.add(t.categoriaId));
    return [...ids].map(id => ({ id, nombre: this.nombreCategoria(id) }));
  });

  /** Filtrados y ordenados del mas nuevo al mas antiguo. */
  readonly filtrados = computed(() => {
    const buscado = this.texto().trim().toLowerCase();
    const estado = this.filtroEstado();
    const prioridad = this.filtroPrioridad();
    const tecnico = this.filtroTecnico();
    const categoria = this.filtroCategoria();
    const soloVencidos = this.soloVencidos();
    const ahora = Date.now();
    return this.tickets()
      .filter(t => !estado || t.estado === estado)
      .filter(t => !prioridad || t.prioridad === prioridad)
      .filter(t => tecnico === '' || t.tecnicoId === tecnico)
      .filter(t => categoria === '' || t.categoriaId === categoria)
      .filter(t => !soloVencidos || this.estaVencido(t, ahora))
      .filter(t => !buscado
        || String(t.id ?? '').includes(buscado)
        || t.titulo.toLowerCase().includes(buscado)
        || (t.descripcion ?? '').toLowerCase().includes(buscado))
      .sort((a, b) => (b.id ?? 0) - (a.id ?? 0));
  });

  readonly paginaActual = computed(() => {
    const ultima = Math.max(0, Math.ceil(this.filtrados().length / this.tamano()) - 1);
    return Math.min(this.pagina(), ultima);
  });

  readonly visibles = computed(() => {
    const desde = this.paginaActual() * this.tamano();
    return this.filtrados().slice(desde, desde + this.tamano());
  });

  readonly hayFiltros = computed(() =>
    !!this.texto() || !!this.filtroEstado() || !!this.filtroPrioridad()
    || this.filtroTecnico() !== '' || this.filtroCategoria() !== '' || this.soloVencidos()
  );

  trackPorId = (_: number, t: TicketMetrica) => t.id;

  ngOnInit(): void {
    this.cargarReferencias();
    this.cargarDatos();
    this.autoRefreshSub = interval(5000).subscribe(() => this.cargarDatos());
  }

  ngOnDestroy(): void {
    this.autoRefreshSub?.unsubscribe();
  }

  // --- CARGA DE DATOS ---
  cargarReferencias(): void {
    this.helpdeskService.getUsuarios().subscribe({
      next: (res) => {
        const firma = JSON.stringify(res);
        if (firma !== this.firmaUsuarios) {
          this.firmaUsuarios = firma;
          this.usuarios.set([...res]);
        }
      },
      error: () => console.warn('Usuarios no disponibles para reporte')
    });
    this.metricas.categorias().subscribe(res => {
      const firma = JSON.stringify(res);
      if (firma !== this.firmaCategorias) {
        this.firmaCategorias = firma;
        this.categorias.set(res);
      }
    });
  }

  cargarDatos(): void {
    forkJoin({
      tickets: this.helpdeskService.getTickets().pipe(catchError(() => of(null))),
      encuestas: this.metricas.encuestas()
    }).subscribe(({ tickets, encuestas }) => {
      this.cargado.set(true);
      if (tickets === null) {
        this.ticketsDisponibles.set(false);
      } else {
        this.ticketsDisponibles.set(true);
        const firma = JSON.stringify(tickets);
        if (firma !== this.firmaTickets) {
          this.firmaTickets = firma;
          this.tickets.set([...(tickets as TicketMetrica[])]);
        }
      }
      const firmaE = JSON.stringify(encuestas);
      if (firmaE !== this.firmaEncuestas) {
        this.firmaEncuestas = firmaE;
        this.encuestas.set(encuestas);
      }
    });
  }

  actualizar(): void {
    this.cargarReferencias();
    this.cargarDatos();
  }

  // --- AYUDAS DE FORMATO ---
  private claveDia(d: Date): string {
    return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`;
  }

  private barras(items: { etiqueta: string; cantidad: number; clase: string }[]): Barra[] {
    const total = items.reduce((s, i) => s + i.cantidad, 0);
    const maximo = Math.max(1, ...items.map(i => i.cantidad));
    return items.map(i => ({
      ...i,
      pct: total ? (i.cantidad / total) * 100 : 0,
      ancho: (i.cantidad / maximo) * 100
    }));
  }

  private estaVencido(t: TicketMetrica, ahora: number): boolean {
    if (t.estado === 'RESUELTO') return false;
    if (typeof t.vencido === 'boolean') return t.vencido;
    return !!t.fechaLimite && Date.parse(t.fechaLimite) < ahora;
  }

  estadoSla(t: TicketMetrica): 'vencido' | 'resuelto' | 'plazo' | 'sin' {
    if (!t.fechaLimite) return 'sin';
    if (t.estado === 'RESUELTO') return 'resuelto';
    return this.estaVencido(t, Date.now()) ? 'vencido' : 'plazo';
  }

  nombreCategoria(id?: number | null): string {
    if (!id) return 'Sin categoria';
    return this.categorias().find(c => c.id === id)?.nombre ?? `Categoria #${id}`;
  }

  getNombreUsuario(id?: number | null): string {
    if (!id) return 'Sin Asignar';
    const u = this.usuarios().find(x => x.id === id);
    return u ? u.nombre : `Tecnico #${id}`;
  }

  extraerSolicitante(ticket: TicketMetrica): string {
    if (!ticket.descripcion) return 'Usuario General';
    if (ticket.descripcion.startsWith('Solicitante:')) {
      const fin = ticket.descripcion.indexOf('. Detalle:');
      if (fin !== -1) {
        return ticket.descripcion.substring(13, fin).trim();
      }
      return ticket.descripcion.replace('Solicitante:', '').replace('.', '').trim();
    }
    return 'Usuario General';
  }

  // --- FILTROS Y PAGINACION ---
  cambiarTexto(valor: string): void { this.texto.set(valor ?? ''); this.pagina.set(0); }
  cambiarEstado(valor: string): void { this.filtroEstado.set(valor ?? ''); this.pagina.set(0); }
  cambiarPrioridad(valor: string): void { this.filtroPrioridad.set(valor ?? ''); this.pagina.set(0); }
  cambiarTecnico(valor: number | ''): void { this.filtroTecnico.set(valor ?? ''); this.pagina.set(0); }
  cambiarCategoria(valor: number | ''): void { this.filtroCategoria.set(valor ?? ''); this.pagina.set(0); }
  cambiarVencidos(valor: boolean): void { this.soloVencidos.set(!!valor); this.pagina.set(0); }

  limpiarFiltros(): void {
    this.texto.set('');
    this.filtroEstado.set('');
    this.filtroPrioridad.set('');
    this.filtroTecnico.set('');
    this.filtroCategoria.set('');
    this.soloVencidos.set(false);
    this.pagina.set(0);
  }

  cambiarPagina(evento: PageEvent): void {
    this.tamano.set(evento.pageSize);
    this.pagina.set(evento.pageIndex);
  }

  // --- ACCIONES ---
  exportarCsv(): void {
    if (this.exportando()) return;
    this.exportando.set(true);
    const tecnico = this.filtroTecnico();
    const categoria = this.filtroCategoria();
    this.metricas.exportarCsv({
      estado: this.filtroEstado() || undefined,
      prioridad: this.filtroPrioridad() || undefined,
      tecnicoId: tecnico === '' ? undefined : tecnico,
      categoriaId: categoria === '' ? undefined : categoria,
      q: this.texto().trim() || undefined,
      vencidos: this.soloVencidos() || undefined
    }).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const enlace = document.createElement('a');
        enlace.href = url;
        enlace.download = 'tickets.csv';
        enlace.click();
        URL.revokeObjectURL(url);
        this.exportando.set(false);
      },
      error: () => {
        this.exportando.set(false);
        this.snackBar.open('No se pudo exportar el CSV (solo el administrador puede hacerlo)', 'Cerrar', { duration: 4000 });
      }
    });
  }

  resetearEstadisticas(): void {
    this.helpdeskService.resetearEstadisticas().subscribe({
      next: () => {
        this.snackBar.open('Contadores reiniciados en servicio-estadisticas', 'Cerrar', { duration: 3000 });
        this.cargarDatos();
      },
      error: () => this.snackBar.open('Error al resetear metricas', 'Cerrar', { duration: 3000 })
    });
  }
}
