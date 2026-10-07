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
package org.apache.plc4x.malbec.projecttype.panelcategory.model;

import java.util.List;

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

    public String label() {
        return name;
    }

    public int byteSize() {
        return byteSize;
    }

    public boolean bitAddressed() {
        return bitAddressed;
    }

    public boolean habilitado() {
        return habilitado;
    }

    public int slotBits() {
        return bitAddressed ? 1 : byteSize * 8;
    }

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
            default -> List.of();
        };
    }
    
    private static boolean esReal(int bitsArea) {
        return bitsArea == 0 || bitsArea >= 32;
    }

    public static int bitsDeTipo(String nombre) {
        if (nombre == null) {
            return 0;
        }
        return switch (nombre.toUpperCase()) {
            case "BOOL" -> 1;
            case "BYTE", "SINT", "USINT", "CHAR" -> 8;
            case "WORD", "INT", "UINT", "WCHAR" -> 16;
            case "DWORD", "DINT", "UDINT", "REAL" -> 32;
            case "LWORD", "LINT", "ULINT", "LREAL" -> 64;
            default -> 0;
        };
    }
}