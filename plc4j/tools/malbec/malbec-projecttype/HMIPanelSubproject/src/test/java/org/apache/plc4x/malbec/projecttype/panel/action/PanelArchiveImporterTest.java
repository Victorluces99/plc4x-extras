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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PanelArchiveImporterTest {

    @Test
    void importaUnPanelExportadoEnLaRaiz(@TempDir Path temp) throws IOException {
        File zip = zip(temp, entries(
                "panel.cfg", "configuracion",
                "Image/", null,
                "Image/CELDA1/CELDA1.bob", "plantilla"));
        FileObject hmiDirectory = hmiDirectory(temp);

        FileObject imported = PanelArchiveImporter.importPanel(hmiDirectory, "Imported", zip);

        assertNotNull(imported);
        assertEquals("configuracion", imported.getFileObject("panel.cfg").asText());
        assertEquals("plantilla",
                imported.getFileObject("Image/CELDA1/CELDA1.bob").asText());
    }

    @Test
    void importaUnPanelConUnaCarpetaRaiz(@TempDir Path temp) throws IOException {
        File zip = zip(temp, entries(
                "Panel/", null,
                "Panel/panel.cfg", "configuracion",
                "Panel/Image/CELDA1.bob", "plantilla"));
        FileObject hmiDirectory = hmiDirectory(temp);

        FileObject imported = PanelArchiveImporter.importPanel(hmiDirectory, "Imported", zip);

        assertNotNull(imported.getFileObject("panel.cfg"));
        assertEquals("plantilla", imported.getFileObject("Image/CELDA1.bob").asText());
        assertNull(imported.getFileObject("Panel"));
    }

    @Test
    void rechazaUnZipSinPanelCfgYNoCreaLaCarpeta(@TempDir Path temp) throws IOException {
        File zip = zip(temp, entries("readme.txt", "no es un panel"));
        FileObject hmiDirectory = hmiDirectory(temp);

        assertThrows(IOException.class,
                () -> PanelArchiveImporter.importPanel(hmiDirectory, "Imported", zip));

        assertNull(hmiDirectory.getFileObject("Imported"));
    }

    @Test
    void rechazaRutasQueEscapanDelPanel(@TempDir Path temp) throws IOException {
        File zip = zip(temp, entries(
                "panel.cfg", "configuracion",
                "../evil.txt", "malicioso"));
        FileObject hmiDirectory = hmiDirectory(temp);

        assertThrows(IOException.class,
                () -> PanelArchiveImporter.importPanel(hmiDirectory, "Imported", zip));

        assertNull(hmiDirectory.getFileObject("Imported"));
    }

    @Test
    void rechazaUnPanelConNombreExistente(@TempDir Path temp) throws IOException {
        File zip = zip(temp, entries("panel.cfg", "configuracion"));
        FileObject hmiDirectory = hmiDirectory(temp);
        hmiDirectory.createFolder("Imported");

        assertThrows(IOException.class,
                () -> PanelArchiveImporter.importPanel(hmiDirectory, "Imported", zip));

        assertNotNull(hmiDirectory.getFileObject("Imported"));
    }

    private static FileObject hmiDirectory(Path temp) throws IOException {
        FileObject directory = FileUtil.createMemoryFileSystem().getRoot();
        assertNotNull(directory);
        return directory;
    }

    private static Map<String, String> entries(String... namesAndContents) {
        Map<String, String> entries = new LinkedHashMap<>();
        for (int i = 0; i < namesAndContents.length; i += 2) {
            entries.put(namesAndContents[i], namesAndContents[i + 1]);
        }
        return entries;
    }

    private static File zip(Path directory, Map<String, String> entries) throws IOException {
        Path zip = directory.resolve("panel.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (Map.Entry<String, String> entry : entries.entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                if (entry.getValue() != null) {
                    output.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                }
                output.closeEntry();
            }
        }
        return zip.toFile();
    }
}
