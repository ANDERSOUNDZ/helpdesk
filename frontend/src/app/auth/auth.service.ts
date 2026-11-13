import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export type Rol = 'SOLICITANTE' | 'TECNICO' | 'ADMINISTRADOR';

export interface SesionUsuario {
  id: number;
  nombre: string;
  username: string;
  rol: Rol;
}

interface RespuestaLogin {
  token: string;
  tipo: string;
  expiraEn: number;
  usuario: SesionUsuario;
}

const CLAVE_TOKEN = 'helpdesk.token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);

  private readonly _token = signal<string | null>(this.leerTokenVigente());
  private readonly _usuario = signal<SesionUsuario | null>(this.usuarioDe(this._token()));

  readonly token = this._token.asReadonly();
  readonly usuario = this._usuario.asReadonly();
  readonly logueado = computed(() => this._token() !== null);

  /** Mientras se averigua si el servidor exige iniciar sesion. */
  readonly cargando = signal(true);
  /** true cuando el servidor tiene AUTH_ENABLED=true; false = modo abierto, como antes. */
  readonly requiereLogin = signal(false);
  readonly sesionVencida = signal(false);

  /**
   * Detecta el modo del servidor: si pedir la lista de usuarios sin token da 401, hay que iniciar sesion.
   * Cualquier otra respuesta (200, servicio caido...) se trata como modo abierto, igual que antes.
   */
  iniciar(): void {
    if (this.logueado()) {
      this.requiereLogin.set(true);
      this.cargando.set(false);
      return;
    }
    this.http.get('/usuarios').subscribe({
      next: () => this.terminarDeteccion(false),
      error: (e: HttpErrorResponse) => this.terminarDeteccion(e.status === 401)
    });
  }

  login(username: string, clave: string): Observable<RespuestaLogin> {
    return this.http.post<RespuestaLogin>('/auth/login', { username, clave }).pipe(
      tap((respuesta) => {
        sessionStorage.setItem(CLAVE_TOKEN, respuesta.token);
        this._token.set(respuesta.token);
        this._usuario.set(respuesta.usuario);
        this.sesionVencida.set(false);
        this.requiereLogin.set(true);
        localStorage.setItem('helpdesk.usuarioActualId', String(respuesta.usuario.id));
        localStorage.setItem('helpdesk.usuarioActualNombre', respuesta.usuario.nombre);
      })
    );
  }

  cerrarSesion(vencida = false): void {
    sessionStorage.removeItem(CLAVE_TOKEN);
    localStorage.removeItem('helpdesk.usuarioActualId');
    localStorage.removeItem('helpdesk.usuarioActualNombre');
    this._token.set(null);
    this._usuario.set(null);
    this.requiereLogin.set(true);
    this.sesionVencida.set(vencida);
  }

  /** true si el usuario tiene alguno de esos roles. En modo abierto (sin login) siempre es true. */
  puede(...roles: Rol[]): boolean {
    if (!this.requiereLogin()) {
      return true;
    }
    const actual = this._usuario();
    return !!actual && roles.includes(actual.rol);
  }

  private terminarDeteccion(requiereLogin: boolean): void {
    this.requiereLogin.set(requiereLogin);
    this.cargando.set(false);
  }

  private leerTokenVigente(): string | null {
    try {
      const token = sessionStorage.getItem(CLAVE_TOKEN);
      const datos = token ? this.decodificar(token) : null;
      if (!token || !datos || (typeof datos['exp'] === 'number' && datos['exp'] * 1000 <= Date.now())) {
        sessionStorage.removeItem(CLAVE_TOKEN);
        return null;
      }
      return token;
    } catch {
      return null;
    }
  }

  private usuarioDe(token: string | null): SesionUsuario | null {
    const datos = token ? this.decodificar(token) : null;
    if (!datos) {
      return null;
    }
    const roles = datos['roles'] as Rol[] | undefined;
    return {
      id: Number(datos['uid']),
      nombre: String(datos['nombre'] ?? datos['sub'] ?? ''),
      username: String(datos['sub'] ?? ''),
      rol: roles?.[0] ?? 'SOLICITANTE'
    };
  }

  /** Solo para mostrar el nombre y el rol: el servidor es quien valida el token de verdad. */
  private decodificar(token: string): Record<string, unknown> | null {
    try {
      const carga = token.split('.')[1];
      const binario = atob(carga.replace(/-/g, '+').replace(/_/g, '/'));
      const texto = new TextDecoder().decode(Uint8Array.from(binario, (c) => c.charCodeAt(0)));
      return JSON.parse(texto);
    } catch {
      return null;
    }
  }
}
