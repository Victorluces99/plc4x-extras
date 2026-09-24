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

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;

public class TablaTabPanel extends JPanel {

    private final JTable table;
    private final DynamicTableModel tableModel;
    private final List<ColumnConfig> columnConfigs;

    public TablaTabPanel(List<ColumnConfig> columnConfigs) {
        this.columnConfigs = columnConfigs;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        tableModel = new DynamicTableModel(columnConfigs);
        table = new JTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        setupEditors();

        JScrollPane scrollPane = new JScrollPane(table);

        JLabel lblNoContent = new JLabel("No content in table", SwingConstants.CENTER);
        lblNoContent.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblNoContent.setForeground(Color.GRAY);

        JPanel centerContainer = new JPanel(new CardLayout());
        centerContainer.add(lblNoContent, "EMPTY");
        centerContainer.add(scrollPane, "TABLE");

        CardLayout cl = (CardLayout) centerContainer.getLayout();
        cl.show(centerContainer, "EMPTY");

        tableModel.addTableModelListener(e -> {
            if (tableModel.getRowCount() == 0) {
                cl.show(centerContainer, "EMPTY");
            } else {
                cl.show(centerContainer, "TABLE");
            }
        });

        add(centerContainer, BorderLayout.CENTER);

    }

    private void setupEditors() {
        for (int i = 0; i < columnConfigs.size(); i++) {
            ColumnConfig config = columnConfigs.get(i);
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth(130);

            if (config.getType() == ColumnConfig.ColumnType.COMBOBOX) {
                JComboBox<String> comboBox = new JComboBox<>(config.getOptions());
                column.setCellEditor(new DefaultCellEditor(comboBox));
            } else {
                JTextField textField = new JTextField();
                column.setCellEditor(new DefaultCellEditor(textField));
            }
        }
    }

    public void addNewRow() {
    DynamicTableModel model = getTableModel();
    Object[] newRow = new Object[model.getColumnCount()];

    for (int col = 0; col < model.getColumnCount(); col++) {
        ColumnConfig config = columnConfigs.get(col);
        if (config.getType() == ColumnConfig.ColumnType.COMBOBOX) {
            String[] options = config.getOptions();
            newRow[col] = (options != null && options.length > 0) ? options[0] : "";
        } else {
            newRow[col] = "";
        }
    }

    model.addRow(newRow);
}

    public void removeSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow != -1) {
            tableModel.removeRow(selectedRow);
        } else if (tableModel.getRowCount() > 0) {
            tableModel.removeRow(tableModel.getRowCount() - 1);
        }
    }

    public JTable getTable() { return table; }
    public DynamicTableModel getTableModel() { return tableModel; }
    public List<ColumnConfig> getColumnConfigs() { return columnConfigs; }
}