import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, FormGroupDirective } from '@angular/forms';
import { MatDialogRef, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { HelpdeskService } from '../../services/helpdesk.service';
import { Categoria } from '../../models/helpdesk.model';

/** Administracion de categorias (solo administrador): listar, agregar, editar y eliminar las que no tengan tickets. */
@Component({
  selector: 'app-categorias-dialog',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './categorias-dialog.html',
  styleUrl: './categorias-dialog.scss',
})
export class CategoriasDialog implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly helpdeskService = inject(HelpdeskService);
  readonly dialogRef = inject(MatDialogRef<CategoriasDialog>);

  readonly categorias = signal<Categoria[]>([]);
  readonly editando = signal<Categoria | null>(null);
  readonly confirmandoId = signal<number | null>(null);
  readonly guardando = signal(false);
  readonly error = signal('');
  /** Indica al cerrar si hubo cambios, para que la pantalla recargue la lista. */
  private huboCambios = false;

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100), this.noVacio]],
    descripcion: ['', [Validators.maxLength(250)]]
  });

  private noVacio(control: { value: string }) {
    return control.value.length > 0 && control.value.trim().length === 0 ? { vacio: true } : null;
  }

  get f() {
    return this.form.controls;
  }

  ngOnInit(): void {
    this.cargar();
  }

  private cargar(): void {
    this.helpdeskService.getCategorias().subscribe({
      next: (lista) => this.categorias.set(lista),
      error: () => this.error.set('No se pudieron cargar las categorias')
    });
  }

  editar(c: Categoria): void {
    this.editando.set(c);
    this.confirmandoId.set(null);
    this.error.set('');
    this.form.reset({ nombre: c.nombre, descripcion: c.descripcion ?? '' });
  }

  cancelarEdicion(formDirective: FormGroupDirective): void {
    this.editando.set(null);
    this.error.set('');
    formDirective.resetForm({ nombre: '', descripcion: '' });
  }

  guardar(formDirective: FormGroupDirective): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.guardando()) {
      return;
    }
    const v = this.form.getRawValue();
    const datos: Categoria = { nombre: v.nombre.trim(), descripcion: v.descripcion.trim() };
    const actual = this.editando();

    this.guardando.set(true);
    this.error.set('');
    const peticion = actual
      ? this.helpdeskService.actualizarCategoria(actual.id!, datos)
      : this.helpdeskService.crearCategoria(datos);

    peticion.subscribe({
      next: () => {
        this.guardando.set(false);
        this.huboCambios = true;
        this.editando.set(null);
        formDirective.resetForm({ nombre: '', descripcion: '' });
        this.cargar();
      },
      error: (err) => {
        this.guardando.set(false);
        this.error.set(err?.error?.detail ?? err?.error?.message ?? 'No se pudo guardar la categoria');
      }
    });
  }

  pedirConfirmacion(c: Categoria): void {
    this.confirmandoId.set(c.id ?? null);
    this.error.set('');
  }

  eliminar(c: Categoria): void {
    if (!c.id) return;
    this.helpdeskService.eliminarCategoria(c.id).subscribe({
      next: () => {
        this.confirmandoId.set(null);
        this.huboCambios = true;
        this.cargar();
      },
      error: (err) => {
        this.confirmandoId.set(null);
        this.error.set(err?.error?.detail ?? err?.error?.message ?? 'No se pudo eliminar la categoria');
      }
    });
  }

  cerrar(): void {
    this.dialogRef.close(this.huboCambios);
  }
}
