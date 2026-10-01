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
package org.apache.plc4x.malbec.projecttype.panelcategory.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * El formulario arma un {@link DeviceConfigData}, que es inmutable y se construye
 * a partir de un {@link java.util.Properties}. Eso deja el contrato en un único
 * lugar: si se renombra, se agrega o se olvida una clave, el dispositivo se
 * persiste con un campo vacío y no hay error de compilación que lo delate.
 *
 * <p>La segunda mitad de las pruebas cubre el nodo S88. El combo arranca en
 * "-- Seleccione --" y ese texto se estaba escribiendo en el XML como si fuera
 * el nombre de un área real, sin que ninguna validación lo detuviera.
 */
class CreateDevicePanelTest {

    /**
     * Formulario con todos los campos obligatorios resueltos.
     *
     * <p>El nodo S88 se registra a mano porque sin un proyecto real no hay
     * {@code plant-model.xml} que leer, y sin nodo la validación nunca deja
     * llegar al camino que se quiere verificar.
     */
    private CreateDevicePanel panelCompleto() {
        CreateDevicePanel panel = new CreateDevicePanel(null);
        panel.getCbMarca().setSelectedIndex(1);
        panel.getCbModelo().setSelectedIndex(3);
        panel.registrarNodoS88("MD70", "uuid-md70");
        panel.getCbS88Node().setSelectedIndex(1);
        panel.getTxtDeviceName().setText("Compresor 1");
        panel.getTxtDeviceKey().setText("CMP-001");
        panel.getTxtHost().setText("192.168.0.10");
        panel.getTxtPort().setText("102");
        return panel;
    }

    @Test
    void elNodoS88SeleccionadoEsNullConElPlaceholder() {
        assertNull(new CreateDevicePanel(null).nodoS88Seleccionado(),
                "el placeholder del combo no puede pasar por nodo elegido");
    }

    @Test
    void unFormularioVacioPideLosCincoCamposObligatorios() {
        List<String> faltantes = new CreateDevicePanel(null).camposObligatoriosVacios();
        assertEquals(5, faltantes.size(), "faltantes: " + faltantes);
        assertTrue(faltantes.contains("Name"), "faltantes: " + faltantes);
        assertTrue(faltantes.contains("Key"), "faltantes: " + faltantes);
        assertTrue(faltantes.contains("Marca y modelo"), "faltantes: " + faltantes);
        assertTrue(faltantes.contains("Node"), "faltantes: " + faltantes);
    }

    @Test
    void noDejaGuardarSinNodoS88AunqueElRestoEsteCompleto() {
        CreateDevicePanel panel = panelCompleto();
        panel.getCbS88Node().setSelectedIndex(0);
        assertTrue(panel.camposObligatoriosVacios().contains("Node"),
                "un dispositivo sin nodo S88 no debería poder guardarse");
    }

    @Test
    void conTodosLosDatosNoFaltaNinguno() {
        assertTrue(panelCompleto().camposObligatoriosVacios().isEmpty(),
                "el formulario completo no debería pedir nada");
    }

    @Test
    void armaLasDocePropiedadesDelDispositivo() {
        DeviceConfigData data = panelCompleto().getDeviceConfigData();
        assertNotNull(data, "el formulario completo tiene que producir un dispositivo");
        assertEquals("Siemens", data.getBrand());
        assertEquals("S7-1500", data.getModel());
        assertEquals("S7", data.getProtocol());
        assertEquals("Compresor 1", data.getDeviceName());
        assertEquals("CMP-001", data.getDeviceKey());
        assertEquals("MD70", data.getS88Node());
        assertEquals("uuid-md70", data.getS88Uuid());
        assertFalse(data.isEnabled());
        assertFalse(data.getUuid() == null || data.getUuid().isEmpty(), "falta el uuid del dispositivo");
    }

    @Test
    void laUrlSeArmaConHostTransporteYPuerto() {
        DeviceConfigData data = panelCompleto().getDeviceConfigData();
        assertNotNull(data);
        String url = data.getSpecificParameters();
        assertTrue(url.contains("192.168.0.10"), "falta el host en la url: " + url);
        assertTrue(url.contains("102"), "falta el puerto en la url: " + url);
    }
}