export type NivelCalidad = 'sin' | 'buena' | 'regular' | 'mala';

export interface MuestraCalidad {
  nivel: NivelCalidad;
  candidato: 'host' | 'srflx' | 'prflx' | 'relay' | null;
}

export function clasificarCalidad(rttSegundos: number | null, perdida: number): NivelCalidad {
  if (rttSegundos === null) {
    return 'sin';
  }
  if (rttSegundos < 0.25 && perdida < 0.03) {
    return 'buena';
  }
  if (rttSegundos < 0.6 && perdida < 0.1) {
    return 'regular';
  }
  return 'mala';
}

export async function medirCalidad(pc: RTCPeerConnection): Promise<MuestraCalidad> {
  const estadisticas = await pc.getStats();
  let rtt: number | null = null;
  let recibidos = 0;
  let perdidos = 0;
  let candidato: MuestraCalidad['candidato'] = null;
  const locales = new Map<string, string>();
  estadisticas.forEach((reporte) => {
    if (reporte.type === 'local-candidate') {
      locales.set(reporte.id, reporte.candidateType);
    }
  });
  estadisticas.forEach((reporte) => {
    if (reporte.type === 'candidate-pair' && (reporte.nominated || reporte.state === 'succeeded')) {
      if (typeof reporte.currentRoundTripTime === 'number') {
        rtt = reporte.currentRoundTripTime;
      }
      const tipo = locales.get(reporte.localCandidateId);
      if (tipo === 'host' || tipo === 'srflx' || tipo === 'prflx' || tipo === 'relay') {
        candidato = tipo;
      }
    }
    if (reporte.type === 'inbound-rtp' && reporte.kind === 'video') {
      recibidos += reporte.packetsReceived ?? 0;
      perdidos += reporte.packetsLost ?? 0;
    }
  });
  const total = recibidos + perdidos;
  return { nivel: clasificarCalidad(rtt, total > 0 ? perdidos / total : 0), candidato };
}
