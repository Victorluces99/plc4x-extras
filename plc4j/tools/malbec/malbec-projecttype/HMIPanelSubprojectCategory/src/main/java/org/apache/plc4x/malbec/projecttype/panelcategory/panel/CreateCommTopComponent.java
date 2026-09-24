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

import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.xml.HMICommunicationModel;
import org.netbeans.api.project.Project;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

@ConvertAsProperties(
        dtd = "-//org.apache.plc4x.malbec.projecttype.panelcategory.panel//DeviceManager//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "DeviceManagerTopComponent",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.apache.plc4x.malbec.projecttype.panelcategory.panel.DeviceManagerTopComponent")
@ActionReference(path = "Menu/Window", position = 333)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_DeviceManagerAction",
        preferredID = "DeviceManagerTopComponent"
)
@Messages({
    "CTL_DeviceManagerAction=Device Manager",
    "CTL_DeviceManagerTopComponent=Device Configuration Window",
    "HINT_DeviceManagerTopComponent=This is a Device Configuration window"
})
public final class CreateCommTopComponent extends TopComponent {

    public interface OnSaveListener {

        void onSave(CommConfigData config);
    }

    private JTextField txtDeviceName;
    private JTabbedPane tabbedPane;
    private TablaTabPanel tabGroup;
    private TablaTabPanel tabItem;
    private TablaTabPanel tabPv;
    private InstanceContent content = new InstanceContent();
    private String projectPath;
    private Project project;
    private String deviceUuid;
    private OnSaveListener saveListener;
    private boolean hasUnsavedChanges = false;

    public CreateCommTopComponent() {
        setName(Bundle.CTL_DeviceManagerTopComponent());
        setToolTipText(Bundle.HINT_DeviceManagerTopComponent());
        initComponentsCustom();
        associateLookup(new AbstractLookup(content));
    }

    public void setOnSaveListener(OnSaveListener listener) {
        this.saveListener = listener;
    }

    private void initComponentsCustom() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // --- PANEL SUPERIOR (Device Name) ---
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel lblDeviceName = new JLabel("Device name:");
        txtDeviceName = new JTextField("DefaultDevice", 35);
        txtDeviceName.setEditable(false);
        topPanel.add(lblDeviceName);
        topPanel.add(txtDeviceName);
        add(topPanel, BorderLayout.NORTH);

        // --- PESTAÑAS (TabbedPane) ---
        tabbedPane = new JTabbedPane();

        String[] optionsEnable = {"TRUE", "FALSE"};
        String[] optionsPvType = {"INT", "FLOAT", "STRING", "BOOLEAN", "DOUBLE"};

        // Pestaña 1: Group
        List<ColumnConfig> groupCols = new ArrayList<>();
        groupCols.add(new ColumnConfig("GroupName", ColumnConfig.ColumnType.TEXT));
        groupCols.add(new ColumnConfig("GroupDescription", ColumnConfig.ColumnType.TEXT));
        groupCols.add(new ColumnConfig("GroupScantime", ColumnConfig.ColumnType.TEXT));
        groupCols.add(new ColumnConfig("GroupEnable", ColumnConfig.ColumnType.COMBOBOX, optionsEnable));
        tabGroup = new TablaTabPanel(groupCols);
        tabbedPane.addTab("Groups (Pestaña 1)", tabGroup);

        // Pestaña 2: Item
        List<ColumnConfig> itemCols = new ArrayList<>();
        itemCols.add(new ColumnConfig("ItemName", ColumnConfig.ColumnType.TEXT));
        itemCols.add(new ColumnConfig("ItemDescription", ColumnConfig.ColumnType.TEXT));
        itemCols.add(new ColumnConfig("ItemTag", ColumnConfig.ColumnType.TEXT));
        itemCols.add(new ColumnConfig("ItemEnable", ColumnConfig.ColumnType.COMBOBOX, optionsEnable));
        tabItem = new TablaTabPanel(itemCols);
        tabbedPane.addTab("Items (Pestaña 2)", tabItem);

