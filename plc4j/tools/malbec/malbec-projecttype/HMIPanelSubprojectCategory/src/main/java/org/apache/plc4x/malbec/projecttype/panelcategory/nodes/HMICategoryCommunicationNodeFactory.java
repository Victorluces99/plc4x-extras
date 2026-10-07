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

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.apache.plc4x.malbec.projecttype.panelcategory.HMICategoryDefinition;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.netbeans.HMICommunicationModel;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.CommConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CreateDeviceDialog;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.DeviceConfigData;
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
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.actions.SystemAction;
import org.openide.windows.WindowManager;

@NodeFactory.Registration(projectType = "org-apache-plc4x-category", position = 70)
public class HMICategoryCommunicationNodeFactory implements NodeFactory {

    @Override
    public NodeList<?> createNodes(Project project) {
        FileObject projectDir = project.getProjectDirectory();

        // Solo mostrar si la carpeta se llama "Comunicacion"
        if (!HMICategoryDefinition.COMUNICATION.getDisplayName().equalsIgnoreCase(projectDir.getName())) {
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
        private final Map<String, HMICommNode> nodesUuid = new LinkedHashMap<>();

        public HMICategoryCommunicationNodeList(Project project, HMICommunicationModel model) {
            this.project = project;
            this.model = model;
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
            if (device(uuid) == null) {
                return null;
            }
            HMICommNode existente = nodesUuid.get(uuid);
            if (existente != null) {
                return existente;
            }
            HMICommNode creado = new HMICommNode(uuid, project, this);
            nodesUuid.put(uuid, creado);
            return creado;
        }

        DeviceConfigData device(String uuid) {
            return model == null || uuid == null ? null : model.findByUuid(uuid);
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
            refreshNodes();
        }

        private void refreshNodes() {
            nodesUuid.values().removeIf(nodo -> nodo.device() == null);
            for (HMICommNode nodo : new ArrayList<>(nodesUuid.values())) {
                nodo.refresh();
            }
        }
    }

    private static class HMICommNode extends AbstractNode {

        /** Nombres de las propiedades que hay que avisar cuando cambia el modelo. */
        private static final List<String> PROPERTIES = List.of(
                "deviceName", "brand", "model", "protocol", "specificParameters",
                "s88Node", "s88Uuid", "deviceKey", "description", "uuid", "enable");

        /** Texto único para cuando el dispositivo no tiene nombre. */
        private static final String SIN_NOMBRE = "(sin nombre)";

        private final String uuid;
        private final Project project;
        private final HMICategoryCommunicationNodeList list;
        private String displayNameBefore;

        HMICommNode(String uuid, Project project, HMICategoryCommunicationNodeList list) {
            super(Children.LEAF);
            this.uuid = uuid;
            this.project = project;
            this.list = list;
            DeviceConfigData inicial = list.device(uuid);
            if (inicial != null) {
                setLabelsFrom(inicial);
            }
            setIconBaseWithExtension(HMICategoryDefinition.COMUNICATION.getIconPath());
        }

        /** Dispositivo vigente, o null si fue borrado. */
        DeviceConfigData device() {
            return list.device(uuid);
        }

        private void setLabelsFrom(DeviceConfigData d) {
            String name = nameOf(d);
            displayNameBefore = name;
            setName(name);
            setDisplayName(name);
            setShortDescription(tooltipOf(d));
        }

        private static String nameOf(DeviceConfigData d) {
            String name = d.getDeviceName();
            return name == null || name.isBlank() ? SIN_NOMBRE : name;
        }

        /** Tooltip del nodo: lo que identifica al dispositivo de un vistazo. */
        private static String tooltipOf(DeviceConfigData d) {
            return d.getBrand() + " / " + d.getModel() + " / " + d.getProtocol();
        }

        void refresh() {
            DeviceConfigData d = device();
            if (d == null) {
                return;
            }
            String name = nameOf(d);
            if (!Objects.equals(name, displayNameBefore)) {
                setName(name);
                firePropertyChange(Node.PROP_DISPLAY_NAME, displayNameBefore, name);
                displayNameBefore = name;
            }
            setShortDescription(tooltipOf(d));
            for (String property : PROPERTIES) {
                firePropertyChange(property, null, null);
            }
        }

        @Override
        public Action[] getActions(boolean context) {
            List<Action> allActions = new ArrayList<>();
            allActions.add(new AbstractAction("Modificar Dispositivo") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SwingUtilities.invokeLater(() -> openEditForm());
                }
            });
            allActions.add(null);
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
                    deleteDevice();
                }
            });
            return allActions.toArray(new Action[0]);
        }

        private void openEditForm() {
            Frame ventana = WindowManager.getDefault().getMainWindow();
            try {
                DeviceConfigData actual = device();
                if (actual == null) {
                    JOptionPane.showMessageDialog(ventana,
                            "El dispositivo '" + currentName()
                                    + "' ya no está en la configuración.\n"
                                    + "Puede que se haya borrado desde otra vista.",
                            "No se puede modificar", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                CreateDeviceDialog.forEdit(ventana, project, actual).setVisible(true);
            } catch (RuntimeException ex) {
                Exceptions.printStackTrace(ex);
                JOptionPane.showMessageDialog(ventana,
                        "No se pudo abrir la modificación del dispositivo:\n\n" + ex.getMessage(),
                        "Error al modificar", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void deleteDevice() {
            HMICommunicationModel model = project.getLookup().lookup(HMICommunicationModel.class);
            if (model == null) {
                return;
            }
            Frame ventana = WindowManager.getDefault().getMainWindow();
            Object[] opciones = {"Aceptar", "Cancelar"};
            int elegida = JOptionPane.showOptionDialog(ventana,
                    deleteMessage(model.getComms(uuid)),
                    "Eliminar dispositivo", JOptionPane.WARNING_MESSAGE,
                    JOptionPane.DEFAULT_OPTION, null, opciones, opciones[0]);
            if (elegida != 0) {
                return;
            }

            String nombre = currentName();
            if (!model.removeDevice(uuid)) {
                JOptionPane.showMessageDialog(ventana,
                        "No se encontró el dispositivo '" + nombre + "'.",
                        "Nada que eliminar", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean baseActualizada;
            try {
                baseActualizada = model.save();
            } catch (RuntimeException ex) {
                // save() lanza IllegalStateException si falla la escritura del
                // XML, así que sin este catch el error salía sin avisar.
                Exceptions.printStackTrace(ex);
                JOptionPane.showMessageDialog(ventana,
                        "No se pudo eliminar '" + nombre + "'.\n\n" + ex.getMessage(),
                        "Error al eliminar", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!baseActualizada) {
                JOptionPane.showMessageDialog(ventana,
                        "El dispositivo '" + nombre + "' se quitó de comunicacion.xml pero no se pudo\n"
                                + "actualizar boot.db. Revisá la consola.",
                        "Guardado parcial", JOptionPane.WARNING_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(ventana,
                    "Se eliminó el dispositivo '" + nombre + "' correctamente.",
                    "Eliminación exitosa", JOptionPane.INFORMATION_MESSAGE);
        }

        /** Nombre del device, o un texto utilizable si viniera vacío. */
        private String currentName() {
            DeviceConfigData actual = device();
            return actual == null ? SIN_NOMBRE : nameOf(actual);
        }

        private String deleteMessage(CommConfigData comms) {
            StringBuilder texto = new StringBuilder("Va a eliminar el dispositivo '")
                    .append(currentName()).append("'.")
                    .append("\n\nAl borrarlo se pierden todas sus configuraciones.");
            if (comms != null) {
                int grupos = comms.getGroups().size();
                int areas = comms.getItems().size();
                int variables = comms.getPvs().size();
                if (grupos > 0 || areas > 0 || variables > 0) {
                    texto.append("\n\n  • ").append(grupos)
                            .append(grupos == 1 ? " grupo" : " grupos")
                            .append("\n  • ").append(areas)
                            .append(areas == 1 ? " área" : " áreas")
                            .append("\n  • ").append(variables)
                            .append(variables == 1 ? " variable" : " variables");
                }
            }
            return texto.append("\n\nTambién se quitará de comunicacion.xml y de boot.db.")
                    .append("\n\n¿Desea continuar?").toString();
        }

        @Override
        protected Sheet createSheet() {
            Sheet sheet = super.createSheet();
            Sheet.Set props = sheet.get(Sheet.PROPERTIES);
            if (props == null) {
                props = Sheet.createPropertiesSet();
                sheet.put(props);
            }
            // Cada propiedad vuelve a leer el device al consultarse: la
            // ventana de Propiedades muestra el valor que devuelve getValue()
            // cuando se abre o cuando el nodo avisa que cambió.
            props.put(readOnlyProperty("deviceName", "Nombre", "Nombre del dispositivo", DeviceConfigData::getDeviceName));
            props.put(readOnlyProperty("brand", "Marca", "Marca del dispositivo", DeviceConfigData::getBrand));
            props.put(readOnlyProperty("model", "Modelo", "Modelo del dispositivo", DeviceConfigData::getModel));
            props.put(readOnlyProperty("protocol", "Protocolo", "Protocolo de comunicación", DeviceConfigData::getProtocol));
            props.put(readOnlyProperty("specificParameters", "URL", "URL / parámetros específicos de conexión", DeviceConfigData::getSpecificParameters));
            props.put(readOnlyProperty("s88Node", "S88 Node", "Nodo S88 (área) asociado al dispositivo", DeviceConfigData::getS88Node));
            props.put(readOnlyProperty("s88Uuid", "S88 UUID", "UUID del área S88 asociada", DeviceConfigData::getS88Uuid));
            props.put(readOnlyProperty("deviceKey", "Device Key", "Clave del dispositivo", DeviceConfigData::getDeviceKey));
            props.put(readOnlyProperty("description", "Descripción", "Descripción del dispositivo", DeviceConfigData::getDescription));
            props.put(readOnlyProperty("uuid", "UUID", "Identificador único del dispositivo", DeviceConfigData::getUuid));
            props.put(readOnlyBooleanProperty("enable", "Habilitado", "Indica si el dispositivo está habilitado", DeviceConfigData::isEnabled));
            return sheet;
        }

        /** Valor legible de la propiedad, o cadena vacía si no hay dispositivo. */
        private static String safeText(DeviceConfigData d, Function<DeviceConfigData, String> getter) {
            String value = d == null ? null : getter.apply(d);
            return value == null ? "" : value;
        }

        private Property readOnlyProperty(String propertyName, String label, String shortDescription, Function<DeviceConfigData, String> getter) {
            return new PropertySupport.ReadOnly(propertyName, String.class, label, shortDescription) {
                @Override
                public Object getValue() {
                    return safeText(device(), getter);
                }
            };
        }

        private Property readOnlyBooleanProperty(String propertyName, String label, String shortDescription, Function<DeviceConfigData, Boolean> getter) {
            return new PropertySupport.ReadOnly(propertyName, Boolean.class, label, shortDescription) {
                @Override
                public Object getValue() {
                    DeviceConfigData d = device();
                    return d != null && Boolean.TRUE.equals(getter.apply(d));
                }
            };
        }
    }
}