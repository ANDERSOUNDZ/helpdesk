import { Component, OnInit, OnDestroy, ViewChild, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroupDirective, FormsModule, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subscription, interval } from 'rxjs';
import { HelpdeskService } from '../services/helpdesk.service';
import { AuthService } from '../auth/auth.service';
import { Ticket, Usuario, Prioridad, EstadoTicket, Comentario } from '../models/helpdesk.model';
import { AsignarDialog } from './asignar-dialog/asignar-dialog';
import { DetalleDialog } from './detalle-dialog/detalle-dialog';
import { EditarTicketDialog } from './editar-ticket-dialog/editar-ticket-dialog';

/** La descripcion del ticket guarda "Solicitante: ... [Depto]. Detalle: ..." y la base admite 1000 caracteres. */
const MAX_DETALLE = 800;

function noVacio(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && control.value.length > 0 && control.value.trim().length === 0
    ? { vacio: true }
    : null;
}

@Component({
  selector: 'app-tickets',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatPaginatorModule,
    MatDialogModule,
    MatSnackBarModule
  ],
  templateUrl: './tickets.html',
  styleUrl: './tickets.scss',
})
export class Tickets implements OnInit, OnDestroy {
  private helpdeskService = inject(HelpdeskService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  private fb = inject(FormBuilder);
  private autoRefreshSub?: Subscription;
  readonly auth = inject(AuthService);

  /** Id del usuario con sesion (null si el login esta apagado). */
  get miId(): number | null {
    return this.auth.usuario()?.id ?? null;
  }

  /** Administrador todo; tecnico solo los tickets que tiene (o los que nadie ha tomado). */
  puedeAtender(t: Ticket): boolean {
    if (!this.auth.usuario()) return true;
    if (this.auth.puede("ADMINISTRADOR")) return true;
    return this.auth.puede("TECNICO") && (!t.tecnicoId || t.tecnicoId === this.miId);
  }

  /** El tecnico puede ver, pero no escribir, en un ticket que tiene otro tecnico. */
  esSoloLectura(t: Ticket): boolean {
    return !!this.auth.usuario() && this.auth.puede("TECNICO") && !this.puedeAtender(t);
  }

  @ViewChild(FormGroupDirective) private formDirective?: FormGroupDirective;

  readonly tickets = signal<Ticket[]>([]);
  readonly usuarios = signal<Usuario[]>([]);
  columnasTickets: string[] = ['id', 'titulo', 'prioridad', 'estado', 'tecnico', 'bitacora', 'acciones'];
  readonly opcionesPagina = [5, 10, 25];
  readonly maxDetalle = MAX_DETALLE;

  departamentos: string[] = ['Contabilidad', 'Recursos Humanos', 'Ventas', 'Operaciones', 'Tecnologia / TI', 'Logistica'];

  // --- FORMULARIO DE ALTA ---
  readonly enviando = signal(false);
  readonly formulario = this.fb.nonNullable.group({
    solicitante: [this.nombreDeSesion(), [Validators.required, noVacio, Validators.minLength(3), Validators.maxLength(100)]],
    departamento: ['Operaciones', [Validators.required]],
    prioridad: ['MEDIA' as Prioridad, [Validators.required]],
    titulo: ['', [Validators.required, noVacio, Validators.minLength(5), Validators.maxLength(200)]],
    detalle: ['', [Validators.maxLength(MAX_DETALLE)]]
  });

  // --- FILTROS, ORDEN Y PAGINACION DE LA TABLA ---
  readonly texto = signal('');
  readonly filtroEstado = signal<'' | EstadoTicket>('');
  readonly filtroPrioridad = signal<'' | Prioridad>('');
  readonly pagina = signal(0);
  readonly tamano = signal(10);

  /** Filtrados y ordenados del mas nuevo al mas antiguo (el numero crece con cada ticket). */
  readonly filtrados = computed(() => {
    const buscado = this.texto().trim().toLowerCase();
    const estado = this.filtroEstado();
    const prioridad = this.filtroPrioridad();
    return this.tickets()
      .filter(t => !estado || t.estado === estado)
      .filter(t => !prioridad || t.prioridad === prioridad)
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

  readonly tecnicosDisponibles = computed(() =>
    this.usuarios().filter(u => u.rol === 'TECNICO' || u.rol === 'ADMINISTRADOR')
  );

  ngOnInit(): void {
    this.cargarDatos();
    this.autoRefreshSub = interval(3000).subscribe(() => {
      this.cargarTickets();
    });
  }

  ngOnDestroy(): void {
    if (this.autoRefreshSub) {
      this.autoRefreshSub.unsubscribe();
    }
  }

  cargarDatos(): void {
    this.cargarTickets();
    this.cargarUsuarios();
  }

  /** Evita repintar la tabla cada 3 s cuando los datos no cambiaron (los botones no "parpadean" al hacer clic). */
  private firmaTickets = '';

  trackPorId = (_: number, t: Ticket) => t.id;

  cargarTickets(): void {
    this.helpdeskService.getTickets().subscribe({
      next: (res) => {
        const firma = JSON.stringify(res);
        if (firma !== this.firmaTickets) {
          this.firmaTickets = firma;
          this.tickets.set([...res]);
        }
      },
      error: () => console.warn('Servicio de tickets no disponible')
    });
  }

  cargarUsuarios(): void {
    this.helpdeskService.getUsuarios().subscribe({
      next: (res) => this.usuarios.set(res),
      error: () => console.warn('Servicio de identidad no disponible')
    });
  }

  getNombreTecnico(tecnicoId?: number | null): string {
    if (!tecnicoId) return 'Sin Asignar';
    const tec = this.usuarios().find(u => u.id === tecnicoId);
    return tec ? `${tec.nombre} (#${tec.id})` : `Tecnico #${tecnicoId}`;
  }

  // --- FILTROS Y PAGINACION ---
  cambiarTexto(valor: string): void {
    this.texto.set(valor ?? '');
    this.pagina.set(0);
  }

  cambiarEstado(valor: '' | EstadoTicket): void {
    this.filtroEstado.set(valor ?? '');
    this.pagina.set(0);
  }

  cambiarPrioridad(valor: '' | Prioridad): void {
    this.filtroPrioridad.set(valor ?? '');
    this.pagina.set(0);
  }

  limpiarFiltros(): void {
    this.texto.set('');
    this.filtroEstado.set('');
    this.filtroPrioridad.set('');
    this.pagina.set(0);
  }

  cambiarPagina(evento: PageEvent): void {
    this.tamano.set(evento.pageSize);
    this.pagina.set(evento.pageIndex);
  }

  // --- CREAR TICKET ---
  private nombreDeSesion(): string {
    return this.auth.usuario()?.nombre ?? '';
  }

  /** El solicitante que inicio sesion no puede cambiar su nombre; el administrador si (registra por otra persona). */
  get nombreEditable(): boolean {
    return !this.auth.usuario() || this.auth.puede('ADMINISTRADOR');
  }

  get f() {
    return this.formulario.controls;
  }

  /** El servidor responde {"detail": ...} (ProblemDetail) o {"message": ...} segun la version; se aceptan ambos. */
  private mensajeDeError(err: { status?: number; error?: { detail?: string; message?: string } }): string {
    switch (err?.status) {
      case 0: return 'No hay conexion con servicio-tickets';
      case 401: return 'Tu sesion vencio. Vuelve a iniciar sesion';
      case 403: return 'No tienes permiso para crear tickets';
      default: return err?.error?.detail ?? err?.error?.message ?? 'No se pudo registrar el incidente';
    }
  }

  crearTicket(): void {
    this.formulario.markAllAsTouched();
    if (this.formulario.invalid || this.enviando()) {
      return;
    }
    const v = this.formulario.getRawValue();
    const solicitanteInfo = `${v.solicitante.trim()} [${v.departamento}]`;
    const detalle = v.detalle.trim();
    const descripcionCompleta = detalle
      ? `Solicitante: ${solicitanteInfo}. Detalle: ${detalle}`
      : `Solicitante: ${solicitanteInfo}.`;

    const nuevo: Ticket = {
      titulo: v.titulo.trim(),
      descripcion: descripcionCompleta,
      prioridad: v.prioridad,
      solicitanteId: this.auth.usuario()?.id ?? 1
    };

    this.enviando.set(true);
    this.helpdeskService.crearTicket(nuevo).subscribe({
      next: (creado) => {
        this.enviando.set(false);
        this.snackBar.open(`Incidente #${creado.id} registrado para ${solicitanteInfo}`, 'OK', { duration: 3000 });
        this.reiniciarFormulario();
        this.pagina.set(0);
        this.cargarTickets();
      },
      error: (err) => {
        this.enviando.set(false);
        this.snackBar.open(this.mensajeDeError(err), 'Cerrar', { duration: 4000 });
      }
    });
  }

  /** Deja el formulario limpio SIN marcar los campos como tocados (si no, aparecen en rojo). */
  private reiniciarFormulario(): void {
    const iniciales = {
      solicitante: this.nombreDeSesion(),
      departamento: 'Operaciones',
      prioridad: 'MEDIA' as Prioridad,
      titulo: '',
      detalle: ''
    };
    if (this.formDirective) {
      this.formDirective.resetForm(iniciales);
    } else {
      this.formulario.reset(iniciales);
    }
  }

  // --- MODAL DE ASIGNACIÓN DE TÉCNICO ---
  abrirModalAsignar(ticket: Ticket): void {
    const dialogRef = this.dialog.open(AsignarDialog, {
      width: '420px',
      data: {
        ticket,
        tecnicos: this.tecnicosDisponibles()
      }
    });

    dialogRef.afterClosed().subscribe((tecnicoId: number | undefined) => {
      if (tecnicoId && ticket.id) {
        this.helpdeskService.asignarTicket(ticket.id, tecnicoId).subscribe({
          next: () => {
            this.snackBar.open(`Ticket #${ticket.id} asignado a tecnico #${tecnicoId}`, 'OK', { duration: 3000 });
            this.cargarTickets();
          },
          error: () => this.snackBar.open('Error al asignar tecnico', 'Cerrar', { duration: 3000 })
        });
      }
    });
  }

  // --- MODAL DE BITÁCORA Y NOTAS TÉCNICAS (servicio-comunicacion) ---
  abrirModalDetalle(ticket: Ticket): void {
    this.dialog.open(DetalleDialog, {
      width: '550px',
      data: {
        ticket,
        nombreTecnico: this.getNombreTecnico(ticket.tecnicoId),
        soloLectura: this.esSoloLectura(ticket)
      }
    });
  }

  // --- MODAL DE EDICIÓN DE TICKET (UPDATE) ---
  abrirModalEditarTicket(ticket: Ticket): void {
    const dialogRef = this.dialog.open(EditarTicketDialog, {
      width: '480px',
      data: ticket
    });

    dialogRef.afterClosed().subscribe((datosActualizados: Partial<Ticket> | undefined) => {
      if (datosActualizados && ticket.id) {
        this.helpdeskService.actualizarTicket(ticket.id, datosActualizados).subscribe({
          next: () => {
            this.snackBar.open(`Ticket #${ticket.id} actualizado correctamente`, 'OK', { duration: 3000 });
            this.cargarTickets();
          },
          error: () => this.snackBar.open('Error al actualizar ticket', 'Cerrar', { duration: 3000 })
        });
      }
    });
  }

  // --- TÉCNICO RESUELVE EL INCIDENTE ---
  resolverTicket(ticket: Ticket): void {
    if (!ticket.id) return;
    this.helpdeskService.resolverTicket(ticket.id).subscribe({
      next: () => {
        // Registra nota automática de resolución en servicio-comunicacion
        const nota: Comentario = {
          ticketId: ticket.id!,
          autorId: ticket.tecnicoId || 1,
          texto: 'Trabajo tecnico finalizado satisfactoriamente. Incidente resuelto, en espera de validacion del solicitante.'
        };
        this.helpdeskService.crearComentario(nota).subscribe();

        this.snackBar.open(`Ticket #${ticket.id} resuelto por el tecnico con nota de finalizacion`, 'OK', { duration: 3500 });
        this.cargarTickets();
      },
      error: () => this.snackBar.open('Error al resolver ticket', 'Cerrar', { duration: 3000 })
    });
  }

  // --- SOLICITANTE REABRE EL TICKET ---
  reabrirTicket(ticket: Ticket): void {
    if (!ticket.id) return;
    this.helpdeskService.reabrirTicket(ticket.id).subscribe({
      next: () => {
        // Registra nota del solicitante en servicio-comunicacion
        const nota: Comentario = {
          ticketId: ticket.id!,
          autorId: ticket.solicitanteId,
          texto: 'Ticket reabierto por el solicitante: el problema o falla persiste en el mismo caso.'
        };
        this.helpdeskService.crearComentario(nota).subscribe();

        this.snackBar.open(`Ticket #${ticket.id} reabierto. El tecnico fue notificado de que la falla persiste.`, 'OK', { duration: 3500 });
        this.cargarTickets();
      },
      error: () => this.snackBar.open('Error al reabrir ticket', 'Cerrar', { duration: 3000 })
    });
  }

  eliminarTicket(id?: number): void {
    if (!id) return;
    this.helpdeskService.eliminarTicket(id).subscribe({
      next: () => {
        this.snackBar.open(`Ticket #${id} eliminado`, 'OK', { duration: 3000 });
        this.cargarTickets();
      },
      error: () => this.snackBar.open('Error al eliminar ticket', 'Cerrar', { duration: 3000 })
    });
  }
}
