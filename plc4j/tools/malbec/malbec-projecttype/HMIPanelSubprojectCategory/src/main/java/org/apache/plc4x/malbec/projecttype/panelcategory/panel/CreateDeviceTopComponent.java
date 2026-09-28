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
    "CTL_CommunicationsTopComponent=Creación del Dispositivo",
    "HINT_CommunicationsTopComponent=Ventana para configuración de PLC"
})
public final class CreateDeviceTopComponent extends TopComponent {

    private static final Dimension LABEL_SIZE = new Dimension(120, 26);
    private static final Dimension FIELD_SIZE = new Dimension(280, 26);

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
    private JLabel lblHost;
    private JLabel lblPort;
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
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));
        return p;
    }

    private void addFieldRow(JPanel card, GridBagConstraints g, int y, String label, JComponent field) {
        JLabel lbl = new JLabel(label);
        lbl.setPreferredSize(LABEL_SIZE);
        g.insets = new Insets(1, 3, 1, 3);
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
        setLayout(new BorderLayout(5, 5));

        JPanel mainFormPanel = new JPanel(new GridBagLayout());
        mainFormPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        GridBagConstraints mg = new GridBagConstraints();
        mg.insets = new Insets(1, 2, 1, 2);
        mg.anchor = GridBagConstraints.WEST;
        mg.fill = GridBagConstraints.HORIZONTAL;
        mg.weightx = 0.0;
        mg.gridwidth = 1;

        JLabel lblTitle = new JLabel("Seleccione el equipo a Configurar:");
        mg.gridx = 0;
        mg.gridy = 0;
        mainFormPanel.add(lblTitle, mg);

        // --- 1. Panel Marca / Modelo ---
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

        // --- 2. Panel Device Fields ---
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
        txtUUID.setPreferredSize(FIELD_SIZE);
        chkEnable = new JCheckBox("Enable");

        GridBagConstraints dg = new GridBagConstraints();
        addFieldRow(pnlDevice, dg, 0, "Protocol", txtProtocol);
        addFieldRow(pnlDevice, dg, 1, "Name", txtDeviceName);
        addFieldRow(pnlDevice, dg, 2, "Key", txtDeviceKey);
        addFieldRow(pnlDevice, dg, 3, "Description", txtDescription);
        addFieldRow(pnlDevice, dg, 4, "UUID", txtUUID);

        dg.gridx = 1;
        dg.gridy = 5;
        pnlDevice.add(chkEnable, dg);

        mg.gridy = 2;
        mainFormPanel.add(pnlDevice, mg);

        // --- 3. Panel S88 Tree ---
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

        // --- 4. Panel Parámetros de Conexión ---
        JPanel pnlParams = card("Parámetros de conexión");

        txtHost = new JTextField();
        txtHost.setPreferredSize(FIELD_SIZE);

        // Subpanel Transport + Port extendido mediante GridBagLayout
        cbTransport = new JComboBox<>();
        cbTransport.setEditable(true);
        cbTransport.setPreferredSize(new Dimension(110, 22));

        lblPort = new JLabel("Port:");
        lblPort.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));

        txtPort = new JTextField();

        JPanel pnlTransportPort = new JPanel(new GridBagLayout());
        pnlTransportPort.setPreferredSize(FIELD_SIZE);

        GridBagConstraints tgc = new GridBagConstraints();
        tgc.fill = GridBagConstraints.HORIZONTAL;
        tgc.gridy = 0;

        tgc.gridx = 0;
        tgc.weightx = 0.0;
        pnlTransportPort.add(cbTransport, tgc);

        tgc.gridx = 1;
        tgc.weightx = 0.0;
        pnlTransportPort.add(lblPort, tgc);

        tgc.gridx = 2;
        tgc.weightx = 1.0; // Hace que txtPort se extienda hasta el borde derecho
        pnlTransportPort.add(txtPort, tgc);

        cbParameter = new JComboBox<>();
        cbParameter.setPreferredSize(FIELD_SIZE);

        txtParamValue = new JTextField();
        txtParamValue.setPreferredSize(FIELD_SIZE);
        txtParamValue.setEnabled(false);

        JButton btnAddParam = new JButton("+");
        btnAddParam.setMargin(new Insets(1, 6, 1, 6));
        JButton btnRemoveParam = new JButton("-");
        btnRemoveParam.setMargin(new Insets(1, 6, 1, 6));
        JButton btnClear = new JButton("Limpiar");
        btnClear.setMargin(new Insets(1, 6, 1, 6));

        JPanel pnlParamButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        pnlParamButtons.setPreferredSize(FIELD_SIZE);
        pnlParamButtons.add(btnAddParam);
        pnlParamButtons.add(btnRemoveParam);
        pnlParamButtons.add(btnClear);

        txtUrlPreview = new JTextField();
        txtUrlPreview.setEditable(false);
        txtUrlPreview.setPreferredSize(FIELD_SIZE);

        GridBagConstraints pg = new GridBagConstraints();
        lblHost = new JLabel("Host/IP");
        addFieldRow(pnlParams, pg, 0, "Host/IP", txtHost);
        addFieldRow(pnlParams, pg, 1, "Transport/Port", pnlTransportPort);
        addFieldRow(pnlParams, pg, 2, "Parámetro", cbParameter);
        addFieldRow(pnlParams, pg, 3, "Valor", txtParamValue);
        addFieldRow(pnlParams, pg, 4, "Acciones", pnlParamButtons);
        addFieldRow(pnlParams, pg, 5, "URL Preview", txtUrlPreview);

        mg.gridy = 4;
        mainFormPanel.add(pnlParams, mg);

        // --- CENTRADO DEL FORMULARIO ---
        JPanel alignContainer = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        alignContainer.add(mainFormPanel);

        JScrollPane scrollPane = new JScrollPane(alignContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, BorderLayout.CENTER);

        // Listeners del panel de parámetros
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

        cbTransport.addActionListener(e -> onTransportChanged());
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

        // --- Botones Inferiores Compactos y Centrados ---
        JPanel pnlButtons = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        JButton btnOk = new JButton("Ok");
        JButton btnCancel = new JButton("Cancel");
        pnlButtons.add(btnOk);
        pnlButtons.add(btnCancel);
        add(pnlButtons, BorderLayout.SOUTH);

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
        if (cbTransport.getSelectedItem() == null && cbTransport.getItemCount() > 0) {
            cbTransport.setSelectedIndex(0);
        }
        onTransportChanged();
    }

    private void onTransportChanged() {
        if (activeBuilder == null) {
            return;
        }
        Object transport = cbTransport.getSelectedItem();
        String transportValue = transport != null ? transport.toString() : "";
        activeBuilder.setTransport(transportValue);

        lblHost.setText(activeBuilder.getHostLabel());
        boolean portApplicable = activeBuilder.isPortApplicable();
        lblPort.setEnabled(portApplicable);
        txtPort.setEnabled(portApplicable);
        if (!portApplicable) {
            txtPort.setText("");
        }

        refreshParameterCatalog(transportValue);
        refreshUrlPreview();
    }

    private void refreshParameterCatalog(String transport) {
        if (activeBuilder == null) {
            cbParameter.removeAllItems();
            txtParamValue.setEnabled(false);
            txtParamValue.setText("");
            return;
        }
        LinkedHashMap<String, String> catalog = activeBuilder.getParameterDefaults(transport);
        activeBuilder.retainParameters(catalog.keySet());

        String selectedKey = (String) cbParameter.getSelectedItem();
        cbParameter.removeAllItems();
        for (String key : catalog.keySet()) {
            cbParameter.addItem(key);
        }
        if (selectedKey != null && catalog.containsKey(selectedKey)) {
            cbParameter.setSelectedItem(selectedKey);
        }
        Object current = cbParameter.getSelectedItem();
        if (current == null) {
            txtParamValue.setEnabled(false);
            txtParamValue.setText("");
        } else {
            txtParamValue.setEnabled(true);
            txtParamValue.setText(catalog.getOrDefault(current.toString(), ""));
        }
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
        lblHost.setText("Host/IP");
        cbTransport.removeAllItems();
        txtPort.setText("");
        lblPort.setEnabled(true);
        txtPort.setEnabled(true);
        cbParameter.removeAllItems();
        txtParamValue.setText("");
        txtParamValue.setEnabled(false);
        txtUrlPreview.setText("");
    }

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
                    "Debe ingresar la IP/Host o el dispositivo serie.",
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
            HMICategoryCreateDeviceAction.createDevice(currentProject, data);
            resetForm();
            close();
        }
    }

    private void onCancelPressed() {
        resetForm();
        close();
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