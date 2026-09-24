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
package org.apache.plc4x.malbec.projecttype.panelcategory.panel;

import org.netbeans.api.project.Project;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import org.apache.plc4x.malbec.projecttype.panelcategory.action.HMICategoryCreateDeviceAction;

@ConvertAsProperties(
        dtd = "-//com.mycompany.communications.ui//Communications//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "CommunicationsTopComponent",
        persistenceType = TopComponent.PERSISTENCE_NEVER
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "com.mycompany.communications.ui.CommunicationsTopComponent")
@ActionReference(path = "Menu/Window")
@Messages({
    "CTL_CommunicationsTopComponent=Ventana de Comunicaciones",
    "HINT_CommunicationsTopComponent=Ventana para configuración de PLC"
})
public final class CreateDeviceTopComponent extends TopComponent {

    private Project currentProject;

    // Componentes visuales
    private JComboBox<String> cbMarca;
    private JComboBox<DeviceModel> cbModelo;
    private JTextField txtProtocol;
    private JTextField txtDeviceName;
    private JTextField txtDescription;
    private JTextField txtUUID;
    private JCheckBox chkEnable;

    // Sección S88 Tree
    private JComboBox<String> cbS88Node;
    private JTextField txtS88UUID;

    // Panel dinámico inferior
    private JPanel dynamicAreaContainer;
    private DeviceDynamicPanelBuilder activeBuilder;

    // Estructuras de datos locales
    private Map<String, List<DeviceModel>> devicesMap;
    private Map<String, String> s88NodesMap;

    public CreateDeviceTopComponent() {
        setName(Bundle.CTL_CommunicationsTopComponent());
        setToolTipText(Bundle.HINT_CommunicationsTopComponent());

        initData();
        initComponentsUI();
    }

    public void setProject(Project project) {
        this.currentProject = project;
    }

    public Project getProject() {
        return currentProject;
    }

    private void initData() {
        devicesMap = new HashMap<>();
        devicesMap.put("Siemens", Arrays.asList(
                new DeviceModel("Siemens", "S7-1200", "S7"),
                new DeviceModel("Siemens", "S7-1500", "S7")
        ));
        devicesMap.put("Allen Bradley", Arrays.asList(
                new DeviceModel("Allen Bradley", "ControlLogix", "EtherNet/IP"),
                new DeviceModel("Allen Bradley", "CompactLogix", "EtherNet/IP")
        ));

        s88NodesMap = new HashMap<>();
        s88NodesMap.put("Node_Area_01", UUID.nameUUIDFromBytes("Node_Area_01".getBytes()).toString());
        s88NodesMap.put("Node_Unit_02", UUID.nameUUIDFromBytes("Node_Unit_02".getBytes()).toString());
    }

