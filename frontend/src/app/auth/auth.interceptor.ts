import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/**
 * Agrega el token a las peticiones al propio servidor (rutas que empiezan con "/") y, si el servidor responde 401
 * con una sesion abierta, la cierra para volver al login. Nunca envia el token a otros dominios.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();
  const esLogin = req.url.startsWith('/auth/login');
  const esPropia = req.url.startsWith('/');

  const peticion = token && esPropia && !esLogin
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(peticion).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && token && esPropia && !esLogin) {
        auth.cerrarSesion(true);
      }
      return throwError(() => error);
    })
  );
};
