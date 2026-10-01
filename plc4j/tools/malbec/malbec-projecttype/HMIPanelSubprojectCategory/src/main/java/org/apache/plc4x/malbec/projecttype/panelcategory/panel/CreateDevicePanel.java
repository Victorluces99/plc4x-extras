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
 * Formulario de alta de dispositivo de comunicación.
 *
 * <p>Vive en un {@code JPanel} y no en un {@code JDialog} a propósito: el
 * diálogo es el cascaron modal y sólo aporta teclado y guardado. Así el
 * formulario se puede instanciar y verificar en un test sin ventana.
 *
 * <p>No comparte una sola clase con el asistente de comunicación
 * ({@code panel.wizard}): son dos flujos distintos y acoplar el alta de
 * dispositivo a los pasos del asistente sólo serviría para arrastrar cambios de
 * un flujo al otro. Por eso este panel trae sus propias utilidades de layout.
 *
 * <p>Los datos salen por {@link #getDeviceConfigData()}, que arma un
 * {@link Properties} con las doce claves que {@link DeviceConfigData} lee. Esa
 * clase es inmutable, así que no hay setters: todo se concentra en el
 * constructor.
 */
public final class CreateDevicePanel extends JPanel {

    private static final String SELECCIONE = "-- Seleccione --";
    private final transient Project project;
    // --- Marca y modelo ---
    private final JComboBox<String> cbMarca =
            new JComboBox<>(new String[]{SELECCIONE, "Siemens", "Allen Bradley", "Modbus"});
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

    public CreateDevicePanel(Project project) {
        this.project = project;
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        initData();
        buildUi();
        loadS88Nodes();
        resetForm();
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
     * <p>Fijar {@code gridx} y {@code gridwidth} acá no es redundante: las
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

    /** Label en la primera columna y campo ocupando las tres siguientes. */
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

    /** Dos pares label/campo en una misma fila: label, campo, label, campo. */
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
        form.add(bold("Nuevo dispositivo de comunicación", 14f), g);
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
            txtUUID.setText("");
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
                txtUUID.setText(UUID.randomUUID().toString());
            }
        });

        cbModelo.addActionListener(e -> {
            DeviceModel selectedDevice = (DeviceModel) cbModelo.getSelectedItem();
            if (selectedDevice == null) {
                return;
            }
            txtProtocol.setText(selectedDevice.getProtocol());
            txtUUID.setText(UUID.randomUUID().toString());

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
     * Carga las áreas del {@code plant-model.xml} generado al importar el modelo
     * de planta. El archivo vive en el directorio del panel (padre del
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

    private void onTransportChanged() {
        if (activeBuilder == null) {
            return;
        }
        Object transport = cbTransport.getSelectedItem();
        String transportValue = transport != null ? transport.toString() : "";
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
            txtParamValue.setText(catalog.getOrDefault(current.toString(), ""));
        }
    }

    private void refreshUrlPreview() {
        if (activeBuilder == null) {
            txtUrlPreview.setText("");
            return;
        }
        activeBuilder.setHost(txtHost.getText().trim());
        Object transport = cbTransport.getSelectedItem();
        activeBuilder.setTransport(transport != null ? transport.toString() : "");
        activeBuilder.setPort(txtPort.getText().trim());
        txtUrlPreview.setText(activeBuilder.getSpecificParametersAsString());
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
     * <p>Es la diferencia entre un dispositivo sin nodo y un dispositivo
     * apuntando a un área llamada "-- Seleccione --": el segundo caso se
     * estaba escribiendo en el XML sin que nadie lo pidiera.
     */
    String nodoS88Seleccionado() {
        Object selected = cbS88Node.getSelectedItem();
        String text = selected == null ? "" : selected.toString().trim();
        return text.isEmpty() || SELECCIONE.equals(text) ? null : text;
    }

    /**
     * Etiquetas de los campos obligatorios que están vacíos.
     *
     * <p>No muestra diálogos a propósito: el mensaje vive en
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
        return faltantes;
    }

    /**
     * Valida y arma el {@link DeviceConfigData}, o devuelve {@code null} si
     * falta algún campo obligatorio.
     *
     * <p>Las doce claves tienen que coincidir con las que lee el constructor de
     * {@code DeviceConfigData(Properties)}.
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
            Object transport = cbTransport.getSelectedItem();
            activeBuilder.setTransport(transport != null ? transport.toString() : "");
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
        // Boolean a propósito: DeviceConfigData lo lee con get("enable") y no
        // con getProperty, que devolvería null para un valor no-String.
        pDevice.put("enable", chkEnable.isSelected());
        pDevice.put("s88Node", nodoS88Seleccionado());
        pDevice.put("s88Uuid", txtS88UUID.getText());
        pDevice.put("specificParameters", activeBuilder == null
                ? "" : activeBuilder.getSpecificParametersAsString());

        return new DeviceConfigData(pDevice);
    }

    // --- Accesores de test ----------------------------------------------------

    /**
     * Registra un nodo S88 como si estuviera presente en {@code plant-model.xml}.
     *
     * <p>Existe porque sin un proyecto real no hay archivo que leer, y sin un
     * nodo la validación nunca deja llegar al camino feliz. Sólo para tests.
     */
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

    // --- Utilidades -----------------------------------------------------------

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