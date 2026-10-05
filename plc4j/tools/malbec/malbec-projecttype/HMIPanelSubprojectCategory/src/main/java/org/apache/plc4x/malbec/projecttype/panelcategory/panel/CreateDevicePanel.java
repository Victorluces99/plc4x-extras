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
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.plc4x.malbec.projecttype.panelcategory.comms.xml.HMICommunicationModel;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Formulario de alta y modificación de dispositivo de comunicación.
 *
 * <p>
 * Vive en un {@code JPanel} y no en un {@code JDialog} a propósito: el diálogo
 * es el cascaron modal y sólo aporta teclado y guardado. Así el formulario se
 * puede instanciar y verificar en un test sin ventana.
 *
 * <p>
 * No comparte una sola clase con el asistente de comunicación
 * ({@code panel.wizard}): son dos flujos distintos y acoplar el alta de
 * dispositivo a los pasos del asistente sólo serviría para arrastrar cambios de
 * un flujo al otro. Por eso este panel trae sus propias utilidades de layout.
 *
 * <p>
 * El mismo formulario sirve para los dos casos; lo que cambia es qué se deja
 * tocar. Al modificar, la marca y el nodo S88 quedan bloqueados y el UUID es
 * inmutable: {@code CommunicationsConfig.upsertDevice} matchea por UUID, así
 * que regenerarlo convertiría una modificación en un dispositivo nuevo. El
 * resto —modelo, nombre, clave, descripción, host, puerto, transporte y
 * parámetros— se edita igual que en el alta.
 *
 * <p>
 * Los datos salen por {@link #getDeviceConfigData()}, que arma un
 * {@link Properties} con las doce claves que {@link DeviceConfigData} lee. Esa
 * clase es inmutable, así que no hay setters: todo se concentra en el
 * constructor.
 */
public final class CreateDevicePanel extends JPanel {

    private static final String SELECCIONE = "-- Seleccione --";
    private final transient Project project;
    // --- Marca y modelo ---
    private final JComboBox<String> cbMarca
            = new JComboBox<>(new String[]{SELECCIONE, "Siemens", "Allen Bradley", "Modbus"});
    private final JComboBox<DeviceModel> cbModelo = new JComboBox<>();
    private final JTextField txtProtocol = new JTextField();
    // --- Dispositivo ---
    private final JTextField txtDeviceName = new JTextField();
    private final JTextField txtDeviceKey = new JTextField();
    private final JTextField txtDescription = new JTextField();
    private final JTextField txtUUID = new JTextField();
    private final JCheckBox chkEnable = new JCheckBox("Enable");
    // --- Árbol S88 ---
    private final JComboBox<String> cbS88Node = new JComboBox<>();
    private final JTextField txtS88UUID = new JTextField();
    // --- Parámetros de conexión ---
    private final JLabel lblHost = new JLabel("Host/IP:");
    private final JLabel lblPort = new JLabel("Port:");
    private final JTextField txtHost = new JTextField();
    private final JComboBox<String> cbTransport = new JComboBox<>();
    private final JTextField txtPort = new JTextField();
    private final JComboBox<String> cbParameter = new JComboBox<>();
    private final JTextField txtParamValue = new JTextField();
    private final JTextField txtUrlPreview = new JTextField();
    private DeviceDynamicPanelBuilder activeBuilder;
    // --- Datos locales ---
    private final Map<String, List<DeviceModel>> devicesMap = new HashMap<>();
    private final Map<String, String> s88NodesMap = new HashMap<>();
    private final Set<String> usedS88Nodes = new HashSet<>();
    // --- Modo edición ---
    private final transient DeviceConfigData editing;
    private final String modeloAlAbrir;
    private final boolean editando;

    public CreateDevicePanel(Project project) {
        this(project, null);
    }

    public CreateDevicePanel(Project project, DeviceConfigData edit) {
        this.project = project;
        this.editing = edit;
        this.editando = edit != null;
        this.modeloAlAbrir = edit == null ? null : edit.getModel();
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initData();
        buildUi();
        loadS88Nodes();
        if (editando) {
            cargarParaEditar(edit);
        } else {
            resetForm();
        }
    }

    // --- Utilidades de layout propias ----------------------
    private static GridBagConstraints formConstraints() {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 8, 3, 8);
        g.anchor = GridBagConstraints.WEST;
        g.weightx = 1.0;
        g.fill = GridBagConstraints.HORIZONTAL;
        return g;
    }

    private static JLabel label(String text) {
        return new JLabel(text);
    }

    private static JLabel bold(String text, float size) {
        JLabel result = new JLabel(text);
        result.setFont(result.getFont().deriveFont(Font.BOLD, size));
        return result;
    }

    /**
     * Título de sección a lo ancho de la fila.
     *
     * <p>
     * Fijar {@code gridx} y {@code gridwidth} acá no es redundante: las
     * restricciones se reutilizan de fila en fila y, si la fila anterior dejó
     * {@code gridx} en 3, el título sale pegado al borde derecho en vez de
     * arrancar en la primera columna.
     */
    private int addSectionTitle(JPanel form, GridBagConstraints g, int y, String text) {
        g.gridx = 0;
        g.gridy = y;
        g.gridwidth = 4;
        g.weightx = 0.0;
        g.fill = GridBagConstraints.NONE;
        form.add(bold(text, 12f), g);
        g.gridwidth = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        return y + 1;
    }

    /**
     * Label en la primera columna y campo ocupando las tres siguientes.
     */
    private int addWide(JPanel form, GridBagConstraints g, int y, JLabel label, JComponent field) {
        g.gridy = y;
        g.gridwidth = 1;
        g.gridx = 0;
        g.weightx = 0.0;
        form.add(label, g);
        g.gridx = 1;
        g.gridwidth = 3;
        g.weightx = 1.0;
        form.add(field, g);
        g.gridwidth = 1;
        return y + 1;
    }

    /**
     * Dos pares label/campo en una misma fila: label, campo, label, campo.
     */
    private int addPair(JPanel form, GridBagConstraints g, int y,
            JLabel label1, JComponent field1, JLabel label2, JComponent field2) {
        g.gridy = y;
        g.gridwidth = 1;
        g.gridx = 0;
        g.weightx = 0.0;
        form.add(label1, g);
        g.gridx = 1;
        g.weightx = 1.0;
        form.add(field1, g);
        g.gridx = 2;
        g.weightx = 0.0;
        form.add(label2, g);
        g.gridx = 3;
        g.weightx = 1.0;
        form.add(field2, g);
        return y + 1;
    }

    /* Campo sin label, alineado con la columna de los campos. */
    private int addSpanning(JPanel form, GridBagConstraints g, int y, JComponent field) {
        g.gridy = y;
        g.gridx = 1;
        g.gridwidth = 3;
        g.weightx = 1.0;
        form.add(field, g);
        g.gridwidth = 1;
        return y + 1;
    }

    // --- Construcción de la interfaz ----------------------------
    private void buildUi() {
        txtProtocol.setEditable(false);
        txtUUID.setEditable(false);
        txtS88UUID.setEditable(false);
        txtUrlPreview.setEditable(false);
        txtParamValue.setEnabled(false);
        cbTransport.setEditable(true);
        lblPort.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 2));

        cbS88Node.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel rendered = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);
                if (value instanceof String node && usedS88Nodes.contains(node)) {
                    rendered.setEnabled(false);
                    rendered.setText(node + "  (en uso)");
                } else {
                    rendered.setEnabled(true);
                }
                return rendered;
            }
        });

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = formConstraints();

        int y = 0;
        g.gridx = 0;
        g.gridy = y++;
        g.gridwidth = 4;
        form.add(bold(editando ? "Modificar dispositivo de comunicación"
                : "Nuevo dispositivo de comunicación", 14f), g);
        g.gridwidth = 1;

        y = addSectionTitle(form, g, y, "Marca y modelo");
        y = addWide(form, g, y, label("Marca"), cbMarca);
        y = addWide(form, g, y, label("Modelo"), cbModelo);
        y = addWide(form, g, y, label("Protocol"), txtProtocol);
        y = addSpanning(form, g, y, chkEnable);

        y = addSectionTitle(form, g, y, "Dispositivo");
        y = addWide(form, g, y, label("Name"), txtDeviceName);
        y = addWide(form, g, y, label("Key"), txtDeviceKey);
        y = addWide(form, g, y, label("Description"), txtDescription);
        y = addWide(form, g, y, label("UUID"), txtUUID);

        y = addSectionTitle(form, g, y, "S88 tree");
        y = addWide(form, g, y, label("Node"), cbS88Node);
        y = addWide(form, g, y, label("UUID"), txtS88UUID);

        y = addSectionTitle(form, g, y, "Parámetros de conexión");
        y = addWide(form, g, y, lblHost, txtHost);
        y = addPair(form, g, y, label("Transport"), cbTransport, lblPort, txtPort);
        y = addPair(form, g, y, label("Parámetro"), cbParameter, label("Valor"), txtParamValue);
        y = addSpanning(form, g, y, parameterButtons());
        y = addWide(form, g, y, label("URL Preview"), txtUrlPreview);

        add(form, BorderLayout.CENTER);

        setupBrandAndModelListeners();
        setupNodeListener();
        setupConnectionListeners();
    }

    private JPanel parameterButtons() {
        JButton btnAddParam = new JButton("+");
        JButton btnRemoveParam = new JButton("-");
        JButton btnClear = new JButton("Limpiar");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        buttons.add(btnAddParam);
        buttons.add(btnRemoveParam);
        buttons.add(btnClear);

        btnAddParam.addActionListener(e -> {
            if (activeBuilder == null) {
                return;
            }
            String key = (String) cbParameter.getSelectedItem();
            if (key == null) {
                return;
            }
            activeBuilder.addParameter(key, txtParamValue.getText().trim());
            refreshUrlPreview();
        });
        btnRemoveParam.addActionListener(e -> {
            if (activeBuilder != null) {
                activeBuilder.removeLastParameter();
            }
            refreshUrlPreview();
        });
        btnClear.addActionListener(e -> {
            if (activeBuilder != null) {
                activeBuilder.clearParameters();
            }
            refreshUrlPreview();
        });
        return buttons;
    }

    private void setupBrandAndModelListeners() {
        cbMarca.addActionListener(e -> {
            cbModelo.removeAllItems();
            txtProtocol.setText("");
            if (!editando) {
                txtUUID.setText("");
            }
            activeBuilder = null;
            txtHost.setText("");
            cbTransport.removeAllItems();
            txtPort.setText("");
            cbParameter.removeAllItems();
            txtParamValue.setText("");
            txtParamValue.setEnabled(false);
            txtUrlPreview.setText("");

            String selectedBrand = (String) cbMarca.getSelectedItem();
            if (devicesMap.containsKey(selectedBrand)) {
                for (DeviceModel dm : devicesMap.get(selectedBrand)) {
                    cbModelo.addItem(dm);
                }
                cbModelo.setSelectedIndex(0);
                generarUuidSiCorresponde();
            }
        });

        cbModelo.addActionListener(e -> {
            DeviceModel selectedDevice = (DeviceModel) cbModelo.getSelectedItem();
            if (selectedDevice == null) {
                return;
            }
            txtProtocol.setText(selectedDevice.getProtocol());
            generarUuidSiCorresponde();

            // Los catálogos de parámetros no son compatibles entre sí: Siemens
            // expone claves cotp.*, Modbus las suyas por transporte y Allen
            // Bradley sólo path y slot. Si el usuario cambia de modelo, arrastrar
            // los parámetros del anterior produciría una URL sin sentido.
            //
            // Va antes de armar el builder a propósito: limpiar y después dejar
            // que loadBuilderParams corra es exactamente lo que pasa en la creación,
            // donde el modelo nuevo queda con sus defaults y el controller-type
            // que initForModel deduce del modelo elegido.
            if (editando && !Objects.equals(modeloAlAbrir, selectedDevice.getModel())) {
                limpiarConexion();
            }

            String brand = selectedDevice.getBrand();
            if ("Allen Bradley".equalsIgnoreCase(brand)) {
                activeBuilder = new AllenBradleyPanelBuilder();
            } else if ("Modbus".equalsIgnoreCase(brand)) {
                activeBuilder = new ModbusPanelBuilder();
            } else {
                activeBuilder = new SiemensPanelBuilder();
            }

            loadBuilderParams();
        });
    }

    /**
     * En creación el UUID se genera al elegir marca o modelo. En edición nunca.
     */
    private void generarUuidSiCorresponde() {
        if (!editando) {
            txtUUID.setText(UUID.randomUUID().toString());
        }
    }

    private void setupNodeListener() {
        cbS88Node.addActionListener(e -> {
            String selectedNode = (String) cbS88Node.getSelectedItem();
            if (s88NodesMap.containsKey(selectedNode)) {
                if (usedS88Nodes.contains(selectedNode)) {
                    JOptionPane.showMessageDialog(this,
                            "El área '" + selectedNode + "' ya está asignada a otro dispositivo.",
                            "Área en uso", JOptionPane.WARNING_MESSAGE);
                    cbS88Node.setSelectedIndex(0);
                    txtS88UUID.setText("");
                    return;
                }
                txtS88UUID.setText(s88NodesMap.get(selectedNode));
            } else {
                txtS88UUID.setText("");
            }
        });
    }

    private void setupConnectionListeners() {
        cbParameter.addActionListener(e -> {
            String key = (String) cbParameter.getSelectedItem();
            if (key == null) {
                txtParamValue.setEnabled(false);
                txtParamValue.setText("");
                return;
            }
            txtParamValue.setEnabled(true);
            txtParamValue.setText(activeBuilder.getParameterDefaults().get(key));
        });

        txtHost.getDocument().addDocumentListener(new SimpleDocumentListener(this::refreshUrlPreview));

        cbTransport.addActionListener(e -> onTransportChanged());
        JComponent transportEditor = (JComponent) cbTransport.getEditor().getEditorComponent();
        if (transportEditor instanceof JTextField transportField) {
            transportField.getDocument().addDocumentListener(
                    new SimpleDocumentListener(this::refreshUrlPreview));
        }

        txtPort.getDocument().addDocumentListener(new SimpleDocumentListener(this::refreshUrlPreview));
    }

    // --- Datos ----------------------------------------------------------------
    private void initData() {
        devicesMap.put("Siemens", Arrays.asList(
                new DeviceModel("Siemens", "S7-300", "S7"),
                new DeviceModel("Siemens", "S7-400", "S7"),
                new DeviceModel("Siemens", "S7-1200", "S7"),
                new DeviceModel("Siemens", "S7-1500", "S7")
        ));
        devicesMap.put("Allen Bradley", Arrays.asList(
                new DeviceModel("Allen Bradley", "ControlLogix", "EtherNet/IP"),
                new DeviceModel("Allen Bradley", "CompactLogix", "EtherNet/IP")
        ));
        devicesMap.put("Modbus", Arrays.asList(
                new DeviceModel("Modbus", "Modbus TCP", "modbus-tcp"),
                new DeviceModel("Modbus", "Modbus RTU", "modbus-rtu"),
                new DeviceModel("Modbus", "Modbus ASCII", "modbus-ascii")
        ));
    }

    /**
     * Carga las áreas del {@code plant-model.xml} generado al importar el
     * modelo de planta. El archivo vive en el directorio del panel (padre del
     * subproyecto categoría), por lo que se busca subiendo en el árbol.
     */
    private void loadS88Nodes() {
        cbS88Node.removeAllItems();
        cbS88Node.addItem(SELECCIONE);
        cbS88Node.setEnabled(false);
        txtS88UUID.setText("");

        usedS88Nodes.clear();
        HMICommunicationModel model = project != null
                ? project.getLookup().lookup(HMICommunicationModel.class) : null;
        if (model != null) {
            for (DeviceConfigData device : model.getDevices()) {
                String s88Node = device.getS88Node();
                if (s88Node != null && !s88Node.isEmpty()) {
                    // Al editar, el nodo del propio dispositivo tiene que quedar
                    // disponible: el combo rechaza lo que está en uso y el
                    // dispositivo que se está editando no compite consigo mismo.
                    if (Objects.equals(device.getUuid(),
                            editing == null ? null : editing.getUuid())) {
                        continue;
                    }
                    usedS88Nodes.add(s88Node);
                }
            }
        }

        FileObject plantModel = findPlantModelFile(
                project != null ? project.getProjectDirectory() : null);
        if (plantModel == null) {
            return;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(FileUtil.toFile(plantModel));
            Element root = doc.getDocumentElement();
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element element = (Element) node;
                if (!"area".equals(element.getTagName())) {
                    continue;
                }
                String id = element.getAttribute("id");
                if (id.isEmpty()) {
                    continue;
                }
                cbS88Node.addItem(id);
                s88NodesMap.put(id, element.getAttribute("uuid"));
            }
            cbS88Node.setEnabled(true);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
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

    private void loadBuilderParams() {
        cbParameter.removeAllItems();
        txtParamValue.setEnabled(false);
        txtParamValue.setText("");
        if (activeBuilder == null) {
            refreshUrlPreview();
            return;
        }
        activeBuilder.initForModel((DeviceModel) cbModelo.getSelectedItem());

        cbTransport.removeAllItems();
        cbTransport.setEditable(true);
        for (String option : activeBuilder.getTransportOptions()) {
            cbTransport.addItem(option);
        }
        cbTransport.setSelectedItem(activeBuilder.getDefaultTransport());
        if (cbTransport.getSelectedItem() == null && cbTransport.getItemCount() > 0) {
            cbTransport.setSelectedIndex(0);
        }
        onTransportChanged();
    }

    /**
     * Transporte que está realmente en pantalla.
     *
     * <p>
     * El combo es editable y un transporte guardado no siempre está en la lista
     * que propone el catálogo —{@code s7:cotp://} es el caso normal y el
     * catálogo de Siemens sólo ofrece "" y "tcp"—. El valor tipeado manda sobre
     * el seleccionado: mientras no se confirma con Enter,
     * {@code getSelectedItem} sigue devolviendo el anterior y el transporte
     * nuevo se perdería al guardar.
     */
    private String transportActual() {
        if (cbTransport.isEditable()) {
            Component editor = cbTransport.getEditor().getEditorComponent();
            if (editor instanceof JTextField campo) {
                String tipeado = campo.getText().trim();
                if (!tipeado.isEmpty()) {
                    return tipeado;
                }
            }
        }
        Object selected = cbTransport.getSelectedItem();
        return selected == null ? "" : selected.toString().trim();
    }

    private void onTransportChanged() {
        if (activeBuilder == null) {
            return;
        }
        String transportValue = transportActual();
        activeBuilder.setTransport(transportValue);

        lblHost.setText(activeBuilder.getHostLabel());
        boolean portApplicable = activeBuilder.isPortApplicable();
        lblPort.setEnabled(portApplicable);
        txtPort.setEnabled(portApplicable);
        if (!portApplicable) {
            txtPort.setText("");
        }

        refreshParameterCatalog(transportValue);
        refreshUrlPreview();
    }

    private void refreshParameterCatalog(String transport) {
        if (activeBuilder == null) {
            cbParameter.removeAllItems();
            txtParamValue.setEnabled(false);
            txtParamValue.setText("");
            return;
        }
        LinkedHashMap<String, String> catalog = activeBuilder.getParameterDefaults(transport);
        activeBuilder.retainParameters(catalog.keySet());

        String selectedKey = (String) cbParameter.getSelectedItem();
        cbParameter.removeAllItems();
        for (String key : catalog.keySet()) {
            cbParameter.addItem(key);
        }
        if (selectedKey != null && catalog.containsKey(selectedKey)) {
            cbParameter.setSelectedItem(selectedKey);
        }
        Object current = cbParameter.getSelectedItem();
        if (current == null) {
            txtParamValue.setEnabled(false);
            txtParamValue.setText("");
        } else {
            txtParamValue.setEnabled(true);
            // Primero lo que el dispositivo tiene realmente cargado y después el
            // default del catálogo. Al precargar un dispositivo existente, un
            // parámetro como cotp.remote-slot=3 se muestra como 3 y no como el
            // 0 de fábrica.
            txtParamValue.setText(activeBuilder.getParameters()
                    .getOrDefault(current.toString(), catalog.getOrDefault(current.toString(), "")));
        }
    }

    private void refreshUrlPreview() {
        if (activeBuilder == null) {
            txtUrlPreview.setText("");
            return;
        }
        activeBuilder.setHost(txtHost.getText().trim());
        activeBuilder.setTransport(transportActual());
        activeBuilder.setPort(txtPort.getText().trim());
        txtUrlPreview.setText(activeBuilder.getSpecificParametersAsString());
    }

    /**
     * Precarga el formulario con un dispositivo existente.
     *
     * <p>
     * El orden importa. Elegir la marca dispara el listener de marca, que
     * limpia todo y selecciona el primer modelo; elegir el modelo dispara el
     * listener de modelo, que arma el builder correspondiente y su catálogo de
     * parámetros. Recién con el builder listo tiene sentido desensamblar la URL
     * guardada.
     *
     * <p>
     * La marca y el nodo S88 quedan deshabilitados al final: son los dos
     * valores que no admiten cambio una vez creado el dispositivo.
     */
    private void cargarParaEditar(DeviceConfigData d) {
        cbMarca.setSelectedItem(d.getBrand());
        for (int i = 0; i < cbModelo.getItemCount(); i++) {
            if (cbModelo.getItemAt(i).getModel().equals(d.getModel())) {
                cbModelo.setSelectedIndex(i);
                break;
            }
        }
        cargarConexion(d.getSpecificParameters());
        restaurarNodoS88(d);
        txtS88UUID.setText(d.getS88Uuid());
        txtUUID.setText(d.getUuid());
        txtDeviceName.setText(d.getDeviceName());
        txtDeviceKey.setText(d.getDeviceKey());
        txtDescription.setText(d.getDescription());
        chkEnable.setSelected(d.isEnabled());

        cbMarca.setEnabled(false);
        cbS88Node.setEnabled(false);
    }

    /**
     * Vuelca en el formulario la URL de conexión guardada.
     *
     * <p>
     * Es el camino inverso de {@link UrlDisassembler}: lo que se desensambla se
     * vuelve a armar con el mismo builder, de modo que la URL que se vea al
     * abrir el formulario sea la misma que quedó guardada.
     */
    private void cargarConexion(String url) {
        if (activeBuilder == null || url == null || url.isBlank()) {
            return;
        }
        UrlDisassembler.Partes partes = UrlDisassembler.disassemble(url);
        if (partes == null) {
            // Se muestra la URL original en vez de descartarla en silencio: si
            // el usuario guarda, al menos la ve antes de perderla.
            txtUrlPreview.setText(url.trim());
            JOptionPane.showMessageDialog(this,
                    "No se pudo interpretar la URL de conexión guardada:\n\n" + url.trim()
                    + "\n\nRevisá host, puerto y parámetros antes de guardar.",
                    "Conexión a revisar", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // El transporte va primero: onTransportChanged repuebla el catálogo de
        // parámetros y decide si el puerto aplica, y el puerto se asigna después
        // porque ese mismo método vacía el campo cuando no aplica.
        //
        // Seleccionar el transporte desensamblado es lo que hace que la URL que
        // se guarda sea la misma que se leyó. Si se dejara el que ya venía del
        // modelo, un dispositivo guardado con s7:cotp:// volvería como s7:// y
        // el cambio se perdería sin que nada lo avise.
        cbTransport.setSelectedItem(partes.transport());
        onTransportChanged();
        txtHost.setText(partes.host());
        txtPort.setText(partes.port());
        // clearParameters antes del for: refreshParameterCatalog llama
        // retainParameters con el catálogo del transporte y podaría lo recién
        // cargado si se invirtiera el orden.
        activeBuilder.clearParameters();
        partes.params().forEach(activeBuilder::addParameter);
        refreshUrlPreview();
    }

    /**
     * Deja en el combo el nodo S88 que tiene el dispositivo.
     *
     * <p>
     * {@code setSelectedItem} no hace nada cuando el valor no está en el
     * modelo, y el modelo sale de {@code plant-model.xml}. Si ese archivo no
     * existe —que es lo que pasa en los proyectos sin modelo de planta
     * importado— el combo queda con el placeholder solamente, la validación ve
     * el nodo como vacío y el dispositivo queda sin forma de modificarse: el
     * formulario se abre precargado pero nunca supera la validación.
     *
     * <p>
     * El valor guardado se agrega al combo para que se vea y se conserve tal
     * cual. El combo va deshabilitado en edición, así que agregar un ítem es
     * inocuo: no habilita nada que el usuario pueda cambiar por error.
     */
    private void restaurarNodoS88(DeviceConfigData d) {
        String node = d.getS88Node();
        if (node == null || node.isBlank()) {
            return;
        }
        boolean yaEsta = false;
        for (int i = 0; i < cbS88Node.getItemCount(); i++) {
            if (node.equals(cbS88Node.getItemAt(i))) {
                yaEsta = true;
                break;
            }
        }
        if (!yaEsta) {
            cbS88Node.addItem(node);
        }
        cbS88Node.setSelectedItem(node);
    }

    /**
     * Deja la conexión en blanco, con el estado por defecto del modelo activo.
     */
    private void limpiarConexion() {
        txtHost.setText("");
        txtPort.setText("");
        if (activeBuilder != null) {
            activeBuilder.clearParameters();
        }
        refreshUrlPreview();
    }

    public void resetForm() {
        cbMarca.setSelectedIndex(0);
        cbModelo.removeAllItems();
        txtProtocol.setText("");
        txtDeviceName.setText("");
        txtDeviceKey.setText("");
        txtDescription.setText("");
        txtUUID.setText("");
        chkEnable.setSelected(false);
        cbS88Node.setSelectedIndex(0);
        txtS88UUID.setText("");
        activeBuilder = null;
        txtHost.setText("");
        lblHost.setText("Host/IP:");
        cbTransport.removeAllItems();
        txtPort.setText("");
        lblPort.setEnabled(true);
        txtPort.setEnabled(true);
        cbParameter.removeAllItems();
        txtParamValue.setText("");
        txtParamValue.setEnabled(false);
        txtUrlPreview.setText("");
    }

    // --- Validación y armado del dispositivo ----------------------------------
    /**
     * Nodo S88 elegido, o {@code null} si el combo sigue en el placeholder.
     *
     * <p>
     * Es la diferencia entre un dispositivo sin nodo y un dispositivo apuntando
     * a un área llamada "-- Seleccione --": el segundo caso se estaba
     * escribiendo en el XML sin que nadie lo pidiera.
     */
    String nodoS88Seleccionado() {
        Object selected = cbS88Node.getSelectedItem();
        String text = selected == null ? "" : selected.toString().trim();
        return text.isEmpty() || SELECCIONE.equals(text) ? null : text;
    }

    /**
     * Etiquetas de los campos obligatorios que están vacíos.
     *
     * <p>
     * No muestra diálogos a propósito: el mensaje vive en
     * {@link #getDeviceConfigData()} y esto se puede verificar en un test sin
     * ventana gráfica.
     */
    List<String> camposObligatoriosVacios() {
        List<String> faltantes = new ArrayList<>();
        if (txtDeviceName.getText().trim().isEmpty()) {
            faltantes.add("Name");
        }
        if (txtDeviceKey.getText().trim().isEmpty()) {
            faltantes.add("Key");
        }
        if (cbModelo.getSelectedItem() == null) {
            faltantes.add("Marca y modelo");
        }
        if (txtHost.getText().trim().isEmpty()) {
            faltantes.add(lblHost.getText().replace(":", ""));
        }
        if (nodoS88Seleccionado() == null) {
            faltantes.add("Node");
        }
        if (txtUUID.getText().trim().isEmpty()) {
            faltantes.add("UUID");
        }
        return faltantes;
    }

    /**
     * Valida y arma el DeviceConfigData, o devuelve null si
     * falta algún campo obligatorio.
     *
     * 
     * Las doce claves tienen que coincidir con las que lee el constructor de
     * DeviceConfigData(Properties).
     */
    public DeviceConfigData getDeviceConfigData() {
        List<String> faltantes = camposObligatoriosVacios();
        if (!faltantes.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Revisá estos campos antes de guardar:\n\n  • " + String.join("\n  • ", faltantes),
                    "Faltan datos", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        String host = txtHost.getText().trim();
        if (activeBuilder != null) {
            activeBuilder.setHost(host);
            activeBuilder.setTransport(transportActual());
            activeBuilder.setPort(txtPort.getText().trim());
        }

        Properties pDevice = new Properties();
        pDevice.put("brand", cbMarca.getSelectedItem());
        pDevice.put("model", ((DeviceModel) cbModelo.getSelectedItem()).getModel());
        pDevice.put("protocol", txtProtocol.getText());
        pDevice.put("deviceName", txtDeviceName.getText().trim());
        pDevice.put("deviceKey", txtDeviceKey.getText().trim());
        pDevice.put("description", txtDescription.getText());
        pDevice.put("uuid", txtUUID.getText());
        pDevice.put("enable", chkEnable.isSelected());
        pDevice.put("s88Node", nodoS88Seleccionado());
        pDevice.put("s88Uuid", txtS88UUID.getText());
        pDevice.put("specificParameters", activeBuilder == null
                ? "" : activeBuilder.getSpecificParametersAsString());

        return new DeviceConfigData(pDevice);
    }

    // --- Accesores de test ----------------------------------------------------
    void registrarNodoS88(String id, String uuid) {
        cbS88Node.addItem(id);
        s88NodesMap.put(id, uuid);
        cbS88Node.setEnabled(true);
    }

    JComboBox<String> getCbMarca() {
        return cbMarca;
    }

    JComboBox<DeviceModel> getCbModelo() {
        return cbModelo;
    }

    JComboBox<String> getCbS88Node() {
        return cbS88Node;
    }

    JTextField getTxtDeviceName() {
        return txtDeviceName;
    }

    JTextField getTxtDeviceKey() {
        return txtDeviceKey;
    }

    JTextField getTxtHost() {
        return txtHost;
    }

    JTextField getTxtPort() {
        return txtPort;
    }
    JTextField getTxtUUID() {
        return txtUUID;
    }
    String urlActual() {
        return activeBuilder == null ? "" : activeBuilder.getSpecificParametersAsString();
    }

    // --- Utilidades de Swing -----------------------------------------------------------
    private static final class SimpleDocumentListener implements DocumentListener {

        private final Runnable action;

        SimpleDocumentListener(Runnable action) {
            this.action = action;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            action.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            action.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            action.run();
        }
    }
}
