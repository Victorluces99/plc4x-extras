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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.nio.file.Path;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.DeviceConfigData;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.DeviceModel;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.DeviceDynamicPanelBuilder;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.PanelBuilderRegistry;

public final class CreateDevicePanel extends JPanel {

    private static final String SELECT = "-- Select --";
    private static final int WIDTH_FIELD = 220;
    private static final int WIDTH_FIELD_SHORT = 100;
    private static final int WIDTH_MINIMUN_LABEL = 100;

    private static void minLabelWidth(JLabel label) {
        label.setMinimumSize(new Dimension(WIDTH_MINIMUN_LABEL, label.getPreferredSize().height));
        label.setPreferredSize(new Dimension(WIDTH_MINIMUN_LABEL, label.getPreferredSize().height));
    }

    private static final java.util.logging.Logger LOGGER
            = java.util.logging.Logger.getLogger(CreateDevicePanel.class.getName());
    private final Path projectDir;
    private final JComboBox<String> cbBrand
            = new JComboBox<>(new String[]{SELECT, "Siemens", "Allen Bradley", "Modbus"});
    private final JComboBox<DeviceModel> cbModel = new JComboBox<>();
    private final JTextField txtProtocol = new JTextField();
    private final JTextField txtDeviceName = new JTextField();
    private final JTextField txtDeviceKey = new JTextField();
    private final JTextField txtDescription = new JTextField();
    private final JTextField txtUUID = new JTextField();
    private final JCheckBox chkEnable = new JCheckBox("Enable");
    private final JComboBox<String> cbS88Node = new JComboBox<>();
    private final JTextField txtS88UUID = new JTextField();
    private final ConnectionSection connection = new ConnectionSection();

    private DeviceDynamicPanelBuilder activeBuilder;
    private final PanelBuilderRegistry registry = PanelBuilderRegistry.withDefaults();
    private final Map<String, List<DeviceModel>> devicesMap = new HashMap<>();
    private final Map<String, String> s88NodesMap = new HashMap<>();
    private final transient DeviceConfigData editing;
    private final String modelOnOpen;
    private final boolean edit;

    public CreateDevicePanel(Path projectDir) {
        this(projectDir, null);
    }

    public CreateDevicePanel(Path projectDir, DeviceConfigData edit) {
        this.projectDir = projectDir;
        this.editing = edit;
        this.edit = edit != null;
        this.modelOnOpen = edit == null ? null : edit.getModel();
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initData();
        setFieldWidths();
        buildUi();
        loadS88Nodes();
        if (this.edit) {
            loadForEdit(edit);
        } else {
            resetForm();
        }
    }

    private static JLabel shortLabel(String text, int px) {
        JLabel lbl = new JLabel(text);
        lbl.setPreferredSize(new Dimension(px, lbl.getPreferredSize().height));
        lbl.setMinimumSize(new Dimension(px, lbl.getPreferredSize().height));
        return lbl;
    }

    private static void width(JComponent campo, int px) {
        campo.setPreferredSize(new Dimension(px, campo.getPreferredSize().height));
        campo.setMaximumSize(new Dimension(px, campo.getPreferredSize().height));
    }

    private void setFieldWidths() {
        for (JComponent field : List.of(txtProtocol, txtDeviceName, txtDeviceKey,
                txtDescription, txtUUID, txtS88UUID,
                connection.hostPanel(), connection.txtParamValue(),
                connection.txtUrlPreview(), cbBrand, cbModel, cbS88Node,
                connection.cbParameter())) {
            width(field, WIDTH_FIELD);
        }
        for (JComponent field : List.of(connection.txtPort(), connection.cbTransport())) {
            width(field, WIDTH_FIELD_SHORT);
        }
    }

