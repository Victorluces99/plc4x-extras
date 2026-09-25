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
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.table.DefaultTableModel;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.xml.HMICommunicationModel;
import org.netbeans.api.project.Project;
import org.openide.windows.WindowManager;

/**
 * Wizard de comunicación en 3 pasos: Dispositivo -&gt; Grupo/Área -&gt; Variable.
 * Reemplaza al antiguo editor por pestañas. El PvId de cada variable guarda el
 * uuid del Item (área) seleccionado (ver contrato en {@link CommConfigData.PvConfig}).
 */
public class CommunicationWizardDialog extends JDialog {

    private static final String STEP_DEVICE = "device";
    private static final String STEP_AREA = "area";
    private static final String STEP_PV = "pv";
    private static final String[] PV_TYPES = {"INT", "FLOAT", "STRING", "BOOLEAN", "DOUBLE"};

    private final Project project;
    private final HMICommunicationModel model;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    // Paso 1: Dispositivo
    private final JComboBox<DeviceConfigData> cbDevice = new JComboBox<>();
    private final JLabel lblDeviceInfo = new JLabel(" ");

    // Paso 2: Grupo / Área
    private final JLabel lblAreaStatus = new JLabel(" ");
    private final DefaultTableModel entityModel =
            new DefaultTableModel(new String[]{"Tipo", "Nombre", "Detalle"}, 0);
    private final JTable tbEntities = new JTable(entityModel);
    private final JTextField txtGroupName = new JTextField(14);
    private final JTextField txtGroupDescription = new JTextField(14);
    private final JTextField txtGroupScantime = new JTextField(8);
    private final JTextField txtItemName = new JTextField(14);
    private final JTextField txtItemDescription = new JTextField(14);
    private final JTextField txtItemTag = new JTextField(14);
    private final JComboBox<CommConfigData.GroupConfig> cbItemGroup = new JComboBox<>();

    // Navegación
    private final JButton btnBack2 = new JButton("< Atrás");
    private final JButton btnNext2 = new JButton("Continuar a variable >");
    private final JButton btnSave2 = new JButton("Guardar");
    private final JButton btnClose = new JButton("Cerrar");
    private final JButton btnBack3 = new JButton("< Atrás");
    private final JButton btnFinish = new JButton("Guardar y cerrar");
    private final JButton btnAddGroup = new JButton("Añadir grupo");
    private final JButton btnAddItem = new JButton("Añadir área");
    private final JButton btnAddPv = new JButton("Añadir variable");

    // Paso 3: Variable
    private final JComboBox<CommConfigData.ItemConfig> cbArea = new JComboBox<>();
    private final JTextField txtName = new JTextField(14);
    private final JComboBox<String> cbType = new JComboBox<>(PV_TYPES);
    private final JTextField txtOffset = new JTextField(10);
    private final JTextField txtDescriptor = new JTextField(14);
    private final JTextField txtScanTime = new JTextField(8);
    private final JCheckBox chkScanEnable = new JCheckBox("ScanEnable", true);
    private final JCheckBox chkWriteEnable = new JCheckBox("WriteEnable", false);
    private final JTextField txtDisplayLimitLow = new JTextField(8);
    private final JTextField txtDisplayLimitHigh = new JTextField(8);
    private final JTextField txtDisplayDescription = new JTextField(14);
    private final JTextField txtDisplayFormat = new JTextField(8);
    private final JTextField txtDisplayUnits = new JTextField(8);
    private final JTextField txtControlLimitLow = new JTextField(8);
    private final JTextField txtControlLimitHigh = new JTextField(8);
    private final JTextField txtControlMinStep = new JTextField(8);
    private final JLabel lblPvCount = new JLabel("Variables: 0");

    // Estado del wizard
    private DeviceConfigData selectedDevice;
    private final List<CommConfigData.GroupConfig> groups = new ArrayList<>();
    private final List<CommConfigData.ItemConfig> items = new ArrayList<>();
    private final List<CommConfigData.PvConfig> pvs = new ArrayList<>();
    private final Map<String, String> itemsGroup = new HashMap<>();