        // Pestaña 3: PV
        List<ColumnConfig> pvCols = new ArrayList<>();
        pvCols.add(new ColumnConfig("PvName", ColumnConfig.ColumnType.TEXT));
        pvCols.add(new ColumnConfig("PvType", ColumnConfig.ColumnType.COMBOBOX, optionsPvType));
        pvCols.add(new ColumnConfig("PvId", ColumnConfig.ColumnType.TEXT));
        pvCols.add(new ColumnConfig("PvOffset", ColumnConfig.ColumnType.TEXT));
        pvCols.add(new ColumnConfig("PvDescriptor", ColumnConfig.ColumnType.TEXT));
        pvCols.add(new ColumnConfig("PvScanTime", ColumnConfig.ColumnType.TEXT));
        pvCols.add(new ColumnConfig("PvScanEnable", ColumnConfig.ColumnType.COMBOBOX, optionsEnable));
        pvCols.add(new ColumnConfig("PvWriteEnable", ColumnConfig.ColumnType.COMBOBOX, optionsEnable));
        pvCols.add(new ColumnConfig("PvDisplayLimitLow", ColumnConfig.ColumnType.TEXT));
        pvCols.add(new ColumnConfig("PvDisplayLimitHigh", ColumnConfig.ColumnType.TEXT));
        pvCols.add(new ColumnConfig("PvDisplayDescription", ColumnConfig.ColumnType.TEXT));
        pvCols.add(new ColumnConfig("PvDisplayFormat", ColumnConfig.ColumnType.TEXT));
        pvCols.add(new ColumnConfig("PvDisplayUnits", ColumnConfig.ColumnType.TEXT));
        pvCols.add(new ColumnConfig("PvControlLimitLow", ColumnConfig.ColumnType.TEXT));
        pvCols.add(new ColumnConfig("PvControlLimitHigh", ColumnConfig.ColumnType.TEXT));
        pvCols.add(new ColumnConfig("PvControlMinStep", ColumnConfig.ColumnType.TEXT));
        tabPv = new TablaTabPanel(pvCols);
        tabbedPane.addTab("PVs (Pestaña 3)", tabPv);

        add(tabbedPane, BorderLayout.CENTER);

        // --- PANEL INFERIOR (Cancel, Apply, Ok) ---
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton btnAddAll = new JButton("Add Row (All)");
        JButton btnCancel = new JButton("Cancel");
        JButton btnApply = new JButton("Apply");
        JButton btnOk = new JButton("Ok");

        btnAddAll.addActionListener(e -> addRowToAllTabs());
        btnCancel.addActionListener(e -> onCancel());
        btnApply.addActionListener(e -> onApply());
        btnOk.addActionListener(e -> onOk());

        bottomPanel.add(btnAddAll);
        bottomPanel.add(btnCancel);
        bottomPanel.add(btnApply);
        bottomPanel.add(btnOk);

        add(bottomPanel, BorderLayout.SOUTH);
        
        javax.swing.event.TableModelListener tableListener = e -> markAsModified();
        tabGroup.getTableModel().addTableModelListener(tableListener);
        tabItem.getTableModel().addTableModelListener(tableListener);
        tabPv.getTableModel().addTableModelListener(tableListener);