    private void initComponentsUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel mainFormPanel = new JPanel();
        mainFormPanel.setLayout(new BoxLayout(mainFormPanel, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Seleccione el equipo para establecer la comunicación", SwingConstants.CENTER);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        mainFormPanel.add(lblTitle);
        mainFormPanel.add(Box.createVerticalStrut(10));

        // --- Panel Marca / Modelo ---
        JPanel pnlBrandModel = new JPanel(new GridLayout(2, 2, 5, 5));
        pnlBrandModel.add(new JLabel("Marca"));
        pnlBrandModel.add(new JLabel("Modelo"));

        cbMarca = new JComboBox<>(new String[]{"-- Seleccione --", "Siemens", "Allen Bradley"});
        cbModelo = new JComboBox<>();
        pnlBrandModel.add(cbMarca);
        pnlBrandModel.add(cbModelo);
        mainFormPanel.add(pnlBrandModel);

        // --- Panel Device Fields ---
        JPanel pnlDevice = new JPanel(new GridLayout(4, 2, 5, 5));
        txtProtocol = new JTextField();
        txtProtocol.setEditable(false);
        txtDeviceName = new JTextField();
        txtDescription = new JTextField();
        txtUUID = new JTextField();
        txtUUID.setEditable(false);
        chkEnable = new JCheckBox("Enable");

        pnlDevice.add(new JLabel("Device Protocol"));
        pnlDevice.add(new JLabel("Device name"));
        pnlDevice.add(txtProtocol);
        pnlDevice.add(txtDeviceName);
        pnlDevice.add(new JLabel("Device description"));
        pnlDevice.add(chkEnable);
        pnlDevice.add(txtDescription);
        pnlDevice.add(new JPanel());

        JPanel pnlUUID = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlUUID.add(new JLabel("UUID: "));
        pnlUUID.add(txtUUID);

        mainFormPanel.add(pnlDevice);
        mainFormPanel.add(pnlUUID);
        mainFormPanel.add(Box.createVerticalStrut(10));

        // --- Panel S88 Tree ---
        JPanel pnlS88 = new JPanel(new GridLayout(2, 2, 5, 5));
        pnlS88.setBorder(BorderFactory.createTitledBorder("S88 tree"));

        cbS88Node = new JComboBox<>(new String[]{"-- Seleccione --", "Node_Area_01", "Node_Unit_02"});
        txtS88UUID = new JTextField();
        txtS88UUID.setEditable(false);

        pnlS88.add(new JLabel("Node:"));
        pnlS88.add(new JLabel("UUID"));
        pnlS88.add(cbS88Node);
        pnlS88.add(txtS88UUID);
        mainFormPanel.add(pnlS88);

        add(mainFormPanel, BorderLayout.NORTH);

        // --- Panel Dinámico Inferior ---
        dynamicAreaContainer = new JPanel(new BorderLayout());
        dynamicAreaContainer.setBorder(BorderFactory.createEtchedBorder());
        add(dynamicAreaContainer, BorderLayout.CENTER);

        // --- Botones Inferiores ---
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton btnOk = new JButton("Ok");
        JButton btnCancel = new JButton("Cancel");
        pnlButtons.add(btnOk);
        pnlButtons.add(btnCancel);
        add(pnlButtons, BorderLayout.SOUTH);

        // Events de Ok y Cancel
        btnOk.addActionListener(e -> onOkPressed());
        btnCancel.addActionListener(e -> onCancelPressed());

        setupListeners();
    }

    private void setupListeners() {
        cbMarca.addActionListener(e -> {
            cbModelo.removeAllItems();
            txtProtocol.setText("");
            txtUUID.setText("");
            activeBuilder = null;
            dynamicAreaContainer.removeAll();
            dynamicAreaContainer.revalidate();
            dynamicAreaContainer.repaint();

            String selectedBrand = (String) cbMarca.getSelectedItem();
            if (devicesMap.containsKey(selectedBrand)) {
                for (DeviceModel dm : devicesMap.get(selectedBrand)) {
                    cbModelo.addItem(dm);
                }
            }
        });

        cbModelo.addActionListener(e -> {
            DeviceModel selectedDevice = (DeviceModel) cbModelo.getSelectedItem();
            if (selectedDevice != null) {
                txtProtocol.setText(selectedDevice.getProtocol());
                txtUUID.setText(UUID.randomUUID().toString());

                if ("Siemens".equalsIgnoreCase(selectedDevice.getBrand())) {
                    activeBuilder = new SiemensPanelBuilder();
                } else if ("Allen Bradley".equalsIgnoreCase(selectedDevice.getBrand())) {
                    activeBuilder = new AllenBradleyPanelBuilder();
                }

                if (activeBuilder != null) {
                    activeBuilder.buildParametersUI();
                    dynamicAreaContainer.removeAll();

                    // USAR BorderLayout.NORTH para evitar que estire el panel a lo alto
                    dynamicAreaContainer.add(activeBuilder.getPanel(), BorderLayout.NORTH);

                    dynamicAreaContainer.revalidate();
                    dynamicAreaContainer.repaint();
                }
            }
        });

        cbS88Node.addActionListener(e -> {
            String selectedNode = (String) cbS88Node.getSelectedItem();
            if (s88NodesMap.containsKey(selectedNode)) {
                txtS88UUID.setText(s88NodesMap.get(selectedNode));
            } else {
                txtS88UUID.setText("");
            }
        });
    }

    /**
     * Limpia completamente la vista
     */
    public void resetForm() {
        cbMarca.setSelectedIndex(0);
        cbModelo.removeAllItems();
        txtProtocol.setText("");
        txtDeviceName.setText("");
        txtDescription.setText("");
        txtUUID.setText("");
        chkEnable.setSelected(false);
        cbS88Node.setSelectedIndex(0);
        txtS88UUID.setText("");
        activeBuilder = null;
        dynamicAreaContainer.removeAll();
        dynamicAreaContainer.revalidate();
        dynamicAreaContainer.repaint();
    }

    /**
     * Valida y construye los datos. Retorna null si hay inconsistencias.
     */
    public DeviceConfigData getDeviceConfigData() {
        if (txtDeviceName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese un nombre para el dispositivo.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        DeviceModel selectedModel = (DeviceModel) cbModelo.getSelectedItem();
        if (selectedModel == null) {
            JOptionPane.showMessageDialog(this, "Por favor seleccione una marca y modelo válidos.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        String specificParams = (activeBuilder != null) ? activeBuilder.getSpecificParametersAsString() : "";

        return new DeviceConfigData(
                (String) cbMarca.getSelectedItem(),
                selectedModel.getModel(),
                txtProtocol.getText(),
                txtDeviceName.getText().trim(),
                txtDescription.getText(),
                txtUUID.getText(),
                chkEnable.isSelected(),
                (String) cbS88Node.getSelectedItem(),
                txtS88UUID.getText(),
                specificParams
        );
    }

    private void onOkPressed() {
        DeviceConfigData data = getDeviceConfigData();
        if (data != null && currentProject != null) {
            // Guardar el archivo en el proyecto
            HMICategoryCreateDeviceAction.createDeviceFileInProject(currentProject, data);
            // Resetear formulario y cerrar la pestaña
            resetForm();
            close();
        }
    }

    private void onCancelPressed() {
        resetForm();
        close(); // Cierra el TopComponent en NetBeans
    }

    void writeProperties(Properties p) {
    }

    void readProperties(Properties p) {
    }
}
