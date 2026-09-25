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

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.plc4x.malbec.projecttype.panelcategory.action.HMICategoryCreateCommAction;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.xml.HMICommunicationModel;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceConfigData;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.NodeFactory;
import org.netbeans.spi.project.ui.support.NodeFactorySupport;
import org.netbeans.spi.project.ui.support.NodeList;
import org.openide.filesystems.FileObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

@NodeFactory.Registration(projectType = "org-apache-plc4x-category", position = 70)
public class HMICategoryCommunicationNodeFactory implements NodeFactory {

    @Override
    public NodeList<?> createNodes(Project project) {
        FileObject projectDir = project.getProjectDirectory();

        // Solo mostrar si la carpeta se llama "Comunicacion"
        if (!"Comunicacion".equalsIgnoreCase(projectDir.getName())) {
            return NodeFactorySupport.fixedNodeList();
        }

        HMICommunicationModel model = project.getLookup().lookup(HMICommunicationModel.class);
        return new HMICategoryCommunicationNodeList(project, model);
    }

    private static class HMICategoryCommunicationNodeList implements NodeList<String>, ChangeListener {

        private final Project project;
        private final HMICommunicationModel model;
        private final List<ChangeListener> listeners = new ArrayList<>();
        private final List<String> keys = new ArrayList<>();

        public HMICategoryCommunicationNodeList(Project project, HMICommunicationModel model) {
            this.project = project;
            this.model = model;
            if (model != null) {
                model.addChangeListener(this);
            }
            refreshKeys();
        }

        @Override
        public List<String> keys() {
            return keys;
        }

        @Override
        public Node node(String uuid) {
            if (model == null) {
                return null;
            }
            DeviceConfigData device = model.findByUuid(uuid);
            if (device == null) {
                return null;
            }
            return new HMICommNode(device, project);
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
            if (model != null) {
                model.addChangeListener(this);
            }
            refreshKeys();
        }

        @Override
        public void removeNotify() {
            if (model != null) {
                model.removeChangeListener(this);
            }
        }

        private void refreshKeys() {
            keys.clear();
            if (model != null) {
                for (DeviceConfigData device : model.getDevices()) {
                    keys.add(device.getUuid());
                }
            }
            fireChange();
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            refreshKeys();
        }
    }

    private static class HMICommNode extends AbstractNode {

        private final DeviceConfigData device;
        private final Project project;

        public HMICommNode(DeviceConfigData device, Project project) {
            super(Children.LEAF);
            this.device = device;
            this.project = project;
            setName(device.getDeviceName());
            setDisplayName(device.getDeviceName());
            setShortDescription(device.getBrand() + " / " + device.getModel() + " / " + device.getProtocol());
            setIconBaseWithExtension("org/apache/plc4x/malbec/projecttype/hmipanelsubprojectcategory/comm.png");
        }

        @Override
        public Action[] getActions(boolean context) {
            List<Action> allActions = new ArrayList<>();
//            allActions.add(new HMICategoryCreateCommAction(project, device.getUuid()));
//            allActions.add(null);
            allActions.add(new AbstractAction("Eliminar Dispositivo") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    HMICommunicationModel model = project.getLookup().lookup(HMICommunicationModel.class);
                    if (model != null && model.removeDevice(device.getUuid())) {
                        model.save();
                    }
                }
            });
            return allActions.toArray(new Action[0]);
        }
    }
}