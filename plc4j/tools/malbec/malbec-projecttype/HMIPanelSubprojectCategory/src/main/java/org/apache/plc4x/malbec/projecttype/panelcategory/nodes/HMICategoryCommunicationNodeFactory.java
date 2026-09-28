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
import java.util.function.Supplier;
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
import org.openide.actions.PropertiesAction;
import org.openide.filesystems.FileObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Node.Property;
import org.openide.nodes.Node.PropertySet;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.actions.SystemAction;

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
            allActions.add(new AbstractAction("Propiedades") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SystemAction.get(PropertiesAction.class).performAction();
                }
            });
            allActions.add(null);
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

        @Override
        protected Sheet createSheet() {
            Sheet sheet = super.createSheet();
            Sheet.Set props = sheet.get(Sheet.PROPERTIES);
            if (props == null) {
                props = Sheet.createPropertiesSet();
                sheet.put(props);
            }
            props.put(readOnlyProperty("deviceName", "Nombre", "Nombre del dispositivo", device::getDeviceName));
            props.put(readOnlyProperty("brand", "Marca", "Marca del dispositivo", device::getBrand));
            props.put(readOnlyProperty("model", "Modelo", "Modelo del dispositivo", device::getModel));
            props.put(readOnlyProperty("protocol", "Protocolo", "Protocolo de comunicación", device::getProtocol));
            props.put(readOnlyProperty("specificParameters", "URL", "URL / parámetros específicos de conexión", device::getSpecificParameters));
            props.put(readOnlyProperty("s88Node", "S88 Node", "Nodo S88 (área) asociado al dispositivo", device::getS88Node));
            props.put(readOnlyProperty("s88Uuid", "S88 UUID", "UUID del área S88 asociada", device::getS88Uuid));
            props.put(readOnlyProperty("deviceKey", "Device Key", "Clave del dispositivo", device::getDeviceKey));
            props.put(readOnlyProperty("description", "Descripción", "Descripción del dispositivo", device::getDescription));
            props.put(readOnlyProperty("uuid", "UUID", "Identificador único del dispositivo", device::getUuid));
            props.put(readOnlyBooleanProperty("enable", "Habilitado", "Indica si el dispositivo está habilitado", device::isEnabled));
            return sheet;
        }

        private static Property readOnlyProperty(String name, String displayName, String shortDescription, Supplier<String> getter) {
            return new PropertySupport.ReadOnly(name, String.class, displayName, shortDescription) {
                @Override
                public Object getValue() {
                    return getter.get();
                }
            };
        }

        private static Property readOnlyBooleanProperty(String name, String displayName, String shortDescription, Supplier<Boolean> getter) {
            return new PropertySupport.ReadOnly(name, Boolean.class, displayName, shortDescription) {
                @Override
                public Object getValue() {
                    return getter.get();
                }
            };
        }
    }
}