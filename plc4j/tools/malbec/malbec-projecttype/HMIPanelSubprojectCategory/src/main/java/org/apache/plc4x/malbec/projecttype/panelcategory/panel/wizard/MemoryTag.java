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
package org.apache.plc4x.malbec.projecttype.panelcategory.panel.wizard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

                   
public final class MemoryTag {
    private static final Pattern RANGO = Pattern.compile("\\[\\s*(\\d+)\\s*\\.\\.\\s*(\\d+)\\s*]");
    private static final Pattern CORCHETE = Pattern.compile("\\[\\s*(\\d*)\\s*]");
    private static final Pattern LONGITUD = Pattern.compile("\\((\\d+)\\)");
    private static final Pattern DB_CON_CODIGO =
            Pattern.compile("^([A-Za-z]+)(\\d+)\\.DB([A-Za-z]*)(\\d+)$");
    private static final Pattern DB_CORTO = Pattern.compile("^([A-Za-z]+)(\\d+):(\\d+)$");
    private static final Pattern CODIGO_Y_NUMERO = Pattern.compile("^([A-Za-z]+)(\\d+)$");
    private static final Pattern SOLO_NUMERO = Pattern.compile("^(\\d+)$");
    private static final Pattern TIPO =
            Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(\\s*\\(\\s*\\d+\\s*\\))?(\\s*\\[\\s*\\d*\\s*])?$");

    private final String raw;
    private final String block;
    private final String code;
    private final int baseByte;
    private final int bit;
    private final int firstByte;
    private final int lastByte;
    private final int cantidad;
    private final String family;

    private MemoryTag(String raw, String block, String code, int baseByte, int bit,
            int firstByte, int lastByte, int cantidad, String family) {
        this.raw = raw;
        this.block = block;
        this.code = code;
        this.baseByte = baseByte;
        this.bit = bit;
        this.firstByte = firstByte;
        this.lastByte = lastByte;
        this.cantidad = cantidad;
        this.family = family;
    }

    public static MemoryTag parse(String raw) {
        if (raw == null) {
            return null;
        }
        String original = raw;
        String tag = raw.trim();
        if (tag.isEmpty()) {
            return null;
        }
        String resto = tag;
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

        Matcher rango = RANGO.matcher(direccionTags);
        if (rango.find()) {
            seleccion[0] = Integer.parseInt(rango.group(1));
            seleccion[1] = Integer.parseInt(rango.group(2));
            direccionTags = rango.replaceFirst("");
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
            block = dbCodigo.group(1) + dbCodigo.group(2);
            code = dbCodigo.group(3).isEmpty() ? "" : "DB" + dbCodigo.group(3).toUpperCase();
            numero = Integer.parseInt(dbCodigo.group(4));
        } else if (dbCorto.matches()) {
            block = dbCorto.group(1) + dbCorto.group(2);
            numero = Integer.parseInt(dbCorto.group(3));
        } else if (codNum.matches()) {
            code = codNum.group(1).toUpperCase();
            numero = Integer.parseInt(codNum.group(2));
        } else if (soloNum.matches()) {
            numero = Integer.parseInt(soloNum.group(1));
        } else {
            block = direccion;
        }

        String familia = tipo;

        return new MemoryTag(original, block, code, numero, bit,
                seleccion[0], seleccion[1], cantidad, familia);
    }

    private static String[] separarTipo(String texto) {
        String cuerpo = texto;
        for (int i = 0; i < cuerpo.length(); i++) {
            if (cuerpo.charAt(i) != ':' || i + 1 >= cuerpo.length()) {
                continue;
            }
            String tras = cuerpo.substring(i + 1).trim();
            if (TIPO.matcher(tras).matches()) {
                return new String[]{cuerpo.substring(0, i), tras};
            }
        }
        return new String[]{cuerpo, null};
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

    public int firstByte() {
        return firstByte;
    }

    public int lastByte() {
        return lastByte;
    }

    /** Tipo declarado en el tag, o null si no lo lleva. */
    public String family() {
        return family;
    }

    public boolean hasRange() {
        return lastByte >= 0;
    }

    public boolean rangoValido() {
        return lastByte < 0 || lastByte >= firstByte;
    }

    public boolean tieneDireccion() {
        return !code.isEmpty() || baseByte >= 0;
    }

    public int codeBits() {
        if (bit >= 0) {
            return 1;
        }
        if (code.isEmpty()) {
            return 0;
        }
        String c = code.toUpperCase();
        if (c.endsWith("LD")) {
            return 64;
        }
        if (c.endsWith("X")) {
            return 1;
        }
        if (c.endsWith("B")) {
            return 8;
        }
        if (c.endsWith("W")) {
            return 16;
        }
        if (c.endsWith("D")) {
            return 32;
        }
        if (c.endsWith("L")) {
            return 64;
        }
        return 0;
    }

    public int byteCapacity() {
        if (!rangoValido()) {
            return 0;
        }
        if (lastByte >= 0) {
            return lastByte - firstByte + 1;
        }
        if (cantidad > 0) {
            return cantidad;
        }
        return codeBits() / 8;
    }

    public int absoluteFirst() {
        return Math.max(0, baseByte) + Math.max(0, firstByte);
    }

    public int absoluteLast() {
        return Math.max(0, baseByte) + Math.max(0, lastByte);
    }

    public String direccion() {
        if (!tieneDireccion()) {
            return raw;
        }
        String d = block.isEmpty()
                ? code.toUpperCase() + baseByte
                : block.toUpperCase() + "." + code.toUpperCase() + baseByte;
        return bit >= 0 ? d + "." + bit : d;
    }

    public boolean solapaCon(MemoryTag otro) {
        if (otro == null || !tieneDireccion() || !otro.tieneDireccion()) {
            return false;
        }
        if (!block.equalsIgnoreCase(otro.block)) {
            return false;
        }
        if (bit >= 0 && otro.bit >= 0) {
            return baseByte == otro.baseByte && bit == otro.bit;
        }
        if (hasRange() && otro.hasRange()) {
            return absoluteFirst() <= otro.absoluteLast()
                    && absoluteLast() >= otro.absoluteFirst();
        }
        return code.equalsIgnoreCase(otro.code) && baseByte == otro.baseByte;
    }
}