        // 2. Escuchar cambios en el nombre del dispositivo
        txtDeviceName.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { markAsModified(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { markAsModified(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { markAsModified(); }
        });
    }

    private void addRowToAllTabs() {
        // Agrega una nueva fila en cada una de las pestañas
        tabGroup.addNewRow();
        tabItem.addNewRow();
        tabPv.addNewRow();

        // Opcional: Seleccionar y hacer visible la fila recién creada en la pestaña actual
        int selectedIndex = tabbedPane.getSelectedIndex();
        TablaTabPanel currentTab = (TablaTabPanel) tabbedPane.getComponentAt(selectedIndex);
        JTable currentTable = currentTab.getTable();
        int lastRow = currentTable.getRowCount() - 1;

        if (lastRow >= 0) {
            currentTable.setRowSelectionInterval(lastRow, lastRow);
            currentTable.scrollRectToVisible(currentTable.getCellRect(lastRow, 0, true));
        }
    }

    private boolean validateAllTabs() {
        TablaTabPanel[] tabs = {tabGroup, tabItem, tabPv};
        String[] tabNames = {"Groups (Pestaña 1)", "Items (Pestaña 2)", "PVs (Pestaña 3)"};

        for (int t = 0; t < tabs.length; t++) {
            TablaTabPanel tab = tabs[t];
            JTable table = tab.getTable();

            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }

            DynamicTableModel model = tab.getTableModel();
            for (int r = 0; r < model.getRowCount(); r++) {
                for (int c = 0; c < model.getColumnCount(); c++) {
                    Object val = model.getValueAt(r, c);
                    if (val == null || val.toString().trim().isEmpty()) {
                        tabbedPane.setSelectedIndex(t);
                        table.changeSelection(r, c, false, false);

                        JOptionPane.showMessageDialog(this,
                                "Por favor complete todos los campos obligatorios.\n"
                                + "Pestaña: " + tabNames[t] + "\n"
                                + "Fila: " + (r + 1) + ", Columna: '" + model.getColumnName(c) + "'",
                                "Campo Incompleto",
                                JOptionPane.WARNING_MESSAGE);
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void onCancel() {
        this.close();
    }

    public void setProjectPath(String path) {
        this.projectPath = path;
    }

    /**
     * Asocia el proyecto y el dispositivo activo (por uuid) para el que se
     * editan los grupos/items/pvs. Precarga la configuración existente.
     */
    public void setDeviceTarget(Project project, String deviceUuid) {
        this.project = project;
        this.deviceUuid = deviceUuid;
        if (project == null) {
            return;
        }
        HMICommunicationModel model = project.getLookup().lookup(HMICommunicationModel.class);
        if (model == null) {
            return;
        }
        DeviceConfigData device = (deviceUuid != null)
                ? model.findByUuid(deviceUuid)
                : (model.getDevices().isEmpty() ? null : model.getDevices().get(0));
        if (device != null) {
            this.deviceUuid = device.getUuid();
            txtDeviceName.setText(device.getDeviceName());
        }
        preload(model.getComms(this.deviceUuid));
    }

    private void preload(CommConfigData config) {
        resetTables();
        if (config == null) {
            return;
        }
        DynamicTableModel groupModel = tabGroup.getTableModel();
        for (CommConfigData.GroupConfig g : config.getGroups()) {
            groupModel.addRow(new Object[]{
                g.getName(), g.getDescription(), g.getScantime(),
                g.isEnable() ? "TRUE" : "FALSE"
            });
        }
        DynamicTableModel itemModel = tabItem.getTableModel();
        for (CommConfigData.ItemConfig i : config.getItems()) {
            itemModel.addRow(new Object[]{
                i.getName(), i.getDescription(), i.getTag(), i.isEnable() ? "TRUE" : "FALSE"
            });
        }
        DynamicTableModel pvModel = tabPv.getTableModel();
        for (CommConfigData.PvConfig p : config.getPvs()) {
            pvModel.addRow(new Object[]{
                p.getName(), p.getType(), p.getId(), p.getOffset(), p.getDescriptor(),
                p.getScanTime(), p.isScanEnable() ? "TRUE" : "FALSE",
                p.isWriteEnable() ? "TRUE" : "FALSE", p.getDisplayLimitLow(),
                p.getDisplayLimitHigh(), p.getDisplayDescription(), p.getDisplayFormat(),
                p.getDisplayUnits(), p.getControlLimitLow(), p.getControlLimitHigh(),
                p.getControlMinStep()
            });
        }
    }

    private void resetTables() {
        clearModel(tabGroup.getTableModel());
        clearModel(tabItem.getTableModel());
        clearModel(tabPv.getTableModel());
    }

    private static void clearModel(DynamicTableModel model) {
        for (int r = model.getRowCount() - 1; r >= 0; r--) {
            model.removeRow(r);
        }
    }

    public void markAsModified() {
        this.hasUnsavedChanges = true;
    }

    private void onApply() {
        if (validateAllTabs()) {
            CommConfigData config = new CommConfigData(
                    txtDeviceName.getText(),
                    getGroupData(),
                    getItemData(),
                    getPvData()
            );

            content.set(Collections.singleton(config), null);

            if (saveListener != null) {
                saveListener.onSave(config);
            }
            hasUnsavedChanges = false;

            JOptionPane.showMessageDialog(this, "Configuración guardada con éxito en comunicacion.xml.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void onOk() {
        if (validateAllTabs()) {
            if (hasUnsavedChanges) {
                onApply();
            }
            this.close();
        }
    }

    public String UUIDGenerator() {
        return UUID.randomUUID().toString();
    }

    public List<CommConfigData.GroupConfig> getGroupData() {
        List<CommConfigData.GroupConfig> list = new ArrayList<>();
        DynamicTableModel model = tabGroup.getTableModel();
        for (int r = 0; r < model.getRowCount(); r++) {
            String uuid = UUIDGenerator();
            String name = (String) model.getValueAt(r, 0);
            String desc = (String) model.getValueAt(r, 1);
            String scantime = (String) model.getValueAt(r, 2);
            boolean enable = "TRUE".equalsIgnoreCase((String) model.getValueAt(r, 3));
            String md5 = "md5_group_hash";

            list.add(new CommConfigData.GroupConfig(uuid, name, desc, scantime, enable, md5));
        }
        return list;
    }

    public List<CommConfigData.ItemConfig> getItemData() {
        List<CommConfigData.ItemConfig> list = new ArrayList<>();
        DynamicTableModel model = tabItem.getTableModel();
        for (int r = 0; r < model.getRowCount(); r++) {
            String uuid = UUIDGenerator();
            String name = (String) model.getValueAt(r, 0);
            String desc = (String) model.getValueAt(r, 1);
            String tag = (String) model.getValueAt(r, 2);
            boolean enable = "TRUE".equalsIgnoreCase((String) model.getValueAt(r, 3));
            String md5 = "md5_item_hash";

            list.add(new CommConfigData.ItemConfig(uuid, name, desc, tag, enable, md5));
        }
        return list;
    }

    public List<CommConfigData.PvConfig> getPvData() {
        List<CommConfigData.PvConfig> list = new ArrayList<>();
        DynamicTableModel model = tabPv.getTableModel();
        for (int r = 0; r < model.getRowCount(); r++) {
            list.add(new CommConfigData.PvConfig(
                    UUIDGenerator(),
                    (String) model.getValueAt(r, 0),
                    (String) model.getValueAt(r, 1),
                    (String) model.getValueAt(r, 2),
                    (String) model.getValueAt(r, 3),
                    (String) model.getValueAt(r, 4),
                    (String) model.getValueAt(r, 5),
                    "TRUE".equalsIgnoreCase((String) model.getValueAt(r, 6)),
                    "TRUE".equalsIgnoreCase((String) model.getValueAt(r, 7)),
                    (String) model.getValueAt(r, 8),
                    (String) model.getValueAt(r, 9),
                    (String) model.getValueAt(r, 10),
                    (String) model.getValueAt(r, 11),
                    (String) model.getValueAt(r, 12),
                    (String) model.getValueAt(r, 13),
                    (String) model.getValueAt(r, 14),
                    (String) model.getValueAt(r, 15),
                    "md5_pv_hash"
            ));
        }
        return list;
    }

    void writeProperties(java.util.Properties p) {
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p) {
    }
}
