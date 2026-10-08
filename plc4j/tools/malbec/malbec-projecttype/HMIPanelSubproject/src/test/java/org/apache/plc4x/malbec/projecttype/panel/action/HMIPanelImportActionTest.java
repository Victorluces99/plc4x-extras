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
package org.apache.plc4x.malbec.projecttype.panel.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.plc4x.malbec.s88.api.S88Element;
import org.apache.plc4x.malbec.s88.api.S88Level;
import org.apache.plc4x.malbec.s88.api.S88Repository;
import org.apache.plc4x.malbec.s88.api.S88RepositoryProvider;
import org.apache.plc4x.malbec.s88.api.S88Storage;
import org.junit.jupiter.api.Test;

public class HMIPanelImportActionTest {

    @Test
    void extensionValida() {
        assertTrue(HMIPanelImportAction.extensionAdmitida(new File("planta.xml")));
        assertTrue(HMIPanelImportAction.extensionAdmitida(new File("PLANTA.XML")),
                "la extension no deberia distinguir mayusculas");
    }

    @Test
    void extensionInvalida() {
        assertFalse(HMIPanelImportAction.extensionAdmitida(new File("planta.txt")));
        assertFalse(HMIPanelImportAction.extensionAdmitida(new File("planta.csv")));
        assertFalse(HMIPanelImportAction.extensionAdmitida(new File("planta")),
                "un archivo sin extension no es una planta");
        assertFalse(HMIPanelImportAction.extensionAdmitida(new File("planta.")),
                "un punto final no cuenta como extension");
    }

    @Test
    void soloXmlEstaDeclarado() {
        assertEquals(List.of("xml"), List.copyOf(HMIPanelImportAction.FORMATOS.keySet()));
        assertEquals("Modelo de planta (*.xml)", HMIPanelImportAction.FORMATOS.get("xml"));
    }

    @Test
    void elFiltroYLaValidacionCoinciden() {
        for (String extension : HMIPanelImportAction.FORMATOS.keySet()) {
            File archivo = new File("planta." + extension);
            assertTrue(HMIPanelImportAction.extensionAdmitida(archivo),
                    "el filtro deja ver ." + extension + " pero la validacion lo rechaza");
        }
    }

    @Test
    void elFiltroDelChooserAdmiteSoloXml() {
        FilenameFilter filtro = HMIPanelImportAction.filtroDeArchivo();
        File directorio = new File(".");

        assertTrue(filtro.accept(directorio, "planta.xml"));
        assertTrue(filtro.accept(directorio, "PLANTA.XML"));
        assertFalse(filtro.accept(directorio, "planta.txt"));
        assertFalse(filtro.accept(directorio, "planta"));
    }

    @Test
    void elFiltroDelChooserDejaVerLasCarpetas() {
        FilenameFilter filtro = HMIPanelImportAction.filtroDeArchivo();

        assertTrue(filtro.accept(new File("."), "."),
                "sin esto no se podria navegar a una subcarpeta");
    }

    @Test
    void formatosAdmitidosSeMuestranParaElUsuario() {
        assertTrue(HMIPanelImportAction.formatosAdmitidos().contains("(*.xml)"));
    }

    @Test
    void cadaFormatoDeclaradoLoAceptaAlgunProvider() {
        assertTrue(HMIPanelImportAction.formatosSinProvider(
                        List.of(new ProviderQueSoloAcepta("xml")),
                        HMIPanelImportAction.FORMATOS.keySet()).isEmpty(),
                "xml esta declarado y hay un provider que lo acepta");
    }

    @Test
    void unFormatoSinProviderSeDetecta() {
        assertEquals(Set.of("xml"), HMIPanelImportAction.formatosSinProvider(
                List.of(new ProviderQueSoloAcepta("otro")),
                HMIPanelImportAction.FORMATOS.keySet()),
                "debe avisar si el provider no acepta el formato declarado");
    }

    @Test
    void unProviderQueAceptaVariosFormatosCubreTodos() {
        assertTrue(HMIPanelImportAction.formatosSinProvider(
                List.of(new ProviderQueSoloAcepta("xml"), new ProviderQueSoloAcepta("axml")),
                List.of("xml", "axml")).isEmpty());
    }

    @Test
    void sinProvidersTodoDeclaradoQuedaSinProvider() {
        assertEquals(Set.of("xml"), HMIPanelImportAction.formatosSinProvider(
                List.of(), HMIPanelImportAction.FORMATOS.keySet()));
    }

    private record ProviderQueSoloAcepta(String formato) implements S88RepositoryProvider {

        @Override
        public boolean accepts(String format) {
            return formato.equalsIgnoreCase(format);
        }

