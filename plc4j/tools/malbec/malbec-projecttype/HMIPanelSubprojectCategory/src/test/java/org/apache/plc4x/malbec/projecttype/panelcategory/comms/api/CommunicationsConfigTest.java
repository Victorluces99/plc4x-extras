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
package org.apache.plc4x.malbec.projecttype.panelcategory.comms.api;

import java.util.Properties;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * El UUID es la identidad del dispositivo: {@code upsertDevice} matchea por él y
 * sin él no hay forma de saber cuál se está modificando.
 *
 * <p>Estas pruebas fijan ese contrato porque el síntoma del problema es
 * silencioso: con UUID vacío, modificar un dispositivo lo agregaba como uno
 * nuevo y el original quedaba intacto, sin ningún error en pantalla.
 */
class CommunicationsConfigTest {

    private DeviceConfigData device(String uuid, String nombre) {
        Properties p = new Properties();
        p.put("brand", "Siemens");
        p.put("model", "S7-1500");
        p.put("protocol", "S7");
        p.put("deviceName", nombre);
        p.put("deviceKey", "K-" + nombre);
        p.put("description", "");
        p.put("uuid", uuid == null ? "" : uuid);
        p.put("enable", Boolean.TRUE);
        p.put("s88Node", "MD70");
        p.put("s88Uuid", "uuid-md70");
        p.put("specificParameters", "s7:cotp://192.168.0.10:102");
        return new DeviceConfigData(p);
    }

    @Test
    void modificarUnDispositivoLoReemplazaYNoLoDuplica() {
        CommunicationsConfig config = new CommunicationsConfig();
        config.upsertDevice(device("uuid-1", "Compresor 1"));

        config.upsertDevice(device("uuid-1", "Compresor 1 renombrado"));

        assertEquals(1, config.getDevices().size(),
                "modificar no puede agregar un dispositivo nuevo");
        assertEquals("Compresor 1 renombrado", config.getDevices().get(0).getDeviceName());
    }

    @Test
    void unDispositivoSinUuidNoSeModificaSinoQueSeAgrega() {
        CommunicationsConfig config = new CommunicationsConfig();
        config.upsertDevice(device("", "Sin uuid"));

        config.upsertDevice(device("", "Sin uuid editado"));

        assertEquals(2, config.getDevices().size(),
                "sin UUID no hay identidad: el comportamiento es agregar, y por eso avisa");
    }

    @Test
    void removeDeviceSacaElDispositivoYSuComunicacion() {
        CommunicationsConfig config = new CommunicationsConfig();
        config.upsertDevice(device("uuid-1", "Compresor 1"));
        config.upsertComms("uuid-1", null);

        assertTrue(config.removeDevice("uuid-1"));
        assertEquals(0, config.getDevices().size());
    }

    @Test
    void replaceDeviceCambiaElDispositivoDeLaPosicion() {
        CommunicationsConfig config = new CommunicationsConfig();
        config.upsertDevice(device("", "Sin uuid"));

        config.replaceDevice(0, device("uuid-reparado", "Sin uuid"));

        assertEquals(1, config.getDevices().size());
        assertNotNull(config.getDevices().get(0).getUuid());
        assertEquals("uuid-reparado", config.getDevices().get(0).getUuid());
        assertEquals("Sin uuid", config.getDevices().get(0).getDeviceName(),
                "la reparación del UUID no debe perder el resto de los datos");
    }
}