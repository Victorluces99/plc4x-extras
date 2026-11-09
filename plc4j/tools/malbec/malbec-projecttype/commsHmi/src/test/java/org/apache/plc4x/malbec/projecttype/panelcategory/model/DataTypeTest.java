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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;

class DataTypeTest {

    @Test
    void primitivesHaveTheWeightTheyShould() {
        assertEquals(1, DataType.BOOLEAN.byteSize());
        assertEquals(1, DataType.BYTE.byteSize());
        assertEquals(1, DataType.UBYTE.byteSize());
        assertEquals(2, DataType.SHORT.byteSize());
        assertEquals(2, DataType.USHORT.byteSize());
        assertEquals(4, DataType.INT.byteSize());
        assertEquals(4, DataType.UINT.byteSize());
        assertEquals(4, DataType.FLOAT.byteSize());
        assertEquals(8, DataType.LONG.byteSize());
        assertEquals(8, DataType.ULONG.byteSize());
        assertEquals(8, DataType.DOUBLE.byteSize());
    }

    @Test
    void longYUlongOcupanOchoBytesYNoCuatro() {
        assertEquals(8, DataType.LONG.byteSize());
        assertEquals(8, DataType.ULONG.byteSize());
    }

    @Test
    void onlyBooleanIsBitAddressed() {
        assertTrue(DataType.BOOLEAN.bitAddressed());
        assertFalse(DataType.BYTE.bitAddressed());
    }

    @Test
    void nameIsStoredLowercaseAndLookupIgnoresCase() {
        assertEquals("boolean", DataType.BOOLEAN.label());
        assertEquals("ubyte", DataType.UBYTE.label());
        assertEquals(DataType.FLOAT, DataType.findIgnoreCase("float"));
        assertEquals(DataType.ULONG, DataType.findIgnoreCase("ULONG"));
    }

    @Test
    void stringNoTienePesoNiSePuedeElegir() {
        assertEquals(0, DataType.STRING.byteSize());
        assertFalse(DataType.STRING.isEnabled());
        assertFalse(DataType.STRING.isByteBlock());
    }

    @Test
    void tiposS5S7TienenPesoYSeDireccionanComoBloqueDeBytes() {
        List<DataType> especiales = List.of(DataType.S5TIME, DataType.S7DATE,
                DataType.S7TIME, DataType.S7TOD, DataType.S7DAT, DataType.S7COUNTER,
                DataType.S7DI, DataType.S7AI, DataType.S7AO,
                DataType.S7VLV, DataType.S7AVLV, DataType.S7MOTOR);
        for (DataType t : especiales) {
            assertTrue(t.byteSize() > 0, t.label() + " debería tener peso");
            assertTrue(t.isEnabled(), t.label() + " se habilita en el catálogo general");
            assertTrue(t.isEnabledFor("s7"), t.label() + " debería poder elegirse para S7");
            assertTrue(t.isByteBlock(), t.label() + " debería ocupar un bloque de bytes");
            assertTrue(t.supportsProtocol("S7"), t.label() + " debería soportar s7");
            assertFalse(t.isEnabledFor("MODBUS"),
                    t.label() + " no debería ofrecerse para otros protocolos");
            assertFalse(t.supportsProtocol("MODBUS"),
                    t.label() + " no debería soportar otros protocolos");
        }
    }

    @Test
    void valuesForProtocolOffersStandardTypesEverywhereAndSiemensTypesOnlyOnS7() {
        List<DataType> s7 = DataType.valuesForProtocol("s7");
        List<DataType> modbus = DataType.valuesForProtocol("MODBUS");

        assertTrue(s7.contains(DataType.S7AI));
        assertFalse(modbus.contains(DataType.S7AI));
        assertTrue(modbus.contains(DataType.BYTE));
        assertFalse(s7.contains(DataType.STRING));
        assertFalse(modbus.contains(DataType.STRING));
        assertEquals(modbus.size() + 12, s7.size());
    }

    @Test
    void primitivesCanBeChosen() {
        for (DataType t : DataType.values()) {
            if (t.byteSize() > 0) {
                assertTrue(t.isEnabled(), t.label() + " debería poder elegirse");
            }
        }
    }

    @Test
    void caseInsensitiveLookupCoversAllTypes() {
        for (DataType t : DataType.values()) {
            assertEquals(t, DataType.findIgnoreCase(t.label()),
                    "el indice debe contener " + t.label());
            assertEquals(t, DataType.findIgnoreCase(t.label().toUpperCase()),
                    "el indice sin mayusculas debe cubrir " + t.label());
        }
        assertNull(DataType.findIgnoreCase(null));
    }

    @Test
    void siemensTypesAreDistinguishedFromStandardOnes() {
        for (DataType t : List.of(DataType.S5TIME, DataType.S7DATE, DataType.S7TIME,
                DataType.S7TOD, DataType.S7DAT, DataType.S7COUNTER, DataType.S7DI,
                DataType.S7AI, DataType.S7AO, DataType.S7VLV,
                DataType.S7AVLV, DataType.S7MOTOR)) {
            assertTrue(t.isProtocolSpecific(),
                    t.label() + " solo existe en Siemens");
        }
    }

    @Test
    void standardTypesAreNotProtocolSpecific() {
        for (DataType t : List.of(DataType.BYTE, DataType.INT, DataType.FLOAT,
                DataType.BOOLEAN, DataType.DOUBLE, DataType.STRING)) {
            assertFalse(t.isProtocolSpecific(),
                    t.label() + " es un tipo estandar, no de Siemens");
        }
    }

    @Test
    void stringNoEsDeProtocoloAunqueTampocoSeOfrezca() {
        assertFalse(DataType.STRING.isProtocolSpecific());
        assertFalse(DataType.STRING.isEnabled());
    }
}
