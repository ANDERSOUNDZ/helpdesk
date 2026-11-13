import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

import { AuthService } from './auth/auth.service';
import { Login } from './auth/login/login';
import { DashboardComponent } from './dashboard/dashboard.component';
import { Tickets } from './tickets/tickets';
import { Comunicacion } from './comunicacion/comunicacion';
import { Identidad } from './identidad/identidad';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule,
    MatToolbarModule,
    MatTabsModule,
    MatIconModule,
    MatButtonModule,
    Login,
    DashboardComponent,
    Tickets,
    Comunicacion,
    Identidad
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  readonly auth = inject(AuthService);

  constructor() {
    this.auth.iniciar();
  }
}
