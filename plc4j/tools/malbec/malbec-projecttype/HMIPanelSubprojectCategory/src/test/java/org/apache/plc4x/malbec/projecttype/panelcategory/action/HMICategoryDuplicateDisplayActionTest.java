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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HMICategoryDuplicateDisplayActionTest {

    @Test
    void duplicaElContenidoYAnadeSufijoParaNoSobrescribir() throws Exception {
        FileObject root = FileUtil.createMemoryFileSystem().getRoot();
        FileObject original = root.createData("pantalla.bob");
        byte[] content = "<bob>contenido</bob>".getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = original.getOutputStream()) {
            out.write(content);
        }

        HMICategoryDuplicateDisplayAction action =
                new HMICategoryDuplicateDisplayAction(original);
        Method duplicate = HMICategoryDuplicateDisplayAction.class
                .getDeclaredMethod("duplicate", FileObject.class);
        duplicate.setAccessible(true);

        duplicate.invoke(action, original);

        FileObject firstCopy = root.getFileObject("pantalla_copy", "bob");
        assertNotNull(firstCopy, "la primera copia debe llamarse pantalla_copy.bob");
        assertArrayEquals(content, read(firstCopy));

        duplicate.invoke(action, original);

        FileObject secondCopy = root.getFileObject("pantalla_copy2", "bob");
        assertNotNull(secondCopy, "la segunda copia no puede pisar la primera");
        assertArrayEquals(content, read(secondCopy));
        assertEquals("pantalla.bob", original.getNameExt());
    }

    private static byte[] read(FileObject file) throws Exception {
        try (InputStream in = file.getInputStream()) {
            return in.readAllBytes();
        }
    }
}
