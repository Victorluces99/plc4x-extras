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
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
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
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;
import org.openide.util.Exceptions;

/**
 * Paso 3 de 3: variables de proceso de un área de memoria.
 *
 * <p>Tres reglas gobiernan este paso:</p>
 *
 * <ul>
 *   <li>Un área es un bloque de bytes homogéneo. La primera variable elegida fija
 *       su tipo y todas las siguientes deben compartirlo, porque si no el offset
 *       de las siguientes apuntaría a una posición equivocada del PLC.</li>
 *   <li>El offset no se tipea: sale de {@code (Mediación − 1) × Tam} según la
 *       posición de la variable dentro del área. No hay campo para él porque
 *       escribirlo a mano rompería la convención en silencio.</li>
 *   <li>El PvId, la variable y el tipo son la identidad del registro y no se
 *       modifican. Mover una variable a otro PvId exige borrarla y crearla de
 *       nuevo, que es la única forma de que los offsets vuelvan a ser correctos.</li>
 * </ul>
 *
 * <p>La tabla no es editable ({@link NonEditableTableModel}): hace de espejo.
 * Al elegir una fila, sus valores modificables se cargan en el formulario y
 * {@code Añadir} se convierte en {@code Modificar} y {@code Eliminar}. Como no
 * hay botón de cancelar, volver a hacer clic en la fila seleccionada abandona la
 * modificación y limpia el formulario.</p>
 *
 * <p>Las tres operaciones escriben y persisten: el guardado sale por dos
 * enganches con el diálogo, {@code beforeSave} valida la configuración completa
 * antes de tocar nada y {@code afterSaved} vuelca a {@code comunicacion.xml} y a
 * {@code boot.db} e informa el resultado. Así el panel no muestra modales de
 * guardado ni escribe en disco.</p>
 */
public class StepPvPanel extends JPanel {

    private final CommunicationWizardController controller;
    private final PlantModelReader plantModelReader;

    private final JComboBox<CommConfigData.ItemConfig> cbArea = new JComboBox<>();
    private final JComboBox<PlantVariable> cbVariable = new JComboBox<>();
    private final JComboBox<String> cbType = new JComboBox<>();
    private final JLabel lblTypeLock = new JLabel(" ");
    private final JLabel lblFormTitle = WizardUi.formTitle("Nueva variable de proceso");

    private static final int COLUMNAS = 20;

    /**
     * Tabla espejo: no se edita en la celda y el texto largo se muestra en un
     * tooltip, porque con veinte columnas siempre hay alguna celda más angosta
     * que su contenido y hay que poder ampliarla a mano.
     */
    private final NonEditableTableModel pvModel = new NonEditableTableModel(new String[]{
        "Variable", "Área", "PvId", "Tipo", "Offset", "Descriptor", "ScanTime",
        "Scan", "Write", "Lím. bajo", "Lím. alto", "Descripción", "Formato", "Unidades",
        "Ctrl. bajo", "Ctrl. alto", "Ctrl. MinStep", "Ruta S88", "UUID", "Md5"});
    private final JTable tbPvs = new JTable(pvModel) {
        private static final long serialVersionUID = 1L;

        @Override
        public String getToolTipText(MouseEvent event) {
            int row = rowAtPoint(event.getPoint());
            int column = columnAtPoint(event.getPoint());
            if (row >= 0 && column >= 0) {
                Object value = getValueAt(row, column);
                return value == null ? null : value.toString();
            }
            return super.getToolTipText(event);
        }
    };

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

    private final JButton btnAddPv = new JButton("Añadir");
    private final JButton btnUpdatePv = new JButton("Modificar");
    private final JButton btnDeletePv = new JButton("Eliminar");
    private final JButton btnCancelPv = new JButton("Cancelar");

    /** Uuid de la variable en modo modificación, o null si se está agregando. */
    private String editPvUuid;

    /** Ancho elegido a mano por el usuario, que ya no cede ante el autoajuste. */
    private final int[] anchosUsuario = new int[COLUMNAS];
    private final boolean[] anchoEditado = new boolean[COLUMNAS];
    private boolean ajustandoAnchos;

    private boolean populating;
    /** Suprime el action listener del combo de variables al cargar una fila. */
    private boolean loadingEdit;
    /** Suprime el action listener de la tabla cuando el panel vacía la selección. */
    private boolean suppressSelection;

