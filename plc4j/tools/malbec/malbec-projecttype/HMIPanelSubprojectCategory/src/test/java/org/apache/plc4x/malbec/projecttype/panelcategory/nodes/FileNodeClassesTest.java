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

import java.util.List;
import org.junit.jupiter.api.Test;
import org.openide.loaders.DataNode;
import org.openide.nodes.FilterNode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileNodeClassesTest {

    private static final List<String> FILE_NODE_CLASSES = List.of(
            "org.apache.plc4x.malbec.projecttype.panelcategory.nodes."
            + "HMICategoryDisplayNodeFactory$HMIBobNode",
            "org.apache.plc4x.malbec.projecttype.panelcategory.nodes."
            + "CategoryFilesNodeFactory$CategoryFileNode",
            "org.apache.plc4x.malbec.projecttype.panelcategory.nodes."
            + "HMICategoryIconNodeFactory$IconFileNode",
            "org.apache.plc4x.malbec.projecttype.panelcategory.nodes."
            + "HMICategoryIconNodeFactory$IconFolderNode",
            "org.apache.plc4x.malbec.projecttype.panelcategory.nodes."
            + "HMICategoryNestedSubprojectNodeFactory$NestedCategoryNodeList$HMINestedNode"
    );

    @Test
    void losNodosDeFicheroNoVuelvenADataNodeNiFilterNode() throws Exception {
        for (String name : FILE_NODE_CLASSES) {
            Class<?> nodeClass = Class.forName(name);

            assertTrue(AbstractFileNode.class.isAssignableFrom(nodeClass),
                    name + " debe extender AbstractFileNode: DataNode/FilterNode avisan de la "
                            + "destruccion antes de que el NodeList quite la clave y dejan "
                            + "una fila vacia en el arbol");
            assertFalse(DataNode.class.isAssignableFrom(nodeClass),
                    name + " no puede volver a DataNode");
            assertFalse(FilterNode.class.isAssignableFrom(nodeClass),
                    name + " no puede volver a FilterNode");
        }
    }
}
