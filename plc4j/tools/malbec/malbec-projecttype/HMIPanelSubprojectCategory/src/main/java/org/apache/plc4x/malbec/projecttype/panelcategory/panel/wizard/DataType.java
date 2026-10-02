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

import java.util.List;

/**
 * Tipos de dato que admite el programa, con el tamaño que ocupa cada uno.
 *
 * <p>El nombre es exactamente el que se guarda en {@code PvConfig.type}, así que
 * no se renombran: son el contrato con el XML ya escrito.</p>
 *
 * <p>El peso importa porque determina el salto de una variable a la siguiente. Por
 * eso {@code long} y {@code ulong} valen 8 bytes y no 4.</p>
 *
 * <p>Los tipos sin soporte quedan en la lista pero no se pueden elegir: se
 * desconoce su tamaño en bytes, y sin tamaño no hay forma de calcular el offset.
 * Aparecen en gris en el desplegable para dejar constancia de que se han
 * contemplado.</p>
 */
public enum DataType {

    BOOLEAN("boolean", 1, true, true),
    BYTE("byte", 1, false, true),
    UBYTE("ubyte", 1, false, true),
    WORD("word", 2, false, true),
    SHORT("short", 2, false, true),
    USHORT("ushort", 2, false, true),
    INT("int", 4, false, true),
    UINT("uint", 4, false, true),
    FLOAT("float", 4, false, true),
    LONG("long", 8, false, true),
    ULONG("ulong", 8, false, true),
    DOUBLE("double", 8, false, true),

    /** Tamaño variable: no admite cálculo de offset. */
    STRING("string", 0, false, false),

    S5TIME("s5time", 0, false, false),
    S7DATE("s7date", 0, false, false),
    S7TIME("s7time", 0, false, false),
    S7TOD("s7tod", 0, false, false),
    S7DAT("s7dat", 0, false, false),
    S7COUNTER("s7counter", 0, false, false),
    S7DI("s7di", 0, false, false),
    S7AI("s7ai", 0, false, false),
    S7AO("s7ao", 0, false, false),
    S7VALVE("s7valve", 0, false, false),
    S7VLV("s7vlv", 0, false, false),
    S7AVLV("s7avlv", 0, false, false),
    S7MOTOR("s7motor", 0, false, false);

    private final String name;
    private final int byteSize;
    private final boolean bitAddressed;
    private final boolean habilitado;

    DataType(String name, int byteSize, boolean bitAddressed, boolean habilitado) {
        this.name = name;
        this.byteSize = byteSize;
        this.bitAddressed = bitAddressed;
        this.habilitado = habilitado;
    }

    /**
     * Nombre con el que se persiste el tipo.
     *
     * <p>No se llama {@code name()} porque ese método de {@link Enum} es final y
     * devolvería el nombre de la constante, que es mayúsculas y no el valor que se
     * guarda en el XML.</p>
     */
    public String label() {
        return name;
    }

    /** Tamaño en bytes; 0 cuando todavía no está definido. */
    public int byteSize() {
        return byteSize;
    }

    /** true si se direcciona bit a bit y su offset se escribe {@code byte.bit}. */
    public boolean bitAddressed() {
        return bitAddressed;
    }

    /** false si el tamaño es variable o aún no se conoce. */
    public boolean habilitado() {
        return habilitado;
    }

    /** Tamaño en bits que ocupa una variable de este tipo. */
    public int slotBits() {
        return bitAddressed ? 1 : byteSize * 8;
    }

    /** Busca por el nombre exacto del tipo, sin adivinar ni normalizar. */
    public static DataType find(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (DataType t : values()) {
            if (t.name.equals(name)) {
                return t;
            }
        }
        return null;
    }

    /** Busca ignorando mayúsculas y minúsculas, para los tipos que llegan del XML. */
    public static DataType findIgnoreCase(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (DataType t : values()) {
            if (t.name.equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    /**
     * Todos los nombres de tipo en una línea, para los mensajes de ayuda del
     * formulario: el usuario necesita ver la lista en el aviso, no buscarla en el
     * desplegable.
     */
    public static String nombres() {
        StringBuilder texto = new StringBuilder();
        for (DataType t : values()) {
            if (texto.length() > 0) {
                texto.append(", ");
            }
            texto.append(t.name);
        }
        return texto.toString();
    }

    /**
     * Tipos que caben en la familia que declara el tag, al tamaño que impone su
     * código de área.
     *
     * <p>Las familias INTEGER y REAL abarcan tres y dos tamaños respectivamente,
     * así que sin el código de área no hay forma de saber cuál es: por eso hace
     * falta el segundo argumento.</p>
     *
     * <p>BOOL admite 1 bit (DBX) y 8 bits (DBB), porque un byte entero empaqueta
     * ocho booleanos.</p>
     *
     * <p>REAL devuelve los dos tamaños, float y double, siempre que el área tenga
     * sitio para al menos un float. Que los dos entren no significa que quepan:
     * un double necesita ocho bytes y en un área de cuatro no va. La diferencia
     * con INTEGER es que allí los tamaños son de la misma familia aritmética y el
     * código de área dice cuál toca, mientras que float y double son ambos números
     * reales y el usuario decide cuál quiere. Si se escondiera el que no cabe,
     * un área de ocho bytes vería las dos opciones y una de cuatro ninguna, y en
     * ambos casos es el mismo sitio: no cabe un double en cuatro bytes.</p>
     *
     * @return los tipos posibles, o lista vacía si la combinación no encaja
     */
    public static List<DataType> candidatos(String familia, int bitsArea) {
        if (familia == null) {
            return List.of();
        }
        return switch (familia.toUpperCase()) {
            case "BYTE" -> bitsArea == 8 ? List.of(BYTE, UBYTE) : List.of();
            case "WORD" -> bitsArea == 16 ? List.of(WORD) : List.of();
            case "INTEGER" -> switch (bitsArea) {
                case 16 -> List.of(SHORT, USHORT);
                case 32 -> List.of(INT, UINT);
                case 64 -> List.of(LONG, ULONG);
                default -> List.of();
            };
            case "REAL" -> esReal(bitsArea) ? List.of(FLOAT, DOUBLE) : List.of();
            case "BOOL" -> bitsArea == 1 || bitsArea == 8 ? List.of(BOOLEAN) : List.of();
            default -> List.of();
        };
    }

    /**
     * true si el área es lo bastante ancha para guardar un número real.
     *
     * <p>El mínimo es el float, cuatro bytes. Un double son ocho, así que en un
     * área de cuatro bytes no entra: se ofrece igualmente y es la comprobación de
     * capacidad la que dice que no cabe, en lugar de esconderlo del desplegable.
     * Ocultarlo daría la impresión de que el área no admite reales, que no es
     * cierto.</p>
     */
    private static boolean esReal(int bitsArea) {
        return bitsArea == 0 || bitsArea >= 32;
    }
}