    public CommunicationWizardDialog(Project project, String deviceUuid) {
        super(WindowManager.getDefault().getMainWindow(), "Nueva Comunicación", ModalityType.APPLICATION_MODAL);
        this.project = project;
        this.model = project != null ? project.getLookup().lookup(HMICommunicationModel.class) : null;

        buildUi();
        loadDevices(deviceUuid);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getRootPane().registerKeyboardAction(e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(getOwner());
    }

    private void buildUi() {
        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        setContentPane(content);

        cardPanel.add(buildStepDevice(), STEP_DEVICE);
        cardPanel.add(buildStepArea(), STEP_AREA);
        cardPanel.add(buildStepPv(), STEP_PV);
        content.add(cardPanel, BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------- paso 1

    private JPanel buildStepDevice() {
        cbDevice.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                label.setText(value instanceof DeviceConfigData d
                        ? d.getDeviceName() + "  [" + d.getProtocol() + "]"
                        : " ");
                return label;
            }
        });
        cbDevice.addActionListener(e -> onDeviceSelected());

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JPanel inner = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 8, 6, 8);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;

        JLabel title = new JLabel("Paso 1 de 3 — Dispositivo");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 2;
        inner.add(title, g);

        g.gridy = 1;
        inner.add(new JLabel("Seleccione el dispositivo para la comunicación:"), g);

        g.gridy = 2; g.gridwidth = 2; g.weightx = 1.0;
        inner.add(cbDevice, g);
        g.gridwidth = 1;

        g.gridy = 3; g.gridx = 0; g.gridwidth = 2; g.weightx = 1.0;
        inner.add(lblDeviceInfo, g);

        g.gridy = 4;
        JLabel hint = new JLabel("<html><i>Si no aparece su dispositivo, créelo con "
                + "«Crear Dispositivo», cierre este asistente y vuelva a abrirlo.</i></html>");
        inner.add(hint, g);

        panel.add(inner, BorderLayout.CENTER);

