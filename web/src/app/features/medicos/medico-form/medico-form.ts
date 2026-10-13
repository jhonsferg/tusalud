import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-medico-form',
  imports: [],
  templateUrl: './medico-form.html',
  styleUrl: './medico-form.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MedicoForm {}
