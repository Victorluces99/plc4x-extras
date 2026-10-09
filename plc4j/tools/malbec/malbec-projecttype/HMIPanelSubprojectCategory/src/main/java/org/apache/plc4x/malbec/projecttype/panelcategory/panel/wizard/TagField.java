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
import java.awt.IllegalComponentStateException;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.TagDiagnostico;
import org.apache.plc4x.malbec.projecttype.panelcategory.model.TagIssue;

/**
 * El campo de tag del area de memoria, con su diagnostico.
 *
 * <p>Es un {@code JTextField} de los de siempre, con una cosa anadida: al escribir, se
 * diagnostica el tag y se marca con borde rojo si esta mal. El globo con el texto entero
 * sale al pasar el raton por encima.
 *
 * <p>Es subclase y no un envoltorio a proposito: el formulario lo coloca en su rejilla
 * como cualquier otro campo, y las pruebas lo leen como {@code JTextField}. Con un
 * envoltorio habria que tocar las dos cosas.
 *
 * <p>Lo del globo necesita pantalla, asi que en un entorno sin display se sale antes de
 * tocarla: el borde rojo sigue puesto, que es el aviso que importa, y el CI no se cae.
 */
final class TagField extends JTextField {

    private static final long serialVersionUID = 1L;

    private static final PopupFactory FACTORIA_AVISOS = new PopupFactory();

    private final Border bordeOriginal;
    private boolean ratonSobreTag;
    private Popup popupTag;

    TagField(int columnas) {
        super(columnas);
        bordeOriginal = getBorder();
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                ratonSobreTag = true;
                avisarTag();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                ratonSobreTag = false;
                ocultarAvisoTag();
            }
        });
        getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                avisarTag();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                avisarTag();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                avisarTag();
            }
        });
    }

    private void avisarTag() {
        ocultarAvisoTag();
        List<TagIssue> errores = TagDiagnostico.errores(getText());
        List<TagIssue> avisos = TagDiagnostico.avisos(getText());
        List<TagIssue> todo = new ArrayList<>(errores);
        todo.addAll(avisos);
        setBorder(errores.isEmpty()
                ? bordeOriginal
                : BorderFactory.createLineBorder(new Color(180, 0, 0), 2));
        // isShowing() se queda: en un test el campo no esta visible y no tiene por que
        // aparecer un globo. La proteccion de headless va dentro de mostrarAvisoTag(),
        // que es donde se toca la pantalla de verdad.
        if (!todo.isEmpty() && ratonSobreTag && isShowing()) {
            mostrarAvisoTag(todo);
        }
    }

    /**
     * Traduce los codigos de {@link TagIssue} al texto que ve el usuario.
     *
     * <p>Las plantillas viven aqui, no en el modelo: el diagnostico no sabe nada de
     * Swing ni de como se maquetan los parrafos (T3.8).
     */
    private static String mensajeDe(TagIssue issue) {
        List<String> a = issue.args();
        return switch (issue.codigo()) {
            case TagIssue.CORCHETE_SIN_CERRAR -> "Falta cerrar el corchete: hay un '[' sin su ']'.";
            case TagIssue.CORCHETE_SOBRANTE -> "Hay un ']' sin el '[' que lo abre.";
            case TagIssue.RANGO_INVALIDO -> "El rango «[" + a.get(0) + "]» no es válido."
                    + " Un rango se escribe con dos puntos, del tipo [0..9], y un solo"
                    + " número es un índice, del tipo [10].";
            case TagIssue.CARACTERES_RAROS -> "El tag tiene caracteres que no pueden ser parte"
                    + " de una dirección: «" + a.get(0) + "». Se guardará tal cual, pero el PLC"
                    + " puede que no lo entienda.";
            case TagIssue.NO_ES_DIRECCION -> "«" + a.get(0) + "» no es una dirección válida."
                    + " El área se puede guardar, pero se tratará como un nombre: no se le"
                    + " calculará el tamaño y no se podrán comprobar solapes con las"
                    + " demás áreas. Si es una dirección, revise la notación:"
                    + " Ej: %DB22.DBB4[0..9]:BYTE.";
            case TagIssue.SIN_TIPOS -> "Con el tipo '" + a.get(0) + "' el área no va a ofrecer"
                    + " ningún tipo de dato, así que no se le podrán añadir variables."
                    + " Los tipos del programa son: " + a.get(1) + ".";
            case TagIssue.SIN_TAMANO -> "No se sabe el tamaño del área, así que no se le podrá"
                    + " añadir ninguna variable. Añada un rango al tag, del tipo [0..9],"
                    + " o un tipo tras los dos puntos, del tipo :INT.";
            default -> issue.codigo();
        };
    }

    /**
     * Muestra el globo de avisos junto al campo.
     *
     * <p>Sin pantalla no hay donde pegarlo, y aqui fallan dos llamadas por motivos
     * distintos. {@code getMaximumWindowBounds()} lanza {@link java.awt.HeadlessException}
     * directamente, pero antes que eso ya revienta {@code getLocationOnScreen()} con
     * {@link IllegalComponentStateException} ("component must be showing on the screen"),
     * porque en un test el campo nunca esta visible. En un CI sin display eso tumba el
     * test entero con una excepcion que no dice de donde sale.
     *
     * <p>En headless no se muestra el globo y se sale: el borde rojo ya avisa del error.
     */
    private void mostrarAvisoTag(List<TagIssue> issues) {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        Point enPantalla = getLocationOnScreen();
        int anchoCampo = getWidth();
        int altoCampo = getHeight();
        int ancho = Math.max(240, Math.min(320, anchoCampo - 20));

        List<String> parrafos = new ArrayList<>(issues.size());
        for (TagIssue issue : issues) {
            parrafos.add(mensajeDe(issue));
        }
        JLabel etiqueta = new JLabel("<html><body style='width:" + (ancho - 16) + "px'>"
                + String.join("<br><br>", parrafos) + "</body></html>");
        etiqueta.setFont(getFont());

        JPanel caja = new JPanel(new BorderLayout());
        caja.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 0, 0)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        caja.setBackground(new Color(255, 250, 235));
        caja.add(etiqueta, BorderLayout.CENTER);

        Rectangle pantalla = GraphicsEnvironment
                .getLocalGraphicsEnvironment().getMaximumWindowBounds();
        int alto = caja.getPreferredSize().height;
        int y = enPantalla.y + altoCampo + 2;
        if (y + alto + 8 > pantalla.y + pantalla.height) {
            y = enPantalla.y - alto;
        }
        popupTag = FACTORIA_AVISOS.getPopup(this, caja, enPantalla.x, y);
        popupTag.show();
    }

    private void ocultarAvisoTag() {
        if (popupTag != null) {
            popupTag.hide();
            popupTag = null;
        }
    }
}