        JButton btnCancel = new JButton("Cancelar");
        JButton btnNext = new JButton("Siguiente >");
        btnCancel.addActionListener(e -> dispose());
        btnNext.addActionListener(e -> {
            if (selectedDevice == null) {
                JOptionPane.showMessageDialog(this,
                        "Seleccione un dispositivo para continuar.",
                        "Sin dispositivo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            showStep(STEP_AREA);
        });
        panel.add(nav(btnCancel, btnNext), BorderLayout.SOUTH);
        return panel;
    }

    // ---------------------------------------------------------------- paso 2

    private JPanel buildStepArea() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel center = new JPanel(new BorderLayout(6, 6));
        lblAreaStatus.setBorder(BorderFactory.createEmptyBorder(0, 2, 8, 2));
        center.add(lblAreaStatus, BorderLayout.NORTH);
        center.add(new JScrollPane(tbEntities), BorderLayout.CENTER);

        JPanel forms = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 2, 8);
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel grpTitle = new JLabel("Nuevo grupo de escaneo");
        grpTitle.setFont(grpTitle.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 4; g.weightx = 0.0;
        forms.add(grpTitle, g); g.gridwidth = 1;

        g.gridy = 1;
        g.gridx = 0; g.weightx = 0.0; forms.add(new JLabel("Nombre:"), g);
        g.gridx = 1; g.weightx = 0.5; forms.add(txtGroupName, g);
        g.gridx = 2; g.weightx = 0.0; forms.add(new JLabel("Descripción:"), g);
        g.gridx = 3; g.weightx = 0.5; forms.add(txtGroupDescription, g);

        g.gridy = 2;
        g.gridx = 0; g.weightx = 0.0; forms.add(new JLabel("Scantime (ms):"), g);
        g.gridx = 1; g.weightx = 0.5; forms.add(txtGroupScantime, g);
        g.gridx = 2; g.weightx = 0.0; g.fill = GridBagConstraints.NONE; g.anchor = GridBagConstraints.WEST;
        forms.add(btnAddGroup, g);
        g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.WEST;
        btnAddGroup.addActionListener(e -> addGroup());

        JLabel itemTitle = new JLabel("Nueva área de memoria (del dispositivo seleccionado)");
        itemTitle.setFont(itemTitle.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        g.gridy = 3; g.gridx = 0; g.gridwidth = 4; g.weightx = 0.0;
        forms.add(itemTitle, g); g.gridwidth = 1;

        g.gridy = 4;
        g.gridx = 0; g.weightx = 0.0; forms.add(new JLabel("Nombre:"), g);
        g.gridx = 1; g.weightx = 0.5; forms.add(txtItemName, g);
        g.gridx = 2; g.weightx = 0.0; forms.add(new JLabel("Descripción:"), g);
        g.gridx = 3; g.weightx = 0.5; forms.add(txtItemDescription, g);

        g.gridy = 5;
        g.gridx = 0; g.weightx = 0.0; forms.add(new JLabel("Tag:"), g);
        g.gridx = 1; g.weightx = 0.5; forms.add(txtItemTag, g);
        g.gridx = 2; g.weightx = 0.0; forms.add(new JLabel("Grupo:"), g);
        g.gridx = 3; g.weightx = 0.5; forms.add(cbItemGroup, g);

        g.gridy = 6; g.gridx = 3; g.weightx = 0.0; g.fill = GridBagConstraints.NONE; g.anchor = GridBagConstraints.EAST;
        forms.add(btnAddItem, g);
        g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.WEST;
        btnAddItem.addActionListener(e -> addItem());

        cbItemGroup.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                label.setText(value instanceof CommConfigData.GroupConfig grp
                        ? grp.getName() + "  (scan " + grp.getScantime() + " ms)"
                        : " ");
                return label;
            }
        });

        center.add(forms, BorderLayout.SOUTH);

        btnBack2.addActionListener(e -> showStep(STEP_DEVICE));
        btnNext2.addActionListener(e -> showStep(STEP_PV));
        btnSave2.addActionListener(e -> saveNow());
        btnClose.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.add(nav(btnBack2), BorderLayout.WEST);
        buttons.add(nav(btnSave2, btnClose, btnNext2), BorderLayout.EAST);
        panel.add(center, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    // ---------------------------------------------------------------- paso 3

    private JPanel buildStepPv() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 8, 5, 8);
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1.0;

        JLabel title = new JLabel("Paso 3 de 3 — Variable de proceso");
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        g.gridx = 0; g.gridy = 0; g.gridwidth = 4;
        form.add(title, g);
        g.gridwidth = 1;

        int row = 0;
        addLabel(form, g, ++row, "Área (PvId):");
        g.gridx = 1; g.gridwidth = 3;
        form.add(cbArea, g); g.gridwidth = 1;

        addLabel(form, g, ++row, "Nombre:");
        g.gridx = 1; g.gridwidth = 3; form.add(txtName, g); g.gridwidth = 1;

        addLabel(form, g, ++row, "Tipo:");
        g.gridx = 1; g.gridwidth = 1; form.add(cbType, g);
        g.gridx = 2; form.add(new JLabel("Offset:"), g);
        g.gridx = 3; form.add(txtOffset, g);

        addLabel(form, g, ++row, "Descriptor:");
        g.gridx = 1; g.gridwidth = 1; form.add(txtDescriptor, g);
        g.gridx = 2; form.add(new JLabel("ScanTime:"), g);
        g.gridx = 3; form.add(txtScanTime, g);

        addLabel(form, g, ++row, "Opciones:");
        g.gridx = 1; form.add(chkScanEnable, g);
        g.gridx = 2; form.add(chkWriteEnable, g);

        addLabel(form, g, ++row, "Display Low/High:");
        g.gridx = 1; form.add(txtDisplayLimitLow, g);
        g.gridx = 2; form.add(txtDisplayLimitHigh, g);

        addLabel(form, g, ++row, "Display Desc.:");
        g.gridx = 1; g.gridwidth = 3; form.add(txtDisplayDescription, g); g.gridwidth = 1;

        addLabel(form, g, ++row, "Display Format/Units:");
        g.gridx = 1; form.add(txtDisplayFormat, g);
        g.gridx = 2; form.add(txtDisplayUnits, g);

        addLabel(form, g, ++row, "Control Low/High:");
        g.gridx = 1; form.add(txtControlLimitLow, g);
        g.gridx = 2; form.add(txtControlLimitHigh, g);

        addLabel(form, g, ++row, "Control MinStep:");
        g.gridx = 1; form.add(txtControlMinStep, g);

        ++row;
        JPanel addRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        addRow.add(btnAddPv);
        addRow.add(lblPvCount);
        g.gridx = 0; g.gridy = row; g.gridwidth = 4;
        form.add(addRow, g); g.gridwidth = 1;

        cbArea.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                label.setText(value instanceof CommConfigData.ItemConfig i
                        ? i.getName() + "  (" + i.getTag() + ")"
                        : " ");
                return label;
            }
        });

        btnAddPv.addActionListener(e -> addPv());
        btnBack3.addActionListener(e -> showStep(STEP_AREA));

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> dispose());
        btnFinish.addActionListener(e -> saveAndClose());

        panel.add(new JScrollPane(form), BorderLayout.CENTER);
        panel.add(nav(btnBack3, btnCancel, btnFinish), BorderLayout.SOUTH);
        return panel;
    }

    // ------------------------------------------------------------- helpers ui

    private static JPanel nav(JButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        for (JButton b : buttons) {
            panel.add(b);
        }
        return panel;
    }

    private static void addLabel(JPanel target, GridBagConstraints g, int y, String text) {
        g.gridx = 0;
        g.gridy = y;
        g.gridwidth = 1;
        g.weightx = 0.0;
        target.add(new JLabel(text), g);
    }

    // ---------------------------------------------------------------- logica

    private void loadDevices(String deviceUuid) {
        refreshDevices();
        if (deviceUuid != null) {
            for (int i = 0; i < cbDevice.getItemCount(); i++) {
                DeviceConfigData d = cbDevice.getItemAt(i);
                if (d.getUuid().equals(deviceUuid)) {
                    cbDevice.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void refreshDevices() {
        DeviceConfigData prev = selectedDevice;
        cbDevice.removeAllItems();
        if (model == null) {
            return;
        }
        for (DeviceConfigData device : model.getDevices()) {
            cbDevice.addItem(device);
        }
        if (prev != null) {
            for (int i = 0; i < cbDevice.getItemCount(); i++) {
                if (cbDevice.getItemAt(i).getUuid().equals(prev.getUuid())) {
                    cbDevice.setSelectedIndex(i);
                    break;
                }
            }
        }
        onDeviceSelected();
    }

    private void onDeviceSelected() {
        selectedDevice = (DeviceConfigData) cbDevice.getSelectedItem();
        if (selectedDevice == null) {
            lblDeviceInfo.setText(" ");
            groups.clear();
            items.clear();
            pvs.clear();
            return;
        }
        lblDeviceInfo.setText(selectedDevice.getBrand() + " / " + selectedDevice.getModel()
                + " / " + selectedDevice.getProtocol());
        loadComms(selectedDevice.getUuid());
    }

    private void loadComms(String deviceUuid) {
        groups.clear();
        items.clear();
        pvs.clear();
        itemsGroup.clear();
        if (model == null) {
            return;
        }
        CommConfigData comms = model.getComms(deviceUuid);
        if (comms == null) {
            refreshAreaView();
            return;
        }
        if (comms.getGroups() != null) {
            groups.addAll(comms.getGroups());
        }
        if (comms.getItems() != null) {
            items.addAll(comms.getItems());
        }
        if (comms.getPvs() != null) {
            pvs.addAll(comms.getPvs());
        }
        refreshAreaView();
    }

    private void refreshAreaView() {
        entityModel.setRowCount(0);
        for (CommConfigData.GroupConfig g : groups) {
            entityModel.addRow(new Object[]{
                "Grupo", g.getName(), "scan " + g.getScantime() + " ms"
            });
        }
        for (CommConfigData.ItemConfig i : items) {
            String groupName = groupNameOf(i.getUuid());
            String detail = groupName.isEmpty() ? i.getTag() : i.getTag() + "  →  " + groupName;
            entityModel.addRow(new Object[]{
                "Área", i.getName(), detail
            });
        }
        refreshAreaStatus();
        refreshPvAreas();
        refreshGroupCombo();
        btnNext2.setEnabled(!items.isEmpty());
    }

    private String groupNameOf(String itemUuid) {
        String groupUuid = itemsGroup.get(itemUuid);
        if (groupUuid == null) {
            return "";
        }
        for (CommConfigData.GroupConfig g : groups) {
            if (g.getUuid().equals(groupUuid)) {
                return g.getName();
            }
        }
        return "";
    }

    private void refreshGroupCombo() {
        CommConfigData.GroupConfig sel = (CommConfigData.GroupConfig) cbItemGroup.getSelectedItem();
        cbItemGroup.removeAllItems();
        for (CommConfigData.GroupConfig g : groups) {
            cbItemGroup.addItem(g);
        }
        if (sel != null) {
            for (int i = 0; i < cbItemGroup.getItemCount(); i++) {
                if (cbItemGroup.getItemAt(i).getUuid().equals(sel.getUuid())) {
                    cbItemGroup.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void refreshAreaStatus() {
        if (items.isEmpty()) {
            lblAreaStatus.setText("<html><b>El dispositivo no tiene áreas de memoria.</b> "
                    + "Cree un grupo de escaneo y al menos un área para poder añadir variables.</html>");
        } else {
            lblAreaStatus.setText("Grupos: " + groups.size() + "  ·  Áreas: " + items.size()
                    + "  ·  Variables: " + pvs.size());
        }
    }

    private void refreshPvAreas() {
        CommConfigData.ItemConfig sel = (CommConfigData.ItemConfig) cbArea.getSelectedItem();
        cbArea.removeAllItems();
        for (CommConfigData.ItemConfig i : items) {
            cbArea.addItem(i);
        }
        if (sel != null) {
            for (int i = 0; i < cbArea.getItemCount(); i++) {
                if (cbArea.getItemAt(i).getUuid().equals(sel.getUuid())) {
                    cbArea.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void addGroup() {
        String name = txtGroupName.getText().trim();
        String scantime = txtGroupScantime.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese el nombre del grupo de escaneo.",
                    "Campo incompleto", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (scantime.isEmpty()) {
            scantime = "500";
        }
        groups.add(new CommConfigData.GroupConfig(
                UUID.randomUUID().toString(), name, txtGroupDescription.getText().trim(),
                scantime, true, "md5_group_hash"));
        txtGroupName.setText("");
        txtGroupDescription.setText("");
        txtGroupScantime.setText("");
        refreshAreaView();
    }

    private void addItem() {
        CommConfigData.GroupConfig group = (CommConfigData.GroupConfig) cbItemGroup.getSelectedItem();
        if (group == null) {
            JOptionPane.showMessageDialog(this,
                    "Cree primero un grupo de escaneo; el área debe pertenecer a un grupo.",
                    "Falta grupo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String name = txtItemName.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese el nombre del área de memoria.",
                    "Campo incompleto", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String itemUuid = UUID.randomUUID().toString();
        items.add(new CommConfigData.ItemConfig(
                itemUuid, name, txtItemDescription.getText().trim(),
                txtItemTag.getText().trim(), true, "md5_item_hash"));
        itemsGroup.put(itemUuid, group.getUuid());
        txtItemName.setText("");
        txtItemDescription.setText("");
        txtItemTag.setText("");
        refreshAreaView();
    }

    private void addPv() {
        CommConfigData.ItemConfig area = (CommConfigData.ItemConfig) cbArea.getSelectedItem();
        String name = txtName.getText().trim();
        if (area == null) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un área de memoria (PvId) para la variable.",
                    "Falta área", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese el nombre de la variable.",
                    "Campo incompleto", JOptionPane.WARNING_MESSAGE);
            return;
        }
        pvs.add(new CommConfigData.PvConfig(
                UUID.randomUUID().toString(),
                name,
                String.valueOf(cbType.getSelectedItem()),
                area.getUuid(),
                txtOffset.getText().trim(),
                txtDescriptor.getText().trim(),
                txtScanTime.getText().trim(),
                chkScanEnable.isSelected(),
                chkWriteEnable.isSelected(),
                txtDisplayLimitLow.getText().trim(),
                txtDisplayLimitHigh.getText().trim(),
                txtDisplayDescription.getText().trim(),
                txtDisplayFormat.getText().trim(),
                txtDisplayUnits.getText().trim(),
                txtControlLimitLow.getText().trim(),
                txtControlLimitHigh.getText().trim(),
                txtControlMinStep.getText().trim(),
                "md5_pv_hash"));
        txtName.setText("");
        txtOffset.setText("");
        txtDescriptor.setText("");
        txtScanTime.setText("");
        lblPvCount.setText("Variables: " + pvs.size());
        refreshAreaStatus();
    }

    private void saveNow() {
        if (selectedDevice == null || model == null) {
            return;
        }
        persist();
        JOptionPane.showMessageDialog(this,
                "Grupos y áreas guardados en comunicacion.xml.",
                "Guardado", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveAndClose() {
        if (selectedDevice == null || model == null) {
            return;
        }
        persist();
        JOptionPane.showMessageDialog(this,
                "Comunicación guardada en comunicacion.xml.",
                "Éxito", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    private void persist() {
        CommConfigData config = new CommConfigData(
                selectedDevice.getDeviceName(),
                new ArrayList<>(groups),
                new ArrayList<>(items),
                new ArrayList<>(pvs));
        model.upsertComms(selectedDevice.getUuid(), config);
        model.save();
    }

    private void showStep(String step) {
        cardLayout.show(cardPanel, step);
        if (STEP_AREA.equals(step)) {
            btnNext2.setEnabled(!items.isEmpty());
            refreshAreaView();
        } else if (STEP_PV.equals(step)) {
            refreshPvAreas();
        }
    }
}