    private Supplier<Boolean> beforeSave;
    private Consumer<String> afterSaved;

    public StepPvPanel(CommunicationWizardController controller, PlantModelReader plantModelReader) {
        this.controller = controller;
        this.plantModelReader = plantModelReader;
        buildUi();
    }

    private void buildUi() {
        WizardUi.fixComboWidth(cbVariable, 340);
        WizardUi.fixComboWidth(cbArea, 300);

        cbType.addItem("—");
        // El tipo nunca se edita: se deduce de la variable elegida o del área.
        cbType.setEnabled(false);

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
                    String text = v.getPath() == null || v.getPath().isEmpty()
                            ? v.getName() + "  (" + v.getType() + ")"
                            : v.getPath() + "/" + v.getName();
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
        cbArea.addActionListener(e -> {
            if (!populating) {
                refreshVariables();
            }
        });

        tbPvs.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || suppressSelection) {
                return;
            }
            // Cambiar de fila dispara dos eventos (deselección y selección). El
            // trabajo se corre para el próximo ciclo, cuando la selección ya se
            // quedó quieta, y así se lee una sola vez la fila definitiva.
            SwingUtilities.invokeLater(this::onPvRowSelected);
        });

        btnAddPv.addActionListener(e -> agregarPv());
        btnUpdatePv.addActionListener(e -> modificarPv());
        btnDeletePv.addActionListener(e -> eliminarPv());
        btnCancelPv.addActionListener(e -> salirDeEdicionPv());

        JPanel form = WizardUi.formGrid();
        GridBagConstraints g = WizardUi.formConstraintsTight();

        g.gridx = 0; g.gridy = 0; g.gridwidth = 4;
        form.add(WizardUi.stepTitle("Paso 3 de 3 — Variable de proceso"), g);
        g.gridwidth = 1;

        int row = 0;
        WizardUi.addLabel(form, g, ++row, "Área (PvId):");
        g.gridx = 1; g.gridwidth = 3;
        form.add(cbArea, g);
        g.gridwidth = 1;

        WizardUi.addLabel(form, g, ++row, "Variable (del área S88):");
        g.gridx = 1; g.gridwidth = 3;
        form.add(cbVariable, g);
        g.gridwidth = 1;

        g.gridx = 0; g.gridy = ++row; g.gridwidth = 4; g.weightx = 1.0;
        form.add(lblTypeLock, g);
        g.gridwidth = 1;

        WizardUi.addLabel(form, g, ++row, "Tipo:");
        g.gridx = 1;
        form.add(cbType, g);

        WizardUi.addLabel(form, g, ++row, "Descriptor:");
        g.gridx = 1;
        form.add(txtDescriptor, g);
        g.gridx = 2;
        form.add(new JLabel("ScanTime:"), g);
        g.gridx = 3;
        form.add(txtScanTime, g);

        WizardUi.addLabel(form, g, ++row, "Opciones:");
        g.gridx = 1;
        form.add(chkScanEnable, g);
        g.gridx = 2;
        form.add(chkWriteEnable, g);

        WizardUi.addLabel(form, g, ++row, "Display Low/High:");
        g.gridx = 1;
        form.add(txtDisplayLimitLow, g);
        g.gridx = 2;
        form.add(txtDisplayLimitHigh, g);

        WizardUi.addLabel(form, g, ++row, "Display Description:");
        g.gridx = 1; g.gridwidth = 3; g.weightx = 0.0; g.fill = GridBagConstraints.NONE;
        form.add(txtDisplayDescription, g);
        g.gridwidth = 1; g.weightx = 1.0; g.fill = GridBagConstraints.HORIZONTAL;

        WizardUi.addLabel(form, g, ++row, "Display Format/Units:");
        g.gridx = 1;
        form.add(txtDisplayFormat, g);
        g.gridx = 2;
        form.add(txtDisplayUnits, g);

        WizardUi.addLabel(form, g, ++row, "Control Low/High:");
        g.gridx = 1;
        form.add(txtControlLimitLow, g);
        g.gridx = 2;
        form.add(txtControlLimitHigh, g);

        WizardUi.addLabel(form, g, ++row, "Control MinStep:");
        g.gridx = 1;
        form.add(txtControlMinStep, g);

        g.gridx = 0; g.gridy = ++row; g.gridwidth = 4; g.weightx = 0.0;
        form.add(lblFormTitle, g);
        g.gridwidth = 1;

        WizardUi.addLabel(form, g, ++row, "Acciones:");
        g.gridx = 1; g.gridwidth = 3; g.weightx = 1.0;
        form.add(WizardUi.buttonRow(btnAddPv, btnUpdatePv, btnDeletePv, btnCancelPv), g);
        g.gridwidth = 1;

        JPanel pvTable = new JPanel(new BorderLayout(0, 4));
        pvTable.setBorder(BorderFactory.createTitledBorder("Variables configuradas"));
        tbPvs.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tbPvs.setFillsViewportHeight(false);
        tbPvs.setIntercellSpacing(new Dimension(6, 2));
        JScrollPane pvScroll = new JScrollPane(tbPvs);
        WizardUi.limitTableSpace(pvScroll, 700, 460);
        pvTable.add(pvScroll, BorderLayout.CENTER);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        formScroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, formScroll, pvTable);
        split.setResizeWeight(0.0);
        split.setDividerLocation(380);
        split.setContinuousLayout(true);

        setLayout(new BorderLayout(8, 8));
        add(split, BorderLayout.CENTER);

        instalarAnchosAjustables();
        refreshPvFormState();
    }

    /** Sólo para pruebas: permite medir la tabla sin montar la ventana. */
    JTable getPvTable() {
        return tbPvs;
    }

    public void setSaveHooks(Supplier<Boolean> beforeSave, Consumer<String> afterSaved) {
        this.beforeSave = beforeSave;
        this.afterSaved = afterSaved;
    }

    /** Cadena de refresco del paso, en el mismo orden que el asistente original. */
    public void refresh() {
        refreshPvAreas();
        refreshVariables();
        refreshPvTable();
        refreshPvFormState();
    }

    public void refreshData() {
        refresh();
    }

    private CommConfigData.ItemConfig selectedArea() {
        return (CommConfigData.ItemConfig) cbArea.getSelectedItem();
    }

    private String selectedAreaUuid() {
        CommConfigData.ItemConfig area = selectedArea();
        return area == null ? null : area.getUuid();
    }

    /**
     * Sólo se listan áreas con grupo de escaneo: una variable no puede quedar
     * asociada a un área incompleta.
     */
    private void refreshPvAreas() {
        CommConfigData.ItemConfig sel = selectedArea();
        populating = true;
        try {
            cbArea.removeAllItems();
            for (CommConfigData.ItemConfig i : controller.getState().getItems()) {
                if (!controller.getState().groupNameOf(i.getUuid()).isEmpty()) {
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
        } finally {
            populating = false;
        }
    }

    /**
     * Llena el combo de variables de planta: sólo las del tipo al que está fijado
     * el área, marcando como usadas las que ya están configuradas.
     *
     * <p>La variable de la fila que se está modificando queda exceptuada del
     * marcado: si no, aparecería como ocupada y no podría volver a seleccionarse
     * en su propio formulario.</p>
     */
    private void refreshVariables() {
        cbVariable.removeAllItems();
        cbVariable.addItem(null);
        cbType.removeAllItems();
        cbType.addItem("—");
        refreshTypeLock();

        if (controller.getState().getSelectedDevice() == null) {
            return;
        }
        String areaId = controller.getState().getSelectedDevice().getS88Node();
        if (areaId == null || areaId.isEmpty()) {
            return;
        }
        List<PlantVariable> variables;
        try {
            variables = plantModelReader.loadAreaVariables(areaId);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
            return;
        }
        String lockedType = controller.itemLockedType(selectedAreaUuid());
        Set<String> usedKeys = controller.getUsedPvKeys();
        CommConfigData.PvConfig enEdicion = controller.getState().pvByUuid(editPvUuid);
        if (enEdicion != null) {
            usedKeys.remove(CommunicationWizardController.pvKey(
                    enEdicion.getS88Path(), enEdicion.getName()));
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

    private void refreshTypeLock() {
        CommConfigData.ItemConfig area = selectedArea();
        if (area == null) {
            lblTypeLock.setText(" ");
            return;
        }
        String locked = controller.itemLockedType(area.getUuid());
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
        if (loadingEdit) {
            return;
        }
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

    private void refreshPvTable() {
        pvModel.setRowCount(0);
        for (CommConfigData.PvConfig pv : controller.getState().getPvs()) {
            CommConfigData.ItemConfig area = controller.getState().itemByUuid(pv.getId());
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
        ajustarAnchos();
        // La tabla queda sin selección: el modelo se reemplazó entero, así que
        // cualquier fila que quedara marcada sería una posición vieja. Sin esto,
        // la reconstrucción dispararía el listener y entraría en modo edición
        // sobre una fila al azar.
        suppressSelection = true;
        try {
            tbPvs.clearSelection();
        } finally {
            suppressSelection = false;
        }
    }

    // --- ancho de columnas ---------------------------------------------------

    /**
     * Registra el ancho que el usuario elija arrastrando el borde. Se escucha el
     * cambio de la propiedad {@code width} porque es el único evento que dispara
     * un redimensionado manual, y el flag {@code ajustandoAnchos} evita que el
     * autoajuste se confunda con una elección del usuario.
     */
    private void instalarAnchosAjustables() {
        for (int i = 0; i < tbPvs.getColumnModel().getColumnCount(); i++) {
            final int indice = i;
            tbPvs.getColumnModel().getColumn(i).addPropertyChangeListener(e -> {
                if (ajustandoAnchos || !TableColumn.COLUMN_WIDTH_PROPERTY.equals(e.getPropertyName())) {
                    return;
                }
                TableColumn columna = tbPvs.getColumnModel().getColumn(indice);
                anchosUsuario[indice] = columna.getWidth();
                anchoEditado[indice] = true;
            });
        }
    }

    /**
     * Ajusta cada columna a su contenido y después devuelve el control al
     * usuario en las que ya agrandó a mano. Se recalcula en cada refresco, no una
     * sola vez: una variable agregada más tarde trae una descripción más larga
     * que las que había cuando se hizo el ajuste inicial.
     */
    private void ajustarAnchos() {
        ajustandoAnchos = true;
        try {
            WizardUi.sizeColumnsToContent(tbPvs, 90, 320);
            for (int i = 0; i < COLUMNAS && i < tbPvs.getColumnModel().getColumnCount(); i++) {
                if (anchoEditado[i]) {
                    TableColumn columna = tbPvs.getColumnModel().getColumn(i);
                    columna.setPreferredWidth(anchosUsuario[i]);
                    columna.setWidth(anchosUsuario[i]);
                }
            }
        } finally {
            ajustandoAnchos = false;
        }
    }

    // --- selección de fila ----------------------------------------------------

    private CommConfigData.PvConfig pvAt(int row) {
        List<CommConfigData.PvConfig> pvs = controller.getState().getPvs();
        return row >= 0 && row < pvs.size() ? pvs.get(row) : null;
    }

    /**
     * Al elegir una fila se carga el formulario y se entra en modo modificación.
     * Un segundo clic sobre la misma fila abandona la modificación.
     */
    private void onPvRowSelected() {
        if (suppressSelection) {
            return;
        }
        CommConfigData.PvConfig pv = pvAt(tbPvs.getSelectedRow());
        if (pv == null || pv.getUuid().equals(editPvUuid)) {
            salirDeEdicionPv();
            return;
        }
        editPvUuid = pv.getUuid();
        cargarEnFormulario(pv);
        refreshPvFormState();
    }

    /**
     * Carga la fila seleccionada. El PvId, la variable y el tipo quedan
     * bloqueados; el resto de los campos es editable.
     */
    private void cargarEnFormulario(CommConfigData.PvConfig pv) {
        CommConfigData.ItemConfig area = controller.getState().itemByUuid(pv.getId());
        if (area != null) {
            for (int i = 0; i < cbArea.getItemCount(); i++) {
                if (cbArea.getItemAt(i).getUuid().equals(area.getUuid())) {
                    cbArea.setSelectedIndex(i);
                    break;
                }
            }
        }
        txtDescriptor.setText(pv.getDescriptor());
        txtScanTime.setText(pv.getScanTime());
        chkScanEnable.setSelected(pv.isScanEnable());
        chkWriteEnable.setSelected(pv.isWriteEnable());
        txtDisplayLimitLow.setText(pv.getDisplayLimitLow());
        txtDisplayLimitHigh.setText(pv.getDisplayLimitHigh());
        txtDisplayDescription.setText(pv.getDisplayDescription());
        txtDisplayFormat.setText(pv.getDisplayFormat());
        txtDisplayUnits.setText(pv.getDisplayUnits());
        txtControlLimitLow.setText(pv.getControlLimitLow());
        txtControlLimitHigh.setText(pv.getControlLimitHigh());
        txtControlMinStep.setText(pv.getControlMinStep());
        lblFormTitle.setText("Modificando variable: " + pv.getName());
        seleccionarVariable(pv);
    }

    /**
     * Deja el combo mostrando la variable de la fila y el tipo que tiene
     * guardado. Se reprime el action listener porque la variable llega con su
     * propio tipo, que es el dato a mostrar, no el que se deduce de la selección.
     */
    private void seleccionarVariable(CommConfigData.PvConfig pv) {
        String key = CommunicationWizardController.pvKey(pv.getS88Path(), pv.getName());
        loadingEdit = true;
        try {
            for (int i = 0; i < cbVariable.getItemCount(); i++) {
                PlantVariable v = cbVariable.getItemAt(i);
                if (v != null && key.equals(v.getKey())) {
                    cbVariable.setSelectedIndex(i);
                    break;
                }
            }
            cbType.removeAllItems();
            cbType.addItem(pv.getType());
        } finally {
            loadingEdit = false;
        }
    }

    private void salirDeEdicionPv() {
        editPvUuid = null;
        suppressSelection = true;
        try {
            tbPvs.clearSelection();
        } finally {
            suppressSelection = false;
        }
        limpiarFormularioPv();
        refreshPvFormState();
    }

    private void limpiarFormularioPv() {
        txtDescriptor.setText("");
        txtScanTime.setText("");
        chkScanEnable.setSelected(true);
        chkWriteEnable.setSelected(false);
        txtDisplayLimitLow.setText("");
        txtDisplayLimitHigh.setText("");
        txtDisplayDescription.setText("");
        txtDisplayFormat.setText("");
        txtDisplayUnits.setText("");
        txtControlLimitLow.setText("");
        txtControlLimitHigh.setText("");
        txtControlMinStep.setText("");
        cbVariable.setSelectedIndex(0);
        cbType.removeAllItems();
        cbType.addItem("—");
        lblFormTitle.setText("Nueva variable de proceso");
    }

    /**
     * Estado del formulario: PvId, variable y tipo quedan bloqueados al
     * modificar porque son la identidad del registro y de la posición en memoria.
     */
    private void refreshPvFormState() {
        boolean editando = editPvUuid != null;
        boolean hayArea = selectedArea() != null;

        btnAddPv.setEnabled(hayArea && !editando);
        btnUpdatePv.setEnabled(hayArea && editando);
        btnDeletePv.setEnabled(hayArea && editando);
        btnCancelPv.setEnabled(editando);

        cbArea.setEnabled(hayArea && !editando);
        cbVariable.setEnabled(hayArea && !editando);
        cbType.setEnabled(false);

        txtDescriptor.setEnabled(hayArea);
        txtScanTime.setEnabled(hayArea);
        chkScanEnable.setEnabled(hayArea);
        chkWriteEnable.setEnabled(hayArea);
        txtDisplayLimitLow.setEnabled(hayArea);
        txtDisplayLimitHigh.setEnabled(hayArea);
        txtDisplayDescription.setEnabled(hayArea);
        txtDisplayFormat.setEnabled(hayArea);
        txtDisplayUnits.setEnabled(hayArea);
        txtControlLimitLow.setEnabled(hayArea);
        txtControlLimitHigh.setEnabled(hayArea);
        txtControlMinStep.setEnabled(hayArea);
    }

    // --- acciones -------------------------------------------------------------

    /**
     * Agrega la variable del formulario al área seleccionada y persiste. El
     * offset lo calcula el controlador a partir de la posición que le toca.
     */
    private void agregarPv() {
        if (!validarCamposObligatorios()) {
            return;
        }
        // Nada se escribe si la configuración no es válida: el grupo de escaneo
        // del área es obligatorio.
        if (!validarAntesDeEscribir()) {
            return;
        }
        CommConfigData.ItemConfig area = selectedArea();
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
        int offset = controller.nextOffset(area.getUuid(), variable.getType());
        if (offset < 0) {
            JOptionPane.showMessageDialog(this,
                    "No se puede calcular el offset automático para el tipo '" + variable.getType()
                            + "'; el tamaño en bytes no está definido.",
                    "Offset no calculable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        controller.addPv(new CommConfigData.PvConfig(
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
                CommunicationWizardController.pvMd5(),
                variable.getPath()));
        salirDeEdicionPv();
        refrescarYPersistir("Variable '" + variable.getName() + "' guardada en '"
                + area.getName() + "' con offset " + offset + ".");
    }

    /**
     * Aplica los valores editables del formulario sobre la variable seleccionada.
     * El PvId, la variable, el tipo y el offset quedan intactos.
     */
    private void modificarPv() {
        CommConfigData.PvConfig pv = controller.getState().pvByUuid(editPvUuid);
        if (pv == null) {
            salirDeEdicionPv();
            return;
        }
        if (!validarCamposObligatorios()) {
            return;
        }
        if (!validarAntesDeEscribir()) {
            return;
        }
        controller.updatePv(pv.getUuid(),
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
                txtControlMinStep.getText().trim());
        salirDeEdicionPv();
        refrescarYPersistir("Variable '" + pv.getName() + "' actualizada.");
    }

    /**
     * Elimina la variable del área. La fila desaparece de {@code boot.db}
     * porque la tabla se regenera entera desde el XML, que es la fuente de verdad.
     */
    private void eliminarPv() {
        CommConfigData.PvConfig pv = controller.getState().pvByUuid(editPvUuid);
        if (pv == null) {
            salirDeEdicionPv();
            return;
        }
        CommConfigData.ItemConfig area = controller.getState().itemByUuid(pv.getId());
        if (!validarAntesDeEscribir()) {
            return;
        }
        int opcion = JOptionPane.showConfirmDialog(this,
                "Va a eliminar la variable '" + pv.getName() + "'"
                        + (area == null ? "" : " del área '" + area.getName() + "'") + ".\n\n"
                        + "También se quitará de comunicacion.xml y de boot.db.\n\n"
                        + "¿Desea continuar?",
                "Eliminar variable", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (opcion != JOptionPane.YES_OPTION) {
            return;
        }
        controller.deletePv(pv.getUuid());
        salirDeEdicionPv();
        refrescarYPersistir("Variable '" + pv.getName() + "' eliminada.");
    }

    /**
     * Los diez campos de texto del formulario son obligatorios: un valor en
     * blanco se escribiría como atributo vacío en {@code comunicacion.xml} y
     * llegaría al PLC como un valor sin definir, que es peor que un valor
     * incorrecto visible. Scan y Write no se validan porque son casillas y
     * siempre tienen estado.
     *
     * @return las etiquetas de los campos vacíos
     */
    private List<String> camposObligatoriosVacios() {
        List<String> vacios = new ArrayList<>();
        agregarSiVacio(vacios, "Descriptor", txtDescriptor);
        agregarSiVacio(vacios, "ScanTime", txtScanTime);
        agregarSiVacio(vacios, "Display límite bajo", txtDisplayLimitLow);
        agregarSiVacio(vacios, "Display límite alto", txtDisplayLimitHigh);
        agregarSiVacio(vacios, "Display descripción", txtDisplayDescription);
        agregarSiVacio(vacios, "Display formato", txtDisplayFormat);
        agregarSiVacio(vacios, "Display unidades", txtDisplayUnits);
        agregarSiVacio(vacios, "Control límite bajo", txtControlLimitLow);
        agregarSiVacio(vacios, "Control límite alto", txtControlLimitHigh);
        agregarSiVacio(vacios, "Control MinStep", txtControlMinStep);
        return vacios;
    }

    private void agregarSiVacio(List<String> vacios, String etiqueta, JTextField campo) {
        if (campo.getText().trim().isEmpty()) {
            vacios.add(etiqueta);
        }
    }

    /** @return true si no falta ningún campo obligatorio */
    private boolean validarCamposObligatorios() {
        List<String> vacios = camposObligatoriosVacios();
        if (vacios.isEmpty()) {
            return true;
        }
        JOptionPane.showMessageDialog(this,
                "Complete estos campos antes de guardar la variable:\n\n"
                        + String.join("\n", vacios),
                "Campos incompletos", JOptionPane.WARNING_MESSAGE);
        return false;
    }

    /** @return true si la configuración completa es válida y se puede escribir */
    private boolean validarAntesDeEscribir() {
        return beforeSave == null || Boolean.TRUE.equals(beforeSave.get());
    }

    private void refrescarYPersistir(String what) {
        refreshVariables();
        refreshPvTable();
        refreshPvFormState();
        if (afterSaved != null) {
            afterSaved.accept(what);
        }
    }
}
