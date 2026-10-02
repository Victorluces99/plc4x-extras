/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
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

/**
 * Tag de un área de memoria, junto con lo que se ha conseguido interpretar de él.
 *
 * <p>El tag no es una dirección absoluta con una forma fija. En WinCC el campo
 * depende del driver y del PLC: con S7-1200 en direccionamiento absoluto empieza
 * por % ({@code %DB10.DBX0.0}, {@code %MW20}), con S7-300 clásico no lleva %
 * ({@code DB1.DBX0.0}, {@code M0.0}, {@code MW2}) y con tags optimizados o Modbus
 * no hay ni % ni estructura ({@code Control_Panel.Start_Button},
 * {@code 40001}).</p>
 *
 * <p>Por eso aquí <strong>nada es obligatorio y nada se rechaza</strong>. Se
 * aparta el rango, el tipo y el bit si están, y del resto se saca el bloque, el
 * código de área y el número si se reconocen. Lo que no se reconoce se guarda tal
 * cual y no estorba: un tag del que sólo sabemos el rango sirve para calcular
 * capacidad, y uno del que no sabemos nada se acepta y simplemente no se acota.</p>
 *
 * <p>El único caso que se considera inválido es un rango al revés, porque ahí sí
 * hay un error de tecleo evidente.</p>
 *
 * <p>El rango, cuando lo hay, es relativo al byte base. En
 * {@code %DB21.DBB4[0..9]} el byte base es el 4 y los diez bytes del área son del
 * 4 al 13. Por eso los offsets que se guardan en cada variable también son
 * relativos.</p>
 */
public final class MemoryTag {

    private static final Pattern RANGO = Pattern.compile("\\[(\\d+)\\s*\\.\\.\\s*(\\d+)\\]");
    private static final Pattern FAMILIA = Pattern.compile(":(\\w+)\\s*$");
    private static final Pattern BIT = Pattern.compile("\\.(\\d)\\s*$");
    private static final Pattern BLOQUE_Y_CODIGO = Pattern.compile("^(\\w+)\\.([A-Za-z]+)(\\d+)$");
    private static final Pattern CODIGO_Y_NUMERO = Pattern.compile("^([A-Za-z]+)(\\d+)$");
    private static final Pattern SOLO_NUMERO = Pattern.compile("^(\\d+)$");

    private final String raw;
    private final String block;
    private final String code;
    private final int baseByte;
    private final int bit;
    private final int firstByte;
    private final int lastByte;
    private final String family;

    private MemoryTag(String raw, String block, String code, int baseByte, int bit,
            int firstByte, int lastByte, String family) {
        this.raw = raw;
        this.block = block;
        this.code = code;
        this.baseByte = baseByte;
        this.bit = bit;
        this.firstByte = firstByte;
        this.lastByte = lastByte;
        this.family = family;
    }

    /**
     * Interpreta un tag.
     *
     * <p>No falla nunca por formato: si no reconoce una parte, esa parte queda
     * vacía. Sólo devuelve null si el tag viene vacío, porque un área sin tag no
     * tiene sentido y eso sí lo avisa la vista.</p>
     *
     * @return el tag descompuesto, o null si el tag es null o está en blanco
     */
    public static MemoryTag parse(String tag) {
        if (tag == null || tag.isBlank()) {
            return null;
        }
        // Los espacios no significan nada en una dirección y rompen el
        // desglose: en "%DB20. DBB4" el punto queda separado del código, el tag no
        // se reconoce como bloque y se acaba comparando como texto suelto, donde
        // el rango vuelve a importar y %DB20.DBB4[0..16] pasa por ser distinto de
        // %DB20.DBB4[0..10]. Se quitan antes de mirar nada.
        String resto = tag.replaceAll("\\s+", "");
        if (resto.startsWith("%")) {
            resto = resto.substring(1);
        }

        int first = 0;
        int last = -1;
        Matcher rango = RANGO.matcher(resto);
        if (rango.find()) {
            first = Integer.parseInt(rango.group(1));
            last = Integer.parseInt(rango.group(2));
            resto = rango.replaceAll("").trim();
        }

        String family = null;
        Matcher fam = FAMILIA.matcher(resto);
        if (fam.find()) {
            family = fam.group(1);
            resto = resto.substring(0, fam.start()).trim();
        }

        // El bit se aparta antes de buscar bloque y código, porque con el bit detrás
        // M0.0 nunca encajaría con CODIGO_Y_NUMERO. Luego sólo se confirma si lo que
        // queda es una dirección de verdad: en Data_Block_1.Motor1 el 1 final es
        // parte del nombre, no un bit.
        int bit = -1;
        String candidatoBit = resto;
        Matcher b = BIT.matcher(resto);
        if (b.find()) {
            bit = Integer.parseInt(b.group(1));
            candidatoBit = resto.substring(0, b.start()).trim();
        }

        String block = "";
        String code = "";
        int numero = 0;
        Matcher conBloque = BLOQUE_Y_CODIGO.matcher(candidatoBit);
        Matcher conCodigo = CODIGO_Y_NUMERO.matcher(candidatoBit);
        Matcher conNumero = SOLO_NUMERO.matcher(candidatoBit);
        boolean esDireccion = conBloque.matches();
        if (esDireccion) {
            block = conBloque.group(1);
            code = conBloque.group(2);
            numero = Integer.parseInt(conBloque.group(3));
        } else if (conCodigo.matches()) {
            esDireccion = true;
            code = conCodigo.group(1);
            numero = Integer.parseInt(conCodigo.group(2));
        } else if (conNumero.matches()) {
            esDireccion = true;
            numero = Integer.parseInt(conNumero.group(1));
        }
        if (!esDireccion) {
            bit = -1;
        }

        return new MemoryTag(tag.trim(), block, code, numero, bit, first, last, family);
    }

