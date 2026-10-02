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
package org.apache.plc4x.malbec.projecttype.panelcategory.panel.wizard;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;

/**
 * Paso 2 de 3: grupos de escaneo y áreas de memoria.
 *
 * <p>La tabla de áreas no es editable ({@link NonEditableTableModel}): la única
 * forma de cambiar algo es el formulario, que valida. El grupo de escaneo es
 * requisito previo para crear un área y no se modifica una vez creado el
 * área, así que ambos campos quedan bloqueados mientras se edita.</p>
 */
public class StepGroupAreaPanel extends JPanel {

    private static final String SIN_GRUPO = "SIN GRUPO";

    private final CommunicationWizardController controller;

    private final DefaultTableModel groupModel = new DefaultTableModel(new String[]{
        "Nombre", "Descripción", "Scantime (ms)", "Enable", "UUID"}, 0);
    private final JTable tbGroups = new JTable(groupModel);

    private final NonEditableTableModel itemModel = new NonEditableTableModel(new String[]{
        "Nombre", "Descripción", "Tag", "Tipo", "Libre", "Grupo", "Enable", "UUID"});
    private final JTable tbItems = new JTable(itemModel);

    private final JTextField txtGroupName = new JTextField(14);
    private final JTextField txtGroupDescription = new JTextField(14);
    private final JComboBox<String> cbGroupScantime = new JComboBox<>();
    private final JCheckBox chkGroupEnable = new JCheckBox("Enable", true);

    private final JTextField txtItemName = new JTextField(14);
    private final JTextField txtItemDescription = new JTextField(14);
    private final JTextField txtItemTag = new JTextField(14);
    private final JComboBox<CommConfigData.GroupConfig> cbItemGroup = new JComboBox<>();
    private final JCheckBox chkItemEnable = new JCheckBox("Enable", true);

    private final JButton btnAddGroup = new JButton("Añadir");
    private final JButton btnUpdateGroup = new JButton("Modificar");
    private final JButton btnDeleteGroup = new JButton("Eliminar");
    private final JButton btnCancelGroup = new JButton("Cancelar");
    private final JLabel lblGroupFormTitle = WizardUi.formTitle("Nuevo grupo de escaneo");

    private final JButton btnAddItem = new JButton("Añadir");
    private final JButton btnUpdateItem = new JButton("Modificar");
    private final JButton btnDeleteItem = new JButton("Eliminar");
    private final JButton btnCancelItem = new JButton("Cancelar");
    private final JLabel lblItemFormTitle =
            WizardUi.formTitle("Nueva área de memoria (del dispositivo seleccionado)");

    /** Uuid del grupo en modo modificación, o null si se está agregando uno. */
    private String editGroupUuid;
    /** Uuid del área en modo modificación, o null si se está agregando una. */
    private String editItemUuid;

    private boolean groupColumnsSized;
    private boolean itemColumnsSized;
    private boolean rebuildingTables;

    public StepGroupAreaPanel(CommunicationWizardController controller) {
        this.controller = controller;
        buildUi();
        refresh();
    }

    private void buildUi() {
        for (int ms = 100; ms <= 1000; ms += 100) {
            cbGroupScantime.addItem(String.valueOf(ms));
        }
        cbGroupScantime.setEditable(false);
        cbGroupScantime.setSelectedIndex(-1);

        WizardUi.fixComboWidth(cbItemGroup, 300);
        WizardUi.fixComboWidth(cbGroupScantime, 110);
        tbGroups.setIntercellSpacing(new Dimension(6, 2));
        tbItems.setIntercellSpacing(new Dimension(6, 2));

        JScrollPane groupScroll = new JScrollPane(tbGroups);
        WizardUi.limitTableSpace(groupScroll, 700, 240);

        JScrollPane itemScroll = new JScrollPane(tbItems);
        WizardUi.limitTableSpace(itemScroll, 700, 240);

        JPanel groupTable = new JPanel(new BorderLayout(0, 4));
        groupTable.setBorder(BorderFactory.createTitledBorder("Grupos de escaneo"));
        groupTable.add(groupScroll, BorderLayout.CENTER);

        JPanel itemTable = new JPanel(new BorderLayout(0, 4));
        itemTable.setBorder(BorderFactory.createTitledBorder("Áreas de memoria"));
        itemTable.add(itemScroll, BorderLayout.CENTER);

        JSplitPane tableSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, groupTable, itemTable);
        tableSplit.setResizeWeight(0.5);
        tableSplit.setDividerLocation(0.5);
        tableSplit.setContinuousLayout(true);