        @Override
        public S88Repository createRepository(S88Storage storage) {
            return null;
        }
    }

    @Test
    void collectElementMapeaAreaProcessCellYEquipo() {
        S88Element area = new S88Element().setId("CELDA1").setLevel(S88Level.AREA);
        S88Element cell = new S88Element().setId("CELDA1_P1").setLevel(S88Level.PROCESSCELL);
        S88Element equipo = new S88Element().setId("TANQUE_1").setLevel(S88Level.UNIT);
        cell.addChild(equipo);
        area.addChild(cell);

        LinkedHashMap<String, Object> snapshot =
                HMIPanelImportAction.collectElement(area);

        assertEquals("CELDA1", snapshot.get("id"));
        assertEquals("area", snapshot.get("tag"));

        List<?> hijos = (List<?>) snapshot.get("children");
        assertEquals(1, hijos.size());

        Map<?, ?> celda = (Map<?, ?>) hijos.get(0);
        assertEquals("CELDA1_P1", celda.get("id"));
        assertEquals("processcell", celda.get("tag"));

        Map<?, ?> tanque = (Map<?, ?>) ((List<?>) celda.get("children")).get(0);
        assertEquals("TANQUE_1", tanque.get("id"));
        assertEquals("element", tanque.get("tag"),
                "UNIT no tiene tag propio y cae en element");
    }

    @Test
    void tagForCaeEnElementSinNivelConocido() {
        assertEquals("element", HMIPanelImportAction.tagFor(new S88Element()));
        assertEquals("element",
                HMIPanelImportAction.tagFor(new S88Element().setLevel(S88Level.EQUIPMENTMODULE)));
        assertEquals("element",
                HMIPanelImportAction.tagFor(new S88Element().setLevel(S88Level.NULL)));
        assertEquals("area",
                HMIPanelImportAction.tagFor(new S88Element().setLevel(S88Level.AREA)));
        assertEquals("processcell",
                HMIPanelImportAction.tagFor(new S88Element().setLevel(S88Level.PROCESSCELL)));
    }

    @Test
    void collectElementSeparaVariablesParametrosYReport() {
        S88Element area = new S88Element().setId("CELDA1").setLevel(S88Level.AREA);

        Map<String, Object> directa = new LinkedHashMap<>();
        directa.put("Type", "REAL");
        area.setProperty("Temperatura", directa);

        Map<String, Object> enParametros = new LinkedHashMap<>();
        Map<String, Object> tipo = new LinkedHashMap<>();
        tipo.put("Type", "INT");
        enParametros.put("Presion", tipo);
        area.setProperty("Parameters", new LinkedHashMap<>(Map.of("Presion", enParametros)));

        Map<String, Object> enReports = new LinkedHashMap<>();
        Map<String, Object> tipoReporte = new LinkedHashMap<>();
        tipoReporte.put("Type", "STRING");
        enReports.put("Historico", tipoReporte);
        area.setProperty("Reports", new LinkedHashMap<>(Map.of("Historico", enReports)));

        LinkedHashMap<String, Object> snapshot = HMIPanelImportAction.collectElement(area);

        assertEquals(1, ((List<?>) snapshot.get("variables")).size());
        assertEquals(1, ((List<?>) snapshot.get("parameters")).size());
        assertEquals(1, ((List<?>) snapshot.get("report")).size());

        Map<?, ?> variable = (Map<?, ?>) ((List<?>) snapshot.get("variables")).get(0);
        assertEquals("Temperatura", variable.get("name"));
        assertEquals("REAL", variable.get("type"));
    }

    @Test
    void collectVariablesIgnoraNodosSinType() {
        Map<String, Object> nodo = new LinkedHashMap<>();
        Map<String, Object> sinTipo = new LinkedHashMap<>();
        sinTipo.put("Otro", "valor");
        nodo.put("VariableSinTipo", sinTipo);

        List<Object> collected = HMIPanelImportAction.collectVariables(nodo);

        assertTrue(collected.isEmpty(), "una variable sin Type no debe entrar en el snapshot");
    }

    @Test
    void variableNormalizaElTipoAusente() {
        assertEquals("", HMIPanelImportAction.variable("Nombre", null).get("type"));
    }

    @Test
    void unAreaSinIdNoRompeElSnapshot() {
        LinkedHashMap<String, Object> snapshot =
                HMIPanelImportAction.collectElement(new S88Element().setLevel(S88Level.AREA));
        assertEquals("", snapshot.get("id"));
        assertTrue(((List<?>) snapshot.get("variables")).isEmpty(),
                "sin propiedades la lista queda vacia, no nula");
        assertTrue(((List<?>) snapshot.get("children")).isEmpty());
    }
}
