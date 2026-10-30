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
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Subscription, interval } from 'rxjs';
import { HelpdeskService } from '../services/helpdesk.service';
import { Usuario } from '../models/helpdesk.model';
import { EditarTecnicoDialog } from './editar-tecnico-dialog/editar-tecnico-dialog';

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
  private cdr = inject(ChangeDetectorRef);
  private autoRefreshSub?: Subscription;

  usuarios: Usuario[] = [];
  columnasUsuarios: string[] = ['id', 'nombre', 'rol', 'acciones'];
  nuevoUsuarioNombre = '';
  nuevoUsuarioRol: 'USUARIO' | 'TECNICO' | 'ADMINISTRADOR' = 'TECNICO';

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

  // --- MODAL DE EDICIÓN DE TÉCNICO ---
  abrirModalEditar(u: Usuario): void {
    const dialogRef = this.dialog.open(EditarTecnicoDialog, {
      width: '440px',
      data: u
    });

    dialogRef.afterClosed().subscribe((datosActualizados: Usuario | undefined) => {
      if (datosActualizados && datosActualizados.id) {
        this.helpdeskService.actualizarUsuario(datosActualizados.id, datosActualizados).subscribe({
          next: (res) => {
            this.snackBar.open(`Tecnico #${res.id} actualizado con exito`, 'OK', { duration: 3000 });
            this.cargarUsuarios();
          },
          error: () => this.snackBar.open('Error al actualizar tecnico', 'Cerrar', { duration: 3000 })
        });
      }
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
