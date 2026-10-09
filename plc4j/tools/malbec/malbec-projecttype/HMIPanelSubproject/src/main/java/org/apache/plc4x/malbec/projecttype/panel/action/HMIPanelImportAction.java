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
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.plc4x.malbec.projecttype.panelcategory.xml.XmlSafe;
import org.apache.plc4x.malbec.s88.api.S88Element;
import org.apache.plc4x.malbec.s88.api.S88PlantModel;
import org.apache.plc4x.malbec.s88.api.S88Repository;
import org.apache.plc4x.malbec.s88.api.S88RepositoryProvider;
import org.apache.plc4x.malbec.s88.api.S88Storage;
import org.apache.plc4x.malbec.s88.plant.services.S88ProjectServices;
import org.netbeans.api.progress.ProgressRunnable;
import org.netbeans.api.progress.ProgressUtils;
import org.netbeans.api.project.ProjectManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.windows.WindowManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class HMIPanelImportAction extends AbstractAction {

    private static final String PLANT_MODEL_DUMP = "plant-model.xml";

    private static final Logger LOGGER =
            Logger.getLogger(HMIPanelImportAction.class.getName());

    static final Map<String, String> FORMATOS = Map.of("xml", "Modelo de planta (*.xml)");

    private final FileObject panel;

    public HMIPanelImportAction(FileObject panel) {
        super("Importar proyecto");
        this.panel = panel;
    }

    static String extensionDe(File archivo) {
        String nombre = archivo.getName();
        int punto = nombre.lastIndexOf('.');
        return punto < 0 || punto == nombre.length() - 1
                ? "" : nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
    }

    static boolean extensionAdmitida(File archivo) {
        return FORMATOS.containsKey(extensionDe(archivo));
    }

    static String formatosAdmitidos() {
        return String.join(" o ", FORMATOS.values());
    }

    static FilenameFilter filtroDeArchivo() {
        return (directorio, nombre) -> new File(directorio, nombre).isDirectory()
                || FORMATOS.containsKey(extensionDe(new File(nombre)));
    }

    static Set<String> formatosSinProvider(Collection<? extends S88RepositoryProvider> providers,
            Collection<String> declarados) {
        Set<String> sinProvider = new TreeSet<>(declarados);
        for (S88RepositoryProvider provider : providers) {
            sinProvider.removeIf(provider::accepts);
        }
        return sinProvider;
    }

    static void avisarFormatosDesalineados() {
        Set<String> sinProvider = formatosSinProvider(
                Lookup.getDefault().lookupAll(S88RepositoryProvider.class),
                FORMATOS.keySet());
        if (!sinProvider.isEmpty()) {
            LOGGER.log(Level.WARNING,
                    "HMIPanelImportAction declara formatos que ningun S88RepositoryProvider acepta: {0}",
                    sinProvider);
        }
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
        avisarFormatosDesalineados();

        FileDialog chooser =
                new FileDialog(WindowManager.getDefault().getMainWindow(),
                        "Seleccionar modelo de planta", FileDialog.LOAD);
        chooser.setDirectory(System.getProperty("user.home"));
        chooser.setFilenameFilter(filtroDeArchivo());
        chooser.setVisible(true);

        File[] seleccion = chooser.getFiles();
        if (seleccion == null || seleccion.length == 0) {
            return;
        }
        File archivo = seleccion[0];
        if (!extensionAdmitida(archivo)) {
            avisarFallo(archivo, "solo se admiten " + formatosAdmitidos() + ".");
            return;
        }

        Resultado resultado = ProgressUtils.showProgressDialogAndRun(
                handle -> {
                    handle.setDisplayName("Importando modelo de planta...");
                    handle.start(400);
                    try {
                        return importarEnSegundoPlano(archivo);
                    } finally {
                        handle.finish();
                    }
                },
                "Importando modelo de planta...",
                false);
        mostrar(resultado);
    }

    private Resultado importarEnSegundoPlano(File archivo) {
        S88Repository repositorio;
        try {
            repositorio = S88ProjectServices.createRepository(extensionDe(archivo), new Storage(archivo));
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "No hay repositorio para " + archivo.getName(), ex);
            return new Fallo(archivo, "el formato " + extensionDe(archivo) + " no está soportado.");
        }

        S88PlantModel modelo;
        try {
            modelo = repositorio.loadPlant();
        } catch (RuntimeException ex) {
            LOGGER.log(Level.WARNING, "No se pudo interpretar " + archivo.getName(), ex);
            return new Fallo(archivo, describe(ex));
        }

        S88Element root = modelo == null ? null : modelo.getRoot();
        String areaId = root == null || root.getId() == null ? "" : root.getId().trim();
        if (areaId.isEmpty()) {
            return new Fallo(archivo, "el archivo no define un área de planta.");
        }

        File panelFile = FileUtil.toFile(panel);
        if (panelFile != null) {
            FileUtil.refreshFor(panelFile);
        }

        // H-08(a): antes se pedia la carpeta 'image' y, si no existia,
        // reversechildstack hacia return en silencio y el flujo llegaba igualmente a
        // "importación exitosa" sin haber creado nada. Ahora la ausencia es un fallo.
        FileObject imageFo = panel.getFileObject("image");
        String problema = problemaDelDestino(imageFo);
        if (problema != null) {
            LOGGER.log(Level.WARNING, "No se importó {0}: {1}",
                    new Object[]{archivo.getName(), problema});
            return new Fallo(archivo, problema);
        }

        int fallidos = 0;
        String nombreInvalido = primerNombreInvalido(root);
        if (nombreInvalido != null) {
            // H-08(d): se comprueba todo el modelo antes de crear la primera carpeta,
            // para no dejar estructura a medias en el panel.
            LOGGER.log(Level.WARNING, "No se importó {0}: {1}",
                    new Object[]{archivo.getName(), nombreInvalido});
            return new Fallo(archivo, nombreInvalido);
        }

        if (root.getChildren() != null) {
            for (S88Element child : root.getChildren()) {
                fallidos += reversechildstack(child, imageFo);
            }
        }
        if (fallidos > 0) {
            // H-08(b): antes cada IOException por nodo se imprimia y el bucle seguia,
            // y el resultado se reportaba como exito. Una importacion parcial no es un
            // exito: se informa del numero de nodos que quedaron fuera.
            return new Fallo(archivo, fallidos + " nodo(s) del modelo no se pudieron crear;"
                    + " la importación está incompleta.");
        }

        try {
            if (!exportPlantModel(root)) {
                return new Fallo(archivo, "no se pudo escribir " + PLANT_MODEL_DUMP + " en el proyecto.");
            }
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "No se pudo escribir " + PLANT_MODEL_DUMP, ex);
            return new Fallo(archivo, describe(ex));
        }

        if (panelFile != null) {
            FileUtil.refreshAll();
            ProjectManager.getDefault().clearNonProjectCache();
        }
        return new Exito(archivo, areaId);
    }

    /** Caracteres que Windows no admite en un nombre de fichero o carpeta. */
    private static final String INVALIDOS = "/\\:*?\"<>|";

    /**
     * Comprueba que un nombre de nodo se puede usar como carpeta y como fichero.
     *
     * <p>H-08(d): el {@code id} viene del fichero de modelo que elige el usuario y se
     * usaba tal cual en {@code createFolder}, {@code createData} y
     * {@code getFileObject}. Un id como {@code ../../evil} escapaba del panel y
     * creaba carpetas fuera, y caracteres como {@code :} o {@code *} producen rutas
     * invalidas en Windows.
     *
     * <p>Se rechaza en lugar de renombrar: renombrar en silencio crearia carpetas con
     * un nombre que el usuario no reconoce y romperia la correspondencia con el modelo.
     *
     * @return el motivo por el que el nombre no vale, o {@code null} si es valido
     */
    static String problemaDeNombre(String id) {
        if (id == null || id.isBlank()) {
            return "un nodo del modelo no tiene identificador";
        }
        String nombre = id.trim();
        if (nombre.isEmpty()) {
            return "un nodo del modelo tiene un identificador vacío";
        }
        // Se comprueba '..' antes que los caracteres invalidos: un identificador como
        // '../../x' es un intento de salir del panel y merece el aviso mas grave, no el
        // mensaje generico de 'caracter no valido'.
        if (nombre.contains("..")) {
            return "el identificador '" + nombre + "' contiene '..' y podría crear carpetas"
                    + " fuera del panel";
        }
        for (int i = 0; i < INVALIDOS.length(); i++) {
            char c = INVALIDOS.charAt(i);
            if (nombre.indexOf(c) >= 0) {
                return "el identificador '" + nombre + "' contiene el carácter '" + c
                        + "', que no se puede usar en el nombre de una carpeta";
            }
        }
        if (Character.isISOControl(nombre.charAt(0))
                || Character.isISOControl(nombre.charAt(nombre.length() - 1))) {
            return "el identificador '" + nombre + "' empieza o acaba con un carácter de control";
        }
        return null;
    }

    /**
     * Primer identificador invalido del modelo, buscando en todo el arbol.
     *
     * <p>Se recorre antes de crear nada: si se detectara durante la creacion, ya
     * habria carpetas a medias en el panel.
     *
     * @return el motivo del primer id invalido, o {@code null} si todos valen
     */
    static String primerNombreInvalido(S88Element root) {
        Deque<S88Element> pendientes = new ArrayDeque<>();
        if (root != null) {
            pendientes.push(root);
        }
        while (!pendientes.isEmpty()) {
            S88Element node = pendientes.pop();
            String problema = problemaDeNombre(node.getId());
            if (problema != null) {
                return problema;
            }
            List<S88Element> children = node.getChildren();
            if (children != null) {
                for (int i = children.size() - 1; i >= 0; i--) {
                    S88Element child = children.get(i);
                    if (child != null) {
                        pendientes.push(child);
                    }
                }
            }
        }
        return null;
    }

    private static String describe(Throwable ex) {
        String mensaje = ex.getMessage();
        return mensaje == null || mensaje.isBlank() ? ex.getClass().getSimpleName() : mensaje;
    }

    /**
     * Comprueba que el panel tiene donde crear la estructura del modelo (H-08(a)).
     *
     * @return el motivo por el que no se puede importar, o {@code null} si todo va bien
     */
    static String problemaDelDestino(FileObject imageFo) {
        if (imageFo == null) {
            return "el panel no tiene la carpeta 'image', que es donde se crea la estructura;"
                    + " no se ha creado nada.";
        }
        if (!imageFo.isFolder()) {
            return "'image' existe pero no es una carpeta; no se ha creado nada.";
        }
        return null;
    }

    private void mostrar(Resultado resultado) {
        if (resultado instanceof Exito exito) {
            avisarExito(exito.archivo, exito.areaId);
        } else if (resultado instanceof Fallo fallo) {
            avisarFallo(fallo.archivo, fallo.motivo);
        }
    }

    private sealed interface Resultado permits Exito, Fallo {
    }

    private record Exito(File archivo, String areaId) implements Resultado {
    }

    private record Fallo(File archivo, String motivo) implements Resultado {
    }

    private void avisarExito(File file, String areaId) {
        JOptionPane.showMessageDialog(null,
                "Se importó el modelo de planta '" + areaId + "' desde '" + file.getName() + "'.",
                "Importación exitosa", JOptionPane.INFORMATION_MESSAGE);
    }

    private void avisarFallo(File file, String motivo) {
        JOptionPane.showMessageDialog(null,
                "No se pudo importar '" + file.getName() + "'.\n\n" + motivo,
                "Error de importación", JOptionPane.ERROR_MESSAGE);
    }

    private boolean exportPlantModel(S88Element root) throws IOException {
        LinkedHashMap<String, Object> snapshot = collectElement(root);
        File dir = FileUtil.toFile(panel);
        if (dir == null) {
            return false;
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
            LOGGER.log(Level.INFO, "Snapshot del modelo de planta en: {0}",
                    target.getAbsolutePath());
        return true;
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
                // H-08(e): sin XmlSafe, una DOCTYPE con entidades externas permitiria
                // leer ficheros locales o hacer peticiones salientes desde un
                // plant-model.xml manipulado.
                DocumentBuilder builder = XmlSafe.newFactory().newDocumentBuilder();
                return builder.parse(target);
            } catch (SAXException | ParserConfigurationException ex) {
                throw new IOException(PLANT_MODEL_DUMP + " ya existe pero no se puede leer, "
                        + "por lo que la importación se canceló para no perder su contenido. "
                        + "Revisá el archivo y, si ya no sirve, borralo a mano.", ex);
            }
        }
        try {
            DocumentBuilder builder = XmlSafe.newFactory().newDocumentBuilder();
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
        Path temporal = Files.createTempFile(target.toPath().getParent(), PLANT_MODEL_DUMP, ".tmp");
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.transform(new DOMSource(doc), new StreamResult(temporal.toFile()));
            verificarLegible(temporal.toFile());
            Files.move(temporal, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (TransformerException ex) {
            throw new IOException(ex);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private void verificarLegible(File archivo) throws IOException {
        try {
            // H-08(e): misma proteccion que en la lectura.
            XmlSafe.newFactory().newDocumentBuilder().parse(archivo);
        } catch (SAXException | ParserConfigurationException ex) {
            throw new IOException("El " + PLANT_MODEL_DUMP + " generado no se pudo leer, "
                    + "por lo que no se reemplazó el archivo existente.", ex);
        }
    }

    static LinkedHashMap<String, Object> collectElement(S88Element node) {
        LinkedHashMap<String, Object> snap = new LinkedHashMap<>();
        snap.put("id", node.getId() == null ? "" : node.getId());
        snap.put("tag", tagFor(node));
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

    static LinkedHashMap<String, Object> variable(String name, Object type) {
        LinkedHashMap<String, Object> variable = new LinkedHashMap<>();
        variable.put("name", name);
        variable.put("type", type == null ? "" : String.valueOf(type));
        return variable;
    }

    static String tagFor(S88Element node) {
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

    static List<Object> collectVariables(Object value) {
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

    /**
     * Crea la estructura de carpetas y ficheros del modelo a partir de {@code root}.
     *
     * @return cuantos nodos no se pudieron crear (H-08(b)). Antes el metodo no
     *         devolvia nada: cada fallo se imprimia y el bucle continuaba, y quien
     *         llama acababa anunciando una importacion successful.
     */
    public int reversechildstack(S88Element root, FileObject baseDir) {
        if (root == null || baseDir == null) {
            return 0;
        }

        Deque<S88Element> stack = new ArrayDeque<>();
        Map<S88Element, FileObject> dirMap = new HashMap<>();
        int fallidos = 0;

        stack.push(root);
        dirMap.put(root, baseDir);

        while (!stack.isEmpty()) {
            S88Element node = stack.pop();
            FileObject parentDir = dirMap.remove(node);

            if (parentDir == null) {
                continue;
            }

            try {
                // H-08(d): guarda defensiva. El flujo normal ya valida todo el modelo
                // antes de crear nada, pero este metodo es publico.
                String problema = problemaDeNombre(node.getId());
                if (problema != null) {
                    fallidos++;
                    LOGGER.log(Level.WARNING, "Nodo rechazado: {0}", problema);
                    continue;
                }
                FileObject currentDir = createFolder(node, parentDir);
                createCfgFile(currentDir);

                LOGGER.fine(() -> "Propiedades en " + node.getId() + ": " + node.getProperties());
                LOGGER.fine(() -> "check: " + node.isCheck());
                if (node.isCheck()) {
                    LOGGER.fine(() -> "Creando BOB para: " + node.getId());
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
                fallidos++;
                LOGGER.log(Level.WARNING, "No se pudo crear el nodo '" + node.getId() + "'", ex);
            }
        }
        return fallidos;
    }

}