        JPanel forms = WizardUi.formGrid();
        GridBagConstraints g = WizardUi.formConstraints();

        g.gridx = 0; g.gridy = 0; g.gridwidth = 4; g.weightx = 0.0;
        forms.add(WizardUi.stepTitle("Paso 2 de 3 — Grupo de escaneo y área de memoria"), g);

        g.gridy = 1; g.gridwidth = 4;
        forms.add(lblGroupFormTitle, g);

        g.gridy = 2; g.gridwidth = 1;
        g.gridx = 0; g.weightx = 0.0;
        forms.add(new JLabel("Nombre:"), g);
        g.gridx = 1; g.weightx = 0.5;
        forms.add(txtGroupName, g);
        g.gridx = 2; g.weightx = 0.0;
        forms.add(new JLabel("Descripción:"), g);
        g.gridx = 3; g.weightx = 0.5;
        forms.add(txtGroupDescription, g);

        g.gridy = 3;
        g.gridx = 0; g.weightx = 0.0;
        forms.add(new JLabel("Scantime (ms):"), g);
        g.gridx = 1; g.weightx = 0.5;
        forms.add(cbGroupScantime, g);
        g.gridx = 2; g.weightx = 0.0;
        g.fill = GridBagConstraints.NONE;
        forms.add(chkGroupEnable, g);
        g.gridx = 3; g.weightx = 0.0;
        g.anchor = GridBagConstraints.EAST;
        forms.add(WizardUi.buttonRow(
                btnAddGroup, btnUpdateGroup, btnDeleteGroup, btnCancelGroup), g);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

        g.gridy = 4; g.gridx = 0; g.gridwidth = 4; g.weightx = 0.0;
        forms.add(lblItemFormTitle, g);

        g.gridy = 5; g.gridwidth = 1;
        g.gridx = 0; g.weightx = 0.0;
        forms.add(new JLabel("Nombre:"), g);
        g.gridx = 1; g.weightx = 0.5;
        forms.add(txtItemName, g);
        g.gridx = 2; g.weightx = 0.0;
        forms.add(new JLabel("Descripción:"), g);
        g.gridx = 3; g.weightx = 0.5;
        forms.add(txtItemDescription, g);

        g.gridy = 6;
        g.gridx = 0; g.weightx = 0.0;
        forms.add(new JLabel("Tag:"), g);
        g.gridx = 1; g.weightx = 0.5;
        forms.add(txtItemTag, g);
        g.gridx = 2; g.weightx = 0.0;
        forms.add(new JLabel("Grupo:"), g);
        g.gridx = 3; g.weightx = 0.5;
        forms.add(cbItemGroup, g);

        g.gridy = 7;
        g.gridx = 1; g.weightx = 0.5;
        forms.add(chkItemEnable, g);
        g.gridx = 3; g.weightx = 0.0;
        g.fill = GridBagConstraints.NONE;
        g.anchor = GridBagConstraints.EAST;
        forms.add(WizardUi.buttonRow(
                btnAddItem, btnUpdateItem, btnDeleteItem, btnCancelItem), g);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.WEST;

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

        btnAddGroup.addActionListener(e -> addGroup());
        btnUpdateGroup.addActionListener(e -> updateGroup());
        btnDeleteGroup.addActionListener(e -> deleteGroup());
        btnCancelGroup.addActionListener(e -> salirDeEdicionGrupo());

        btnAddItem.addActionListener(e -> addItem());
        btnUpdateItem.addActionListener(e -> updateItem());
        btnDeleteItem.addActionListener(e -> deleteItem());
        btnCancelItem.addActionListener(e -> salirDeEdicionItem());

        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.add(forms, BorderLayout.CENTER);

