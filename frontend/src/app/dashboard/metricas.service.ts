import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, map, of } from 'rxjs';
import { Ticket } from '../models/helpdesk.model';

/** Resumen de las encuestas de satisfaccion (servicio-comunicacion). Definido aqui porque el modelo compartido ya no lo incluye. */
export interface ResumenEncuestas {
  totalEncuestas: number;
  respondidas: number;
  pendientes: number;
  promedio: number;
  distribucion: Record<number, number>;
}

/** Ticket con los campos de fechas y SLA que expone servicio-tickets (los viejos pueden traerlos en null). */
export interface TicketMetrica extends Ticket {
  categoriaId?: number | null;
  creadoEn?: string | null;
  asignadoEn?: string | null;
  resueltoEn?: string | null;
  fechaLimite?: string | null;
  vencido?: boolean;
}

export interface CategoriaMetrica {
  id: number;
  nombre: string;
  descripcion?: string | null;
}

export interface FiltrosExportacion {
  estado?: string;
  prioridad?: string;
  tecnicoId?: number;
  categoriaId?: number;
  q?: string;
  vencidos?: boolean;
}

/** Llamadas propias del panel de metricas (lo que no esta en HelpdeskService). Cada una falla con gracia. */
@Injectable({ providedIn: 'root' })
export class MetricasService {
  private http = inject(HttpClient);

  /** Si el proxy no enruta /categorias la respuesta no es JSON: se devuelve lista vacia y el panel usa "Categoria #id". */
  categorias(): Observable<CategoriaMetrica[]> {
    return this.http.get<CategoriaMetrica[]>('/categorias').pipe(
      map(lista => (Array.isArray(lista) ? lista : [])),
      catchError(() => of([]))
    );
  }

  /** null = el servicio de comunicacion no respondio (el panel muestra "sin datos"). */
  encuestas(): Observable<ResumenEncuestas | null> {
    return this.http.get<ResumenEncuestas>('/encuestas/resumen').pipe(
      map(r => (r && typeof r.promedio === 'number' ? r : null)),
      catchError(() => of(null))
    );
  }

  /** Descarga /tickets/export.csv (solo ADMINISTRADOR) con los filtros del reporte. */
  exportarCsv(filtros: FiltrosExportacion): Observable<Blob> {
    let params = new HttpParams();
    for (const [clave, valor] of Object.entries(filtros)) {
      if (valor !== undefined && valor !== null && valor !== '' && valor !== false) {
        params = params.set(clave, String(valor));
      }
    }
    return this.http.get('/tickets/export.csv', { params, responseType: 'blob' });
  }
}
