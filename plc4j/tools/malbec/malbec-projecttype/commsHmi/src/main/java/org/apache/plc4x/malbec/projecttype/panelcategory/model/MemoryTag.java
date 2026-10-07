/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.plc4x.malbec.projecttype.panelcategory.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MemoryTag {
    private static final Pattern LONGITUD = Pattern.compile("\\((\\d+)\\)");
    private static final Pattern CORCHETE = Pattern.compile("\\[\\s*(\\d*)\\s*]");
    private static final Pattern RANGO = Pattern.compile("\\[\\s*(\\d+)\\s*\\.\\.\\s*(\\d+)\\s*]");
    
    private static final Pattern DB_CON_CODIGO = Pattern.compile("^([A-Za-z]+)(\\d+)\\.DB([A-Za-z]*)(\\d+)$");
    private static final Pattern DB_CORTO = Pattern.compile("^([A-Za-z]+)(\\d+):(\\d+)$");
    private static final Pattern CODIGO_Y_NUMERO = Pattern.compile("^((?:[MIQE]|[MIQE][BWDL]|C|T))(\\d+)$");
    private static final Pattern SOLO_NUMERO = Pattern.compile("^(\\d+)$");
    private static final Pattern TIPO = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(\\s*\\(\\s*\\d+\\s*\\))?(\\s*\\[\\s*\\d*\\s*])?$");

    private final String raw;
    private final String block;
    private final String code;
    private final int baseByte;
    private final int bit;
    private final int firstByte;
    private final int lastByte;
    private final int cantidad;
    private final String family;
    private final boolean hasRange;

    private MemoryTag(String raw, String block, String code, int baseByte, int bit,
                      int firstByte, int lastByte, int cantidad, String family, boolean hasRange) {
        this.raw = raw;
        this.block = block;
        this.code = code;
        this.baseByte = baseByte;
        this.bit = bit;
        this.firstByte = firstByte;
        this.lastByte = lastByte;
        this.cantidad = cantidad;
        this.family = family;
        this.hasRange = hasRange;
    }

    public static MemoryTag parse(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String original = raw;
        String resto = raw.trim();
        if (resto.startsWith("%")) {
            resto = resto.substring(1);
        }

        String tipo = null;
        int cantidad = -1;
        int[] seleccion = new int[]{-1, -1};

        String[] partes = separarTipo(resto);
        String direccionTags = partes[0];
        if (partes[1] != null) {
            tipo = partes[1];
            Matcher mt = LONGITUD.matcher(tipo);
            if (mt.find()) {
                cantidad = Integer.parseInt(mt.group(1));
            }
            Matcher mc = CORCHETE.matcher(tipo);
            if (mc.find() && !mc.group(1).isEmpty()) {
                cantidad = Integer.parseInt(mc.group(1));
            }
            tipo = tipo.replaceFirst("\\s*\\(\\s*\\d+\\s*\\)", "")
                       .replaceFirst("\\s*\\[\\s*\\d*\\s*]", "")
                       .trim().toUpperCase();
        }

        boolean rangoExplicito = false;
        Matcher rango = RANGO.matcher(direccionTags);
        if (rango.find()) {
            seleccion[0] = Integer.parseInt(rango.group(1));
            seleccion[1] = Integer.parseInt(rango.group(2));
            direccionTags = rango.replaceFirst("");
            rangoExplicito = true;
        } else {
            Matcher indice = CORCHETE.matcher(direccionTags);
            if (indice.find()) {
                String valor = indice.group(1);
                seleccion[0] = valor.isEmpty() ? -1 : Integer.parseInt(valor);
                seleccion[1] = seleccion[0];
                direccionTags = indice.replaceFirst("");
            }
        }

        String direccion = direccionTags.replaceAll("\\s+", "");
        int bit = -1;
        int posPunto = direccion.lastIndexOf('.');
        if (posPunto > 0 && posPunto < direccion.length() - 1) {
            String posibleBit = direccion.substring(posPunto + 1);
            if (posibleBit.matches("[0-7]")) {
                bit = Integer.parseInt(posibleBit);
                direccion = direccion.substring(0, posPunto);
            }
        }

        String block = "";
        String code = "";
        int numero = -1;

        Matcher dbCodigo = DB_CON_CODIGO.matcher(direccion);
        Matcher dbCorto = DB_CORTO.matcher(direccion);
        Matcher codNum = CODIGO_Y_NUMERO.matcher(direccion);
        Matcher soloNum = SOLO_NUMERO.matcher(direccion);

        if (dbCodigo.matches()) {
            block = dbCodigo.group(1).toUpperCase() + dbCodigo.group(2);
            code = dbCodigo.group(3).isEmpty() ? "" : "DB" + dbCodigo.group(3).toUpperCase();
            numero = Integer.parseInt(dbCodigo.group(4));
        } else if (dbCorto.matches()) {
            block = dbCorto.group(1).toUpperCase() + dbCorto.group(2);
            numero = Integer.parseInt(dbCorto.group(3));
        } else if (codNum.matches()) {
            code = codNum.group(1).toUpperCase();
            numero = Integer.parseInt(codNum.group(2));
        } else if (soloNum.matches()) {
            numero = Integer.parseInt(soloNum.group(1));
        } else {
            block = direccion;
        }

        return new MemoryTag(original, block, code, numero, bit,
                seleccion[0], seleccion[1], cantidad, tipo, rangoExplicito);
    }

    private static String[] separarTipo(String texto) {
        for (int i = 0; i < texto.length(); i++) {
            if (texto.charAt(i) == ':' && i + 1 < texto.length()) {
                String tras = texto.substring(i + 1).trim();
                if (TIPO.matcher(tras).matches()) {
                    return new String[]{texto.substring(0, i), tras};
                }
            }
        }
        return new String[]{texto, null};
    }

    public boolean tieneDireccion() {
        return !code.isEmpty() || baseByte >= 0;
    }

    public String raw() {
        return raw;
    }

    public String block() {
        return block;
    }

    public String code() {
        return code;
    }

    public int baseByte() {
        return baseByte;
    }

    public int bit() {
        return bit;
    }

    public boolean hasRange() {
        return hasRange;
    }

    public String family() {
        return family;
    }

    public int firstByte() {
        return firstByte;
    }

    public int lastByte() {
        return lastByte;
    }

    public boolean rangoValido() {
        return firstByte >= 0 && lastByte >= 0 && lastByte >= firstByte;
    }

    public int byteCapacity() {
        if (firstByte >= 0 && lastByte >= 0) {
            return Math.max(0, lastByte - firstByte + 1);
        }

        if (cantidad > 0) {
            return cantidad;
        }

        if (!tieneDireccion() || bit >= 0) {
            return 0;
        }

        if ("STRING".equalsIgnoreCase(family)) {
            return 254;
        }
        if ("WSTRING".equalsIgnoreCase(family)) {
            return 254 * 2;
        }

        int bitsCodigo = codeBits();
        if (bitsCodigo > 0) {
            return bitsCodigo / 8;
        }

        int bitsTipo = DataType.bitsDeTipo(family);
        return bitsTipo > 0 ? bitsTipo / 8 : 0;
    }

    public int codeBits() {
        if (bit >= 0) return 1;
        if (code.isEmpty()) return 0;
        String c = code.toUpperCase();
        if (c.endsWith("X")) return 1;
        if (c.endsWith("LD") || c.endsWith("L")) return 64;
        if (c.endsWith("B")) return 8;
        if (c.endsWith("W")) return 16;
        if (c.endsWith("D")) return 32;
        return 0;
    }

    public int startByteEffective() {
        int base = Math.max(0, baseByte);
        int offset = Math.max(0, firstByte);
        return base + offset;
    }

    public int endByteEffective() {
        int start = startByteEffective();
        int cap = byteCapacity();
        return start + Math.max(1, cap) - 1;
    }

    public boolean solapaCon(MemoryTag otro) {
        if (otro == null || !tieneDireccion() || !otro.tieneDireccion()) {
            return false;
        }
        if (!block.equalsIgnoreCase(otro.block)) {
            return false;
        }
        // Comparación de bits explícitos en la misma posición de byte
        if (bit >= 0 && otro.bit >= 0) {
            return baseByte == otro.baseByte && bit == otro.bit;
        }
        // Verificación de intersección de rangos de bytes
        return this.startByteEffective() <= otro.endByteEffective() && 
               this.endByteEffective() >= otro.startByteEffective();
    }
}