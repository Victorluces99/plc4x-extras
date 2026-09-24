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
package org.apache.plc4x.malbec.projecttype.panel;



import java.util.HashSet;
import java.util.Set;
import javax.swing.event.ChangeListener;
import org.apache.plc4x.malbec.projecttype.panelcategory.HMICategoryDefinition;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.spi.project.SubprojectProvider;

import org.openide.filesystems.FileObject;


public class HMIPanelCategorySubprojectProviderImpl implements SubprojectProvider {

    public static final String CATEGORY_SUBPROJECT_FILE = "category.cfg";
    private Project project;

    private final String[] folders = new String[HMICategoryDefinition.values().length];

    public HMIPanelCategorySubprojectProviderImpl(Project project) {
        this.project = project;

    }

    @Override
    public Set<? extends Project> getSubprojects() {
        return loadProjects(project.getProjectDirectory());
    }

    @Override
    public void addChangeListener(ChangeListener cl) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    private Set loadProjects(FileObject dir) {
        Set<Project> subProjects = new HashSet<>();

        for (FileObject child : project.getProjectDirectory().getChildren()) {
            if (child.isFolder() && child.getFileObject(CATEGORY_SUBPROJECT_FILE) != null) {

                try {
                    Project subProject = ProjectManager.getDefault().findProject(child);

                    if (subProject != null) {
                        subProjects.add(project);
                    }
                } catch (Exception e) {
                }
            }
        }
        return subProjects;
    }

    private void fillAllNames() {
        HMICategoryDefinition[] values = HMICategoryDefinition.values();
        for (int i = 0; i < values.length; i++) {
            folders[i] = values[i].getDisplayName();
            System.out.println(values[i].getDisplayName());
        }
    }
//    private Set<HMIPanelCategorySubprojectImpl> loadProjects(FileObject dir) {
//        Set<HMIPanelCategorySubprojectImpl> result = new LinkedHashSet<>();
//
//        for (FileObject child : dir.getChildren()) {
//            // Ignorar si no es una carpeta
//            if (!child.isFolder()) {
//                continue;
//            }
//
//            // Buscar si el nombre de la carpeta coincide con algún elemento de tu enum
//            HMICategoryDefinition match = findCategoryByName(child.getName());
//
//            if (match != null) {
//                try {
//                    // Intentar obtener el proyecto mediante NetBeans ProjectManager
//                    Project p = ProjectManager.getDefault().findProject(child);
//
//                    if (p instanceof HMIPanelCategorySubprojectImpl) {
//                        result.add((HMIPanelCategorySubprojectImpl) p);
//                        System.out.println("Proyecto cargado exitosamente: " + match.getDisplayName());
//                    } else {
//                        System.out.println("La carpeta '" + child.getName() + "' existe pero NetBeans no la reconoce como HMIPanelCategorySubprojectImpl.");
//                    }
//                } catch (IOException | IllegalArgumentException ex) {
//                    Exceptions.printStackTrace(ex);
//                }
//            }
//        }
//        return Collections.unmodifiableSet(result);
//    }
//
//    /**
//     * Busca coincidencia entre el nombre del directorio en disco y el Enum
//     */
//    private HMICategoryDefinition findCategoryByName(String folderName) {
//        for (HMICategoryDefinition cat : HMICategoryDefinition.values()) {
//            // Compara ignorando mayúsculas/minúsculas con el DisplayName o con cat.name()
//            if (cat.getDisplayName().equalsIgnoreCase(folderName) || cat.name().equalsIgnoreCase(folderName)) {
//                return cat;
//            }
//        }
//        return null;
//    }

}
