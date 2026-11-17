import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { HelpdeskService } from '../../services/helpdesk.service';
import { Usuario } from '../../models/helpdesk.model';

export interface UsuarioDialogData {
  /** Usuario a modificar; si no viene, el modal registra uno nuevo. */
  usuario?: Usuario;
  /** Usuarios ya registrados (para validar el usuario de acceso repetido sin esperar al servidor). */
  existentes: Usuario[];
}

const FORMATO_USUARIO = /^[a-z0-9._-]{3,60}$/;

function noVacio(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && control.value.length > 0 && control.value.trim().length === 0
    ? { vacio: true }
    : null;
}

@Component({
  selector: 'app-usuario-dialog',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule
  ],
  templateUrl: './usuario-dialog.html',
  styleUrl: './usuario-dialog.scss',
})
export class UsuarioDialog {
  private readonly fb = inject(FormBuilder);
  private readonly helpdeskService = inject(HelpdeskService);
  readonly dialogRef = inject(MatDialogRef<UsuarioDialog>);
  readonly data: UsuarioDialogData = inject(MAT_DIALOG_DATA);

  readonly editando = !!this.data.usuario;
  readonly guardando = signal(false);
  readonly verClave = signal(false);
  readonly errorServidor = signal('');

  /** Al editar un usuario que ya tiene acceso, la clave se puede dejar vacia (se conserva la actual). */
  private readonly tieneClaveGuardada = !!this.data.usuario?.username;

  readonly form = this.fb.nonNullable.group({
    nombre: [
      this.data.usuario?.nombre ?? '',
      [Validators.required, noVacio, Validators.minLength(3), Validators.maxLength(150)]
    ],
    rol: [this.data.usuario?.rol ?? 'TECNICO', [Validators.required]],
    username: [
      this.data.usuario?.username ?? '',
      [Validators.pattern(FORMATO_USUARIO), this.usuarioNoRepetido(), this.usuarioSiHayClave()]
    ],
    clave: ['', [Validators.minLength(8), Validators.maxLength(72), this.claveSiHayUsuario()]],
    confirmar: ['', [this.igualQueLaClave()]]
  });

  constructor() {
    // Las reglas dependen de varios campos: al cambiar uno se vuelven a revisar los otros.
    this.f.username.valueChanges.subscribe(() => this.f.clave.updateValueAndValidity({ emitEvent: false }));
    this.f.clave.valueChanges.subscribe(() => {
      this.f.username.updateValueAndValidity({ emitEvent: false });
      this.f.confirmar.updateValueAndValidity({ emitEvent: false });
    });
  }

  private claveSiHayUsuario(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const username = String(control.parent?.get('username')?.value ?? '').trim();
      return username && !control.value && !this.tieneClaveGuardada ? { claveObligatoria: true } : null;
    };
  }

  private usuarioSiHayClave(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const clave = control.parent?.get('clave')?.value ?? '';
      return clave && !String(control.value ?? '').trim() ? { usuarioObligatorio: true } : null;
    };
  }

  private igualQueLaClave(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const clave = control.parent?.get('clave')?.value ?? '';
      return clave !== (control.value ?? '') ? { noCoinciden: true } : null;
    };
  }

  private usuarioNoRepetido(): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const valor = String(control.value ?? '').trim().toLowerCase();
      if (!valor) {
        return null;
      }
      const repetido = this.data.existentes.some(
        u => u.username?.toLowerCase() === valor && u.id !== this.data.usuario?.id
      );
      return repetido ? { repetido: true } : null;
    };
  }

  get f() {
    return this.form.controls;
  }

  cancelar(): void {
    this.dialogRef.close();
  }

  guardar(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.guardando()) {
      return;
    }
    const v = this.form.getRawValue();
    const datos: Usuario = { nombre: v.nombre.trim(), rol: v.rol as Usuario['rol'] };
    if (v.username.trim()) {
      datos.username = v.username.trim();
    }
    if (v.clave) {
      datos.clave = v.clave;
    }

    this.guardando.set(true);
    this.errorServidor.set('');
    const peticion = this.editando
      ? this.helpdeskService.actualizarUsuario(this.data.usuario!.id!, datos)
      : this.helpdeskService.crearUsuario(datos);

    peticion.subscribe({
      next: (guardado) => this.dialogRef.close(guardado),
      error: (err) => {
        this.guardando.set(false);
        const mensaje: string = err?.error?.detail ?? err?.error?.message ?? 'No se pudo guardar el usuario';
        if (err?.status === 409 && /nombre de acceso/i.test(mensaje)) {
          this.f.username.setErrors({ ...(this.f.username.errors ?? {}), repetido: true });
          this.f.username.markAsTouched();
        } else {
          this.errorServidor.set(mensaje);
        }
      }
    });
  }
}
