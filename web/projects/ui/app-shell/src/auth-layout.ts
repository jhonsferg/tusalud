import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { CarruselPortada } from '@tusalud/ui/carrusel-portada';
import { ObjetosFlotantes } from '@tusalud/ui/objetos-flotantes';

@Component({
  selector: 'app-auth-layout',
  imports: [RouterOutlet, CarruselPortada, ObjetosFlotantes],
  templateUrl: './auth-layout.html',
  styleUrl: './auth-layout.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AuthLayout {}
