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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public enum DataType {

    BOOLEAN("boolean", 1, true, true),
    BYTE("byte", 1, false, true),
    SINT("sint", 1, false, true),
    USINT("usint", 1, false, true),
    INT("int", 4, false, true),
    UINT("uint", 4, false, true),
    DINT("dint", 4, false, true),
    UDINT("udint", 4, false, true),

    UBYTE("ubyte", 1, false, true),
    SHORT("short", 2, false, true),
    USHORT("ushort", 2, false, true),
    FLOAT("float", 4, false, true),
    LONG("long", 8, false, true),
    ULONG("ulong", 8, false, true),
    DOUBLE("double", 8, false, true),

    /** Tamaño variable de bytes (String por definir) */
    STRING("string", 0, false, false),

    S5TIME("s5time", 2, false, true, "s7"),
    S7DATE("s7date", 4, false, true, "s7"),
    S7TIME("s7time", 4, false, true, "s7"),
    S7TOD("s7tod", 2, false, true, "s7"),
    S7DAT("s7dat", 8, false, true, "s7"),
    S7COUNTER("s7counter", 2, false, true, "s7"),
    S7DI("s7di", 3, false, true, "s7"),
    S7AI("s7ai", 64, false, true, "s7"),
    S7AO("s7ao", 36, false, true, "s7"),
    S7VLV("s7vlv", 14, false, true, "s7"),
    S7AVLV("s7avlv", 36, false, true, "s7"),
    S7MOTOR("s7motor", 14, false, true, "s7");

    private final String name;
    private final int byteSize;
    private final boolean bitAddressed;
    private final boolean enabled;
    private final String protocol;

    private static final Map<String, DataType> BY_NAME_IN_LOWCASE = new HashMap<>();

    static {
        for (DataType t : values()) {
            BY_NAME_IN_LOWCASE.put(t.name.toUpperCase(Locale.ROOT), t);
        }
    }

    DataType(String name, int byteSize, boolean bitAddressed, boolean enabled) {
        this(name, byteSize, bitAddressed, enabled, null);
    }

    DataType(String name, int byteSize, boolean bitAddressed, boolean enabled,
            String protocol) {
        this.name = name;
        this.byteSize = byteSize;
        this.bitAddressed = bitAddressed;
        this.enabled = enabled;
        this.protocol = protocol;
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

    public boolean isEnabled() {
        return enabled;
    }

    public String protocol() {
        return protocol;
    }

    public boolean isProtocolSpecific() {
        return protocol != null;
    }

    public boolean isByteBlock() {
        return protocol != null && !bitAddressed && byteSize > 0;
    }

    public boolean supportsProtocol(String protocol) {
        if (this.protocol == null) {
            return true;
        }
        return protocol != null && this.protocol.equalsIgnoreCase(protocol.trim());
    }

    public boolean isEnabledFor(String protocol) {
        if (!supportsProtocol(protocol)) {
            return false;
        }
        return enabled || (isProtocolSpecific() && byteSize > 0);
    }

    public static List<DataType> valuesForProtocol(String protocol) {
        List<DataType> result = new ArrayList<>();
        for (DataType dataType : values()) {
            if (dataType.isEnabledFor(protocol)) {
                result.add(dataType);
            }
        }
        return result;
    }

    public static DataType findIgnoreCase(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return BY_NAME_IN_LOWCASE.get(name.toUpperCase(Locale.ROOT));
    }
}
