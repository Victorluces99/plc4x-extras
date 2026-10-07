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
       
    }

    @Override
    public void removeChangeListener(ChangeListener cl) {

    }

    private Set loadProjects(FileObject dir) {
        Set<Project> subProjects = new HashSet<>();

        for (FileObject child : project.getProjectDirectory().getChildren()) {
            if (child.isFolder() && child.getFileObject(CATEGORY_SUBPROJECT_FILE) != null) {

                try {
                    Project subProject = ProjectManager.getDefault().findProject(child);

                    if (subProject != null) {
                        subProjects.add(subProject);
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

}
