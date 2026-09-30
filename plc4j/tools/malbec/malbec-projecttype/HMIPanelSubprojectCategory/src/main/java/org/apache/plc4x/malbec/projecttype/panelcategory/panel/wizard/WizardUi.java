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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * Utilidades de layout compartidas por los paneles del asistente. Son todas
 * estáticas y sin estado: no contienen lógica del wizard, sólo el modo en que
 * los tres paneles arman sus tablas, combos y filas de botones.
 */
public final class WizardUi {

    private WizardUi() {
    }

    /**
     * Limita el espacio que ocupa una tabla en el diálogo.
     *
     * <p>El límite va en el {@link JScrollPane}, nunca en la tabla. El scroll pane
     * dimensiona su viewport con {@code getPreferredScrollableViewportSize()}, que
     * en un {@link JTable} devuelve el preferred size de la tabla. Si se fija ahí,
     * el viewport queda con el ancho fijado, el scroll pane cree que todo el
     * contenido entra, no dibuja barra horizontal y las columnas que sobresalen
     * quedan recortadas. Peor: al quedar fuera de la ventana, su borde derecho
     * tampoco se puede arrastrar y parecen no redimensionables.
     */
    public static void limitTableSpace(JScrollPane scroll, int width, int height) {
        scroll.setPreferredSize(new Dimension(width, height));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * Ancho inicial legible por columna: mide el encabezado y el contenido real
     * y lo acota entre minWidth y maxWidth. Evita el ancho por defecto de
     * {@link JTable} (75px), que dejaba todas las celdas pegadas, sin dejar que
     * un nombre kilométrico infle la columna.
     */
    public static void sizeColumnsToContent(JTable table, int minWidth, int maxWidth) {
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

    public static void fixComboWidth(JComboBox<?> combo, int width) {
        Dimension pref = combo.getPreferredSize();
        combo.setPreferredSize(new Dimension(width, pref.height));
        combo.setMinimumSize(new Dimension(Math.min(120, width), pref.height));
    }

    public static JPanel nav(JButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        for (JButton b : buttons) {
            panel.add(b);
        }
        return panel;
    }

    /** Fila de botones de agregar / modificar / eliminar / cancelar. */
    public static JPanel buttonRow(JButton... buttons) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        for (JButton b : buttons) {
            panel.add(b);
        }
        return panel;
    }

    public static void addLabel(JPanel target, GridBagConstraints g, int y, String text) {
        g.gridx = 0;
        g.gridy = y;
        g.gridwidth = 1;
        g.weightx = 0.0;
        target.add(new JLabel(text), g);
    }

    /** Título de paso en negrita, usado en los tres paneles. */
    public static JLabel stepTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        return title;
    }

    /** Título de formulario en negrita, un punto más chico que el de paso. */
    public static JLabel formTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 12f));
        return title;
    }

    public static GridBagConstraints formConstraints() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 8, 2, 8);
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        return g;
    }

    public static GridBagConstraints formConstraintsTight() {
        GridBagConstraints g = formConstraints();
        g.insets = new Insets(2, 8, 2, 8);
        return g;
    }

    public static JPanel formGrid() {
        return new JPanel(new GridBagLayout());
    }
}