    private static GridBagConstraints formConstraints() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 8, 3, 8);
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 0.0;
        g.fill = GridBagConstraints.NONE;
        return g;
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        minLabelWidth(label);
        return label;
    }

    private static JLabel bold(String text, float size) {
        JLabel result = new JLabel(text);
        result.setFont(result.getFont().deriveFont(Font.BOLD, size));
        return result;
    }

    private int addSectionTitle(JPanel form, GridBagConstraints g, int y, String text) {
        g.gridx = 0;
        g.gridy = y;
        g.gridwidth = 4;
        g.weightx = 0.0;
        g.fill = GridBagConstraints.NONE;
        form.add(bold(text, 12f), g);
        g.gridwidth = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        return y + 1;
    }

    private int addWide(JPanel form, GridBagConstraints g, int y, JLabel label, JComponent field) {
        g.gridy = y;
        g.gridwidth = 1;
        g.gridx = 0;
        g.weightx = 0.0;
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.WEST;
        form.add(label, g);

        g.gridx = 1;
        g.gridwidth = 3;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;
        form.add(field, g);

        g.gridwidth = 1;
        return y + 1;
    }

    private int addPair(JPanel form, GridBagConstraints g, int y,
            JLabel label1, JComponent field1, JLabel label2, JComponent field2) {
        g.gridy = y;
        g.gridwidth = 1;
        g.gridx = 0;
        g.weightx = 0.0;
        form.add(label1, g);
        g.gridx = 1;
        g.weightx = 0.0;
        form.add(field1, g);
        g.gridx = 2;
        g.weightx = 0.0;
        form.add(label2, g);
        g.gridx = 3;
        g.weightx = 0.0;
        form.add(field2, g);
        return y + 1;
    }

    private int addSpanning(JPanel form, GridBagConstraints g, int y, JComponent field) {
        g.gridy = y;
        g.gridx = 1;
        g.gridwidth = 3;
        g.weightx = 0.0;
        form.add(field, g);
        g.gridwidth = 1;
        return y + 1;
    }

    private void buildUi() {
        txtProtocol.setEditable(false);
        txtUUID.setEditable(false);
        txtS88UUID.setEditable(false);

        cbS88Node.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel rendered = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (DummyPlant.ID.equals(value)) {
                    rendered.setText(DummyPlant.LABEL);
                }
                return rendered;
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = formConstraints();

        int y = 0;
        g.gridx = 0;
        g.gridy = y++;
        g.gridwidth = 4;
        form.add(bold(edit ? "Modify Device"
                : "New Device", 14f), g);
        g.gridwidth = 1;

        y = addSectionTitle(form, g, y, "Brand and Model");
        y = addWide(form, g, y, label("Brand"), cbBrand);
        y = addWide(form, g, y, label("Model"), cbModel);
        y = addWide(form, g, y, label("Protocol"), txtProtocol);
        y = addSpanning(form, g, y, chkEnable);

        y = addSectionTitle(form, g, y, "Device");
        y = addWide(form, g, y, label("Name"), txtDeviceName);
        y = addWide(form, g, y, label("Key"), txtDeviceKey);
        y = addWide(form, g, y, label("Description"), txtDescription);
        y = addWide(form, g, y, label("UUID"), txtUUID);

        y = addSectionTitle(form, g, y, "S88 tree");
        y = addWide(form, g, y, label("Node"), cbS88Node);
        y = addWide(form, g, y, label("UUID"), txtS88UUID);

        y = addSectionTitle(form, g, y, "Connection Parameters");
        y = addWide(form, g, y, connection.lblHost(), connection.hostPanel());
        
        y = addPair(form, g, y, label("Transport"), connection.cbTransport(),
                shortLabel("Port:", 80), connection.txtPort());

        y = addPair(form, g, y, label("Parameters"), connection.cbParameter(),
                shortLabel("Value:", 80), connection.txtParamValue());
        y = addSpanning(form, g, y, connection.parameterButtons());
        y = addWide(form, g, y, label("URL Preview"), connection.txtUrlPreview());

        GridBagConstraints gFillerY = new GridBagConstraints();
        gFillerY.gridx = 0;
        gFillerY.gridy = y;
        gFillerY.gridwidth = 4;
        gFillerY.weighty = 1.0;
        form.add(new JPanel(), gFillerY);

        add(form, BorderLayout.CENTER);

        setupBrandAndModelListeners();
        setupNodeListener();
        setupConnectionListeners();
    }

    private JPanel parameterButtons() {
        return connection.parameterButtons();
    }

    private void setupBrandAndModelListeners() {
        cbBrand.addActionListener(e -> {
            cbModel.removeAllItems();
            txtProtocol.setText("");
            if (!edit) {
                txtUUID.setText("");
            }
            activeBuilder = null;
            connection.setBuilder(null);
            connection.reset();

            String selectedBrand = (String) cbBrand.getSelectedItem();
            if (devicesMap.containsKey(selectedBrand)) {
                for (DeviceModel dm : devicesMap.get(selectedBrand)) {
                    cbModel.addItem(dm);
                }
                cbModel.setSelectedIndex(0);
                generateUuidIfNeeded();
            }
        });

        cbModel.addActionListener(e -> {
            DeviceModel selectedDevice = (DeviceModel) cbModel.getSelectedItem();
            if (selectedDevice == null) {
                return;
            }
            txtProtocol.setText(selectedDevice.getProtocol());
            generateUuidIfNeeded();

            if (edit && !Objects.equals(modelOnOpen, selectedDevice.getModel())) {
                clearConnection();
            }

            activeBuilder = registry.forBrand(selectedDevice.getBrand());
            connection.setBuilder(activeBuilder);

            loadBuilderParams();
        });
    }

    private void generateUuidIfNeeded() {
        if (!edit) {
            txtUUID.setText(UUID.randomUUID().toString());
        }
    }

    private void setupNodeListener() {
        cbS88Node.addActionListener(e -> {
            Object selected = cbS88Node.getSelectedItem();
            String selectedNode = selected == null ? null : selected.toString();
            txtS88UUID.setText(selectedNode != null
                    ? s88NodesMap.getOrDefault(selectedNode, "") : "");
        });
    }

    private void setupConnectionListeners() {
        connection.conectarListeners();
    }

    private void initData() {
        for (String brand : DeviceCatalog.brands()) {
            devicesMap.put(brand, DeviceCatalog.from(brand));
        }
        if (devicesMap.size() != cbBrand.getItemCount() - 1) {
            LOGGER.warning("DeviceCatalog has " + devicesMap.size()
                    + " brands and the drop-down menu " + (cbBrand.getItemCount() - 1)
                    + "; Check it before touching either of the two.");
        }
    }

    private void loadS88Nodes() {
        cbS88Node.removeAllItems();
        cbS88Node.addItem(SELECT);
        cbS88Node.setEnabled(true);
        txtS88UUID.setText("");
        s88NodesMap.clear();

        Map<String, String> areas = PlantNodeLoader.areasOf(projectDir);
        if (areas != null) {
            for (Map.Entry<String, String> area : areas.entrySet()) {
                cbS88Node.addItem(area.getKey());
                s88NodesMap.put(area.getKey(), area.getValue());
            }
        }
        if (!s88NodesMap.containsKey(DummyPlant.ID)) {
            cbS88Node.addItem(DummyPlant.ID);
            s88NodesMap.put(DummyPlant.ID, DummyPlant.UUID);
        } else {
            String uuid = s88NodesMap.get(DummyPlant.ID);
            if (uuid == null || uuid.isBlank()) {
                s88NodesMap.put(DummyPlant.ID, DummyPlant.UUID);
            }
        }
    }

    private void loadBuilderParams() {
        connection.loadBuilderParams((DeviceModel) cbModel.getSelectedItem());
    }

    private String currentTransport() {
        return connection.transporteActual();
    }

    boolean transporteEditable() {
        return connection.transporteEditable();
    }


    private void loadForEdit(DeviceConfigData d) {
        cbBrand.setSelectedItem(d.getBrand());
        for (int i = 0; i < cbModel.getItemCount(); i++) {
            if (cbModel.getItemAt(i).getModel().equals(d.getModel())) {
                cbModel.setSelectedIndex(i);
                break;
            }
        }
        loadConnection(d.getSpecificParameters());
        restoreS88Node(d);
        txtS88UUID.setText(d.getS88Uuid());
        txtUUID.setText(d.getUuid());
        txtDeviceName.setText(d.getDeviceName());
        txtDeviceKey.setText(d.getDeviceKey());
        txtDescription.setText(d.getDescription());
        chkEnable.setSelected(d.isEnabled());

        cbBrand.setEnabled(false);
        cbS88Node.setEnabled(false);
    }

    private void loadConnection(String url) {
        connection.loadConnection(url, this);
    }

    private void restoreS88Node(DeviceConfigData d) {
        String node = d.getS88Node();
        if (node == null || node.isBlank()) {
            return;
        }
        boolean ready = false;
        for (int i = 0; i < cbS88Node.getItemCount(); i++) {
            if (node.equals(cbS88Node.getItemAt(i))) {
                ready = true;
                break;
            }
        }
        if (!ready) {
            cbS88Node.addItem(node);
        }
        cbS88Node.setSelectedItem(node);
    }

    private void clearConnection() {
        connection.clearConnection();
    }

    public void resetForm() {
        cbBrand.setSelectedIndex(0);
        cbModel.removeAllItems();
        txtProtocol.setText("");
        txtDeviceName.setText("");
        txtDeviceKey.setText("");
        txtDescription.setText("");
        txtUUID.setText("");
        chkEnable.setSelected(false);
        cbS88Node.setSelectedIndex(0);
        txtS88UUID.setText("");
        activeBuilder = null;
        connection.reset();
    }

    String selectedS88Node() {
        Object selected = cbS88Node.getSelectedItem();
        String text = selected == null ? "" : selected.toString().trim();
        return text.isEmpty() || SELECT.equals(text) ? null : text;
    }

    List<String> missingRequiredFields() {
        List<String> missing = new ArrayList<>();
        if (txtDeviceName.getText().trim().isEmpty()) {
            missing.add("Name");
        }
        if (txtDeviceKey.getText().trim().isEmpty()) {
            missing.add("Key");
        }
        if (cbModel.getSelectedItem() == null) {
            missing.add("Brand and Model");
        }
        if (connection.host().isEmpty()) {
            missing.add(connection.hostLabel());
        }
        if (selectedS88Node() == null) {
            missing.add("Node");
        }
        if (txtUUID.getText().trim().isEmpty()) {
            missing.add("UUID");
        }
        return missing;
    }

    public DeviceConfigData getDeviceConfigData() {
        List<String> missing = missingRequiredFields();
        if (!missing.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Check these fields before saving:\n\n  • " + String.join("\n  • ", missing),
                    "Data is missing", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String host = connection.host();
        if (activeBuilder != null) {
            activeBuilder.setHost(host);
            activeBuilder.setTransport(currentTransport());
            activeBuilder.setPort(connection.port());
        }

        Properties pDevice = new Properties();
        pDevice.put("brand", cbBrand.getSelectedItem());
        pDevice.put("model", ((DeviceModel) cbModel.getSelectedItem()).getModel());
        pDevice.put("protocol", txtProtocol.getText());
        pDevice.put("deviceName", txtDeviceName.getText().trim());
        pDevice.put("deviceKey", txtDeviceKey.getText().trim());
        pDevice.put("description", txtDescription.getText());
        pDevice.put("uuid", txtUUID.getText());
        pDevice.put("enable", chkEnable.isSelected());
        pDevice.put("s88Node", selectedS88Node());
        pDevice.put("s88Uuid", txtS88UUID.getText());
        pDevice.put("specificParameters", activeBuilder == null
                ? "" : activeBuilder.getSpecificParametersAsString());
        pDevice.put("md5", editing == null || editing.getMd5() == null
                ? "" : editing.getMd5());

        return new DeviceConfigData(pDevice);
    }

    void registrarNodoS88(String id, String uuid) {
        if (!s88NodesMap.containsKey(id)) {
            cbS88Node.addItem(id);
            s88NodesMap.put(id, uuid);
        }
        cbS88Node.setEnabled(true);
    }

    JComboBox<String> getCbMarca() {
        return cbBrand;
    }

    JComboBox<DeviceModel> getCbModel() {
        return cbModel;
    }

    JComboBox<String> getCbS88Node() {
        return cbS88Node;
    }

    JTextField getTxtDeviceName() {
        return txtDeviceName;
    }

    JTextField getTxtDeviceKey() {
        return txtDeviceKey;
    }

    JTextField getTxtHost() {
        return connection.txtHost();
    }

    JTextField getTxtHostSecundary() {
        return connection.txtHostSecundary();
    }

    JTextField getTxtPort() {
        return connection.txtPort();
    }

    JTextField getTxtUUID() {
        return txtUUID;
    }

    String currentUrl() {
        return connection.currentUrl();
    }

    private static final class SimpleDocumentListener implements DocumentListener {

        private final Runnable action;

        SimpleDocumentListener(Runnable action) {
            this.action = action;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            action.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            action.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            action.run();
        }
    }
}
