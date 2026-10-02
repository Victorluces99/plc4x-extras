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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * El peso de cada tipo es lo que decide dónde cae la siguiente variable, así que
 * estos tests son la red de seguridad del cálculo de offsets.
 *
 * <p>El caso que se guarda aparte es {@code long} y {@code ulong}: valían 4 bytes
 * en el código anterior y la mitad de lo que ocupan. Un error ahí no se ve
 * mirando la interfaz, se ve meses después cuando el PLC lee por donde no es.</p>
 */
class DataTypeTest {

    @Test
    void losPrimitivosTienenElPesoQueLesCorresponde() {
        assertEquals(1, DataType.BOOLEAN.byteSize());
        assertEquals(1, DataType.BYTE.byteSize());
        assertEquals(1, DataType.UBYTE.byteSize());
        assertEquals(2, DataType.WORD.byteSize());
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
        assertEquals(8, DataType.find("long").byteSize());
        assertEquals(8, DataType.find("ulong").byteSize());
    }

    @Test
    void soloElBooleanoSeDireccionaPorBit() {
        assertEquals(1, DataType.BOOLEAN.slotBits());
        assertTrue(DataType.BOOLEAN.bitAddressed());
        assertEquals(8, DataType.BYTE.slotBits());
        assertEquals(64, DataType.LONG.slotBits());
        assertFalse(DataType.BYTE.bitAddressed());
    }

    @Test
    void elNombreSePersisteEnMinusculasYSeBuscaExacto() {
        assertEquals("boolean", DataType.BOOLEAN.label());
        assertEquals("ubyte", DataType.UBYTE.label());
        assertEquals(DataType.FLOAT, DataType.find("float"));
        assertEquals(DataType.ULONG, DataType.find("ulong"));
    }

    @Test
    void unNombreDesconocidoNoAdivinaUnTamanoPorDefecto() {
        assertNull(DataType.find("inter"));
        assertNull(DataType.find("INTER"));
        assertNull(DataType.find(""));
        assertNull(DataType.find(null));
        assertNull(DataType.find("Long"), "el nombre es en minúsculas");
    }

    @Test
    void laFamiliaByteAdmiteByteYUbyteYSoloEnUnByte() {
        assertEquals(List.of(DataType.BYTE, DataType.UBYTE),
                DataType.candidatos("BYTE", 8));
        assertTrue(DataType.candidatos("BYTE", 16).isEmpty(),
                "un byte no cabe en un DBW sin cambiar el código de área");
    }

    @Test
    void integerSeResuelveConElCodigoDeArea() {
        assertEquals(List.of(DataType.SHORT, DataType.USHORT),
                DataType.candidatos("INTEGER", 16));
        assertEquals(List.of(DataType.INT, DataType.UINT),
                DataType.candidatos("INTEGER", 32));
        assertEquals(List.of(DataType.LONG, DataType.ULONG),
                DataType.candidatos("INTEGER", 64));
        assertTrue(DataType.candidatos("INTEGER", 8).isEmpty());
    }

    @Test
    void realOfreceFloatYDoubleSiElAreaEsAncha() {
        // Los dos son números reales, así que van los dos en el desplegable. Que el
        // double no quepa en un área de cuatro bytes lo dice la capacidad, no la
        // lista de tipos: si no, un área de ocho bytes escondería el float y una de
        // cuatro no ofrecería ningún real.
        assertEquals(List.of(DataType.FLOAT, DataType.DOUBLE),
                DataType.candidatos("REAL", 32));
        assertEquals(List.of(DataType.FLOAT, DataType.DOUBLE),
                DataType.candidatos("REAL", 64));
        assertEquals(List.of(DataType.FLOAT, DataType.DOUBLE),
                DataType.candidatos("REAL", 128));
        assertTrue(DataType.candidatos("REAL", 16).isEmpty(),
                "ni float ni double caben en dos bytes");
        assertTrue(DataType.candidatos("REAL", 8).isEmpty());
        assertEquals(List.of(DataType.FLOAT, DataType.DOUBLE),
                DataType.candidatos("REAL", 0),
                "sin código de área no se sabe el ancho, así que se ofrecen ambos");
    }

    @Test
    void boolAdmiteUnBitEnDbxYOchoEnDbb() {
        assertEquals(List.of(DataType.BOOLEAN), DataType.candidatos("BOOL", 1));
        assertEquals(List.of(DataType.BOOLEAN), DataType.candidatos("BOOL", 8));
        assertTrue(DataType.candidatos("BOOL", 32).isEmpty());
    }

    @Test
    void wordSoloCabeEnDosBytes() {
        assertEquals(List.of(DataType.WORD), DataType.candidatos("WORD", 16));
        assertTrue(DataType.candidatos("WORD", 32).isEmpty());
    }

    @Test
    void unaFamiliaDesconocidaNoDevuelveNada() {
        assertTrue(DataType.candidatos("STRING", 8).isEmpty());
        assertTrue(DataType.candidatos(null, 8).isEmpty());
    }

    @Test
    void stringYLosTiposS5S7NoTienenPesoNiSePuedenElegir() {
        List<DataType> sinPeso = List.of(DataType.STRING, DataType.S5TIME, DataType.S7DATE,
                DataType.S7TIME, DataType.S7TOD, DataType.S7DAT, DataType.S7COUNTER,
                DataType.S7DI, DataType.S7AI, DataType.S7AO, DataType.S7VALVE,
                DataType.S7VLV, DataType.S7AVLV, DataType.S7MOTOR);
        for (DataType t : sinPeso) {
            assertEquals(0, t.byteSize(), t.label() + " no debería tener peso todavía");
            assertFalse(t.habilitado(), t.label() + " no debería poder elegirse");
        }
    }

    @Test
    void losPrimitivosSiSePuedenElegir() {
        for (DataType t : DataType.values()) {
            if (t.byteSize() > 0) {
                assertTrue(t.habilitado(), t.label() + " debería poder elegirse");
            }
        }
    }
}