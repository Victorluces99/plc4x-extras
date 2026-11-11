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

import java.awt.Component;
import java.awt.Container;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.TreeView;
import org.openide.modules.OnStart;
import org.openide.nodes.Node;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

@OnStart
public final class HmiTreeDragGuard implements Runnable {

    private static final String PROJECT_TAB_ID = "projectTabLogical_tc";
    private static final Logger LOG = Logger.getLogger(HmiTreeDragGuard.class.getName());

    private static Guard guard;

    private static final PropertyChangeListener TOP_COMPONENT_LISTENER = event -> {
        if (TopComponent.Registry.PROP_TC_OPENED.equals(event.getPropertyName())) {
            attach();
        } else if (TopComponent.Registry.PROP_TC_CLOSED.equals(event.getPropertyName())
                && guard != null && guard.projectTab == event.getNewValue()) {
            guard.detach();
            guard = null;
        }
    };

    @Override
    public void run() {
        TopComponent.getRegistry().addPropertyChangeListener(TOP_COMPONENT_LISTENER);
        WindowManager.getDefault().invokeWhenUIReady(HmiTreeDragGuard::attach);
    }

    private static void attach() {
        TopComponent projectTab = WindowManager.getDefault().findTopComponent(PROJECT_TAB_ID);
        if (!(projectTab instanceof ExplorerManager.Provider)) {
            LOG.log(Level.FINE, "Project tab not available; HMI drag guard not installed.");
            return;
        }

        TreeView treeView = findTreeView(projectTab);
        if (treeView == null) {
            LOG.log(Level.WARNING, "TreeView not found in project tab; HMI drag guard not installed.");
            return;
        }

        if (guard != null && guard.projectTab == projectTab && guard.treeView == treeView) {
            return;
        }
        if (guard != null) {
            guard.detach();
        }

        ExplorerManager manager = ((ExplorerManager.Provider) projectTab).getExplorerManager();
        guard = new Guard(projectTab, manager, treeView);
        guard.attach();
    }

    private static TreeView findTreeView(Component component) {
        if (component instanceof TreeView) {
            return (TreeView) component;
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                TreeView found = findTreeView(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean isHmiNode(Node node) {
        for (Node current = node; current != null; current = current.getParentNode()) {
            if (current.getLookup().lookup(NoDragNode.class) != null) {
                return true;
            }
        }
        return false;
    }

    private static final class Guard implements PropertyChangeListener {

        private final TopComponent projectTab;
        private final ExplorerManager manager;
        private final TreeView treeView;
        private final boolean dragSourceEnabled;

        private Guard(TopComponent projectTab, ExplorerManager manager, TreeView treeView) {
            this.projectTab = projectTab;
            this.manager = manager;
            this.treeView = treeView;
            this.dragSourceEnabled = treeView.isDragSource();
        }

        private void attach() {
            manager.addPropertyChangeListener(this);
            update();
        }

        private void detach() {
            manager.removePropertyChangeListener(this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            if (ExplorerManager.PROP_SELECTED_NODES.equals(event.getPropertyName())) {
                update();
            }
        }

        private void update() {
            boolean hmiSelected = Arrays.stream(manager.getSelectedNodes())
                    .anyMatch(HmiTreeDragGuard::isHmiNode);
            treeView.setDragSource(dragSourceEnabled && !hmiSelected);
        }
    }
}
