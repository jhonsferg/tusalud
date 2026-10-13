import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
  selector: 'app-medico-list',
  imports: [],
  templateUrl: './medico-list.html',
  styleUrl: './medico-list.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MedicoList {}
