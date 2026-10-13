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
package org.apache.plc4x.malbec.projecttype.panel.plant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.plc4x.malbec.projecttype.panel.plant.ImportResult.Exito;
import org.apache.plc4x.malbec.projecttype.panel.plant.ImportResult.Fallo;
import org.apache.plc4x.malbec.projecttype.panel.plant.PlantSnapshot.AreaNode;
import org.apache.plc4x.malbec.projecttype.panelcategory.ProjectIds;
import org.apache.plc4x.malbec.s88.api.S88Element;
import org.apache.plc4x.malbec.s88.api.S88PlantModel;
import org.apache.plc4x.malbec.s88.api.S88Repository;
import org.apache.plc4x.malbec.s88.api.S88Storage;
import org.apache.plc4x.malbec.s88.plant.services.S88ProjectServices;
import org.netbeans.api.project.ProjectManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * Importa un modelo de planta en un panel: crea la estructura de carpetas y escribe el
 * {@code plant-model.xml}.
 *
* <p>Sin Swing. Todo lo que aqui ocurre se puede invocar y comprobar desde un test o desde
 * una herramienta de linea de comandos, y devuelve un {@link ImportResult} en vez de
 * enseñar un dialogo. La accion Swing se limita a pedir el fichero, mostrar la barra de
 * progreso y presentar el resultado.
 */
public final class PlantImporter {

    private static final Logger LOGGER = Logger.getLogger(PlantImporter.class.getName());

    private PlantImporter() {
    }

    /** Caracteres que Windows no admite en un nombre de fichero o carpeta. */
    private static final String INVALIDOS = "/\\:*?\"<>|";

    /**
     * Importa {@code archivo} dentro del panel {@code panel}.
     *
     * <p>No lanza excepciones por fallos previsibles: un modelo ilegible, un id invalido o
     * un destino que no existe se devuelven como {@link Fallo}, porque son cosas que el
     * operador tiene que leer en un mensaje, no excepciones que tragar en un log.
     *
     * @param panel carpeta del panel, que debe contener la subcarpeta {@code image}
     */
    public static ImportResult importPlant(File archivo, FileObject panel) {
        S88Repository repositorio;
        try {
            repositorio = S88ProjectServices.createRepository(
                    extensionDe(archivo), new Storage(archivo));
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

        // H-08(a): antes se pedia la carpeta 'image' y, si no existia, el flujo llegaba
        // igualmente a "importación exitosa" sin haber creado nada. Ahora es un fallo.
        FileObject imageFo = panel.getFileObject("image");
        String problema = problemaDelDestino(imageFo);
        if (problema != null) {
            LOGGER.log(Level.WARNING, "No se importó {0}: {1}",
                    new Object[]{archivo.getName(), problema});
            return new Fallo(archivo, problema);
        }

        int fallidos = 0;
        // H-08(d): se comprueba todo el modelo antes de crear la primera carpeta, para no
        // dejar estructura a medias en el panel.
        String nombreInvalido = primerNombreInvalido(root);
        if (nombreInvalido != null) {
            LOGGER.log(Level.WARNING, "No se importó {0}: {1}",
                    new Object[]{archivo.getName(), nombreInvalido});
            return new Fallo(archivo, nombreInvalido);
        }

        if (root.getChildren() != null) {
            for (S88Element child : root.getChildren()) {
                fallidos += reverseChildStack(child, imageFo);
            }
        }
        if (fallidos > 0) {
            // H-08(b): una importacion parcial no es un exito: se informa de cuantos nodos
            // quedaron fuera.
            return new Fallo(archivo, fallidos + " nodo(s) del modelo no se pudieron crear;"
                    + " la importación está incompleta.");
        }

        try {
            File dir = FileUtil.toFile(panel);
            if (dir == null) {
                return new Fallo(archivo, "no se pudo escribir " + PlantModelXml.FILE_NAME
                        + " en el proyecto.");
            }
            AreaNode snapshot = PlantSnapshot.collectElement(root);
            File target = new File(dir, PlantModelXml.FILE_NAME);
            PlantModelXml.export(snapshot, target);
            FileUtil.refreshFor(target);
            LOGGER.log(Level.INFO, "Snapshot del modelo de planta en: {0}",
                    target.getAbsolutePath());
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, "No se pudo escribir " + PlantModelXml.FILE_NAME, ex);
            return new Fallo(archivo, describe(ex));
        }

        if (panelFile != null) {
            FileUtil.refreshAll();
            ProjectManager.getDefault().clearNonProjectCache();
        }
        return new Exito(archivo, areaId);
    }

