import { Component, OnInit, OnDestroy, inject, ChangeDetectorRef } from '@angular/core';
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
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subscription, interval } from 'rxjs';
import { HelpdeskService } from '../services/helpdesk.service';
import { Usuario } from '../models/helpdesk.model';

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
    MatSnackBarModule
  ],
  templateUrl: './identidad.html',
  styleUrl: './identidad.scss',
})
export class Identidad implements OnInit, OnDestroy {
  private helpdeskService = inject(HelpdeskService);
  private snackBar = inject(MatSnackBar);
  private cdr = inject(ChangeDetectorRef);
  private autoRefreshSub?: Subscription;

  usuarios: Usuario[] = [];
  columnasUsuarios: string[] = ['id', 'nombre', 'rol', 'acciones'];
  nuevoUsuarioNombre = '';
  nuevoUsuarioRol: 'USUARIO' | 'TECNICO' | 'ADMINISTRADOR' = 'TECNICO';

  // Edición de Técnico
  tecnicoEnEdicion: Usuario | null = null;
  editarNombre = '';
  editarRol: 'USUARIO' | 'TECNICO' | 'ADMINISTRADOR' = 'TECNICO';

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

  cargarUsuarios(): void {
    this.helpdeskService.getUsuarios().subscribe({
      next: (res) => {
        this.usuarios = [...res];
        this.cdr.markForCheck();
      },
      error: () => console.warn('Servicio de identidad no disponible')
    });
  }

  crearUsuario(): void {
    if (!this.nuevoUsuarioNombre.trim()) {
      this.snackBar.open('El nombre del tecnico es obligatorio', 'Cerrar', { duration: 3000 });
      return;
    }
    const nuevo: Usuario = {
      nombre: this.nuevoUsuarioNombre,
      rol: this.nuevoUsuarioRol
    };
    this.helpdeskService.crearUsuario(nuevo).subscribe({
      next: (creado) => {
        this.snackBar.open(`Tecnico "${creado.nombre}" registrado con exito`, 'OK', { duration: 3000 });
        this.nuevoUsuarioNombre = '';
        this.cargarUsuarios();
      },
      error: () => this.snackBar.open('Error al crear tecnico', 'Cerrar', { duration: 3000 })
    });
  }

  iniciarEdicion(u: Usuario): void {
    this.tecnicoEnEdicion = { ...u };
    this.editarNombre = u.nombre;
    this.editarRol = u.rol;
  }

  cancelarEdicion(): void {
    this.tecnicoEnEdicion = null;
    this.editarNombre = '';
  }

  guardarEdicion(): void {
    if (!this.tecnicoEnEdicion?.id || !this.editarNombre.trim()) {
      this.snackBar.open('El nombre no puede estar vacio', 'Cerrar', { duration: 3000 });
      return;
    }
    const datosActualizados: Usuario = {
      id: this.tecnicoEnEdicion.id,
      nombre: this.editarNombre.trim(),
      rol: this.editarRol
    };
    this.helpdeskService.actualizarUsuario(this.tecnicoEnEdicion.id, datosActualizados).subscribe({
      next: (actualizado) => {
        this.snackBar.open(`Tecnico #${actualizado.id} actualizado correctamente`, 'OK', { duration: 3000 });
        this.cancelarEdicion();
        this.cargarUsuarios();
      },
      error: () => this.snackBar.open('Error al actualizar tecnico', 'Cerrar', { duration: 3000 })
    });
  }

  eliminarUsuario(id?: number): void {
    if (!id) return;
    this.helpdeskService.eliminarUsuario(id).subscribe({
      next: () => {
        this.snackBar.open(`Tecnico #${id} dado de baja`, 'OK', { duration: 3000 });
        this.cargarUsuarios();
      },
      error: () => this.snackBar.open('Error al eliminar tecnico', 'Cerrar', { duration: 3000 })
    });
  }
}
