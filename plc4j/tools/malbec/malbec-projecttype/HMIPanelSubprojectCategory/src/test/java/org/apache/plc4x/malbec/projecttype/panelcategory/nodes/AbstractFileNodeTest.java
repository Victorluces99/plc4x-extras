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
package org.apache.plc4x.malbec.projecttype.panelcategory.nodes;

import java.beans.BeanInfo;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.plc4x.malbec.projecttype.panelcategory.CategoryIcons;
import org.junit.jupiter.api.Test;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.NodeAdapter;
import org.openide.nodes.NodeEvent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class AbstractFileNodeTest {

    private static final class TestNode extends AbstractFileNode {

        private TestNode(DataObject dataObject, FileObject fileObject) {
            super(dataObject, fileObject, Children.LEAF);
        }
    }

    private record Fixture(FileObject file, DataObject dataObject, TestNode node) {

        private static Fixture create() throws IOException {
            FileObject root = FileUtil.createMemoryFileSystem().getRoot();
            FileObject file = root.createData("demo.txt");
            try (OutputStream out = file.getOutputStream()) {
                out.write("contenido".getBytes(StandardCharsets.UTF_8));
            }
            DataObject dataObject = DataObject.find(file);
            return new Fixture(file, dataObject, new TestNode(dataObject, file));
        }
    }

    @Test
    void delegaNombreAccionesYPropiedadesAlNodoDelDataObject() throws Exception {
        Fixture fixture = Fixture.create();
        Node delegate = fixture.dataObject().getNodeDelegate();

        assertEquals(delegate.getName(), fixture.node().getName());
        assertEquals(delegate.getDisplayName(), fixture.node().getDisplayName());
        assertEquals(delegate.getShortDescription(), fixture.node().getShortDescription());
        assertEquals(delegate.getHtmlDisplayName(), fixture.node().getHtmlDisplayName());
        assertArrayEquals(delegate.getActions(false), fixture.node().getActions(false),
                "el menu contextual lo monta el DataObject (Copy/Cut/Paste/Delete/Rename...)");
        assertArrayEquals(delegate.getPropertySets(), fixture.node().getPropertySets());
        assertEquals(delegate.canCopy(), fixture.node().canCopy());
        assertEquals(delegate.canCut(), fixture.node().canCut());
        assertEquals(delegate.canRename(), fixture.node().canRename());
        assertEquals(delegate.canDestroy(), fixture.node().canDestroy());
    }

    @Test
    void destroyBorraElDataObjectSinDispararNodeDestroyed() throws Exception {
        Fixture fixture = Fixture.create();
        AtomicInteger destroyed = new AtomicInteger();
        fixture.node().addNodeListener(new NodeAdapter() {
            @Override
            public void nodeDestroyed(NodeEvent event) {
                destroyed.incrementAndGet();
            }
        });

        fixture.node().destroy();

        assertFalse(fixture.dataObject().isValid(), "el DataObject debe quedar borrado");
        assertFalse(fixture.file().isValid(), "el fichero debe quedar borrado");
        assertEquals(0, destroyed.get(),
                "si el nodo avisa de su destruccion antes de que el NodeList quite la clave, "
                        + "el arbol se queda con una fila vacia (ghost row)");
    }

    @Test
    void sinIconoPropioUsaElDelDataObject() throws Exception {
        Fixture fixture = Fixture.create();
        Node delegate = fixture.dataObject().getNodeDelegate();

        assertSame(delegate.getIcon(BeanInfo.ICON_COLOR_16x16),
                fixture.node().getIcon(BeanInfo.ICON_COLOR_16x16));
        assertSame(delegate.getOpenedIcon(BeanInfo.ICON_COLOR_16x16),
                fixture.node().getOpenedIcon(BeanInfo.ICON_COLOR_16x16));
    }

    @Test
    void setOwnIconBaseReemplazaElIconoDelDataObject() throws Exception {
        Fixture fixture = Fixture.create();
        Node delegate = fixture.dataObject().getNodeDelegate();
        Object delegateIcon = delegate.getIcon(BeanInfo.ICON_COLOR_16x16);

        fixture.node().setOwnIconBase(CategoryIcons.DEFAULT);

        assertNotNull(fixture.node().getIcon(BeanInfo.ICON_COLOR_16x16));
        assertNotSame(delegateIcon, fixture.node().getIcon(BeanInfo.ICON_COLOR_16x16));
    }

    @Test
    void lookupContieneElNodoYElDataObject() throws Exception {
        Fixture fixture = Fixture.create();

        assertSame(fixture.node(), fixture.node().getLookup().lookup(TestNode.class));
        assertSame(fixture.node(), fixture.node().getLookup().lookup(Node.class));
        assertSame(fixture.dataObject(), fixture.node().getLookup().lookup(DataObject.class));
    }

    @Test
    void setNameRenombraElDataObject() throws Exception {
        Fixture fixture = Fixture.create();

        fixture.node().setName("renombrado.txt");

        assertEquals("renombrado.txt", fixture.node().getName());
        assertEquals("renombrado.txt", fixture.dataObject().getNodeDelegate().getName());
        assertEquals("renombrado.txt", fixture.file().getNameExt());
    }
}