    /**
     * Crea la estructura de carpetas y ficheros del modelo a partir de {@code root}.
     *
     * @return cuantos nodos no se pudieron crear (H-08(b)). Antes el metodo no devolvia
     *         nada: cada fallo se imprimia y el bucle continuaba, y quien llamaba acababa
     *         anunciando una importacion successful.
     */
    public static int reverseChildStack(S88Element root, FileObject baseDir) {
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

    /**
     * Comprueba que un nombre de nodo se puede usar como carpeta y como fichero.
     *
     * <p>H-08(d): el {@code id} viene del fichero de modelo que elige el usuario y se usaba
     * tal cual en {@code createFolder}, {@code createData} y {@code getFileObject}. Un id
     * como {@code ../../evil} escapaba del panel y creaba carpetas fuera, y caracteres como
     * {@code :} o {@code *} producen rutas invalidas en Windows.
     *
     * <p>Se rechaza en lugar de renombrar: renombrar en silencio crearia carpetas con un
     * nombre que el usuario no reconoce y romperia la correspondencia con el modelo.
     *
     * @return el motivo por el que el nombre no vale, o {@code null} si es valido
     */
    public static String problemaDeNombre(String id) {
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
     * <p>Se recorre antes de crear nada: si se detectara durante la creacion, ya habria
     * carpetas a medias en el panel.
     *
     * @return el motivo del primer id invalido, o {@code null} si todos valen
     */
    public static String primerNombreInvalido(S88Element root) {
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

    /**
     * Comprueba que el panel tiene donde crear la estructura del modelo (H-08(a)).
     *
     * @return el motivo por el que no se puede importar, o {@code null} si todo va bien
     */
    public static String problemaDelDestino(FileObject imageFo) {
        if (imageFo == null) {
            return "el panel no tiene la carpeta 'image', que es donde se crea la estructura;"
                    + " no se ha creado nada.";
        }
        if (!imageFo.isFolder()) {
            return "'image' existe pero no es una carpeta; no se ha creado nada.";
        }
        return null;
    }

    static String extensionDe(File archivo) {
        String nombre = archivo.getName();
        int punto = nombre.lastIndexOf('.');
        return punto < 0 || punto == nombre.length() - 1
                ? "" : nombre.substring(punto + 1).toLowerCase(java.util.Locale.ROOT);
    }

    private static String describe(Throwable ex) {
        String mensaje = ex.getMessage();
        return mensaje == null || mensaje.isBlank() ? ex.getClass().getSimpleName() : mensaje;
    }

    private static FileObject createFolder(S88Element node, FileObject parentDir) throws IOException {
        FileObject folder = parentDir.getFileObject(node.getId());
        return (folder == null) ? parentDir.createFolder(node.getId()) : folder;
    }

    private static void createCfgFile(FileObject folder) throws IOException {
        if (folder.getFileObject(ProjectIds.CATEGORY_MARKER_FILE) == null) {
            folder.createData(ProjectIds.CATEGORY_MARKER_FILE);
        }
    }

    private static void createBobFile(S88Element node, FileObject folder) throws IOException {
        if (folder.getFileObject(node.getId() + ".bob") == null) {
            folder.createData(node.getId(), "bob");
        }
    }

    /** El fichero del modelo abierto como {@link S88Storage}. */
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
}