import { Directive, Pipe, PipeTransform } from '@angular/core';

import { enmascararCorreo, enmascararDni, enmascararTelefono } from '@tusalud/core';

@Directive({
  selector: '[appSensible]',
  host: {
    class: 'sensible',
    tabindex: '0',
    '[attr.title]': '"Dato sensible. Pasa el cursor o enfoca para verlo."',
  },
})
export class Sensible {}

@Pipe({ name: 'enmascararDni' })
export class EnmascararDniPipe implements PipeTransform {
  transform(valor: string | null | undefined): string {
    return enmascararDni(valor);
  }
}

@Pipe({ name: 'enmascararTelefono' })
export class EnmascararTelefonoPipe implements PipeTransform {
  transform(valor: string | null | undefined): string {
    return enmascararTelefono(valor);
  }
}

@Pipe({ name: 'enmascararCorreo' })
export class EnmascararCorreoPipe implements PipeTransform {
  transform(valor: string | null | undefined): string {
    return enmascararCorreo(valor);
  }
}
