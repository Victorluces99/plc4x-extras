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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.plc4x.malbec.projecttype.panelcategory.HMICategoryDefinition;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeFactorySupport;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.loaders.DataNode;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;

/**
 * Base de las factories que muestran los ficheros de una categoria dentro de su carpeta.
 *
 * <p>Cada categoria tenia su propia copia de esta clase, de unas 195 lineas, que se
 * diferenciaba de las demas solo en tres cosas: la categoria, la extension y el fichero
 * oculto. Los dos ultimos ya viven en {@link HMICategoryDefinition} desde T4.1, asi que aqui
 * solo queda la categoria y, en tres casos, una accion propia.
 *
 * <p>Lo que decide que se muestra esta en {@link HMICategoryDefinition#acepta}, que es logica
 * pura y esta probada. Esta clase solo aporta la parte que necesita la plataforma: leer el
 * {@code FileObject}, enterarse de los cambios y construir el {@code Node}.
 *
 * <p>Cada subclase conserva su anotacion {@link NodeFactory.Registration}, porque la posicion
 * en el arbol se declara en la anotacion y no se puede pasar a un superconstructor. Se expone
 * como constante para que un test pueda comprobar que cada categoria sigue donde estaba.
 *
 * <p>No herendan de aqui las factories de Comunicacion y de subproyecto anidado, que tienen
 * otra estructura, ni la de Display, que quedo excluida en T1.12.
 */
public abstract class CategoryFilesNodeFactory implements NodeFactory {

    /** Categoria cuyas carpetas muestra esta factory. */
    protected abstract HMICategoryDefinition categoria();

    /**
     * Acciones que esta categoria anade a sus ficheros, antes del separador.
     *
     * <p>Por defecto ninguna: la mayoria de categorias solo ofrecen las acciones por defecto.
     */
    protected Action[] accionesPropias(FileObject archivo) {
        return new Action[0];
    }

    @Override
    public NodeList<?> createNodes(Project project) {
        FileObject projectDir = project.getProjectDirectory();

        HMICategoryDefinition definicion = categoria();
        if (!definicion.coincideConCarpeta(projectDir.getName())) {
            return NodeFactorySupport.fixedNodeList();
        }

        return new CategoryFilesNodeList(projectDir, definicion, this::accionesPropias);
    }

    private static final class CategoryFilesNodeList
            implements NodeList<FileObject>, FileChangeListener {

        private final FileObject folder;
        private final HMICategoryDefinition definicion;
        private final Function<FileObject, Action[]> accionesPropias;
        private final List<ChangeListener> listeners = new ArrayList<>();
        private final List<FileObject> keys = new ArrayList<>();

        CategoryFilesNodeList(FileObject folder, HMICategoryDefinition definicion,
                Function<FileObject, Action[]> accionesPropias) {
            this.folder = folder;
            this.definicion = definicion;
            this.accionesPropias = accionesPropias;
            refreshKeys();
        }

        @Override
        public List<FileObject> keys() {
            return keys;
        }

        @Override
        public Node node(FileObject key) {
            try {
                DataObject dataObj = DataObject.find(key);
                return new CategoryFileNode(dataObj, key, definicion, accionesPropias);
            } catch (DataObjectNotFoundException ex) {
                Exceptions.printStackTrace(ex);
                return null;
            }
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
            listeners.add(cl);
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
            listeners.remove(cl);
        }

        private void fireChange() {
            ChangeEvent event = new ChangeEvent(this);
            for (ChangeListener listener : new ArrayList<>(listeners)) {
                listener.stateChanged(event);
            }
        }

        @Override
        public void addNotify() {
            folder.addFileChangeListener(this);
            refreshKeys();
        }

        @Override
        public void removeNotify() {
            folder.removeFileChangeListener(this);
        }

        private void refreshKeys() {
            keys.clear();
            for (FileObject child : folder.getChildren()) {
                if (definicion.acepta(child.getNameExt(), child.getExt(), child.isFolder())) {
                    keys.add(child);
                }
            }
            fireChange();
        }

        private boolean esDeEstaCategoria(FileEvent evento) {
            String extension = definicion.getFileExtension();
            return extension != null && extension.equalsIgnoreCase(evento.getFile().getExt());
        }

        @Override
        public void fileDataCreated(FileEvent fe) {
            if (esDeEstaCategoria(fe)) {
                refreshKeys();
            }
        }

        @Override
        public void fileDeleted(FileEvent fe) {
            if (esDeEstaCategoria(fe)) {
                refreshKeys();
            }
        }

        @Override
        public void fileRenamed(FileRenameEvent fre) {
            refreshKeys();
        }

        @Override
        public void fileFolderCreated(FileEvent fe) {
            refreshKeys();
        }

        @Override
        public void fileChanged(FileEvent fe) {
            // TODO
        }

        @Override
        public void fileAttributeChanged(FileAttributeEvent fae) {
            // TODO
        }
    }

    private static final class CategoryFileNode extends DataNode {

        private final FileObject fileObject;
        private final Function<FileObject, Action[]> accionesPropias;

        CategoryFileNode(DataObject dataObject, FileObject fileObject,
                HMICategoryDefinition definicion, Function<FileObject, Action[]> accionesPropias) {
            super(dataObject, Children.LEAF);
            this.fileObject = fileObject;
            this.accionesPropias = accionesPropias;

            String iconBase = definicion.getIconPath();
            if (iconBase != null && !iconBase.isEmpty()) {
                setIconBaseWithExtension(iconBase);
            }
        }

        @Override
        public Action[] getActions(boolean context) {
            Action[] defaultActions = super.getActions(context);

            List<Action> allActions = new ArrayList<>();
            for (Action action : accionesPropias.apply(fileObject)) {
                allActions.add(action);
            }
            allActions.add(null);
            for (Action action : defaultActions) {
                allActions.add(action);
            }
            return allActions.toArray(new Action[0]);
        }
    }
}