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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.netbeans.api.project.Project;
import org.openide.filesystems.FileObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class PlantModelReader {

    private static final String PLANT_MODEL_FILE = "plant-model.xml";
    private final Project project;

    public PlantModelReader(Project project) {
        this.project = project;
    }

    public List<PlantVariable> loadAreaVariables(String areaId) throws Exception {
        List<PlantVariable> result = new ArrayList<>();
        if (areaId == null || areaId.trim().isEmpty()) {
            return result;
        }

        FileObject plantModel = findPlantModelFile(
                project != null ? project.getProjectDirectory() : null);
        if (plantModel == null) {
            return result;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Protección contra ataques XXE y procesamiento seguro
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Lectura mediante InputStream (compatible con cualquier FileSystem de NetBeans)
        try (InputStream is = plantModel.getInputStream()) {
            Document doc = builder.parse(is);
            Element root = doc.getDocumentElement();
            if (root == null) {
                return result;
            }

            Element area = findAreaRecursive(root, areaId);
            if (area != null) {
                collectPlantVariables(area, areaId, result);
            }
        }

        return result;
    }

    /* Búsqueda recursiva del área para permitir jerarquías más profundas (Enterprise/Site/Area) */
    private Element findAreaRecursive(Element element, String areaId) {
        if ("area".equals(element.getTagName()) && areaId.equals(element.getAttribute("id"))) {
            return element;
        }
        
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element found = findAreaRecursive((Element) node, areaId);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private void collectPlantVariables(Element element, String currentPath, List<PlantVariable> result) {
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element child = (Element) node;
            if ("variable".equals(child.getTagName())) {
                String name = child.getAttribute("name");
                if (!name.isEmpty()) {
                    // Mantiene el atributo "Type" como en tu modelo original
                    result.add(new PlantVariable(currentPath, name, child.getAttribute("Type")));
                }
                continue;
            }

            String id = child.getAttribute("id");
            String childPath = id.isEmpty() ? currentPath : currentPath + "/" + id;
            collectPlantVariables(child, childPath, result);
        }
    }

    private FileObject findPlantModelFile(FileObject projectDir) {
        FileObject fo = projectDir;
        while (fo != null) {
            FileObject xml = fo.getFileObject(PLANT_MODEL_FILE);
            if (xml != null) {
                return xml;
            }
            fo = fo.getParent();
        }
        return null;
    }
}

//import java.util.ArrayList;
//import java.util.List;
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import org.netbeans.api.project.Project;
//import org.openide.filesystems.FileObject;
//import org.openide.filesystems.FileUtil;
//import org.w3c.dom.Document;
//import org.w3c.dom.Element;
//import org.w3c.dom.Node;
//import org.w3c.dom.NodeList;

/**
  Lee el plant-model.xml del proyecto y devuelve las variables de
  proceso que pertenecen a un área S88.
 
  El archivo no tiene esquema (no es PLCopen), así que se parsea con DOM
  estándar en vez de con XmlBeans. El recorrido es en profundidad: cada
  elemento hijo aporta su atributo id al camino, de modo que la ruta
  acumulada desambigua variables homónimas de jerarquías distintas.
 */
//public class PlantModelReader {
//
//    private static final String PLANT_MODEL_FILE = "plant-model.xml";
//
//    private final Project project;
//
//    public PlantModelReader(Project project) {
//        this.project = project;
//    }
//
//    public List<PlantVariable> loadAreaVariables(String areaId) throws Exception {
//        List<PlantVariable> result = new ArrayList<>();
//        if (areaId == null || areaId.isEmpty()) {
//            return result;
//        }
//        FileObject plantModel = findPlantModelFile(
//                project != null ? project.getProjectDirectory() : null);
//        if (plantModel == null) {
//            return result;
//        }
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder builder = factory.newDocumentBuilder();
//        Document doc = builder.parse(FileUtil.toFile(plantModel));
//        Element root = doc.getDocumentElement();
//        if (root == null) {
//            return result;
//        }
//        Element area = findArea(root, areaId);
//        if (area != null) {
//            collectPlantVariables(area, areaId, result);
//        }
//        return result;
//    }
//
//    /* Primer hijo directo de tipo area con ese identificador. */
//    private Element findArea(Element root, String areaId) {
//        NodeList nodes = root.getChildNodes();
//        for (int i = 0; i < nodes.getLength(); i++) {
//            Node node = nodes.item(i);
//            if (node.getNodeType() != Node.ELEMENT_NODE) {
//                continue;
//            }
//            Element element = (Element) node;
//            if ("area".equals(element.getTagName()) && areaId.equals(element.getAttribute("id"))) {
//                return element;
//            }
//        }
//        return null;
//    }
//
//    private void collectPlantVariables(Element element, String currentPath, List<PlantVariable> result) {
//        NodeList nodes = element.getChildNodes();
//        for (int i = 0; i < nodes.getLength(); i++) {
//            Node node = nodes.item(i);
//            if (node.getNodeType() != Node.ELEMENT_NODE) {
//                continue;
//            }
//            Element child = (Element) node;
//            if ("variable".equals(child.getTagName())) {
//                String name = child.getAttribute("name");
//                if (!name.isEmpty()) {
//                    result.add(new PlantVariable(currentPath, name, child.getAttribute("Type")));
//                }
//                continue;
//            }
//            String id = child.getAttribute("id");
//            String childPath = id.isEmpty() ? currentPath : currentPath + "/" + id;
//            collectPlantVariables(child, childPath, result);
//        }
//    }
//
//    // Sube por la jerarquía de carpetas hasta encontrar el archivo de planta. 
//    private FileObject findPlantModelFile(FileObject projectDir) {
//        FileObject fo = projectDir;
//        while (fo != null) {
//            FileObject xml = fo.getFileObject(PLANT_MODEL_FILE);
//            if (xml != null) {
//                return xml;
//            }
//            fo = fo.getParent();
//        }
//        return null;
//    }
//}