        JSplitPane areaSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, north, tableSplit);
        areaSplit.setResizeWeight(0.35);
        areaSplit.setDividerLocation(250);
        areaSplit.setContinuousLayout(true);

        setLayout(new BorderLayout(8, 8));
        add(areaSplit, BorderLayout.CENTER);
    }

    /** Repuebla tablas y reevalúa el estado de los formularios. */
    public void refresh() {
        rebuildTables();
        refreshGroupCombo();
        refreshItemFormState();
        refreshGroupFormState();
        warnMissingGroups();
    }

    private void rebuildTables() {
        rebuildingTables = true;
        try {
            groupModel.setRowCount(0);
            for (CommConfigData.GroupConfig g : controller.getState().getGroups()) {
                groupModel.addRow(new Object[]{
                    g.getName(), g.getDescription(), g.getScantime(),
                    g.isEnable() ? "TRUE" : "FALSE", g.getUuid()
                });
            }
            itemModel.setRowCount(0);
            for (CommConfigData.ItemConfig i : controller.getState().getItems()) {
                String groupName = controller.getState().groupNameOf(i.getUuid());
                itemModel.addRow(new Object[]{
                    i.getName(), i.getDescription(), i.getTag(),
                    tipoDelArea(i), capacidadDe(i),
                    groupName.isEmpty() ? SIN_GRUPO : groupName,
                    i.isEnable() ? "TRUE" : "FALSE", i.getUuid()
                });
            }
        } finally {
            rebuildingTables = false;
        }
        if (!groupColumnsSized && groupModel.getRowCount() > 0) {
            WizardUi.sizeColumnsToContent(tbGroups, 90, 260);
            groupColumnsSized = true;
        }
        if (!itemColumnsSized && itemModel.getRowCount() > 0) {
            WizardUi.sizeColumnsToContent(tbItems, 90, 260);
            itemColumnsSized = true;
        }
    }

    /**
     * Tipo al que está fijada el área. El guion significa que todavía admite varios:
     * el tag no declara familia y el área aún no tiene variables.
     */
    private String tipoDelArea(CommConfigData.ItemConfig item) {
        DataType type = controller.lockedType(item.getUuid());
        if (type == null) {
            return "—";
        }
        return type.bitAddressed() ? type.label() + " (1 bit)" : type.label();
    }

    /** Bytes que abarca el área, o un aviso de que no se pueden calcular. */
    private String capacidadDe(CommConfigData.ItemConfig item) {
        MemoryTag tag = MemoryTag.parse(item.getTag());
        if (tag == null) {
            return "sin tag";
        }
        int bytes = tag.byteCapacity();
        return bytes > 0 ? bytes + " bytes" : "sin límite";
    }

    /**
     * El grupo de escaneo es un requisito previo: sin al menos un grupo no se
     * puede crear ninguna área de memoria, así que el formulario queda bloqueado.
     * Al modificar un área existente el nombre y el grupo van bloqueados: el
     * nombre es la identidad del área y el grupo no se cambia desde la UI.
     */
    private void refreshItemFormState() {
        boolean hayGrupo = !controller.getState().getGroups().isEmpty();
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

    /** Repuebla el combo de grupos conservando la selección. */
    private void refreshGroupCombo() {
        CommConfigData.GroupConfig sel = (CommConfigData.GroupConfig) cbItemGroup.getSelectedItem();
        cbItemGroup.removeAllItems();
        for (CommConfigData.GroupConfig g : controller.getState().getGroups()) {
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

    // --- grupos --------------------------------------------------------------

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
        controller.addGroup(UUID.randomUUID().toString(), name,
                txtGroupDescription.getText().trim(), scantime, chkGroupEnable.isSelected());
        limpiarFormularioGrupo();
        refresh();
    }

    /**
     * Carga el grupo de la fila seleccionada en el formulario, con el nombre
     * bloqueado: el nombre y el uuid son la identidad del grupo y no se modifican.
     */
    private void onGroupRowSelected() {
        int row = tbGroups.getSelectedRow();
        if (row < 0 || row >= controller.getState().getGroups().size()) {
            salirDeEdicionGrupo();
            return;
        }
        CommConfigData.GroupConfig group = controller.getState().getGroups().get(row);
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

    private void updateGroup() {
        if (controller.getState().groupByUuid(editGroupUuid) == null) {
            salirDeEdicionGrupo();
            return;
        }
        String scantime = (String) cbGroupScantime.getSelectedItem();
        controller.updateGroup(editGroupUuid, txtGroupDescription.getText().trim(),
                scantime, chkGroupEnable.isSelected());
        salirDeEdicionGrupo();
        refresh();
    }

    /**
     * Un grupo con áreas asociadas no se puede eliminar: como el grupo es
     * obligatorio para cada área, borrarlo dejaría áreas sin grupo.
     */
    private void deleteGroup() {
        CommConfigData.GroupConfig group = controller.getState().groupByUuid(editGroupUuid);
        if (group == null) {
            salirDeEdicionGrupo();
            return;
        }
        List<String> areasDelGrupo = controller.deleteGroup(group.getUuid());
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
        salirDeEdicionGrupo();
        refresh();
    }

    // --- áreas ---------------------------------------------------------------

    /**
     * Comprueba el tag del área. Sólo se para lo que es un error evidente: que esté
     * vacío, que el rango vaya al revés o que pise a otra área.
     *
     * <p>El formato en sí no se juzga. El tag depende del driver y del PLC, así que
     * se acepta tanto {@code %DB231.DBB0[0..1848]:BOOL} como
     * {@code DB1.DBX0.0}, {@code MW2}, {@code Control_Panel.Start_Button} o
     * {@code 40001}. Un tag del que no se reconoce nada se guarda tal cual.</p>
     *
     * <p>El tag tampoco puede estar repetido. El solapamiento de bytes sólo
     * detecta el conflicto cuando el tag se puede desdoblar en bloque y código, de
     * modo que dos áreas con el mismo tag simbólico se colarían; por eso el tag
     * repetido se comprueba aparte, antes.</p>
     *
     * @param uuidEnEdicion área que se está modificando, que no choca consigo misma
     * @return el mensaje para el usuario, o null si el tag sirve
     */
    private String problemaDelTag(String tag, String uuidEnEdicion) {
        if (tag.isEmpty()) {
            return "Indique el tag del área. Por ejemplo: "
                    + "%DB231.DBB0[0..1848]:BOOL, DB1.DBX0.0, %MW66:WORD, MW2 "
                    + "o 40001.\n\n"
                    + "El símbolo % sólo hace falta con direccionamiento absoluto "
                    + "en S7-1200; con tags optimizados, S7 clásico, Modbus u OPC UA "
                    + "no se pone. El rango [0..9] y el tipo son opcionales.\n\n"
                    + "Tipos admitidos: " + DataType.nombres() + ".";
        }
        MemoryTag t = MemoryTag.parse(tag);
        if (t != null && !t.rangoValido()) {
            return "El rango del tag '" + tag + "' va al revés: empieza en "
                    + t.firstByte() + " y acaba en " + t.lastByte() + ".";
        }
        CommConfigData.ItemConfig mismoTag = controller.areaConMismoTag(tag, uuidEnEdicion);
        if (mismoTag != null) {
            return "El tag '" + tag + "' ya lo usa el área '" + mismoTag.getName() + "'.\n\n"
                    + "Es un tag sin dirección, así que no se puede comprobar si se"
                    + " pisan comparando bytes: se toma que son el mismo sitio.\n\n"
                    + "Si de verdad son sitios distintos, dótalos de una dirección con"
                    + " rango, del tipo %DB22.DBB4[10..16], y el panel lo comprobará.";
        }
        CommConfigData.ItemConfig choca = controller.areaQueChoca(tag, uuidEnEdicion);
        if (choca != null) {
            MemoryTag otro = MemoryTag.parse(choca.getTag());
            return "El tag '" + tag + "' ocupa los bytes " + t.absoluteFirst()
                    + " a " + t.absoluteLast() + " y se pisa con el área '" + choca.getName()
                    + "', que ocupa los bytes " + otro.absoluteFirst()
                    + " a " + otro.absoluteLast() + ".";
        }
        return null;
    }

    private void addItem() {
        if (controller.getState().getGroups().isEmpty()) {
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
        String problemaTag = problemaDelTag(txtItemTag.getText().trim(), null);
        if (problemaTag != null) {
            JOptionPane.showMessageDialog(this, problemaTag,
                    "Tag no válido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        controller.addItem(UUID.randomUUID().toString(), name,
                txtItemDescription.getText().trim(), txtItemTag.getText().trim(),
                chkItemEnable.isSelected(), group.getUuid());
        limpiarFormularioItem();
        refresh();
    }

    /**
     * Carga el área de la fila seleccionada en el formulario. El nombre y el
     * grupo quedan bloqueados: el nombre es la identidad del área y el grupo de
     * escaneo no se cambia desde la UI.
     */
    private void onItemRowSelected() {
        int row = tbItems.getSelectedRow();
        if (row < 0 || row >= controller.getState().getItems().size()) {
            salirDeEdicionItem();
            return;
        }
        CommConfigData.ItemConfig item = controller.getState().getItems().get(row);
        editItemUuid = item.getUuid();
        txtItemName.setText(item.getName());
        txtItemDescription.setText(item.getDescription());
        txtItemTag.setText(item.getTag());
        chkItemEnable.setSelected(item.isEnable());
        CommConfigData.GroupConfig group = controller.getState().groupByUuid(item.getGroupUuid());
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

    private void updateItem() {
        if (controller.getState().itemByUuid(editItemUuid) == null) {
            salirDeEdicionItem();
            return;
        }
        String problemaTag = problemaDelTag(txtItemTag.getText().trim(), editItemUuid);
        if (problemaTag != null) {
            JOptionPane.showMessageDialog(this, problemaTag,
                    "Tag no válido", JOptionPane.WARNING_MESSAGE);
            return;
        }
        controller.updateItem(editItemUuid, txtItemDescription.getText().trim(),
                txtItemTag.getText().trim(), chkItemEnable.isSelected());
        salirDeEdicionItem();
        refresh();
    }

    /**
     * Elimina el área. Si tiene variables configuradas también las elimina, para
     * no dejar variables apuntando a un área que ya no existe; de eso se avisa
     * antes de confirmar.
     */
    private void deleteItem() {
        CommConfigData.ItemConfig item = controller.getState().itemByUuid(editItemUuid);
        if (item == null) {
            salirDeEdicionItem();
            return;
        }
        List<String> variables = new ArrayList<>();
        for (CommConfigData.PvConfig pv : controller.getState().pvsOfArea(item.getUuid())) {
            variables.add(pv.getName());
        }
        if (!variables.isEmpty()) {
            int opcion = JOptionPane.showConfirmDialog(this,
                    "El área '" + item.getName() + "' tiene " + variables.size()
                            + " variable(s) configurada(s):\n\n" + String.join(", ", variables)
                            + "\n\nSi elimina el área, también se eliminan esas variables. "
                            + "¿Desea continuar?",
                    "Eliminar área y variables", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (opcion != JOptionPane.YES_OPTION) {
                return;
            }
        }
        controller.deleteItem(item.getUuid());
        salirDeEdicionItem();
        refresh();
    }

    /**
     * Avisa una vez por apertura si hay áreas sin grupo de escaneo, dato incompleto
     * que solo puede venir de configuraciones anteriores a la persistencia del
     * grupo en el XML.
     */
    private void warnMissingGroups() {
        if (controller.getState().isMissingGroupsWarned()) {
            return;
        }
        List<String> sinGrupo = controller.areasSinGrupoNames();
        if (sinGrupo.isEmpty()) {
            return;
        }
        controller.getState().setMissingGroupsWarned(true);
        JOptionPane.showMessageDialog(this,
                "Estas áreas de memoria quedaron guardadas sin grupo de escaneo:\n\n"
                        + String.join(", ", sinGrupo)
                        + "\n\nPor seguridad la columna \"Grupo\" no se puede editar: "
                        + "hay que volver a crear esas áreas eligiendo su grupo.",
                "Áreas sin grupo de escaneo", JOptionPane.WARNING_MESSAGE);
    }

    /** El paso 3 sólo ofrece áreas configuradas. */
    public boolean hasItems() {
        return !controller.getState().getItems().isEmpty();
    }

    /** El diálogo lo invoca cuando se cambia de dispositivo. */
    public void onDeviceChanged() {
        salirDeEdicionGrupo();
        salirDeEdicionItem();
        refresh();
    }
}
