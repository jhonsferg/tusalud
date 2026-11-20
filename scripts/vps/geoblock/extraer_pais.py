import ipaddress
import struct
import sys

RUTA_BD = sys.argv[1]
PAIS = sys.argv[2]
SALIDA_V4 = sys.argv[3]
SALIDA_V6 = sys.argv[4]

with open(RUTA_BD, "rb") as f:
    datos = f.read()

marcador = b"\xab\xcd\xefMaxMind.com"
inicio_meta = datos.rfind(marcador) + len(marcador)


class Decodificador:
    def __init__(self, buf, base):
        self.buf = buf
        self.base = base

    def decodificar(self, offset):
        buf = self.buf
        ctrl = buf[offset]
        offset += 1
        tipo = ctrl >> 5
        if tipo == 1:
            tam = ctrl & 0x1F
            if tam >= 29:
                pass
            tamano_ptr = (ctrl >> 3) & 0x3
            valor = ctrl & 0x7
            if tamano_ptr == 0:
                p = (valor << 8) | buf[offset]
                offset += 1
            elif tamano_ptr == 1:
                p = ((valor << 16) | int.from_bytes(buf[offset:offset + 2], "big")) + 2048
                offset += 2
            elif tamano_ptr == 2:
                p = ((valor << 24) | int.from_bytes(buf[offset:offset + 3], "big")) + 526336
                offset += 3
            else:
                p = int.from_bytes(buf[offset:offset + 4], "big")
                offset += 4
            resultado, _ = self.decodificar(self.base + p)
            return resultado, offset
        if tipo == 0:
            tipo = 7 + buf[offset]
            offset += 1
        tam = ctrl & 0x1F
        if tam == 29:
            tam = 29 + buf[offset]
            offset += 1
        elif tam == 30:
            tam = 285 + int.from_bytes(buf[offset:offset + 2], "big")
            offset += 2
        elif tam == 31:
            tam = 65821 + int.from_bytes(buf[offset:offset + 3], "big")
            offset += 3
        if tipo == 2:
            return buf[offset:offset + tam].decode("utf-8"), offset + tam
        if tipo == 7:
            mapa = {}
            for _ in range(tam):
                clave, offset = self.decodificar(offset)
                valor, offset = self.decodificar(offset)
                mapa[clave] = valor
            return mapa, offset
        if tipo == 11:
            lista = []
            for _ in range(tam):
                valor, offset = self.decodificar(offset)
                lista.append(valor)
            return lista, offset
        if tipo in (5, 6, 9, 10):
            return int.from_bytes(buf[offset:offset + tam], "big"), offset + tam
        if tipo == 14:
            return bool(tam), offset
        return None, offset + tam


meta, _ = Decodificador(datos, inicio_meta).decodificar(inicio_meta)
nodos = meta["node_count"]
tam_registro = meta["record_size"]
version_ip = meta["ip_version"]
tam_nodo = tam_registro * 2 // 8
tam_arbol = nodos * tam_nodo
base_datos = tam_arbol + 16
dec = Decodificador(datos, base_datos)


def registro(nodo, bit):
    o = nodo * tam_nodo
    if tam_registro == 24:
        o += bit * 3
        return int.from_bytes(datos[o:o + 3], "big")
    if tam_registro == 28:
        medio = datos[o + 3]
        if bit == 0:
            return ((medio & 0xF0) << 20) | int.from_bytes(datos[o:o + 3], "big")
        return ((medio & 0x0F) << 24) | int.from_bytes(datos[o + 4:o + 7], "big")
    o += bit * 4
    return int.from_bytes(datos[o:o + 4], "big")


cache = {}


def pais_de(valor):
    if valor not in cache:
        mapa, _ = dec.decodificar(base_datos + (valor - nodos - 16))
        cache[valor] = (mapa.get("country") or {}).get("iso_code")
    return cache[valor]


bits_totales = 128 if version_ip == 6 else 32
redes4 = []
redes6 = []
pila = [(0, 0, 0)]
while pila:
    nodo, prefijo, largo = pila.pop()
    for bit in (0, 1):
        valor = registro(nodo, bit)
        p = (prefijo << 1) | bit
        l = largo + 1
        if valor == nodos:
            continue
        if valor < nodos:
            pila.append((valor, p, l))
            continue
        if pais_de(valor) != PAIS:
            continue
        completo = p << (bits_totales - l)
        if version_ip == 6:
            if (completo >> 32) == 0 and l >= 96:
                redes4.append(ipaddress.ip_network((completo & 0xFFFFFFFF, l - 96)))
            elif (completo >> 80) == 0 and ((completo >> 32) & 0xFFFF) == 0xFFFF and l >= 96:
                continue
            elif (completo >> 112) == 0x2002:
                continue
            elif l >= 96 and (completo >> 32) == 0:
                continue
            elif (completo >> 32) != 0:
                redes6.append(ipaddress.ip_network((completo, l)))
        else:
            redes4.append(ipaddress.ip_network((completo, l)))

v4 = list(ipaddress.collapse_addresses(redes4))
v6 = list(ipaddress.collapse_addresses(redes6))
with open(SALIDA_V4, "w") as f:
    f.write("\n".join(str(r) for r in v4) + "\n")
with open(SALIDA_V6, "w") as f:
    f.write("\n".join(str(r) for r in v6) + "\n")
print("v4:", len(v4), "v6:", len(v6))
