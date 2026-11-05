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
package org.apache.plc4x.malbec.projecttype.panel.nodes;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.plc4x.malbec.projecttype.panel.HMIPanelSubprojectFactory;
import org.apache.plc4x.malbec.projecttype.panel.HMIPanelSubprojectImpl;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;


@NodeFactory.Registration(projectType = "org-plc4x-hmi-project", position = 10)
public class HMIPanelSubprojectNodeFactory implements NodeFactory {

    @Override
    public NodeList<?> createNodes(Project project) {
        return new PanelNodeList(project.getProjectDirectory());
    }

    private static final class PanelNodeList implements NodeList<Project>, FileChangeListener {

        private final FileObject dir;
        private final List<ChangeListener> listeners = new ArrayList<>();
        private final List<Project> keys = new ArrayList<>();

        PanelNodeList(FileObject dir) {
            this.dir = dir;
            refreshKeys();
        }

        @Override
        public synchronized List<Project> keys() {
            return List.copyOf(keys);
        }

        @Override
        public Node node(Project key) {
            LogicalViewProvider lvp = key.getLookup().lookup(LogicalViewProvider.class);
            return lvp != null ? lvp.createLogicalView() : null;
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
            listeners.add(cl);
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
            listeners.remove(cl);
        }

        @Override
        public void addNotify() {
            dir.addRecursiveListener(this);
            refreshKeys();
        }

        @Override
        public void removeNotify() {
            dir.removeRecursiveListener(this);
        }

        private void fireChange() {
            ChangeEvent event = new ChangeEvent(this);
            for (ChangeListener listener : new ArrayList<>(listeners)) {
                listener.stateChanged(event);
            }
        }

        private void refreshIfRelevant(FileObject file) {
            if (file == null) {
                return;
            }
            FileObject parent = file.getParent();
            if (dir.equals(parent)) {
                refreshKeys();
                return;
            }
            FileObject grandParent = parent != null ? parent.getParent() : null;
            if (dir.equals(grandParent)
                    && HMIPanelSubprojectFactory.PROJECT_FILE.equals(file.getNameExt())) {
                refreshKeys();
            }
        }

        private synchronized void refreshKeys() {
            FileObject[] children = dir.getChildren();
            Arrays.sort(children, Comparator.comparing(FileObject::getName));

            ProjectManager projectManager = ProjectManager.getDefault();
            projectManager.clearNonProjectCache();

            keys.clear();
            for (FileObject child : children) {
                if (!child.isFolder()
                        || child.getFileObject(HMIPanelSubprojectFactory.PROJECT_FILE) == null) {
                    continue;
                }
                try {
                    Project p = projectManager.findProject(child);
                    if (p instanceof HMIPanelSubprojectImpl) {
                        keys.add(p);
                    }
                } catch (IOException | IllegalArgumentException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            fireChange();
        }

        @Override
        public void fileFolderCreated(FileEvent fe) {
            refreshIfRelevant(fe.getFile());
        }

        @Override
        public void fileDataCreated(FileEvent fe) {
            refreshIfRelevant(fe.getFile());
        }

        @Override
        public void fileDeleted(FileEvent fe) {
            refreshIfRelevant(fe.getFile());
        }

        @Override
        public void fileRenamed(FileRenameEvent fre) {
            refreshIfRelevant(fre.getFile());
        }

        @Override
        public void fileChanged(FileEvent fe) {
        }

        @Override
        public void fileAttributeChanged(FileAttributeEvent fae) {
        }
    }
}
