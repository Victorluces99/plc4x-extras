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
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultFormatter;
import javax.swing.table.TableColumn;
import org.apache.plc4x.malbec.projecttype.panelcategory.panel.CommConfigData;
import org.openide.util.Exceptions;

public class StepPvPanel extends JPanel {

    private final CommunicationWizardController controller;
    private final PlantModelReader plantModelReader;
    private final JComboBox<CommConfigData.ItemConfig> cbArea = new JComboBox<>();
    private final JComboBox<PlantVariable> cbVariable = new JComboBox<>();
    private final JComboBox<DataType> cbType = new JComboBox<>();
    private final JLabel lblTypeLock = new JLabel(" ");
    private final JLabel lblFormTitle = WizardUi.formTitle("Nueva variable de proceso");

    private static final int COLUMNAS = 20;
    private static final int LIMITE_MIN = 1;
    private static final int LIMITE_MAX = 65535;
    private static final int LIMITE_INICIAL = LIMITE_MIN;
    private static final int SCANTIME_MIN = 100;
    private static final int SCANTIME_MAX = 1000;
    private static final int SCANTIME_PASO = 100;

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

    private final JTextField txtDescriptor
            = WizardUi.textoLimitado(14, WizardUi.MAX_DESCRIPCION);
    private final JComboBox<String> cbScanTime = new JComboBox<>();
    private final JCheckBox chkScanEnable = new JCheckBox("ScanEnable", true);
    private final JCheckBox chkWriteEnable = new JCheckBox("WriteEnable", false);

    private final JSpinner spDisplayLimitLow = spinnerDeLimite();
    private final JSpinner spDisplayLimitHigh = spinnerDeLimite();
    private final JTextField txtDisplayDescription
            = WizardUi.textoLimitado(14, WizardUi.MAX_DESCRIPCION);
    private final JTextField txtDisplayFormat = new JTextField(8);
    private final JTextField txtDisplayUnits = new JTextField(8);
    private final JSpinner spControlLimitLow = spinnerDeLimite();
    private final JSpinner spControlLimitHigh = spinnerDeLimite();
    private final JSpinner spControlMinStep = spinnerDeLimite();

    private final JButton btnAddPv = new JButton("Añadir");
    private final JButton btnUpdatePv = new JButton("Modificar");
    private final JButton btnDeletePv = new JButton("Eliminar");
    private final JButton btnCancelPv = new JButton("Cancelar");

    private String editPvUuid;
    private final int[] anchosUsuario = new int[COLUMNAS];
    private final boolean[] anchoEditado = new boolean[COLUMNAS];
    private boolean ajustandoAnchos;
    private boolean populating;
    private boolean loadingEdit;
    private boolean suppressSelection;

    private final List<String> ajustesAlCargar = new ArrayList<>();
    private Supplier<Boolean> beforeSave;
    private Consumer<String> afterSaved;

    public StepPvPanel(CommunicationWizardController controller, PlantModelReader plantModelReader) {
        this.controller = controller;
        this.plantModelReader = plantModelReader;
        buildUi();
    }

