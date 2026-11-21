const L = require("./lib");

const NOTAS = {
  enc: "Atenciones R01 a R22: cita reservada por el paciente, confirmada y atendida por el médico, con receta clínica en texto. Las ve el médico que atendió, el paciente y los médicos con el paciente compartido.",
  plan: "Planes de tratamiento de crónicos. Los ven el médico del plan y el paciente. Calidad, gerencia, enfermería y administración no tienen acceso.",
  evento:
    "Eventos adversos. Los ven calidad, gerencia y quien notificó. El anónimo no muestra al notificante.",
  doc: "Documentos médicos. Los ve el médico emisor y el paciente titular, y se verifican con el código en la página pública.",
  cons: "Consentimientos. Los ven el médico solicitante y el paciente. Recepción y administración no los ven.",
  cir: "Cirugías con equipo: dr.cirujano, dra.anestesia, enfermeria3 y programación de enfermeria2. Las ven los médicos y enfermería, no recepción, caja ni finanzas.",
  ciclo:
    "Ciclos de esterilización. Los ve enfermería, y calidad consulta ciclos y alertas.",
  compra:
    "Requisiciones, cotizaciones y órdenes. Farmacia solo ve sus requisiciones, almacén y finanzas ven las órdenes, gerencia aprueba las mayores a 5000 soles.",
  asiento:
    "Asientos contables. Los ven finanzas y gerencia, el resto de roles incluida administración no.",
  arco: "Solicitudes ARCO. Las ve el paciente titular y las gestionan calidad y administración.",
  incidente:
    "Incidentes de seguridad de datos. Los ven calidad y administración.",
  cobro:
    "Cobros de caja y su comprobante electrónico, que queda en ERROR por la falta de certificado.",
  nino: "Pacientes pediátricos con carné de vacunación y curvas de crecimiento. Los ven dra.lopez, enfermería con atención vigente y el propio paciente.",
  odonto:
    "Odontogramas y planes odontológicos de dra.montoya, el cobro lo registra caja.",
  tele: "Citas de teleconsulta confirmadas.",
  canales: "Pacientes con canales y preferencias de aviso configurados.",
  dup: "Pares de pacientes duplicados: D1 ya fusionado, D2 pendiente de fusionar.",
  preaut: "Preautorizaciones de seguros.",
  tesoreria: "Movimientos de tesorería.",
  extracto: "Extracto bancario importado y conciliación en curso.",
};

const salida = {
  generado: new Date().toISOString(),
  notas: NOTAS,
  bloqueos: {},
  datos: {},
};
const todo = L.E.todo();
for (const [k, v] of Object.entries(todo)) {
  if (
    k.startsWith("perfil:") ||
    k.startsWith("dni:") ||
    k === "marcaciones" ||
    k === "dirMedicos"
  )
    continue;
  if (k.startsWith("bloqueo:")) {
    salida.bloqueos[k] = v;
    continue;
  }
  const grupo = k.split(":")[0];
  salida.datos[grupo] = salida.datos[grupo] || {};
  salida.datos[grupo][k.includes(":") ? k.split(":").slice(1).join(":") : k] =
    v;
}
salida.bloqueos["certificado-digital"] =
  "POST /recetas, POST /hce/atenciones/{id}/cierre y la generación del XML de los comprobantes responden o quedan en error: no hay certificado digital configurado en el entorno";
salida.bloqueos["codigo-orden"] =
  "POST /laboratorio/ordenes, POST /imagenes/ordenes e ingresos de hospitalización responden 409: el código temporal PEND-nanoTime excede varchar(20)";
L.escribirJson(L.DIR + "/manifiesto-datos.json", salida);
console.log(
  "manifiesto de datos escrito, grupos:",
  Object.keys(salida.datos).join(", "),
);
