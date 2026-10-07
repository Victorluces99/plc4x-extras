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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;

class UrlDisassemblerTest {

    @Test
    void separaProtocoloYTransporte() {
        UrlDisassembler.Partes partes = UrlDisassembler.disassemble("s7:cotp://192.168.0.10:102");
        assertEquals("s7", partes.protocol());
        assertEquals("cotp", partes.transport());
        assertEquals("192.168.0.10", partes.host());
        assertEquals("102", partes.port());
        assertTrue(partes.params().isEmpty());
    }

    @Test
    void sinTransporteNiPuerto() {
        UrlDisassembler.Partes partes = UrlDisassembler.disassemble("s7://192.168.0.10");
        assertEquals("s7", partes.protocol());
        assertEquals("", partes.transport());
        assertEquals("192.168.0.10", partes.host());
        assertEquals("", partes.port());
    }

    @Test
    void conservaElOrdenDeLosParametros() {
        UrlDisassembler.Partes partes =
                UrlDisassembler.disassemble("eip://10.0.0.5:44818?path=1&slot=0");
        assertEquals("10.0.0.5", partes.host());
        assertEquals("44818", partes.port());
        assertEquals("[path, slot]", partes.params().keySet().toString(),
                "el orden importa porque es el que assembleUrl escribe");
        assertEquals("1", partes.params().get("path"));
        assertEquals("0", partes.params().get("slot"));
    }

    @Test
    void leeUnPuertoSerieComoHost() {
        UrlDisassembler.Partes partes = UrlDisassembler.disassemble("modbus-rtu://COM3");
        assertEquals("modbus-rtu", partes.protocol());
        assertEquals("COM3", partes.host());
        assertEquals("", partes.port());
    }

    @Test
    void devuelveNullSiNoTieneElSeparadorDeProtocolo() {
        assertNull(UrlDisassembler.disassemble("192.168.0.10:102"));
        assertNull(UrlDisassembler.disassemble(""));
        assertNull(UrlDisassembler.disassemble(null));
    }

    @Test
    void elViajeDeIdaYVueltaConservaLaUrl() {
        assertViajeIdaYVuelta("s7:cotp://192.168.0.10:102?controller-type=S7_1500&pdu-size=512");
        assertViajeIdaYVuelta("s7://192.168.0.10");
        assertViajeIdaYVuelta("eip://10.0.0.5:44818?path=1&slot=0");
        assertViajeIdaYVuelta("modbus-tcp://10.0.0.9:502?unit-identifier=3");
        assertViajeIdaYVuelta("modbus-rtu://COM3");
        assertViajeIdaYVuelta("modbus-rtu:///dev/ttyUSB0");
    }

    private void assertViajeIdaYVuelta(String original) {
        UrlDisassembler.Partes partes = UrlDisassembler.disassemble(original);
        assertTrue(partes != null, "no se pudo desensamblar: " + original);
        String reconstruida = DeviceDynamicPanelBuilder.assembleUrl(
                partes.protocol(), partes.transport(), partes.host(),
                partes.port(), new LinkedHashMap<>(partes.params()));
        assertEquals(original, reconstruida,
                "desensamblar y volver a armar tiene que devolver la misma URL");
    }
}