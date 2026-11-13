import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  readonly auth = inject(AuthService);

  usuario = '';
  clave = '';
  // Signals: la vista se actualiza sola al terminar la peticion (la app no depende de zone.js).
  readonly error = signal('');
  readonly enviando = signal(false);

  ingresar(): void {
    if (!this.usuario.trim() || !this.clave) {
      this.error.set('Escribe el usuario y la clave');
      return;
    }
    this.enviando.set(true);
    this.error.set('');
    this.auth.login(this.usuario.trim(), this.clave).subscribe({
      next: () => {
        this.enviando.set(false);
        this.clave = '';
      },
      error: (e: HttpErrorResponse) => {
        this.enviando.set(false);
        this.error.set(this.mensajeDe(e));
      }
    });
  }

  private mensajeDe(e: HttpErrorResponse): string {
    switch (e.status) {
      case 401:
        return 'Usuario o clave incorrectos';
      case 400:
        return 'Escribe el usuario y la clave';
      case 503:
        return 'El servidor aun no tiene configurada la autenticacion';
      case 0:
        return 'No se pudo conectar con el servidor';
      default:
        return 'No se pudo iniciar sesion';
    }
  }
}
