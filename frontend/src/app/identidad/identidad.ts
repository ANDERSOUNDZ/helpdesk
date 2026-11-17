import { Component, OnInit, OnDestroy, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
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
import { Usuario } from '../models/helpdesk.model';
import { UsuarioDialog } from './usuario-dialog/usuario-dialog';
import { ConfirmarEliminarDialog } from './confirmar-eliminar-dialog/confirmar-eliminar-dialog';

type FiltroRol = '' | Usuario['rol'];

@Component({
  selector: 'app-identidad',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
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
  templateUrl: './identidad.html',
  styleUrl: './identidad.scss',
})
export class Identidad implements OnInit, OnDestroy {
  private helpdeskService = inject(HelpdeskService);
  private snackBar = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  private autoRefreshSub?: Subscription;
  readonly auth = inject(AuthService);

  columnasUsuarios: string[] = ['id', 'nombre', 'username', 'rol', 'ingreso', 'acciones'];
  readonly opcionesPagina = [5, 10, 25];

  readonly usuarios = signal<Usuario[]>([]);
  readonly texto = signal('');
  readonly filtroRol = signal<FiltroRol>('');
  readonly pagina = signal(0);
  readonly tamano = signal(10);

  /** Filtrados y ordenados del mas nuevo al mas antiguo. */
  readonly filtrados = computed(() => {
    const buscado = this.texto().trim().toLowerCase();
    const rol = this.filtroRol();
    return this.usuarios()
      .filter(u => !rol || u.rol === rol)
      .filter(u => !buscado || u.nombre.toLowerCase().includes(buscado) || (u.username ?? '').toLowerCase().includes(buscado))
      .sort((a, b) => (b.id ?? 0) - (a.id ?? 0));
  });

  /** Pagina valida aunque los filtros dejen menos registros que antes. */
  readonly paginaActual = computed(() => {
    const ultima = Math.max(0, Math.ceil(this.filtrados().length / this.tamano()) - 1);
    return Math.min(this.pagina(), ultima);
  });

  readonly visibles = computed(() => {
    const desde = this.paginaActual() * this.tamano();
    return this.filtrados().slice(desde, desde + this.tamano());
  });

  readonly totalAdministradores = computed(() => this.usuarios().filter(u => u.rol === 'ADMINISTRADOR').length);

  ngOnInit(): void {
    this.cargarUsuarios();
    this.autoRefreshSub = interval(3000).subscribe(() => {
      this.cargarUsuarios();
    });
  }

  ngOnDestroy(): void {
    if (this.autoRefreshSub) {
      this.autoRefreshSub.unsubscribe();
    }
  }

  /** Evita repintar la tabla cada 3 s cuando los datos no cambiaron (los botones no "parpadean" al hacer clic). */
  private firmaUsuarios = '';

  trackPorId = (_: number, u: Usuario) => u.id;

  cargarUsuarios(): void {
    this.helpdeskService.getUsuarios().subscribe({
      next: (res) => {
        const firma = JSON.stringify(res);
        if (firma !== this.firmaUsuarios) {
          this.firmaUsuarios = firma;
          this.usuarios.set([...res]);
        }
      },
      error: () => console.warn('Servicio de identidad no disponible')
    });
  }

  // --- FILTROS Y PAGINACION ---
  cambiarTexto(valor: string): void {
    this.texto.set(valor ?? '');
    this.pagina.set(0);
  }

  cambiarRol(valor: FiltroRol): void {
    this.filtroRol.set(valor ?? '');
    this.pagina.set(0);
  }

  limpiarFiltros(): void {
    this.texto.set('');
    this.filtroRol.set('');
    this.pagina.set(0);
  }

  cambiarPagina(evento: PageEvent): void {
    this.tamano.set(evento.pageSize);
    this.pagina.set(evento.pageIndex);
  }

  // --- MODAL DE ALTA Y EDICION ---
  abrirModalNuevo(): void {
    this.abrirModal();
  }

  abrirModalEditar(u: Usuario): void {
    this.abrirModal(u);
  }

  private abrirModal(usuario?: Usuario): void {
    const dialogRef = this.dialog.open(UsuarioDialog, {
      width: '460px',
      data: { usuario, existentes: this.usuarios() }
    });

    dialogRef.afterClosed().subscribe((guardado: Usuario | undefined) => {
      if (!guardado) return;
      const mensaje = usuario
        ? `Tecnico #${guardado.id} actualizado con exito`
        : `Usuario "${guardado.nombre}" registrado con exito`;
      this.snackBar.open(mensaje, 'OK', { duration: 3000 });
      this.pagina.set(0);
      this.cargarUsuarios();
    });
  }

  // --- ELIMINACION CON CONFIRMACION Y PROTECCIONES ---
  esUsuarioActual(u: Usuario): boolean {
    return !!this.auth.usuario() && this.auth.usuario()!.id === u.id;
  }

  esUltimoAdministrador(u: Usuario): boolean {
    return u.rol === 'ADMINISTRADOR' && this.totalAdministradores() <= 1;
  }

  motivoSinEliminar(u: Usuario): string {
    if (this.esUsuarioActual(u)) return 'No puedes eliminar tu propio usuario';
    if (this.esUltimoAdministrador(u)) return 'No se puede eliminar al unico administrador';
    return 'Eliminar usuario';
  }

  puedeEliminar(u: Usuario): boolean {
    return !this.esUsuarioActual(u) && !this.esUltimoAdministrador(u);
  }

  eliminarUsuario(u: Usuario): void {
    if (!u.id || !this.puedeEliminar(u)) return;

    this.helpdeskService.getTickets().subscribe({
      next: (tickets) => this.confirmarEliminar(u, tickets.filter(t => t.tecnicoId === u.id || t.solicitanteId === u.id).length),
      error: () => this.confirmarEliminar(u, null)
    });
  }

  private confirmarEliminar(u: Usuario, ticketsAsociados: number | null): void {
    const dialogRef = this.dialog.open(ConfirmarEliminarDialog, {
      width: '440px',
      data: { usuario: u, ticketsAsociados }
    });

    dialogRef.afterClosed().subscribe((confirmado: boolean) => {
      if (!confirmado || !u.id) return;
      this.helpdeskService.eliminarUsuario(u.id).subscribe({
        next: () => {
          this.snackBar.open(`Usuario #${u.id} eliminado`, 'OK', { duration: 3000 });
          this.cargarUsuarios();
        },
        error: (err) =>
          this.snackBar.open(err?.error?.detail ?? err?.error?.message ?? 'Error al eliminar usuario', 'Cerrar', { duration: 4000 })
      });
    });
  }
}
