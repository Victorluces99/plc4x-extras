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
package org.apache.plc4x.malbec.projecttype.panelcategory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class HMICategoryFileRulesTest {

    private static final Map<HMICategoryDefinition, String[]> AT_THE_FACTORY = new LinkedHashMap<>();

    static {
        AT_THE_FACTORY.put(HMICategoryDefinition.RECIPE,
                new String[]{"Recipe", "rcp", "template.rcp"});
        AT_THE_FACTORY.put(HMICategoryDefinition.SCRIPTS,
                new String[]{"Scripts", "scp", "template.scp"});
        AT_THE_FACTORY.put(HMICategoryDefinition.RECORD,
                new String[]{"Record", "record", "template.record"});
        AT_THE_FACTORY.put(HMICategoryDefinition.REPORT,
                new String[]{"Report", "rpt", "template.rpt"});
        AT_THE_FACTORY.put(HMICategoryDefinition.TEXT_GRAPHIC,
                new String[]{"Text and List Charts", "tlc", "template.tlc"});
        AT_THE_FACTORY.put(HMICategoryDefinition.NOTICE_MANAGEMENT,
                new String[]{"Warning Management", "wmg", "template.wmg"});
        AT_THE_FACTORY.put(HMICategoryDefinition.ADMIN_USER,
                new String[]{"Runtime User Management", "rtum", "template.rtum"});
        AT_THE_FACTORY.put(HMICategoryDefinition.CONFIG_PANEL,
                new String[]{"Operator Panel Config", "oppc", "template.oppc"});
    }

    @Test
    void eachCategoryKeepsFolderExtensionAndHiddenFile() {
        for (Map.Entry<HMICategoryDefinition, String[]> entrada : AT_THE_FACTORY.entrySet()) {
            HMICategoryDefinition definition = entrada.getKey();
            String[] esperado = entrada.getValue();
            assertEquals(esperado[0], definition.getDisplayName(),
                    "la carpeta de " + definition.name() + " no puede cambiar: el NodeFactory "
                            + "la busca por este nombre");
            assertEquals(esperado[1], definition.getFileExtension(),
                    "la extension de " + definition.name() + " no puede cambiar");
            assertEquals(esperado[2], definition.getHiddenFile(),
                    "el fichero oculto de " + definition.name() + " no puede cambiar");
        }
    }

    @Test
    void categoriesWithoutOwnFilesDeclareNoExtension() {
        assertNull(HMICategoryDefinition.IMAGE.getFileExtension());
        assertNull(HMICategoryDefinition.COMUNICATION.getFileExtension());
        assertNull(HMICategoryDefinition.IMAGE.getHiddenFile());
        assertNull(HMICategoryDefinition.COMUNICATION.getHiddenFile());
    }

    @Test
    void categoryWithoutExtensionAcceptsNoFile() {
        assertFalse(HMICategoryDefinition.IMAGE.acepta("captura.bob", "bob", false));
        assertFalse(HMICategoryDefinition.COMUNICATION.acepta("communication.xml", "xml", false));
    }

    @Test
    void eachCategoryAcceptsItsOwnFile() {
        for (Map.Entry<HMICategoryDefinition, String[]> entrada : AT_THE_FACTORY.entrySet()) {
            HMICategoryDefinition definition = entrada.getKey();
            String[] esperado = entrada.getValue();
            String name = "mi-recurso." + esperado[1];
            assertTrue(definition.acepta(name, esperado[1], false),
                    definition.name() + " deberia aceptar " + name);
        }
    }

    @Test
    void hiddenFileIsNotListed() {
        for (Map.Entry<HMICategoryDefinition, String[]> entrada : AT_THE_FACTORY.entrySet()) {
            HMICategoryDefinition definition = entrada.getKey();
            String[] esperado = entrada.getValue();
            assertFalse(definition.acepta(esperado[2], esperado[1], false),
                    definition.name() + " no debe mostrar su fichero base: " + esperado[2]);
        }
    }

    @Test
    void folderIsNeverListedAsFile() {
        for (HMICategoryDefinition definition : AT_THE_FACTORY.keySet()) {
            String extension = definition.getFileExtension();
            assertFalse(definition.acepta("subcarpeta." + extension, extension, true),
                    definition.name() + " no debe intentar mostrar un directorio");
        }
    }

    @Test
    void otraExtensionNoSeMuestra() {
        for (HMICategoryDefinition definition : AT_THE_FACTORY.keySet()) {
            assertFalse(definition.acepta("documento.pdf", "pdf", false),
                    definition.name() + " solo muestra " + definition.getFileExtension());
        }
    }

    @Test
    void folderIsComparedIgnoringCaseAndSpaces() {
        assertTrue(HMICategoryDefinition.RECIPE.matchesFolder("Recipe"));
        assertTrue(HMICategoryDefinition.RECIPE.matchesFolder("recipe"));
        assertTrue(HMICategoryDefinition.RECIPE.matchesFolder("RECIPE"));
        assertTrue(HMICategoryDefinition.RECIPE.matchesFolder("  Recipe  "));
        assertFalse(HMICategoryDefinition.RECIPE.matchesFolder("Scripts"));
        assertFalse(HMICategoryDefinition.RECIPE.matchesFolder(null));
        assertFalse(HMICategoryDefinition.RECIPE.matchesFolder(""));
    }

    @Test
    void extensionAndHiddenAreComparedIgnoringCase() {
        HMICategoryDefinition receta = HMICategoryDefinition.RECIPE;

        assertTrue(receta.acepta("Receta.RCP", "RCP", false));
        assertFalse(receta.acepta("TEMPLATE.RCP", "rcp", false),
                "el fichero oculto tambien se compara sin distinguir mayusculas");
    }

    @Test
    void nullExtensionIsNotAccepted() {
        assertFalse(HMICategoryDefinition.RECIPE.acepta("recurso.rcp", null, false));
    }

    @Test
    void identifiersAreUniqueAndDoNotRepeatTheVisibleName() {
        List<String> ids = Arrays.stream(HMICategoryDefinition.values())
                .map(HMICategoryDefinition::getId).collect(Collectors.toList());

        assertEquals(ids.size(), ids.stream().distinct().count(),
                "dos categorias con el mismo id no se podrian distinguir: " + ids);
        ids.forEach(id -> assertFalse(id.isBlank(), "ninguna categoria puede tener el id vacio"));
    }

    @Test
    void noFolderIsRepeatedAcrossCategories() {
        List<String> carpetas = Arrays.stream(HMICategoryDefinition.values())
                .map(HMICategoryDefinition::getDisplayName).collect(Collectors.toList());

        assertEquals(carpetas.size(), carpetas.stream().distinct().count(),
                "dos categorias en la misma carpeta harían que sus NodeFactory colisionasen");
    }
}