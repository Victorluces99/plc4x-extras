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
package org.apache.plc4x.malbec.hmi.impl;

import java.awt.Image;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Action;
import org.apache.plc4x.malbec.hmi.action.CreateProjectFolders;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.netbeans.spi.project.ui.support.NodeFactorySupport;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.Node.Property;
import org.openide.nodes.Node.PropertySet;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;


public class Plc4xHMISubProjectLogicalViewProviderImpl implements  LogicalViewProvider {

    @StaticResource()
    public static final String HMI_SUBPROJECT_ICON = "org/apache/plc4x/malbec/hmi/nodes/PanelOperador.png";
    
    private final Project project;

    public Plc4xHMISubProjectLogicalViewProviderImpl(Project project) {
        this.project = project;
    }
    
    @Override
    public Node createLogicalView() {
        try {
            //Obtain the project directory's node:
            FileObject projectDirectory = project.getProjectDirectory();
            DataFolder projectFolder = DataFolder.findFolder(projectDirectory);
            Node nodeOfProjectFolder = projectFolder.getNodeDelegate();
            //Decorate the project directory's node:
            return new ProjectNode(nodeOfProjectFolder, project);
        } catch (DataObjectNotFoundException donfe) {
            Exceptions.printStackTrace(donfe);
            //Fallback-the directory couldn't be created -
            //read-only filesystem or something evil happened
            return new AbstractNode(Children.LEAF);
        }
    }

    @Override
    public Node findPath(Node node, Object o) {
        //
        return null;
    }
    
    private final class ProjectNode extends FilterNode {

        final Project project;

        public ProjectNode(Node node, Project project)
            throws DataObjectNotFoundException {
            super(node,
                    NodeFactorySupport.createCompositeChildren(project, "Projects/org-plc4x-hmi-project/Nodes"),
                    new ProxyLookup(
                    new Lookup[]{
                        Lookups.singleton(project),
                        node.getLookup()
                    }));
            this.project = project;
        }

        @Override
        public Action[] getActions(boolean arg0) {
            return new Action[]{
                        new CreateProjectFolders(project.getProjectDirectory()),
                        CommonProjectActions.newFileAction(),
                        CommonProjectActions.copyProjectAction(),
                        CommonProjectActions.deleteProjectAction(),
                        CommonProjectActions.closeProjectAction()
                    };
        }

        @Override
        public boolean canRename() {
            return false;
        }

        @Override
        public void setName(String name) {
            // The HMI node must not be renamed.
        }

        @Override
        public PropertySet[] getPropertySets() {
            PropertySet[] propertySets = super.getPropertySets();
            List<PropertySet> filteredPropertySets = new ArrayList<>(propertySets.length);
            for (PropertySet propertySet : propertySets) {
                filteredPropertySets.add(new PropertySet(
                        propertySet.getName(),
                        propertySet.getDisplayName(),
                        propertySet.getShortDescription()) {
                    @Override
                    public Property<?>[] getProperties() {
                        List<Property<?>> properties = new ArrayList<>();
                        for (Property<?> property : propertySet.getProperties()) {
                            if (!DataObject.PROP_NAME.equals(property.getName())) {
                                properties.add(property);
                            }
                        }
                        return properties.toArray(new Property<?>[0]);
                    }
                });
            }
            return filteredPropertySets.toArray(new PropertySet[0]);
        }

        @Override
        public Image getIcon(int type) {
            return ImageUtilities.loadImage(HMI_SUBPROJECT_ICON );
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        @Override
        public String getDisplayName() {
            return project.getProjectDirectory().getName();
        }

    }

   
    
}
