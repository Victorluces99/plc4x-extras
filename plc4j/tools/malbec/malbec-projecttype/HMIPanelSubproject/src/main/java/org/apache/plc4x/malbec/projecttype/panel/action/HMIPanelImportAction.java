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
package org.apache.plc4x.malbec.projecttype.panel.action;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.AbstractAction;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.plc4x.malbec.s88.api.S88Element;
import org.apache.plc4x.malbec.s88.api.S88PlantModel;
import org.apache.plc4x.malbec.s88.api.S88Storage;
import org.apache.plc4x.malbec.s88.plant.services.S88ProjectServices;
import org.netbeans.api.project.ProjectManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class HMIPanelImportAction extends AbstractAction {

    /**
     * Nombre del archivo donde se vuelca el snapshot del modelo de planta.
     */
    private static final String PLANT_MODEL_DUMP = "plant-model.xml";

    private final FileObject panel;

    public HMIPanelImportAction(FileObject panel) {
        super("Importar proyecto");
        this.panel = panel;
    }

    private record Storage(File file) implements S88Storage {

        @Override
        public InputStream openInput() throws IOException, FileNotFoundException {
            return new FileInputStream(file);
        }

        @Override
        public OutputStream openOutput() throws IOException {
            return new FileOutputStream(file);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        importPlant();
    }

    private void importPlant() {
        FileDialog chooser = new FileDialog((Frame) null, "SELECT FILE", FileDialog.LOAD);
        chooser.setVisible(true);
        File[] selected = chooser.getFiles();
        if (selected == null || selected.length == 0) {
            return;
        }
        File file = selected[0];
        Storage storage = new Storage(file);
        S88PlantModel modelo = S88ProjectServices
                .createRepository("xml", storage).loadPlant();
        File panelFile = FileUtil.toFile(panel);
        if (panelFile != null) {
            FileUtil.refreshFor(panelFile);
        }

        FileObject imageFo = panel.getFileObject("image");
        S88Element root = modelo.getRoot();
        if (root != null && root.getChildren() != null) {
            for (S88Element child : root.getChildren()) {
                reversechildstack(child, imageFo);
            }
        }

        if (root != null) {
            try {
                exportPlantModel(root);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        if (panelFile != null) {
            FileUtil.refreshAll();
            ProjectManager.getDefault().clearNonProjectCache();
        }
    }

    /**
     * Construye una representación en {@link LinkedHashMap} del árbol S88 (área
     * y jerarquía de elementos) con sus variables (nombre y Type) y la agrega
     * al {@value #PLANT_MODEL_DUMP} del proyecto. Cada planta importada queda
     * delimitada por su etiqueta {@code <area>} con un {@code uuid} propio y no
     * se sobreescribe: si el archivo ya existe se acumula la nueva planta; si
     * la planta ya estaba importada (mismo id) se reemplaza solo su sección
     * conservando el {@code uuid} original.
     */
    private void exportPlantModel(S88Element root) throws IOException {
        LinkedHashMap<String, Object> snapshot = collectElement(root);
        File dir = FileUtil.toFile(panel);
        if (dir == null) {
            return;
        }
        File target = new File(dir, PLANT_MODEL_DUMP);
        Document doc = loadOrCreateDocument(target);
        String areaId = String.valueOf(snapshot.get("id"));
        String areaUuid = findAreaUuid(doc, areaId);
        Element areaXml = buildElementXml(doc, snapshot);
        areaXml.setAttribute("uuid", areaUuid != null ? areaUuid : UUID.randomUUID().toString());
        removePlant(doc, areaId);
        doc.getDocumentElement().appendChild(areaXml);
        writeSnapshotDocument(target, doc);
        FileUtil.refreshFor(target);
        System.out.println("Snapshot del modelo de planta en: " + target.getAbsolutePath());
    }

    private String findAreaUuid(Document doc, String id) {
        Element root = doc.getDocumentElement();
        if (root == null) {
            return null;
        }
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) node;
            if ("area".equals(element.getTagName()) && id.equals(element.getAttribute("id"))) {
                String uuid = element.getAttribute("uuid");
                return uuid.isEmpty() ? null : uuid;
            }
        }
        return null;
    }

    private Document loadOrCreateDocument(File target) throws IOException {
        if (target.exists()) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                return builder.parse(target);
            } catch (SAXException | ParserConfigurationException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.newDocument();
            doc.appendChild(doc.createElement("plant-model"));
            return doc;
        } catch (ParserConfigurationException ex) {
            throw new IOException(ex);
        }
    }

    private void removePlant(Document doc, String id) {
        Element root = doc.getDocumentElement();
        if (root == null) {
            return;
        }
        NodeList nodes = root.getChildNodes();
        for (int i = nodes.getLength() - 1; i >= 0; i--) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            Element element = (Element) node;
            if ("area".equals(element.getTagName()) && id.equals(element.getAttribute("id"))) {
                root.removeChild(node);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Element buildElementXml(Document doc, LinkedHashMap<String, Object> element) {
        Element xml = doc.createElement(String.valueOf(element.get("tag")));
        xml.setAttribute("id", String.valueOf(element.get("id")));
        appendSectionXml(doc, xml, "variables", (List<Object>) element.get("variables"));
        appendSectionXml(doc, xml, "parameters", (List<Object>) element.get("parameters"));
        appendSectionXml(doc, xml, "report", (List<Object>) element.get("report"));

        List<Object> children = (List<Object>) element.get("children");
        if (children != null && !children.isEmpty()) {
            for (Object child : children) {
                xml.appendChild(buildElementXml(doc, (LinkedHashMap<String, Object>) child));
            }
        }
        return xml;
    }

    private void appendSectionXml(Document doc, Element parent, String tag, List<Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return;
        }
        Element section = doc.createElement(tag);
        for (Object variable : variables) {
            LinkedHashMap<String, Object> v = (LinkedHashMap<String, Object>) variable;
            Element variableXml = doc.createElement("variable");
            variableXml.setAttribute("name", String.valueOf(v.get("name")));
            variableXml.setAttribute("Type", String.valueOf(v.get("type")));
            section.appendChild(variableXml);
        }
        parent.appendChild(section);
    }

    private void writeSnapshotDocument(File target, Document doc) throws IOException {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(doc), new StreamResult(target));
        } catch (TransformerException ex) {
            throw new IOException(ex);
        }
    }

    private LinkedHashMap<String, Object> collectElement(S88Element node) {
        LinkedHashMap<String, Object> snap = new LinkedHashMap<>();
        snap.put("id", node.getId() == null ? "" : node.getId());
        snap.put("tag", tagFor(node));

        /*
         * Variables: las propiedades estructuradas (mapas). Una variable es un
         * mapa con "Type". Las variables directas van a <variables>; las
         * agrupadas bajo "Parameters" (salidas) van a <parameters> y las de
         * "Reports" (entradas) van a <report>. En los EquipmentPropertyChild de
         * esos grupos también se buscan variables (nombre + Type).
         */
        List<Object> variables = new ArrayList<>();
        List<Object> parameters = new ArrayList<>();
        List<Object> report = new ArrayList<>();
        for (Map.Entry<String, Object> entry : node.getProperties().entrySet()) {
            if (!(entry.getValue() instanceof Map<?, ?> map)) {
                continue;
            }
            if (map.containsKey("Type")) {
                variables.add(variable(entry.getKey(), map.get("Type")));
            } else {
                List<Object> target = switch (entry.getKey().toLowerCase()) {
                    case "parameters" ->
                        parameters;
                    case "reports" ->
                        report;
                    default ->
                        variables;
                };
                target.addAll(collectVariables(map));
            }
        }
        snap.put("variables", variables);
        snap.put("parameters", parameters);
        snap.put("report", report);

        List<Object> children = new ArrayList<>();
        if (node.getChildren() != null) {
            for (S88Element child : node.getChildren()) {
                children.add(collectElement(child));
            }
        }
        snap.put("children", children);
        return snap;
    }

    private LinkedHashMap<String, Object> variable(String name, Object type) {
        LinkedHashMap<String, Object> variable = new LinkedHashMap<>();
        variable.put("name", name);
        variable.put("type", type == null ? "" : String.valueOf(type));
        return variable;
    }

    private String tagFor(S88Element node) {
        if (node.getLevel() == null) {
            return "element";
        }
        return switch (node.getLevel()) {
            case AREA ->
                "area";
            case PROCESSCELL ->
                "processcell";
            default ->
                "element";
        };
    }

    private List<Object> collectVariables(Object value) {
        List<Object> collected = new ArrayList<>();
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getValue() instanceof Map<?, ?> nested) {
                    Object type = nested.get("Type");
                    if (type != null) {
                        collected.add(variable(String.valueOf(entry.getKey()), type));
                    } else {
                        collected.addAll(collectVariables(nested));
                    }
                }
            }
        }
        return collected;
    }

    private FileObject createFolder(S88Element node, FileObject parentDir) throws IOException {
        FileObject folder = parentDir.getFileObject(node.getId());
        return (folder == null) ? parentDir.createFolder(node.getId()) : folder;
    }

    private void createCfgFile(FileObject folder) throws IOException {
        if (folder.getFileObject("category.cfg") == null) {
            folder.createData("category.cfg");
        }
    }

    private void createBobFile(S88Element node, FileObject folder) throws IOException {
        if (folder.getFileObject(node.getId() + ".bob") == null) {
            folder.createData(node.getId(), "bob");
        }
    }

    public void reversechildstack(S88Element root, FileObject baseDir) {
        if (root == null || baseDir == null) {
            return;
        }

        Deque<S88Element> stack = new ArrayDeque<>();
        Map<S88Element, FileObject> dirMap = new HashMap<>();

        stack.push(root);
        dirMap.put(root, baseDir);

        while (!stack.isEmpty()) {
            S88Element node = stack.pop();
            FileObject parentDir = dirMap.remove(node);

            if (parentDir == null) {
                continue;
            }

            try {
                FileObject currentDir = createFolder(node, parentDir);
                createCfgFile(currentDir);

                System.out.println("Propiedades en " + node.getId() + ": " + node.getProperties());
                System.out.println("check: " + node.isCheck());
                if (node.isCheck()) {
                    System.out.println("-> Creando BOB File para: " + node.getId());
                    createBobFile(node, currentDir);
                }

                List<S88Element> children = node.getChildren();
                if (children != null) {
                    for (int i = children.size() - 1; i >= 0; i--) {
                        S88Element child = children.get(i);
                        dirMap.put(child, currentDir);
                        stack.push(child);
                    }
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

}