    /** El tag tal como lo escribió el usuario, para los mensajes. */
    public String raw() {
        return raw;
    }

    /** Bloque o memoria del tag, o cadena vacía si no lleva. */
    public String block() {
        return block;
    }

    /** Código de área reconocido: DBX, DBB, DBW, MW, M... o vacío. */
    public String code() {
        return code;
    }

    /** Número del tag, o 0 si no se reconoce la parte que lo acompaña. */
    public int baseByte() {
        return baseByte;
    }

    /** Bit del byte, o -1 si el tag no lo trae. */
    public int bit() {
        return bit;
    }

    /** Primer byte del rango, relativo a {@link #baseByte()}; 0 si no hay rango. */
    public int firstByte() {
        return firstByte;
    }

    /** Último byte del rango, relativo a {@link #baseByte()}; -1 si no hay rango. */
    public int lastByte() {
        return lastByte;
    }

    /** Familia declarada detrás de los dos puntos; null si el tag no la lleva. */
    public String family() {
        return family;
    }

    /** true si el tag trae rango explícito. */
    public boolean hasRange() {
        return lastByte >= 0;
    }

    /** false si el rango va al revés, que es un error de tecleo evidente. */
    public boolean rangoValido() {
        return lastByte < 0 || lastByte >= firstByte;
    }

    /** true si se reconocieron el código de área o el número del tag. */
    public boolean tieneDireccion() {
        return !code.isEmpty() || baseByte > 0;
    }

    /**
     * Ancho en bits que impone el código de área; 0 si el código no se reconoce.
     *
     * <p>Un bit suelto sobre una marca, entrada o salida es un bit, no el byte
     * entero: {@code %M0.0} ocupa un bit mientras que {@code %MW20} ocupa dos
     * bytes.</p>
     */
    public int codeBits() {
        String c = code.toUpperCase();
        if (bit >= 0 && !c.startsWith("DB")) {
            return 1;
        }
        return switch (c) {
            case "X", "DBX" -> 1;
            case "B", "C", "DBB", "MB", "VB", "IB", "QB", "EB" -> 8;
            case "W", "DBW", "MW", "IW", "QW", "EW" -> 16;
            case "D", "DBD", "MD", "ID", "QD", "ED" -> 32;
            case "L", "DBLD" -> 64;
            default -> 0;
        };
    }

    /**
     * Bytes que abarca el área. Con rango, lo que dura el rango; sin rango, lo que
     * ocupa el código de área. Devuelve 0 cuando no hay forma de saberlo, y en ese
     * caso la capacidad no se acota.
     */
    public int byteCapacity() {
        if (!rangoValido()) {
            return 0;
        }
        if (lastByte >= 0) {
            return lastByte - firstByte + 1;
        }
        return codeBits() / 8;
    }

    /** Primer byte del área dentro del bloque, para comparar con otras áreas. */
    public int absoluteFirst() {
        return baseByte + firstByte;
    }

    /** Último byte del área dentro del bloque. */
    public int absoluteLast() {
        return baseByte + Math.max(lastByte, firstByte);
    }

    /**
     * La dirección del tag tal y como se escribe, sin rango ni tipo.
     *
     * <p>Sirve para explicarle al usuario por qué dos áreas se pisan, no para
     * decidirlo. Dos áreas con la misma dirección y rangos distintos conviven sin
     * problema: {@code %DB22.DBB4[10..16]} y {@code %DB22.DBB4[17..22]} arrancan en
     * el mismo byte y no se tocan. Lo único que marca un error es el solapamiento,
     * que ve {@link #solapaCon}.</p>
     *
     * @return la dirección, o el tag tal cual si no se reconoce ninguna
     */
    public String direccion() {
        if (!tieneDireccion()) {
            return raw;
        }
        // El bloque sólo se pone si lo hay: %MW66 es la marca de bits, no un DB.
        String d = block.isEmpty()
                ? code.toUpperCase() + baseByte
                : block.toUpperCase() + "." + code.toUpperCase() + baseByte;
        return bit >= 0 ? d + "." + bit : d;
    }

    /**
     * true si las dos áreas tocan los mismos bytes del mismo bloque.
     *
     * <p>Sólo se comparan áreas del mismo bloque y del mismo código de área, y sólo
     * si las dos tienen dirección reconocible. Dos áreas con códigos distintos
     * pueden pisarse en el PLC, pero distinguirlas exigiría conocer la disposición
     * del bloque y aquí no se supone ninguna: es preferible dejar pasar una
     * coincidencia que bloquear un área legítima.</p>
     */
    public boolean solapaCon(MemoryTag otro) {
        if (otro == null || !tieneDireccion() || !otro.tieneDireccion()) {
            return false;
        }
        if (!block.equalsIgnoreCase(otro.block) || !code.equalsIgnoreCase(otro.code)) {
            return false;
        }
        return absoluteFirst() <= otro.absoluteLast()
                && otro.absoluteFirst() <= absoluteLast();
    }
}