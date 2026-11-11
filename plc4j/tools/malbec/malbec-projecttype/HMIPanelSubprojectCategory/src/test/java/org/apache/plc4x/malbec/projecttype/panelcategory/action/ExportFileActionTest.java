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
package org.apache.plc4x.malbec.projecttype.panelcategory.action;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportFileActionTest {

    @Test
    void writeZipGuardaElFicheroConSuNombreYContenido(@TempDir Path tempDir) throws Exception {
        FileObject root = FileUtil.createMemoryFileSystem().getRoot();
        FileObject source = root.createData("pantalla.bob");
        byte[] content = "<bob>contenido</bob>".getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = source.getOutputStream()) {
            out.write(content);
        }

        File destination = tempDir.resolve("salida.zip").toFile();
        Method writeZip = ExportFileAction.class
                .getDeclaredMethod("writeZip", FileObject.class, File.class);
        writeZip.setAccessible(true);

        writeZip.invoke(null, source, destination);

        assertTrue(Files.exists(destination.toPath()));
        try (ZipFile zip = new ZipFile(destination)) {
            ZipEntry entry = zip.getEntry("pantalla.bob");
            assertNotNull(entry, "el ZIP debe contener el fichero con su extension original");
            try (InputStream in = zip.getInputStream(entry)) {
                assertArrayEquals(content, in.readAllBytes());
            }
        }
    }
}
