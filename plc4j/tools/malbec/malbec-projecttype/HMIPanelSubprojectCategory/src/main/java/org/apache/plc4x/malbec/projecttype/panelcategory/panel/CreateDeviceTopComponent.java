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

    private static final Dimension LABEL_SIZE = new Dimension(150, 26);
    private static final Dimension FIELD_SIZE = new Dimension(260, 26);
    private static final Dimension UUID_FIELD_SIZE = new Dimension(340, 26);
    private static final Dimension HOST_SIZE = new Dimension(340, 26);
    private static final Dimension PORT_SIZE = new Dimension(90, 26);
    private static final Dimension PARAM_VALUE_SIZE = new Dimension(230, 26);
    private static final Dimension URL_SIZE = new Dimension(600, 26);

    private Project currentProject;

    // Componentes visuales
    private JComboBox<String> cbMarca;
    private JComboBox<DeviceModel> cbModelo;
    private JTextField txtProtocol;
    private JTextField txtDeviceName;
    private JTextField txtDeviceKey;
    private JTextField txtDescription;
    private JTextField txtUUID;
    private JCheckBox chkEnable;

    // Sección S88 Tree
    private JComboBox<String> cbS88Node;
    private JTextField txtS88UUID;

    // Área de Parámetros de Conexión
    private JTextField txtHost;
    private JComboBox<String> cbTransport;
    private JTextField txtPort;
    private JComboBox<String> cbParameter;
    private JTextField txtParamValue;
    private JTextField txtUrlPreview;
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
                new DeviceModel("Siemens", "S7-300", "S7"),
                new DeviceModel("Siemens", "S7-400", "S7"),
                new DeviceModel("Siemens", "S7-1200", "S7"),
                new DeviceModel("Siemens", "S7-1500", "S7")

        ));
        devicesMap.put("Allen Bradley", Arrays.asList(
                new DeviceModel("Allen Bradley", "ControlLogix", "EtherNet/IP"),
                new DeviceModel("Allen Bradley", "CompactLogix", "EtherNet/IP")
        ));
        devicesMap.put("Modbus", Arrays.asList(
                new DeviceModel("Modbus", "Modbus TCP", "modbus-tcp"),
                new DeviceModel("Modbus", "Modbus RTU", "modbus-rtu"),
                new DeviceModel("Modbus", "Modbus ASCII", "modbus-ascii")
        ));

        s88NodesMap = new HashMap<>();
        s88NodesMap.put("Node_Area_01", UUID.nameUUIDFromBytes("Node_Area_01".getBytes()).toString());
        s88NodesMap.put("Node_Unit_02", UUID.nameUUIDFromBytes("Node_Unit_02".getBytes()).toString());
    }

    private JPanel card(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        return p;
    }

    private void addFieldRow(JPanel card, GridBagConstraints g, int y, String label, JComponent field) {
        JLabel lbl = new JLabel(label);
        lbl.setPreferredSize(LABEL_SIZE);
        g.insets = new Insets(2, 8, 2, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 0.0;
        g.gridx = 0;
        g.gridy = y;
        g.gridwidth = 1;
        card.add(lbl, g);
        g.gridx = 1;
        card.add(field, g);
    }

    private void initComponentsUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel mainFormPanel = new JPanel(new GridBagLayout());
        mainFormPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints mg = new GridBagConstraints();
        mg.insets = new Insets(2, 4, 2, 4);
        mg.anchor = GridBagConstraints.WEST;
        mg.fill = GridBagConstraints.HORIZONTAL;
        mg.weightx = 0.0;
        mg.gridwidth = 1;

        JLabel lblTitle = new JLabel("Seleccione el equipo para establecer la comunicación");
        mg.gridx = 0;
        mg.gridy = 0;
        mainFormPanel.add(lblTitle, mg);

        // --- Panel Marca / Modelo ---
        JPanel pnlBrandModel = card("Marca y modelo");
        cbMarca = new JComboBox<>(new String[]{"-- Seleccione --", "Siemens", "Allen Bradley", "Modbus"});
        cbMarca.setPreferredSize(FIELD_SIZE);
        cbModelo = new JComboBox<>();
        cbModelo.setPreferredSize(FIELD_SIZE);
        GridBagConstraints bg = new GridBagConstraints();
        addFieldRow(pnlBrandModel, bg, 0, "Marca", cbMarca);
        addFieldRow(pnlBrandModel, bg, 1, "Modelo", cbModelo);
        mg.gridy = 1;
        mainFormPanel.add(pnlBrandModel, mg);

        // --- Panel Device Fields ---
        JPanel pnlDevice = card("Dispositivo");
        txtProtocol = new JTextField();
        txtProtocol.setEditable(false);
        txtDeviceName = new JTextField();
        txtDeviceKey = new JTextField();
        txtDescription = new JTextField();
        txtProtocol.setPreferredSize(FIELD_SIZE);
        txtDeviceName.setPreferredSize(FIELD_SIZE);
        txtDeviceKey.setPreferredSize(FIELD_SIZE);
        txtDescription.setPreferredSize(FIELD_SIZE);
        txtUUID = new JTextField();
        txtUUID.setEditable(false);
        txtUUID.setPreferredSize(UUID_FIELD_SIZE);
        chkEnable = new JCheckBox("Enable");
        GridBagConstraints dg = new GridBagConstraints();
        addFieldRow(pnlDevice, dg, 0, "Device Protocol", txtProtocol);
        addFieldRow(pnlDevice, dg, 1, "Device name", txtDeviceName);
        addFieldRow(pnlDevice, dg, 2, "Device Key", txtDeviceKey);
        addFieldRow(pnlDevice, dg, 3, "Device description", txtDescription);
        addFieldRow(pnlDevice, dg, 4, "UUID", txtUUID);
        dg.gridx = 2;
        dg.gridy = 4;
        dg.gridwidth = 1;
        pnlDevice.add(chkEnable, dg);
        mg.gridy = 2;
        mainFormPanel.add(pnlDevice, mg);

        // --- Panel S88 Tree ---
        JPanel pnlS88 = card("S88 tree");
        cbS88Node = new JComboBox<>(new String[]{"-- Seleccione --", "Node_Area_01", "Node_Unit_02"});
        cbS88Node.setPreferredSize(FIELD_SIZE);
        txtS88UUID = new JTextField();
        txtS88UUID.setEditable(false);
        txtS88UUID.setPreferredSize(FIELD_SIZE);
        GridBagConstraints sg = new GridBagConstraints();
        addFieldRow(pnlS88, sg, 0, "Node", cbS88Node);
        addFieldRow(pnlS88, sg, 1, "UUID", txtS88UUID);
        mg.gridy = 3;
        mainFormPanel.add(pnlS88, mg);

        add(mainFormPanel, BorderLayout.NORTH);

        // --- Área de Parámetros de Conexión ---
        JPanel pnlParams = card("Parámetros de conexión");
        txtHost = new JTextField();
        txtHost.setPreferredSize(HOST_SIZE);
        cbTransport = new JComboBox<>();
        cbTransport.setEditable(true);
        cbTransport.setPreferredSize(FIELD_SIZE);
        txtPort = new JTextField();
        txtPort.setPreferredSize(PORT_SIZE);
        cbParameter = new JComboBox<>();
        cbParameter.setPreferredSize(PARAM_VALUE_SIZE);
        txtParamValue = new JTextField();
        txtParamValue.setPreferredSize(PARAM_VALUE_SIZE);
        txtParamValue.setEnabled(false);
        JButton btnAddParam = new JButton("Añadir");
        JButton btnRemoveParam = new JButton("Quitar último");
        JButton btnClear = new JButton("Limpiar");
        txtUrlPreview = new JTextField();
        txtUrlPreview.setEditable(false);
        txtUrlPreview.setPreferredSize(URL_SIZE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 8, 2, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        gbc.gridy = 0;
        gbc.gridx = 0;
        pnlParams.add(new JLabel("Host/IP:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        pnlParams.add(txtHost, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        pnlParams.add(new JLabel("Transport:"), gbc);
        gbc.gridx = 1;
        pnlParams.add(cbTransport, gbc);
        gbc.gridx = 2;
        pnlParams.add(new JLabel("Port:"), gbc);
        gbc.gridx = 3;
        pnlParams.add(txtPort, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        pnlParams.add(new JLabel("Parámetro:"), gbc);
        gbc.gridx = 1;
        pnlParams.add(cbParameter, gbc);
        gbc.gridx = 2;
        pnlParams.add(txtParamValue, gbc);
        gbc.gridx = 3;
        pnlParams.add(btnAddParam, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        pnlParams.add(btnRemoveParam, gbc);
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        pnlParams.add(btnClear, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        pnlParams.add(new JLabel("URL:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        pnlParams.add(txtUrlPreview, gbc);

        JPanel pnlParamsWrap = new JPanel(new BorderLayout());
        pnlParamsWrap.add(pnlParams, BorderLayout.NORTH);
        add(pnlParamsWrap, BorderLayout.CENTER);

        cbParameter.addActionListener(e -> {
            String key = (String) cbParameter.getSelectedItem();
            if (key == null) {
                txtParamValue.setEnabled(false);
                txtParamValue.setText("");
                return;
            }
            txtParamValue.setEnabled(true);
            txtParamValue.setText(activeBuilder.getParameterDefaults().get(key));
        });

        txtHost.getDocument().addDocumentListener(
                new SimpleDocumentListener(CreateDeviceTopComponent.this::refreshUrlPreview));

        cbTransport.addActionListener(e -> refreshUrlPreview());
        JComponent transportEditor = (JComponent) cbTransport.getEditor().getEditorComponent();
        if (transportEditor instanceof JTextField) {
            ((JTextField) transportEditor).getDocument().addDocumentListener(
                    new SimpleDocumentListener(CreateDeviceTopComponent.this::refreshUrlPreview));
        }
        txtPort.getDocument().addDocumentListener(
                new SimpleDocumentListener(CreateDeviceTopComponent.this::refreshUrlPreview));

        btnAddParam.addActionListener(e -> {
            if (activeBuilder == null) {
                return;
            }
            String key = (String) cbParameter.getSelectedItem();
            if (key == null) {
                return;
            }
            activeBuilder.addParameter(key, txtParamValue.getText().trim());
            refreshUrlPreview();
        });
        btnRemoveParam.addActionListener(e -> {
            if (activeBuilder != null) {
                activeBuilder.removeLastParameter();
            }
            refreshUrlPreview();
        });
        btnClear.addActionListener(e -> {
            if (activeBuilder != null) {
                activeBuilder.clearParameters();
            }
            refreshUrlPreview();
        });

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
            txtHost.setText("");
            cbTransport.removeAllItems();
            txtPort.setText("");
            cbParameter.removeAllItems();
            txtParamValue.setText("");
            txtParamValue.setEnabled(false);
            txtUrlPreview.setText("");

            String selectedBrand = (String) cbMarca.getSelectedItem();
            if (devicesMap.containsKey(selectedBrand)) {
                for (DeviceModel dm : devicesMap.get(selectedBrand)) {
                    cbModelo.addItem(dm);
                }
                // Preseleccionar el primer modelo y generar el UUID explícitamente:
                // el auto-seleccionado al añadir items no siempre dispara el ActionEvent.
                cbModelo.setSelectedIndex(0);
                txtUUID.setText(UUID.randomUUID().toString());
            }
        });

        cbModelo.addActionListener(e -> {
            DeviceModel selectedDevice = (DeviceModel) cbModelo.getSelectedItem();
            if (selectedDevice != null) {
                txtProtocol.setText(selectedDevice.getProtocol());
                txtUUID.setText(UUID.randomUUID().toString());

                String brand = selectedDevice.getBrand();
                if ("Allen Bradley".equalsIgnoreCase(brand)) {
                    activeBuilder = new AllenBradleyPanelBuilder();
                } else if ("Modbus".equalsIgnoreCase(brand)) {
                    activeBuilder = new ModbusPanelBuilder();
                } else {
                    activeBuilder = new SiemensPanelBuilder();
                }

                loadBuilderParams();
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

    private void loadBuilderParams() {
        cbParameter.removeAllItems();
        txtParamValue.setEnabled(false);
        txtParamValue.setText("");
        if (activeBuilder == null) {
            refreshUrlPreview();
            return;
        }
        activeBuilder.initForModel((DeviceModel) cbModelo.getSelectedItem());

        cbTransport.removeAllItems();
        cbTransport.setEditable(true);
        for (String option : activeBuilder.getTransportOptions()) {
            cbTransport.addItem(option);
        }
        cbTransport.setSelectedItem(activeBuilder.getDefaultTransport());

        for (String key : activeBuilder.getParameterDefaults().keySet()) {
            cbParameter.addItem(key);
        }
        refreshUrlPreview();
    }

    private void refreshUrlPreview() {
        if (activeBuilder == null) {
            txtUrlPreview.setText("");
            return;
        }
        activeBuilder.setHost(txtHost.getText().trim());
        Object transport = cbTransport.getSelectedItem();
        activeBuilder.setTransport(transport != null ? transport.toString() : "");
        activeBuilder.setPort(txtPort.getText().trim());
        txtUrlPreview.setText(activeBuilder.getSpecificParametersAsString());
    }

    /**
     * Limpia completamente la vista
     */
    public void resetForm() {
        cbMarca.setSelectedIndex(0);
        cbModelo.removeAllItems();
        txtProtocol.setText("");
        txtDeviceName.setText("");
        txtDeviceKey.setText("");
        txtDescription.setText("");
        txtUUID.setText("");
        chkEnable.setSelected(false);
        cbS88Node.setSelectedIndex(0);
        txtS88UUID.setText("");
        activeBuilder = null;
        txtHost.setText("");
        cbTransport.removeAllItems();
        txtPort.setText("");
        cbParameter.removeAllItems();
        txtParamValue.setText("");
        txtParamValue.setEnabled(false);
        txtUrlPreview.setText("");
    }

    /**
     * Valida y construye los datos. Retorna null si hay inconsistencias.
     */
    public DeviceConfigData getDeviceConfigData() {
        if (txtDeviceName.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese un nombre para el dispositivo.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (txtDeviceKey.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor ingrese una clave (Device Key) para el dispositivo.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        DeviceModel selectedModel = (DeviceModel) cbModelo.getSelectedItem();
        if (selectedModel == null) {
            JOptionPane.showMessageDialog(this, "Por favor seleccione una marca y modelo válidos.", "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        if (txtHost.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debe ingresar la IP/Host del dispositivo.",
                    "Error de Validación", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        String specificParams = "";
        if (activeBuilder != null) {
            activeBuilder.setHost(txtHost.getText().trim());
            Object transport = cbTransport.getSelectedItem();
            activeBuilder.setTransport(transport != null ? transport.toString() : "");
            activeBuilder.setPort(txtPort.getText().trim());
            specificParams = activeBuilder.getSpecificParametersAsString();
        }

        Properties pDevice = new Properties();

        pDevice.put("brand", cbMarca.getSelectedItem());
        pDevice.put("model", selectedModel.getModel());
        pDevice.put("protocol", txtProtocol.getText());
        pDevice.put("deviceName", txtDeviceName.getText().trim());
        pDevice.put("deviceKey", txtDeviceKey.getText().trim());
        pDevice.put("description", txtDescription.getText());
        pDevice.put("uuid", txtUUID.getText());
        pDevice.put("enable", chkEnable.isSelected());
        pDevice.put("s88Node", cbS88Node.getSelectedItem());
        pDevice.put("s88Uuid", txtS88UUID.getText());
        pDevice.put("specificParameters", specificParams);

        return new DeviceConfigData(pDevice);
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

    private static final class SimpleDocumentListener implements javax.swing.event.DocumentListener {

        private final Runnable action;

        SimpleDocumentListener(Runnable action) {
            this.action = action;
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            action.run();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            action.run();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            action.run();
        }
    }
}