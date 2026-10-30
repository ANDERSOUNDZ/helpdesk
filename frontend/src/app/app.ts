import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatTabsModule } from '@angular/material/tabs';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

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
    DashboardComponent,
    Tickets,
    Comunicacion,
    Identidad
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {}
