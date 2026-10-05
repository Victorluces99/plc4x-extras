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
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public final class WizardUi {

    /** Campo de nombre: corto, es un identificador. */
    public static final int MAX_NOMBRE = 20;
    /** Campo de descripción: cabe una frase con la fila ancha de la tabla. */
    public static final int MAX_DESCRIPCION = 60;

    /** Marca de que la tabla ya tiene puesto su listener de reparto de ancho. */
    private static final String CLIENTE_REPARTIDO = "wizardUi.repartoAncho";

    private WizardUi() {
    }

    /**
     * Campo de texto con un número máximo de caracteres.
     *
     * <p>El tope va en el documento y no al guardar, que es lo único que impide que
     * la tecla entre. Lo que sobra se recorta al vuelo en lugar de rechazarse: al
     * teclear se ven los dígitos que caben y al pegar un texto largo se rellena
     * hasta el tope, en vez de que el campo se quede igual sin explicación.</p>
     *
     * @param columnas ancho en columnas, como en el constructor de {@link JTextField}
     * @param maximo caracteres admitidos
     * @return el campo, ya acotado
     */
    public static JTextField textoLimitado(int columnas, int maximo) {
        JTextField campo = new JTextField(columnas);
        campo.setDocument(new DocumentoLimitado(maximo));
        return campo;
    }

    /**
     * Documento que no deja pasar de {@code maximo} caracteres.
     *
     * <p>Lo que no cabe se recorta dentro de {@code insertString}: si el texto a
     * insertar no cabe entero, se inserta sólo la parte que queda. Eso hace que el
     * campo se rellene hasta el tope en lugar de quedarse con lo que tenía antes,
     * que es lo que pasa si el carácter sobrante se rechaza sin más.</p>
     */
    private static final class DocumentoLimitado extends PlainDocument {

        private final int maximo;

        DocumentoLimitado(int maximo) {
            this.maximo = maximo;
        }

        @Override
        public void insertString(int offs, String str, AttributeSet a)
                throws BadLocationException {
            if (str == null) {
                return;
            }
            int caben = maximo - getLength();
            if (caben <= 0) {
                return;
            }
            super.insertString(offs, str.length() > caben ? str.substring(0, caben) : str, a);
        }
    }

    public static void limitTableSpace(JScrollPane scroll, int width, int height) {
        scroll.setPreferredSize(new Dimension(width, height));
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    }

    /**
     * Fija las columnas: no se reordenan y una no se ensancha a costa de las
     * demás.
     */
    public static void configColumns(JTable table) {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);
    }

    /**
     * Reparte el ancho que sobra en el visor entre las columnas, para que la tabla
     * llegue al borde derecho y no quede la franja gris al lado.
     *
     * <p>Hace falta porque las columnas están en {@code AUTO_RESIZE_OFF}, que es lo
     * que permite ensanchar una sin que se encogan las de al lado, pero también
     * significa que ninguna crece para tapar el hueco. Son dos cosas que se estorban
     * y aquí se resuelve repartiendo el sobrante a mano en vez de cambiar el modo de
     * redimensionado.</p>
     *
     * <p>El reparto es proporcional al ancho que tiene cada columna, así que una
     * columna que el usuario haya agrandado a mano conserva su proporción y no
     * vuelve al ancho que le puso el ajuste automático.</p>
     *
     * <p>Si las columnas ya suman más que el visor no se toca ninguna: en ese caso
     * hay barra horizontal y el hueco no llega a verse.</p>
     *
     * <p>Se vuelve a repartir cada vez que cambia el visor, porque mientras la tabla
     * no se ha mostrado el ancho disponible es cero y el reparto se quedaría sin
     * hacer.</p>
     */
    public static void rellenarAnchoVisible(JTable tabla) {
        repartirAnchoVisible(tabla);
        Container padre = tabla.getParent();
        if (!(padre instanceof JViewport viewport)) {
            return;
        }
        // El listener se pone una sola vez. Esta función se llama en cada refresco,
        // y sin la marca cada uno añadiría su propio listener y el reparto se
        // ejecutaría tantas veces como refrescos haya habido.
        if (Boolean.TRUE.equals(tabla.getClientProperty(CLIENTE_REPARTIDO))) {
            return;
        }
        tabla.putClientProperty(CLIENTE_REPARTIDO, Boolean.TRUE);
        viewport.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                repartirAnchoVisible(tabla);
            }
        });
    }

    private static void repartirAnchoVisible(JTable tabla) {
        Container padre = tabla.getParent();
        if (!(padre instanceof JViewport viewport)) {
            return;
        }
        int disponible = viewport.getExtentSize().width;
        if (disponible <= 0) {
            return;
        }
        TableColumnModel columnas = tabla.getColumnModel();
        int n = columnas.getColumnCount();
        if (n == 0) {
            return;
        }
        int[] anchos = new int[n];
        for (int i = 0; i < n; i++) {
            anchos[i] = columnas.getColumn(i).getWidth();
        }
        int[] repartidos = repartirProporcional(anchos, disponible);
        if (repartidos == null) {
            return;
        }
        for (int i = 0; i < n; i++) {
            // Hay que fijar los dos: con AUTO_RESIZE_OFF el ancho lo manda el
            // preferido, así que tocar sólo el ancho se deshace en el siguiente
            // ajuste por contenido.
            columnas.getColumn(i).setPreferredWidth(repartidos[i]);
            columnas.getColumn(i).setWidth(repartidos[i]);
        }
    }

    /**
     * Anchos nuevos tras repartir lo que sobra del visor.
     *
     * <p>Devuelve null cuando no hay nada que repartir: que no haya columna, que el
     * visor no tenga ancho medido todavía, o que las columnas ya llenen el visor, en
     * cuyo caso hay barra horizontal y tocar los anchos solo haría parpadear.</p>
     *
     * @param anchos ancho actual de cada columna
     * @param disponible ancho del visor
     * @return los anchos repartidos, o null si no hay hueco que llenar
     */
    static int[] repartirProporcional(int[] anchos, int disponible) {
        if (anchos == null || anchos.length == 0 || disponible <= 0) {
            return null;
        }
        int total = 0;
        for (int ancho : anchos) {
            total += ancho;
        }
        if (total <= 0 || total >= disponible) {
            return null;
        }
        int sobra = disponible - total;
        int[] resultado = new int[anchos.length];
        int repartido = 0;
        for (int i = 0; i < anchos.length; i++) {
            int extra = sobra * anchos[i] / total;
            repartido += extra;
            resultado[i] = anchos[i] + extra;
        }
        // El redondeo de cada división deja algún píxel sin repartir; se le dan a la
        // columna más ancha, que es la que peor lo lleva si se queda corta.
        int masAncha = 0;
        for (int i = 1; i < resultado.length; i++) {
            if (resultado[i] > resultado[masAncha]) {
                masAncha = i;
            }
        }
        resultado[masAncha] += sobra - repartido;
        return resultado;
    }

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

    // Fila de botones de agregar / modificar / eliminar / cancelar. 
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

    //Título de paso en negrita, usado en los tres paneles. 
    public static JLabel stepTitle(String text) {
        JLabel title = new JLabel(text);
        title.setFont(title.getFont().deriveFont(java.awt.Font.BOLD, 14f));
        return title;
    }

    // Título de formulario en negrita 
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
