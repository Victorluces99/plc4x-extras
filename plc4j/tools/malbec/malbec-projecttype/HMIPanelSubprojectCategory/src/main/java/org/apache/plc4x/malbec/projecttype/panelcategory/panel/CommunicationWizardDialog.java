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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.xml.HMICommunicationModel;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.windows.WindowManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/*
   Wizard de comunicación en 3 pasos: Dispositivo, Grupo/Área, Variable.
   El PvId de cada variable guarda el uuid del Item (área) seleccionado
*/
public class CommunicationWizardDialog extends JDialog {

    private static final String STEP_DEVICE = "device";
    private static final String STEP_AREA = "area";
    private static final String STEP_PV = "pv";
    private static final String SIN_GRUPO = "SIN GRUPO";

    private final Project project;
    private final HMICommunicationModel model;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);

    // Paso 1: Dispositivo
    private final JComboBox<DeviceConfigData> cbDevice = new JComboBox<>();
    private final JLabel lblDeviceInfo = new JLabel(" ");

    // Paso 2: Grupo / Área
    private final DefaultTableModel groupModel = new DefaultTableModel(new String[]{
        "Nombre", "Descripción", "Scantime (ms)", "Enable", "UUID"}, 0);
    private final JTable tbGroups = new JTable(groupModel);
    private final AreaTableModel itemModel = new AreaTableModel(new String[]{
            "Nombre", "Descripción", "Tag", "Grupo", "Enable", "UUID"});
    private final JTable tbItems = new JTable(itemModel);
    private final JTextField txtGroupName = new JTextField(14);
    private final JTextField txtGroupDescription = new JTextField(14);
    private final JComboBox<String> cbGroupScantime = new JComboBox<>();
    private final JCheckBox chkGroupEnable = new JCheckBox("Enable", true);
    private final JCheckBox chkItemEnable = new JCheckBox("Enable", true);
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
    private final JButton btnAddGroup = new JButton("Añadir");
    private final JButton btnUpdateGroup = new JButton("Modificar");
    private final JButton btnDeleteGroup = new JButton("Eliminar");
    private final JButton btnCancelGroup = new JButton("Cancelar");
    private final JButton btnAddItem = new JButton("Añadir");
    private final JButton btnUpdateItem = new JButton("Modificar");
    private final JButton btnDeleteItem = new JButton("Eliminar");
    private final JButton btnCancelItem = new JButton("Cancelar");
    private final JLabel lblGroupFormTitle = new JLabel("Nuevo grupo de escaneo");
    private final JLabel lblItemFormTitle =
            new JLabel("Nueva área de memoria (del dispositivo seleccionado)");
    private final JButton btnSavePv = new JButton("Guardar");

    // Paso 3: Variable
    private final JComboBox<CommConfigData.ItemConfig> cbArea = new JComboBox<>();
    private final JComboBox<PlantVariable> cbVariable = new JComboBox<>();
    private final DefaultTableModel pvModel = new DefaultTableModel(new String[]{
        "Variable", "Área", "PvId", "Tipo", "Offset", "Descriptor", "ScanTime",
        "Scan", "Write", "Lím. bajo", "Lím. alto", "Descripción", "Formato", "Unidades",
        "Ctrl. bajo", "Ctrl. alto", "Ctrl. MinStep", "Ruta S88", "UUID", "Md5"}, 0);
    private final JTable tbPvs = new JTable(pvModel);
    private final JComboBox<String> cbType = new JComboBox<>();
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
    private final JLabel lblTypeLock = new JLabel(" ");

    // Estado del wizard
    private DeviceConfigData selectedDevice;
    private final List<CommConfigData.GroupConfig> groups = new ArrayList<>();
    private final List<CommConfigData.ItemConfig> items = new ArrayList<>();
    private final List<CommConfigData.PvConfig> pvs = new ArrayList<>();
    private final Map<String, String> itemsGroup = new HashMap<>();
    private boolean pvColumnsSized;
    private boolean groupColumnsSized;
    private boolean itemColumnsSized;
    private boolean missingGroupsWarned;
    private boolean rebuildingTables;
    private boolean cambiosSinGuardar;

    /** Uuid del grupo en modo modificación, o null si se está agregando uno. */
    private String editGroupUuid;
    /** Uuid del área en modo modificación, o null si se está agregando una. */
    private String editItemUuid;

    /**
     * Modelo de la tabla de áreas: sólo lectura. La asociación con el grupo de
     * escaneo se fija al crear el área y no se modifica desde la tabla, para que
     * la configuración no pueda quedar con relaciones sin sentido.
     */
    private static final class AreaTableModel extends DefaultTableModel {
        AreaTableModel(String[] columns) {
            super(columns, 0);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    public CommunicationWizardDialog(Project project, String deviceUuid) {
        super(WindowManager.getDefault().getMainWindow(), "Nueva Comunicación", ModalityType.APPLICATION_MODAL);
        this.project = project;
        this.model = project != null ? project.getLookup().lookup(HMICommunicationModel.class) : null;

        buildUi();
        loadDevices(deviceUuid);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                intentarCerrar();
            }
        });
        getRootPane().registerKeyboardAction(e -> intentarCerrar(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        Dimension pref = getPreferredSize();
        Rectangle screen = getGraphicsConfiguration() != null
                ? getGraphicsConfiguration().getBounds()
                : new Rectangle(0, 0, 1024, 768);
        int maxW = Math.min(1000, Math.max(760, screen.width - 120));
        int maxH = Math.min(740, Math.max(600, screen.height - 140));
        setSize(Math.min(pref.width, maxW), Math.min(pref.height, maxH));
        setMinimumSize(new Dimension(Math.min(780, maxW), Math.min(520, maxH)));
        setLocationRelativeTo(null);
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

    // ------------------------------- paso 1---------------------------------

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
        fixComboWidth(cbDevice, 340);

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
                + "«Crear Dispositivo»,primero cierre este asistente y vuelva a abrirlo.</i></html>");
        inner.add(hint, g);

        panel.add(inner, BorderLayout.CENTER);

        JButton btnCancel = new JButton("Cancelar");
        JButton btnNext = new JButton("Siguiente >");
        btnCancel.addActionListener(e -> intentarCerrar());
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

    // ----------------------- paso 2 -----------------------------------------

    private JPanel buildStepArea() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        for (int ms = 100; ms <= 1000; ms += 100) {
            cbGroupScantime.addItem(String.valueOf(ms));
        }
        cbGroupScantime.setEditable(false);
        cbGroupScantime.setSelectedIndex(-1);
        fixComboWidth(cbItemGroup, 300);
        fixComboWidth(cbGroupScantime, 110);
        fixTablePreferredSize(tbGroups, 700, 240);
        fixTablePreferredSize(tbItems, 700, 240);
        tbGroups.setIntercellSpacing(new Dimension(6, 2));
        tbItems.setIntercellSpacing(new Dimension(6, 2));

        JPanel center = new JPanel(new BorderLayout(6, 6));

        JPanel groupTable = new JPanel(new BorderLayout(0, 4));
        groupTable.setBorder(BorderFactory.createTitledBorder("Grupos de escaneo"));
        groupTable.add(new JScrollPane(tbGroups), BorderLayout.CENTER);

        JPanel itemTable = new JPanel(new BorderLayout(0, 4));
        itemTable.setBorder(BorderFactory.createTitledBorder("Áreas de memoria"));
        itemTable.add(new JScrollPane(tbItems), BorderLayout.CENTER);

        JSplitPane tableSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, groupTable, itemTable);
        tableSplit.setResizeWeight(0.5);
        tableSplit.setDividerLocation(0.5);
        tableSplit.setContinuousLayout(true);

        JPanel forms = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 2, 8);
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;

        JLabel stepTitle = new JLabel("Paso 2 de 3 — Grupo de escaneo y área de memoria");
        stepTitle.setFont(stepTitle.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        stepTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.add(stepTitle, BorderLayout.NORTH);
        north.add(forms, BorderLayout.CENTER);
        center.add(north, BorderLayout.NORTH);

        JLabel grpTitle = lblGroupFormTitle;
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
        g.gridx = 1; g.weightx = 0.5; forms.add(cbGroupScantime, g);
        g.gridx = 2; g.weightx = 0.0; g.fill = GridBagConstraints.NONE; g.anchor = GridBagConstraints.WEST;
        forms.add(chkGroupEnable, g);
        g.gridx = 3; g.weightx = 0.0; g.fill = GridBagConstraints.NONE; g.anchor = GridBagConstraints.EAST;
        forms.add(buttonRow(btnAddGroup, btnUpdateGroup, btnDeleteGroup, btnCancelGroup), g);
        g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.WEST;
        btnAddGroup.addActionListener(e -> addGroup());
        btnUpdateGroup.addActionListener(e -> updateGroup());
        btnDeleteGroup.addActionListener(e -> deleteGroup());
        btnCancelGroup.addActionListener(e -> salirDeEdicionGrupo());

        JLabel itemTitle = lblItemFormTitle;
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

        g.gridy = 6;
        g.gridx = 1; g.weightx = 0.5; forms.add(chkItemEnable, g);
        g.gridx = 3; g.weightx = 0.0; g.fill = GridBagConstraints.NONE; g.anchor = GridBagConstraints.EAST;
        forms.add(buttonRow(btnAddItem, btnUpdateItem, btnDeleteItem, btnCancelItem), g);
        g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.WEST;
        btnAddItem.addActionListener(e -> addItem());
        btnUpdateItem.addActionListener(e -> updateItem());
        btnDeleteItem.addActionListener(e -> deleteItem());
        btnCancelItem.addActionListener(e -> salirDeEdicionItem());

        tbGroups.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !rebuildingTables) {
                onGroupRowSelected();
            }
        });
        tbItems.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !rebuildingTables) {
                onItemRowSelected();
            }
        });

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

        JSplitPane areaSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, center, tableSplit);
        areaSplit.setResizeWeight(0.35);
        areaSplit.setDividerLocation(250);
        areaSplit.setContinuousLayout(true);

        btnBack2.addActionListener(e -> showStep(STEP_DEVICE));
        btnNext2.addActionListener(e -> showStep(STEP_PV));
        btnSave2.addActionListener(e -> saveNow());
        btnClose.addActionListener(e -> intentarCerrar());

        JPanel buttons = new JPanel(new BorderLayout());
        buttons.add(nav(btnBack2), BorderLayout.WEST);
        buttons.add(nav(btnSave2, btnClose, btnNext2), BorderLayout.EAST);
        panel.add(areaSplit, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    // --------------------------- paso 3 ------------------------------------ 

    private JPanel buildStepPv() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(2, 8, 2, 8);
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

        addLabel(form, g, ++row, "Variable (del área S88):");
        g.gridx = 1; g.gridwidth = 3; g.fill = GridBagConstraints.HORIZONTAL;
        form.add(cbVariable, g);
        g.gridwidth = 1; g.fill = GridBagConstraints.NONE;

        ++row;
        g.gridx = 0; g.gridy = row; g.gridwidth = 4; g.weightx = 1.0;
        form.add(lblTypeLock, g); g.gridwidth = 1;

        addLabel(form, g, ++row, "Tipo:");
        g.gridx = 1; g.gridwidth = 1; form.add(cbType, g);

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

        cbVariable.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof PlantVariable v) {
                    String text = v.getPath().isEmpty()
                            ? v.getName() + "  (" + v.getType() + ")"
                            : v.getPath() + "/" + v.getName(); //+ "  (" + v.getType() + ")";
                    if (v.isUsed()) {
                        label.setEnabled(false);
                        label.setToolTipText("Variable ya asignada a otra configuración");
                        text += "  — en uso";
                    }
                    label.setText(text);
                } else {
                    label.setText("-- Seleccione --");
                }
                return label;
            }
        });
        cbVariable.addActionListener(e -> onVariableSelected());
        fixComboWidth(cbVariable, 340);
        fixComboWidth(cbArea, 300);
        cbArea.addActionListener(e -> {
            refreshVariables();
        });

        cbType.removeAllItems();
        cbType.addItem("—");
        cbType.setEnabled(false);

        btnBack3.addActionListener(e -> showStep(STEP_AREA));
        btnSavePv.addActionListener(e -> savePv());

        JButton btnCancel = new JButton("Cancelar");
        btnCancel.addActionListener(e -> intentarCerrar());
        btnFinish.addActionListener(e -> saveAndClose());

        JPanel pvTable = new JPanel(new BorderLayout(0, 4));
        pvTable.setBorder(BorderFactory.createTitledBorder("Variables configuradas"));
        tbPvs.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tbPvs.setFillsViewportHeight(false);
        tbPvs.setIntercellSpacing(new Dimension(6, 2));
        fixTablePreferredSize(tbPvs, 700, 460);
        pvTable.add(new JScrollPane(tbPvs), BorderLayout.CENTER);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        formScroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formScroll, pvTable);
        split.setResizeWeight(0.0);
        split.setDividerLocation(340);
        split.setContinuousLayout(true);

        panel.add(split, BorderLayout.CENTER);
        panel.add(nav(btnBack3, btnCancel, btnSavePv, btnFinish), BorderLayout.SOUTH);
        return panel;
    }

    // ------------------------------- helpers ui ------------------------------

    /**
     * Acota el tamaño preferido de una tabla. Sin esto, el preferred size de un
     * {@link JTable} es la suma del ancho de sus columnas y termina estirando
     * el diálogo mucho más allá de lo necesario.
     */
    private static void fixTablePreferredSize(JTable table, int width, int height) {
        table.setPreferredSize(new Dimension(width, height));
    }

    /**
     * Ancho inicial legible por columna: mide el encabezado y el contenido real
     * y lo acota entre minWidth y maxWidth. Evita el ancho por defecto de
     * {@link JTable} (75px), que dejaba todas las celdas pegadas, sin dejar que
     * un nombre kilométrico infle la columna.
     */
    private static void sizeColumnsToContent(JTable table, int minWidth, int maxWidth) {
        FontMetrics fm = table.getFontMetrics(table.getTableHeader().getFont());
        TableColumnModel cm = table.getColumnModel();
        TableCellRenderer cellRenderer = table.getDefaultRenderer(Object.class);
        for (int i = 0; i < cm.getColumnCount(); i++) {
            int width = fm.stringWidth(table.getModel().getColumnName(i)) + 28;
            for (int r = 0; r < table.getRowCount() && width < maxWidth; r++) {
                Object value = table.getModel().getValueAt(r, i);
                if (value == null) {
                    continue;
                }
                Component comp = cellRenderer.getTableCellRendererComponent(
                        table, value, false, false, r, i);
                width = Math.max(width, comp.getPreferredSize().width + 24);
            }
            TableColumn col = cm.getColumn(i);
            col.setPreferredWidth(Math.max(minWidth, Math.min(maxWidth, width)));
            col.setMinWidth(minWidth);
        }
    }

    private static void fixComboWidth(JComboBox<?> combo, int width) {
        Dimension pref = combo.getPreferredSize();
        combo.setPreferredSize(new Dimension(width, pref.height));
        combo.setMinimumSize(new Dimension(Math.min(120, width), pref.height));
    }

    private static JPanel nav(JButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        for (JButton b : buttons) {
            panel.add(b);
        }
        return panel;
    }

    /** Fila de botones de agregar / modificar / eliminar / cancelar. */
    private static JPanel buttonRow(JButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
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

    // ------------------------------logica---------------------------------- 

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
            for (CommConfigData.ItemConfig i : items) {
                if (i.getGroupUuid() != null && !i.getGroupUuid().isEmpty()) {
                    itemsGroup.put(i.getUuid(), i.getGroupUuid());
                }
            }
        }
        if (comms.getPvs() != null) {
            pvs.addAll(comms.getPvs());
        }
        refreshAreaView();
        warnMissingGroups();
    }


    private void refreshAreaView() {
        rebuildingTables = true;
        try {
            groupModel.setRowCount(0);
            for (CommConfigData.GroupConfig g : groups) {
                groupModel.addRow(new Object[]{
                    g.getName(), g.getDescription(), g.getScantime(),
                    g.isEnable() ? "TRUE" : "FALSE", g.getUuid()
                });
            }
            itemModel.setRowCount(0);
            for (CommConfigData.ItemConfig i : items) {
                String groupName = groupNameOf(i.getUuid());
                itemModel.addRow(new Object[]{
                    i.getName(), i.getDescription(), i.getTag(),
                    groupName.isEmpty() ? SIN_GRUPO : groupName,
                    i.isEnable() ? "TRUE" : "FALSE", i.getUuid()
                });
            }
        } finally {
            rebuildingTables = false;
        }
        refreshPvAreas();
        refreshVariables();
        refreshPvTable();
        refreshGroupCombo();
        refreshItemFormState();
        refreshGroupFormState();
        btnNext2.setEnabled(!items.isEmpty());
        if (!groupColumnsSized && !groups.isEmpty()) {
            sizeColumnsToContent(tbGroups, 90, 260);
            groupColumnsSized = true;
        }
        if (!itemColumnsSized && !items.isEmpty()) {
            sizeColumnsToContent(tbItems, 90, 260);
            itemColumnsSized = true;
        }
    }

    /**
     * El grupo de escaneo es un requisito previo: sin al menos un grupo no se
     * puede crear ninguna área de memoria, así que el formulario queda bloqueado.
     * Al modificar un área existente el nombre y el grupo van bloqueados: el
     * nombre es la identidad del área y el grupo no se cambia desde la UI.
     */
    private void refreshItemFormState() {
        boolean hayGrupo = !groups.isEmpty();
        boolean editando = editItemUuid != null;
        btnAddItem.setEnabled(hayGrupo && !editando);
        btnUpdateItem.setEnabled(hayGrupo && editando);
        btnCancelItem.setEnabled(editando);
        btnDeleteItem.setEnabled(editando);
        cbItemGroup.setEnabled(hayGrupo && !editando);
        txtItemName.setEnabled(hayGrupo && !editando);
        txtItemDescription.setEnabled(hayGrupo);
        txtItemTag.setEnabled(hayGrupo);
        chkItemEnable.setEnabled(hayGrupo);
    }

    /**
     * Estado del formulario de grupos: el nombre queda bloqueado al modificar
     * porque es la identidad del grupo.
     */
    private void refreshGroupFormState() {
        boolean editando = editGroupUuid != null;
        btnAddGroup.setEnabled(!editando);
        btnUpdateGroup.setEnabled(editando);
        btnDeleteGroup.setEnabled(editando);
        btnCancelGroup.setEnabled(editando);
        txtGroupName.setEnabled(!editando);
        txtGroupDescription.setEnabled(true);
        cbGroupScantime.setEnabled(!editando);
        chkGroupEnable.setEnabled(true);
    }

    /** Áreas sin grupo de escaneo: dato incompleto, nunca asumido. */
    private List<CommConfigData.ItemConfig> areasSinGrupo() {
        List<CommConfigData.ItemConfig> sinGrupo = new ArrayList<>();
        for (CommConfigData.ItemConfig i : items) {
            if (groupNameOf(i.getUuid()).isEmpty()) {
                sinGrupo.add(i);
            }
        }
        return sinGrupo;
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
                    return;
                }
            }
        }
        if (cbItemGroup.getItemCount() > 0) {
            cbItemGroup.setSelectedIndex(0);
        }
    }

    /**
     * Llena la tabla del paso 3 con las variables de proceso configuradas.
     */
    private void refreshPvTable() {
        pvModel.setRowCount(0);
        for (CommConfigData.PvConfig pv : pvs) {
            CommConfigData.ItemConfig area = itemByUuid(pv.getId());
            pvModel.addRow(new Object[]{
                pv.getName(),
                area == null ? "—" : area.getName(),
                pv.getId(),
                pv.getType(),
                pv.getOffset(),
                pv.getDescriptor(),
                pv.getScanTime(),
                pv.isScanEnable() ? "TRUE" : "FALSE",
                pv.isWriteEnable() ? "TRUE" : "FALSE",
                pv.getDisplayLimitLow(),
                pv.getDisplayLimitHigh(),
                pv.getDisplayDescription(),
                pv.getDisplayFormat(),
                pv.getDisplayUnits(),
                pv.getControlLimitLow(),
                pv.getControlLimitHigh(),
                pv.getControlMinStep(),
                pv.getS88Path(),
                pv.getUuid(),
                pv.getMd5()
            });
        }
        if (!pvColumnsSized && !pvs.isEmpty()) {
            sizeColumnsToContent(tbPvs, 90, 300);
            pvColumnsSized = true;
        }
    }

    private CommConfigData.ItemConfig itemByUuid(String uuid) {
        if (uuid == null) {
            return null;
        }
        for (CommConfigData.ItemConfig i : items) {
            if (uuid.equals(i.getUuid())) {
                return i;
            }
        }
        return null;
    }

    private void refreshPvAreas() {
        CommConfigData.ItemConfig sel = (CommConfigData.ItemConfig) cbArea.getSelectedItem();
        cbArea.removeAllItems();
        // Sólo se listan áreas con grupo de escaneo: una variable no puede
        // quedar asociada a un área incompleta.
        for (CommConfigData.ItemConfig i : items) {
            if (!groupNameOf(i.getUuid()).isEmpty()) {
                cbArea.addItem(i);
            }
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

    private void refreshVariables() {
        cbVariable.removeAllItems();
        cbVariable.addItem(null);
        cbType.removeAllItems();
        cbType.addItem("—");
        refreshTypeLock();
        String lockedType = itemLockedType();
        if (selectedDevice == null) {
            return;
        }
        String areaId = selectedDevice.getS88Node();
        if (areaId == null || areaId.isEmpty()) {
            return;
        }
        List<PlantVariable> variables;
        try {
            variables = loadAreaVariables(areaId);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return;
        }
        Set<String> usedKeys = new HashSet<>();
        for (CommConfigData.PvConfig pv : pvs) {
            usedKeys.add(pvKey(pv.getS88Path(), pv.getName()));
        }
        for (PlantVariable v : variables) {
            if (lockedType != null && !lockedType.equalsIgnoreCase(v.getType())) {
                continue;
            }
            if (usedKeys.contains(v.getKey())) {
                v.setUsed(true);
            }
            cbVariable.addItem(v);
        }
    }

    /**
     * Clave de identidad de una variable de planta: su ruta dentro del área S88
     * más su nombre. Dos variables homónimas de jerarquías distintas (p.ej. la
     * temperatura del tanque 1 y la del tanque 2) tienen claves diferentes y
     * pueden configurarse por separado.
     */
    private static String pvKey(String path, String name) {
        return (path == null || path.isEmpty() ? "" : path + "/") + name;
    }

    /**
     * Variables ya guardadas que pertenecen al área (PvId) seleccionada.
     */
    private List<CommConfigData.PvConfig> pvsOfSelectedArea() {
        List<CommConfigData.PvConfig> result = new ArrayList<>();
        CommConfigData.ItemConfig area = (CommConfigData.ItemConfig) cbArea.getSelectedItem();
        if (area == null) {
            return result;
        }
        for (CommConfigData.PvConfig pv : pvs) {
            if (area.getUuid().equals(pv.getId())) {
                result.add(pv);
            }
        }
        return result;
    }

    /**
     * Tipo al que queda fijado el área seleccionada: el de su primera variable
     * guardada. Si el área está vacía devuelve {@code null} y todavía admite
     * cualquier tipo (la primera variable elegida lo fija).
     */
    private String itemLockedType() {
        List<CommConfigData.PvConfig> areaPvs = pvsOfSelectedArea();
        if (areaPvs.isEmpty()) {
            return null;
        }
        return areaPvs.get(0).getType();
    }

    /**
     * Tamaño en bytes de un tipo de dato. Devuelve 0 si el tamaño no es
     * determinable (p.ej. string), en cuyo caso no se puede aplicar la
     * fórmula estándar de offset.
     */
    private static int typeSizeBytes(String type) {
        if (type == null || type.isEmpty()) {
            return 0;
        }
        return switch (type.toLowerCase()) {
            case "boolean", "byte", "ubyte" -> 1;
            case "short", "ushort" -> 2;
            case "int", "uint", "long", "ulong", "float" -> 4;
            case "double" -> 8;
            case "string" -> 0;
            default -> 4;
        };
    }

    /**
     * Tamaño en bytes (Tam) que aplica al área seleccionada: el del tipo al que
     * está fijada o, si todavía está vacía, el de la variable elegida.
     */
    private int selectedAreaTam() {
        String type = itemLockedType();
        if (type == null || type.isEmpty()) {
            PlantVariable v = (PlantVariable) cbVariable.getSelectedItem();
            type = v == null ? null : v.getType();
        }
        return typeSizeBytes(type);
    }

    /**
     * Offset siguiente para el área seleccionada según la fórmula estándar
     * {@code (Medición - 1) × Tam}: la medición es la posición de la variable
     * dentro del área (empieza en 1) y {@code Tam} el tamaño en bytes del tipo.
     * Devuelve -1 cuando todavía no hay tipo definido.
     */
    private int nextOffset() {
        int tam = selectedAreaTam();
        if (tam <= 0) {
            return -1;
        }
        return pvsOfSelectedArea().size() * tam;
    }

    private void refreshTypeLock() {
        CommConfigData.ItemConfig area = (CommConfigData.ItemConfig) cbArea.getSelectedItem();
        if (area == null) {
            lblTypeLock.setText(" ");
            return;
        }
        String locked = itemLockedType();
        if (locked == null) {
            lblTypeLock.setText("<html>El área <b>" + area.getName()
                    + "</b> está vacía: la primera variable elegida fijará su tipo.</html>");
        } else {
            lblTypeLock.setText("<html>El área <b>" + area.getName()
                    + "</b> está fijada al tipo <b>" + locked
                    + "</b>: solo se listan variables de ese tipo.</html>");
        }
    }

    private void onVariableSelected() {
        PlantVariable v = (PlantVariable) cbVariable.getSelectedItem();
        if (v == null) {
            cbType.removeAllItems();
            cbType.addItem("—");
            return;
        }
        if (v.isUsed()) {
            JOptionPane.showMessageDialog(this,
                    "La variable '" + v.getKey() + "' ya está en uso.",
                    "Variable ocupada", JOptionPane.WARNING_MESSAGE);
            cbVariable.setSelectedIndex(0);
            return;
        }
        cbType.removeAllItems();
        cbType.addItem(v.getType());
    }

    private List<PlantVariable> loadAreaVariables(String areaId) throws Exception {
        List<PlantVariable> result = new ArrayList<>();
        FileObject plantModel = findPlantModelFile(project != null ? project.getProjectDirectory() : null);
        if (plantModel == null) {
            return result;
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(FileUtil.toFile(plantModel));
        Element root = doc.getDocumentElement();
        if (root == null) {
            return result;
        }
        Element area = findArea(root, areaId);
        if (area != null) {
            collectPlantVariables(area, areaId, result);
        }
        return result;
    }

    private Element findArea(Element root, String areaId) {
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) node;
            if ("area".equals(element.getTagName()) && areaId.equals(element.getAttribute("id"))) {
                return element;
            }
        }
        return null;
    }

    private void collectPlantVariables(Element element, String currentPath, List<PlantVariable> result) {
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element child = (Element) node;
            if ("variable".equals(child.getTagName())) {
                String name = child.getAttribute("name");
                if (!name.isEmpty()) {
                    result.add(new PlantVariable(currentPath, name, child.getAttribute("Type")));
                }
                continue;
            }
            String id = child.getAttribute("id");
            String childPath = id.isEmpty() ? currentPath : currentPath + "/" + id;
            collectPlantVariables(child, childPath, result);
        }
    }

    private FileObject findPlantModelFile(FileObject projectDir) {
        FileObject fo = projectDir;
        while (fo != null) {
            FileObject xml = fo.getFileObject("plant-model.xml");
            if (xml != null) {
                return xml;
            }
            fo = fo.getParent();
        }
        return null;
    }

    private void addGroup() {
        String name = txtGroupName.getText().trim();
        String scantime = (String) cbGroupScantime.getSelectedItem();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Ingrese el nombre del grupo de escaneo.",
                    "Campo incompleto", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (scantime == null) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione el scantime del grupo (100 ms a 1000 ms).",
                    "Campo incompleto", JOptionPane.WARNING_MESSAGE);
            return;
        }
        groups.add(new CommConfigData.GroupConfig(
                UUID.randomUUID().toString(), name, txtGroupDescription.getText().trim(),
                scantime, chkGroupEnable.isSelected(), "md5_group_hash"));
        limpiarFormularioGrupo();
        cambiosSinGuardar = true;
        refreshAreaView();
    }

    /**
     * Carga el grupo de la fila seleccionada en el formulario, con el nombre
     * bloqueado: el nombre y el uuid son la identidad del grupo y no se modifican.
     */
    private void onGroupRowSelected() {
        int row = tbGroups.getSelectedRow();
        if (row < 0 || row >= groups.size()) {
            salirDeEdicionGrupo();
            return;
        }
        CommConfigData.GroupConfig group = groups.get(row);
        editGroupUuid = group.getUuid();
        txtGroupName.setText(group.getName());
        txtGroupDescription.setText(group.getDescription());
        cbGroupScantime.setSelectedItem(group.getScantime());
        chkGroupEnable.setSelected(group.isEnable());
        lblGroupFormTitle.setText("Modificando grupo: " + group.getName());
        refreshGroupFormState();
    }

    private void salirDeEdicionGrupo() {
        editGroupUuid = null;
        tbGroups.clearSelection();
        limpiarFormularioGrupo();
        refreshGroupFormState();
    }

    private void limpiarFormularioGrupo() {
        txtGroupName.setText("");
        txtGroupDescription.setText("");
        cbGroupScantime.setSelectedIndex(-1);
        chkGroupEnable.setSelected(true);
        lblGroupFormTitle.setText("Nuevo grupo de escaneo");
    }

    /**
     * Aplica los cambios sobre el grupo ya existente. Se conserva el uuid, de
     * modo que al guardar se sobreescriba el registro existente en el XML y en la
     * base de datos, en lugar de crear uno nuevo.
     */
    private void updateGroup() {
        CommConfigData.GroupConfig actual = groupByUuid(editGroupUuid);
        if (actual == null) {
            salirDeEdicionGrupo();
            return;
        }
        String scantime = (String) cbGroupScantime.getSelectedItem();
        if (scantime == null) {
            scantime = actual.getScantime();
        }
        int idx = groups.indexOf(actual);
        groups.set(idx, new CommConfigData.GroupConfig(
                actual.getUuid(), actual.getName(), txtGroupDescription.getText().trim(),
                scantime, chkGroupEnable.isSelected(), actual.getMd5()));
        salirDeEdicionGrupo();
        cambiosSinGuardar = true;
        refreshAreaView();
    }

    /**
     * Un grupo con áreas asociadas no se puede eliminar: como el grupo es
     * obligatorio para cada área, borrarlo dejaría áreas sin grupo.
     */
    private void deleteGroup() {
        CommConfigData.GroupConfig group = groupByUuid(editGroupUuid);
        if (group == null) {
            salirDeEdicionGrupo();
            return;
        }
        List<String> areasDelGrupo = new ArrayList<>();
        for (CommConfigData.ItemConfig i : items) {
            if (group.getUuid().equals(i.getGroupUuid())) {
                areasDelGrupo.add(i.getName());
            }
        }
        if (!areasDelGrupo.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "El grupo '" + group.getName() + "' tiene " + areasDelGrupo.size()
                            + " área(s) de memoria asociada(s):\n\n"
                            + String.join(", ", areasDelGrupo)
                            + "\n\nUn área siempre debe pertenecer a un grupo, por lo que "
                            + "primero debe reasignar o eliminar esas áreas.",
                    "No se puede eliminar", JOptionPane.WARNING_MESSAGE);
            return;
        }
        groups.remove(group);
        salirDeEdicionGrupo();
        cambiosSinGuardar = true;
        refreshAreaView();
    }

    private CommConfigData.GroupConfig groupByUuid(String uuid) {
        if (uuid == null) {
            return null;
        }
        for (CommConfigData.GroupConfig g : groups) {
            if (g.getUuid().equals(uuid)) {
                return g;
            }
        }
        return null;
    }

    private void addItem() {
        if (groups.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Debe crear al menos un grupo de escaneo antes de agregar un área de memoria.",
                    "Falta grupo de escaneo", JOptionPane.WARNING_MESSAGE);
            return;
        }
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
                txtItemTag.getText().trim(), chkItemEnable.isSelected(), "md5_item_hash",
                group.getUuid()));
        itemsGroup.put(itemUuid, group.getUuid());
        limpiarFormularioItem();
        cambiosSinGuardar = true;
        refreshAreaView();
    }

    /**
     * Carga el área de la fila seleccionada en el formulario. El nombre y el
     * grupo quedan bloqueados: el nombre es la identidad del área y el grupo de
     * escaneo no se cambia desde la UI.
     */
    private void onItemRowSelected() {
        int row = tbItems.getSelectedRow();
        if (row < 0 || row >= items.size()) {
            salirDeEdicionItem();
            return;
        }
        CommConfigData.ItemConfig item = items.get(row);
        editItemUuid = item.getUuid();
        txtItemName.setText(item.getName());
        txtItemDescription.setText(item.getDescription());
        txtItemTag.setText(item.getTag());
        chkItemEnable.setSelected(item.isEnable());
        CommConfigData.GroupConfig group = groupByUuid(item.getGroupUuid());
        if (group != null) {
            cbItemGroup.setSelectedItem(group);
        }
        lblItemFormTitle.setText("Modificando área: " + item.getName());
        refreshItemFormState();
    }

    private void salirDeEdicionItem() {
        editItemUuid = null;
        tbItems.clearSelection();
        limpiarFormularioItem();
        refreshItemFormState();
    }

    private void limpiarFormularioItem() {
        txtItemName.setText("");
        txtItemDescription.setText("");
        txtItemTag.setText("");
        chkItemEnable.setSelected(true);
        lblItemFormTitle.setText("Nueva área de memoria (del dispositivo seleccionado)");
    }

    /**
     * Aplica los cambios sobre el área ya existente. Se conservan el uuid y el
     * grupo de escaneo, de modo que al guardar se sobreescriba el registro
     * existente en el XML y en la base de datos, en lugar de crear uno nuevo.
     */
    private void updateItem() {
        CommConfigData.ItemConfig actual = itemByUuid(editItemUuid);
        if (actual == null) {
            salirDeEdicionItem();
            return;
        }
        int idx = items.indexOf(actual);
        items.set(idx, new CommConfigData.ItemConfig(
                actual.getUuid(), actual.getName(), txtItemDescription.getText().trim(),
                txtItemTag.getText().trim(), chkItemEnable.isSelected(), actual.getMd5(),
                actual.getGroupUuid()));
        itemsGroup.put(actual.getUuid(), actual.getGroupUuid());
        salirDeEdicionItem();
        cambiosSinGuardar = true;
        refreshAreaView();
    }

    /**
     * Elimina el área. Si tiene variables configuradas también las elimina, para
     * no dejar variables apuntando a un área que ya no existe; de eso se avisa
     * antes de confirmar.
     */
    private void deleteItem() {
        CommConfigData.ItemConfig item = itemByUuid(editItemUuid);
        if (item == null) {
            salirDeEdicionItem();
            return;
        }
        List<CommConfigData.PvConfig> variables = new ArrayList<>();
        for (CommConfigData.PvConfig pv : pvs) {
            if (item.getUuid().equals(pv.getId())) {
                variables.add(pv);
            }
        }
        if (!variables.isEmpty()) {
            StringBuilder nombres = new StringBuilder();
            for (CommConfigData.PvConfig pv : variables) {
                if (nombres.length() > 0) {
                    nombres.append(", ");
                }
                nombres.append(pv.getName());
            }
            int opcion = JOptionPane.showConfirmDialog(this,
                    "El área '" + item.getName() + "' tiene " + variables.size()
                            + " variable(s) configurada(s):\n\n" + nombres
                            + "\n\nSi elimina el área, también se eliminan esas variables. "
                            + "¿Desea continuar?",
                    "Eliminar área y variables", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (opcion != JOptionPane.YES_OPTION) {
                return;
            }
            pvs.removeAll(variables);
        }
        items.remove(item);
        itemsGroup.remove(item.getUuid());
        salirDeEdicionItem();
        cambiosSinGuardar = true;
        refreshAreaView();
    }

    /**
     * Guarda la variable de proceso del formulario en el área seleccionada y
     * persiste la configuración en comunicacion.xml sin cerrar el wizard, para
     * que el usuario pueda seguir agregando variables.
     */
    private void savePv() {
        // Nada se agrega al modelo si la configuración no es válida: el grupo de
        // escaneo del área es obligatorio.
        if (!confirmNoMissingGroups()) {
            return;
        }
        CommConfigData.ItemConfig area = (CommConfigData.ItemConfig) cbArea.getSelectedItem();
        PlantVariable variable = (PlantVariable) cbVariable.getSelectedItem();
        if (area == null) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un área de memoria (PvId) para la variable.",
                    "Falta área", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (variable == null) {
            JOptionPane.showMessageDialog(this, "Seleccione la variable de proceso del área S88.",
                    "Falta variable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (variable.isUsed()) {
            JOptionPane.showMessageDialog(this,
                    "La variable '" + variable.getKey() + "' ya está en uso y no puede configurarse de nuevo.",
                    "Variable ocupada", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int offset = nextOffset();
        if (offset < 0) {
            JOptionPane.showMessageDialog(this,
                    "No se puede calcular el offset automático para el tipo '" + variable.getType()
                            + "'; el tamaño en bytes no está definido.",
                    "Offset no calculable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        pvs.add(new CommConfigData.PvConfig(
                UUID.randomUUID().toString(),
                variable.getName(),
                variable.getType(),
                area.getUuid(),
                String.valueOf(offset),
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
                "md5_pv_hash",
                variable.getPath()));
        cbVariable.setSelectedIndex(0);
        txtDescriptor.setText("");
        txtScanTime.setText("");
        refreshVariables();
        refreshPvTable();
        showSavedMessage("Variable '" + variable.getName() + "' guardada en '" + area.getName()
                + "' con offset " + offset + ".");
    }

    private void saveNow() {
        if (selectedDevice == null || model == null) {
            return;
        }
        if (!confirmNoMissingGroups()) {
            return;
        }
        showSavedMessage("Grupos y áreas guardados.");
    }

    private void saveAndClose() {
        if (selectedDevice == null || model == null) {
            return;
        }
        if (!confirmNoMissingGroups()) {
            return;
        }
        if (!showSavedMessage("Comunicación guardada.")) {
            return;
        }
        dispose();
    }

    /**
     * Toda área de memoria debe pertenecer a un grupo de escaneo: el grupo es un
     * requisito previo a la creación del área. Si alguna quedara sin grupo no se
     * guarda nada, porque en la base de datos no se admiten áreas sin grupo.
     *
     * @return true si se puede seguir guardando
     */
    private boolean confirmNoMissingGroups() {
        List<CommConfigData.ItemConfig> sinGrupo = areasSinGrupo();
        if (sinGrupo.isEmpty()) {
            return true;
        }
        StringBuilder nombres = new StringBuilder();
        for (CommConfigData.ItemConfig i : sinGrupo) {
            if (nombres.length() > 0) {
                nombres.append(", ");
            }
            nombres.append(i.getName());
        }
        JOptionPane.showMessageDialog(this,
                "Las siguientes áreas de memoria no tienen grupo de escaneo:\n\n"
                        + nombres + "\n\nEl grupo de escaneo es obligatorio, por lo que no se "
                        + "guarda ni el XML ni la base de datos.\n"
                        + "Vuelva a crear esas áreas eligiendo su grupo.",
                "Áreas sin grupo", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    /**
     * Avisa una vez por apertura si hay áreas sin grupo de escaneo, dato incompleto
     * que solo puede venir de configuraciones anteriores a la persistencia del
     * grupo en el XML.
     */
    private void warnMissingGroups() {
        if (missingGroupsWarned) {
            return;
        }
        List<CommConfigData.ItemConfig> sinGrupo = areasSinGrupo();
        if (sinGrupo.isEmpty()) {
            return;
        }
        missingGroupsWarned = true;
        StringBuilder nombres = new StringBuilder();
        for (CommConfigData.ItemConfig i : sinGrupo) {
            if (nombres.length() > 0) {
                nombres.append(", ");
            }
            nombres.append(i.getName());
        }
        JOptionPane.showMessageDialog(this,
                "Estas áreas de memoria quedaron guardadas sin grupo de escaneo:\n\n"
                        + nombres + "\n\nPor seguridad la columna \"Grupo\" no se puede editar: "
                        + "hay que volver a crear esas áreas eligiendo su grupo.",
                "Áreas sin grupo de escaneo", JOptionPane.WARNING_MESSAGE);
    }

    /**
     * @return true si la configuración quedó guardada en el XML y volcada a la
     *         base de datos {@code boot.db}
     */
    private boolean persist() {
        CommConfigData config = new CommConfigData(
                selectedDevice.getDeviceName(),
                new ArrayList<>(groups),
                new ArrayList<>(items),
                new ArrayList<>(pvs));
        model.upsertComms(selectedDevice.getUuid(), config);
        return model.save();
    }

    private boolean showSavedMessage(String what) {
        boolean databaseUpdated = persist();
        // Sólo queda limpio si se guardó todo: con un guardado parcial se sigue
        // avisando al cerrar, porque el modelo en memoria difiere de la base.
        cambiosSinGuardar = !databaseUpdated;
        if (databaseUpdated) {
            JOptionPane.showMessageDialog(this, what + "\nGuardado en comunicacion.xml y en boot.db.",
                    "Guardado", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, what
                            + "\nGuardado en comunicacion.xml, pero NO se pudo actualizar boot.db."
                            + "\nRevise la salida de la aplicación para el detalle.",
                    "Guardado parcial", JOptionPane.WARNING_MESSAGE);
        }
        return databaseUpdated;
    }

    /**
     * Cierre del asistente, tanto por botón como por la X de la ventana o la
     * tecla ESC. Si hay cambios sin guardar pregunta al usuario antes de salir,
     * porque cerrar descartaría todo lo agregado o modificado.
     */
    private void intentarCerrar() {
        if (cambiosSinGuardar) {
            int opcion = JOptionPane.showConfirmDialog(this,
                    "Hay cambios sin guardar en la configuración de comunicación.\n"
                            + "Si sale ahora, esos cambios se pierden.\n\n"
                            + "¿Desea guardarlos antes de salir?",
                    "Cambios sin guardar", JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (opcion == JOptionPane.CANCEL_OPTION || opcion == JOptionPane.CLOSED_OPTION) {
                return;
            }
            if (opcion == JOptionPane.YES_OPTION) {
                if (!confirmNoMissingGroups() || !showSavedMessage("Comunicación guardada.")) {
                    return;
                }
            }
        }
        dispose();
    }

    private void showStep(String step) {
        cardLayout.show(cardPanel, step);
        if (STEP_AREA.equals(step)) {
            btnNext2.setEnabled(!items.isEmpty());
            refreshAreaView();
        } else if (STEP_PV.equals(step)) {
            refreshPvAreas();
            refreshVariables();
            refreshPvTable();
        }
    }

    /**
     * Variable de proceso leída del {@code plant-model.xml}: nombre, tipo y
     * la ruta de pertenencia dentro del área S88 (jerarquía). La clave de
     * bloqueo es la ruta + nombre, para desambiguar variables homónimas.
     */
    private static final class PlantVariable {

        private final String path;
        private final String name;
        private final String type;
        private boolean used;

        PlantVariable(String path, String name, String type) {
            this.path = path;
            this.name = name;
            this.type = type;
        }

        String getPath() {
            return path;
        }

        String getName() {
            return name;
        }

        String getType() {
            return type;
        }

        boolean isUsed() {
            return used;
        }

        void setUsed(boolean used) {
            this.used = used;
        }

        String getKey() {
            return path == null || path.isEmpty() ? name : path + "/" + name;
        }
    }
}