    private static JSpinner spinnerDeLimite() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(
                LIMITE_INICIAL, LIMITE_MIN, LIMITE_MAX, 1));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "0");
        ((DefaultFormatter) ((JFormattedTextField) editor.getTextField())
                .getFormatter()).setAllowsInvalid(false);
        spinner.setEditor(editor);
        return spinner;
    }

    private static String limiteDe(JSpinner spinner) {
        return String.valueOf(((Number) spinner.getValue()).intValue());
    }

    private void buildUi() {
        WizardUi.fixComboWidth(cbVariable, 340);
        WizardUi.fixComboWidth(cbArea, 300);
        for (int ms = SCANTIME_MIN; ms <= SCANTIME_MAX; ms += SCANTIME_PASO) {
            cbScanTime.addItem(String.valueOf(ms));
        }
        cbScanTime.setEditable(false);
        cbScanTime.setSelectedIndex(-1);
        WizardUi.fixComboWidth(cbScanTime, 110);
        cbType.setSelectedIndex(-1);
        cbType.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof DataType t) {
                    boolean elegible = controller.typeAllowed(selectedAreaUuid(), t);
                    label.setEnabled(elegible);
                    label.setText(t.label() + "  ("
                            + (elegible ? pesoDe(t) : "no disponible") + ")");
                } else {
                    label.setText("—");
                }
                return label;
            }
        });

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
            SwingUtilities.invokeLater(this::onPvRowSelected);
        });

        btnAddPv.addActionListener(e -> agregarPv());
        btnUpdatePv.addActionListener(e -> modificarPv());
        btnDeletePv.addActionListener(e -> eliminarPv());
        btnCancelPv.addActionListener(e -> salirDeEdicionPv());

        JPanel form = WizardUi.formGrid();
        GridBagConstraints g = WizardUi.formConstraintsTight();

        g.gridx = 0;
        g.gridy = 0;
        g.gridwidth = 4;
        form.add(WizardUi.stepTitle("Paso 3 de 3 — Variable de proceso"), g);
        g.gridwidth = 1;

        int row = 0;
        WizardUi.addLabel(form, g, ++row, "Área (PvId):");
        g.gridx = 1;
        g.gridwidth = 3;
        form.add(cbArea, g);
        g.gridwidth = 1;

        WizardUi.addLabel(form, g, ++row, "Variable (del área S88):");
        g.gridx = 1;
        g.gridwidth = 3;
        form.add(cbVariable, g);
        g.gridwidth = 1;

        g.gridx = 0;
        g.gridy = ++row;
        g.gridwidth = 4;
        g.weightx = 1.0;
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
        form.add(cbScanTime, g);

        WizardUi.addLabel(form, g, ++row, "Opciones:");
        g.gridx = 1;
        form.add(chkScanEnable, g);
        g.gridx = 2;
        form.add(chkWriteEnable, g);

        WizardUi.addLabel(form, g, ++row, "Display Low/High:");
        g.gridx = 1;
        form.add(spDisplayLimitLow, g);
        g.gridx = 2;
        form.add(spDisplayLimitHigh, g);

        WizardUi.addLabel(form, g, ++row, "Display Description:");
        g.gridx = 1;
        g.gridwidth = 3;
        g.weightx = 0.0;
        g.fill = GridBagConstraints.NONE;
        form.add(txtDisplayDescription, g);
        g.gridwidth = 1;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;

        WizardUi.addLabel(form, g, ++row, "Display Format/Units:");
        g.gridx = 1;
        form.add(txtDisplayFormat, g);
        g.gridx = 2;
        form.add(txtDisplayUnits, g);

        WizardUi.addLabel(form, g, ++row, "Control Low/High:");
        g.gridx = 1;
        form.add(spControlLimitLow, g);
        g.gridx = 2;
        form.add(spControlLimitHigh, g);

        WizardUi.addLabel(form, g, ++row, "Control MinStep:");
        g.gridx = 1;
        form.add(spControlMinStep, g);

        g.gridx = 0;
        g.gridy = ++row;
        g.gridwidth = 4;
        g.weightx = 0.0;
        form.add(lblFormTitle, g);
        g.gridwidth = 1;

        WizardUi.addLabel(form, g, ++row, "Acciones:");
        g.gridx = 1;
        g.gridwidth = 3;
        g.weightx = 1.0;
        form.add(WizardUi.buttonRow(btnAddPv, btnUpdatePv, btnDeletePv, btnCancelPv), g);
        g.gridwidth = 1;

        JPanel pvTable = new JPanel(new BorderLayout(0, 4));
        pvTable.setBorder(BorderFactory.createTitledBorder("Variables configuradas"));
        tbPvs.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tbPvs.getTableHeader().setReorderingAllowed(false);
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

    JTable getPvTable() {
        return tbPvs;
    }

    JComboBox<String> getScanTimeCombo() {
        return cbScanTime;
    }

    JTextField getDescriptor() {
        return txtDescriptor;
    }

    JTextField getDisplayDescription() {
        return txtDisplayDescription;
    }

    JTextField getDisplayFormat() {
        return txtDisplayFormat;
    }

    JTextField getDisplayUnits() {
        return txtDisplayUnits;
    }

    JSpinner getDisplayLimitLow() {
        return spDisplayLimitLow;
    }

    JSpinner getDisplayLimitHigh() {
        return spDisplayLimitHigh;
    }

    JSpinner getControlLimitLow() {
        return spControlLimitLow;
    }

    JSpinner getControlLimitHigh() {
        return spControlLimitHigh;
    }

    JSpinner getControlMinStep() {
        return spControlMinStep;
    }

    List<String> getAjustesAlCargar() {
        return List.copyOf(ajustesAlCargar);
    }

    public void setSaveHooks(Supplier<Boolean> beforeSave, Consumer<String> afterSaved) {
        this.beforeSave = beforeSave;
        this.afterSaved = afterSaved;
    }

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

    private void refreshVariables() {
        cbVariable.removeAllItems();
        cbVariable.addItem(null);
        rellenarTipos();

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
        DataType lockedType = controller.lockedType(selectedAreaUuid());
        Set<String> usedKeys = controller.getUsedPvKeys();
        CommConfigData.PvConfig enEdicion = controller.getState().pvByUuid(editPvUuid);
        if (enEdicion != null) {
            usedKeys.remove(CommunicationWizardController.pvKey(
                    enEdicion.getS88Path(), enEdicion.getName()));
        }
        for (PlantVariable v : variables) {
            if (!controller.areaAdmiteTipo(selectedAreaUuid(), v.getType(), lockedType)) {
                continue;
            }
            if (usedKeys.contains(v.getKey())) {
                v.setUsed(true);
            }
            cbVariable.addItem(v);
        }
    }

    private void rellenarTipos() {
        cbType.removeAllItems();
        for (DataType t : controller.typesForCombo(selectedAreaUuid())) {
            cbType.addItem(t);
        }
        cbType.setSelectedIndex(-1);
        List<DataType> permitidos = controller.typesAllowed(selectedAreaUuid());
        if (permitidos.size() == 1) {
            cbType.setSelectedItem(permitidos.get(0));
        }
        refreshTypeLock();
    }

    private static String pesoDe(DataType type) {
        return type.bitAddressed() ? "1 bit" : type.byteSize() + " bytes";
    }

    private DataType tipoElegido() {
        Object sel = cbType.getSelectedItem();
        return sel instanceof DataType t ? t : null;
    }

    private void seleccionarTipo(DataType type) {
        for (int i = 0; i < cbType.getItemCount(); i++) {
            if (cbType.getItemAt(i) == type) {
                cbType.setSelectedIndex(i);
                return;
            }
        }
    }

    private void refreshTypeLock() {
        CommConfigData.ItemConfig area = selectedArea();
        if (area == null) {
            lblTypeLock.setText(" ");
            return;
        }
        DataType elegido = tipoElegido();
        StringBuilder texto = new StringBuilder("<html>El área <b>")
                .append(area.getName()).append("</b> ");
        if (elegido == null) {
            texto.append("aún no tiene tipo: elígelo arriba. Todas sus variables")
                    .append(" usarán ese mismo tipo.");
        } else {
            texto.append("usa <b>").append(elegido.label()).append("</b> (")
                    .append(pesoDe(elegido)).append(" por variable). Todas sus")
                    .append(" variables serán de ese tipo.");
        }
        int libres = controller.areaFreeSlots(area.getUuid(), elegido);
        int bytesLibres = controller.areaFreeBytes(area.getUuid());
        if (bytesLibres < 0) {
            texto.append("<br>Del tag no se sabe el tamaño, así que no se lleva la cuenta.");
        } else if (libres <= 0) {
            texto.append("<br><b>No cabe una variable más: se ha llegado al límite.</b>")
                    .append(" El área abarca ")
                    .append(controller.areaCapacityBytes(area.getUuid()))
                    .append(" bytes y están todos ocupados.");
        } else {
            texto.append("<br>Quedan <b>").append(bytesLibres).append(" bytes</b> libres, ")
                    .append("que es lo mismo que <b>").append(libres)
                    .append(" ").append(elegido == null ? "variables" : elegido.label())
                    .append("</b> de ").append(pesoDe(elegido)).append(".");
        }
        lblTypeLock.setText(texto.append("</html>").toString());
    }

    private void onVariableSelected() {
        if (loadingEdit) {
            return;
        }
        PlantVariable v = (PlantVariable) cbVariable.getSelectedItem();
        if (v == null) {
            cbType.setSelectedIndex(-1);
            refreshTypeLock();
            return;
        }
        if (v.isUsed()) {
            JOptionPane.showMessageDialog(this,
                    "La variable '" + v.getKey() + "' ya está en uso.",
                    "Variable ocupada", JOptionPane.WARNING_MESSAGE);
            cbVariable.setSelectedIndex(0);
            return;
        }
        seleccionarTipoDePlanta(v);
        refreshTypeLock();
    }

    private void seleccionarTipoDePlanta(PlantVariable v) {
        List<DataType> permitidos = controller.typesAllowed(selectedAreaUuid());
        DataType exacto = DataType.find(v.getType());
        if (exacto != null && permitidos.contains(exacto)) {
            seleccionarTipo(exacto);
            return;
        }
        MemoryTag tag = controller.areaTag(selectedAreaUuid());
        if (tag != null) {
            for (DataType t : DataType.candidatos(v.getType(), tag.codeBits())) {
                if (permitidos.contains(t)) {
                    seleccionarTipo(t);
                    return;
                }
            }
        }
        seleccionarTipo(permitidos.isEmpty() ? null : permitidos.get(0));
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
        suppressSelection = true;
        try {
            tbPvs.clearSelection();
        } finally {
            suppressSelection = false;
        }
    }

    void instalarAnchosAjustables() {
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

    private CommConfigData.PvConfig pvAt(int row) {
        List<CommConfigData.PvConfig> pvs = controller.getState().getPvs();
        return row >= 0 && row < pvs.size() ? pvs.get(row) : null;
    }

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

    void cargarEnFormulario(CommConfigData.PvConfig pv) {
        CommConfigData.ItemConfig area = controller.getState().itemByUuid(pv.getId());
        if (area != null) {
            for (int i = 0; i < cbArea.getItemCount(); i++) {
                if (cbArea.getItemAt(i).getUuid().equals(area.getUuid())) {
                    cbArea.setSelectedIndex(i);
                    break;
                }
            }
        }
        ajustesAlCargar.clear();
        txtDescriptor.setText(pv.getDescriptor());
        seleccionarScanTime(pv.getScanTime());
        chkScanEnable.setSelected(pv.isScanEnable());
        chkWriteEnable.setSelected(pv.isWriteEnable());
        spDisplayLimitLow.setValue(aLimite(pv.getDisplayLimitLow(), "Display límite bajo"));
        spDisplayLimitHigh.setValue(aLimite(pv.getDisplayLimitHigh(), "Display límite alto"));
        txtDisplayDescription.setText(pv.getDisplayDescription());
        txtDisplayFormat.setText(pv.getDisplayFormat());
        txtDisplayUnits.setText(pv.getDisplayUnits());
        spControlLimitLow.setValue(aLimite(pv.getControlLimitLow(), "Control límite bajo"));
        spControlLimitHigh.setValue(aLimite(pv.getControlLimitHigh(), "Control límite alto"));
        spControlMinStep.setValue(aLimite(pv.getControlMinStep(), "Control MinStep"));
        lblFormTitle.setText("Modificando variable: " + pv.getName());
        seleccionarVariable(pv);
    }

    private void seleccionarScanTime(String scanTime) {
        cbScanTime.setSelectedIndex(-1);
        if (scanTime != null) {
            for (int i = 0; i < cbScanTime.getItemCount(); i++) {
                if (cbScanTime.getItemAt(i).equals(scanTime.trim())) {
                    cbScanTime.setSelectedIndex(i);
                    return;
                }
            }
            ajustesAlCargar.add("ScanTime: '" + scanTime.trim() + "' no es una de las"
                    + " ofertas (" + SCANTIME_MIN + " a " + SCANTIME_MAX
                    + " de " + SCANTIME_PASO + " en " + SCANTIME_PASO + "),"
                    + " así que hay que elegir uno");
        }
    }

    private int aLimite(String valor, String etiqueta) {
        String texto = valor == null ? "" : valor.trim();
        double numero;
        try {
            numero = Double.parseDouble(texto.isEmpty() ? "x" : texto.replace(',', '.'));
        } catch (NumberFormatException e) {
            ajustesAlCargar.add(etiqueta + ": '" + texto + "' no es un número,"
                    + " se guardará " + LIMITE_INICIAL);
            return LIMITE_INICIAL;
        }
        int entero = (int) numero;
        if (entero < LIMITE_MIN) {
            ajustesAlCargar.add(etiqueta + ": '" + texto + "' está por debajo de "
                    + LIMITE_MIN + ", se guardará " + LIMITE_MIN);
            return LIMITE_MIN;
        }
        if (entero > LIMITE_MAX) {
            ajustesAlCargar.add(etiqueta + ": '" + texto + "' está por encima de "
                    + LIMITE_MAX + ", se guardará " + LIMITE_MAX);
            return LIMITE_MAX;
        }
        return entero;
    }

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
            seleccionarTipo(DataType.find(pv.getType()));
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
        ajustesAlCargar.clear();
        txtDescriptor.setText("");
        cbScanTime.setSelectedIndex(-1);
        chkScanEnable.setSelected(true);
        chkWriteEnable.setSelected(false);
        spDisplayLimitLow.setValue(LIMITE_INICIAL);
        spDisplayLimitHigh.setValue(LIMITE_INICIAL);
        txtDisplayDescription.setText("");
        txtDisplayFormat.setText("");
        txtDisplayUnits.setText("");
        spControlLimitLow.setValue(LIMITE_INICIAL);
        spControlLimitHigh.setValue(LIMITE_INICIAL);
        spControlMinStep.setValue(LIMITE_INICIAL);
        cbVariable.setSelectedIndex(0);
        cbType.setSelectedIndex(-1);
        lblFormTitle.setText("Nueva variable de proceso");
    }

    private void refreshPvFormState() {
        boolean editando = editPvUuid != null;
        boolean hayArea = selectedArea() != null;

        btnAddPv.setEnabled(hayArea && !editando);
        btnUpdatePv.setEnabled(hayArea && editando);
        btnDeletePv.setEnabled(hayArea && editando);
        btnCancelPv.setEnabled(editando);

        cbArea.setEnabled(hayArea && !editando);
        cbVariable.setEnabled(hayArea && !editando);
        cbType.setEnabled(hayArea && !editando);

        txtDescriptor.setEnabled(hayArea);
        cbScanTime.setEnabled(hayArea);
        chkScanEnable.setEnabled(hayArea);
        chkWriteEnable.setEnabled(hayArea);
        spDisplayLimitLow.setEnabled(hayArea);
        spDisplayLimitHigh.setEnabled(hayArea);
        txtDisplayDescription.setEnabled(hayArea);
        txtDisplayFormat.setEnabled(hayArea);
        txtDisplayUnits.setEnabled(hayArea);
        spControlLimitLow.setEnabled(hayArea);
        spControlLimitHigh.setEnabled(hayArea);
        spControlMinStep.setEnabled(hayArea);
    }

    private void agregarPv() {
        if (!validarCamposObligatorios()) {
            return;
        }

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
        DataType type = tipoElegido();
        if (!controller.typeAllowed(area.getUuid(), type)) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un tipo de dato que el área admita. Los que salen en "
                    + "gris no tienen tamaño definido y no se pueden usar.",
                    "Falta tipo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int libres = controller.areaFreeSlots(area.getUuid(), type);
        int bytesLibres = controller.areaFreeBytes(area.getUuid());
        if (bytesLibres < 0) {
            JOptionPane.showMessageDialog(this,
                    "No se puede calcular el offset para el tipo '" + type.label()
                    + "': del tag del área '" + area.getName() + "' no se sabe"
                    + " el tamaño.\n\n"
                    + "Añada un rango al tag, del tipo %DB21.DBB0[0..9], para que"
                    + " se pueda calcular dónde va cada variable.",
                    "Offset no calculable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String offset = controller.nextOffset(area.getUuid(), type);
        
        if (offset == null) {
            JOptionPane.showMessageDialog(this,
                    "No cabe una variable más de tipo '" + type.label() + "' en el área '"
                    + area.getName() + "'.\n\n"
                    + "El área abarca " + controller.areaCapacityBytes(area.getUuid())
                    + " bytes y cada " + type.label() + " ocupa "
                    + pesoDe(type) + ".\n"
                    + (bytesLibres > 0
                            ? "Quedan " + bytesLibres + " bytes sueltos, pero no llegan"
                            + " para uno entero, así que se considera llena.\n"
                            : "")
                    + "Amplíe el rango del tag del área si necesita más sitio.",
                    "Se ha llegado al límite del área", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (libres == 0) {
            JOptionPane.showMessageDialog(this,
                    "El área '" + area.getName() + "' ya está llena.\n\n"
                    + "Abarca " + controller.areaCapacityBytes(area.getUuid())
                    + " bytes y admite " + type.label() + " de " + pesoDe(type)
                    + ", así que no cabe una variable más.\n"
                    + "Amplíe el rango del tag del área si necesita más sitio.",
                    "Área de memoria llena", JOptionPane.WARNING_MESSAGE);
            return;
        }
        avisarAjustes();
        controller.addPv(new CommConfigData.PvConfig(
                UUID.randomUUID().toString(),
                variable.getName(),
                type.label(),
                area.getUuid(),
                offset,
                txtDescriptor.getText().trim(),
                scanTimeElegido(),
                chkScanEnable.isSelected(),
                chkWriteEnable.isSelected(),
                limiteDe(spDisplayLimitLow),
                limiteDe(spDisplayLimitHigh),
                txtDisplayDescription.getText().trim(),
                txtDisplayFormat.getText().trim(),
                txtDisplayUnits.getText().trim(),
                limiteDe(spControlLimitLow),
                limiteDe(spControlLimitHigh),
                limiteDe(spControlMinStep),
                CommunicationWizardController.pvMd5(),
                variable.getPath()));
        salirDeEdicionPv();
        refrescarYPersistir("Variable '" + variable.getName() + "' guardada en '"
                + area.getName() + "' con offset " + offset + ".");
    }

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
        avisarAjustes();
        controller.updatePv(pv.getUuid(),
                txtDescriptor.getText().trim(),
                scanTimeElegido(),
                chkScanEnable.isSelected(),
                chkWriteEnable.isSelected(),
                limiteDe(spDisplayLimitLow),
                limiteDe(spDisplayLimitHigh),
                txtDisplayDescription.getText().trim(),
                txtDisplayFormat.getText().trim(),
                txtDisplayUnits.getText().trim(),
                limiteDe(spControlLimitLow),
                limiteDe(spControlLimitHigh),
                limiteDe(spControlMinStep));
        salirDeEdicionPv();
        refrescarYPersistir("Variable '" + pv.getName() + "' actualizada.");
    }

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

    private List<String> camposObligatoriosVacios() {
        List<String> vacios = new ArrayList<>();
        agregarSiVacio(vacios, "Descriptor", txtDescriptor);
        if (cbScanTime.getSelectedItem() == null) {
            vacios.add("ScanTime");
        }
        agregarSiVacio(vacios, "Display descripción", txtDisplayDescription);
        agregarSiVacio(vacios, "Display formato", txtDisplayFormat);
        agregarSiVacio(vacios, "Display unidades", txtDisplayUnits);
        return vacios;
    }

    private String scanTimeElegido() {
        return String.valueOf(cbScanTime.getSelectedItem());
    }

    private void avisarAjustes() {
        if (ajustesAlCargar.isEmpty()) {
            return;
        }
        JOptionPane.showMessageDialog(this,
                "Estos valores se han corregido al abrir la variable, porque no"
                + " encajaban en el formulario:\n\n"
                + String.join("\n", ajustesAlCargar)
                + "\n\nSi se guarda, se escriben los valores corregidos.",
                "Valores corregidos", JOptionPane.WARNING_MESSAGE);
    }

    private void agregarSiVacio(List<String> vacios, String etiqueta, JTextField campo) {
        if (campo.getText().trim().isEmpty()) {
            vacios.add(etiqueta);
        }
    }

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
