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
 * El peso importa porque determina el salto de una variable a la siguiente. 
 * Los tipos sin soporte quedan en la lista pero no se pueden elegir: se
 * desconoce su tamaño en bytes, y sin tamaño no hay forma de calcular el offset.
 * Aparecen en gris en el desplegable para dejar constancia de que se han
 * contemplado.
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
     */
    public static List<DataType> candidatos(String familia, int bitsArea) {
        if (familia == null) {
            return List.of();
        }
        return switch (familia.toUpperCase()) {
            case "BYTE" -> bitsArea == 8 ? List.of(BYTE, UBYTE) : List.of();
            case "UBYTE" -> List.of(BYTE, UBYTE);
            case "WORD" -> bitsArea == 16 ? List.of(WORD) : List.of();
            case "INTEGER" -> switch (bitsArea) {
                case 16 -> List.of(SHORT, USHORT);
                case 32 -> List.of(INT, UINT);
                case 64 -> List.of(LONG, ULONG);
                default -> List.of();
            };
            case "REAL" -> esReal(bitsArea) ? List.of(FLOAT, DOUBLE) : List.of();
            case "BOOL" -> bitsArea == 1 || bitsArea == 8 ? List.of(BOOLEAN) : List.of();

            // Nombres de tipo de PLC4X, que es como llega el tipo en un tag
            // Siemens. Cada uno dice por sí mismo su tamaño, así que no dependen del
            // código de área: INT son 16 bits, DINT 32 y LINT 64, igual que en el
            // driver. Sin estas ramas un tag como %DB1.DBD0:DINT se quedaba sin
            // ningún tipo que ofrecer, porque DINT no era ninguna familia conocida.
            //
            // La tabla del driver es la referencia. En ella los enteros de ocho bits
            // son SINT y USINT, y BYTE es un array de ocho booleanos; aquí los tres
            // casos ofrecen los mismos tipos de un byte porque el espacio que
            // reservan es idéntico y lo único que cambia es cómo se interpreta el
            // contenido. RAW_BYTE_ARRAY es un alias de BYTE, según el driver.
            case "INT", "UINT" -> List.of(SHORT, USHORT);
            case "DINT", "UDINT", "DWORD" -> List.of(INT, UINT);
            case "LINT", "ULINT", "LWORD" -> List.of(LONG, ULONG);
            case "SINT", "USINT" -> List.of(BYTE, UBYTE);
            case "CHAR" -> List.of(BYTE, UBYTE);
            case "WCHAR" -> bitsArea == 16 ? List.of(WORD) : List.of(BYTE, UBYTE);
            case "RAW_BYTE_ARRAY" -> List.of(BYTE, UBYTE);
            case "USHORT" -> List.of(SHORT, USHORT);
            case "ULONG" -> List.of(LONG, ULONG);
            case "LREAL" -> List.of(DOUBLE);

            // STRING, WSTRING y los temporales no llegan aquí: su tamaño depende de
            // los caracteres o de la plataforma, así que no hay forma de calcular un
            // offset con ellos. Los deja el catálogo como tipos sin peso.
            default -> List.of();
        };
    }
    
    private static boolean esReal(int bitsArea) {
        return bitsArea == 0 || bitsArea >= 32;
    }
}