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
package org.apache.plc4x.malbec.projecttype.panelcategory;


public enum HMICategoryDefinition {
  IMAGE("Images", "com/prueba/hmipanelsubproject/category/FolderBlue.png"),
    COMUNICATION("Communication", "com/prueba/hmipanelsubproject/category/FolderBlue.png"),
    NOTICE_MANAGEMENT("Notification Management", "com/prueba/hmipanelsubproject/category/FolderBlue.png"),
    RECIPE("Recipes", "com/prueba/hmipanelsubproject/category/FolderBlue.png"),
    HISTORIAL("Historial", "com/prueba/hmipanelsubproject/category/FolderBlue.png"),
    SCRIPTS("Scripts", "com/prueba/hmipanelsubproject/category/FolderBlue.png"),
    REPORT("Reports", "com/prueba/hmipanelsubproject/category/FolderBlue.png"),
    TEXT_GRAPHIC("Text and List of Charts", "com/prueba/hmipanelsubproject/category/FolderBlue.png"),
    ADMIN_USER("Runtime User Management", "com/prueba/hmipanelsubproject/category/FolderBlue.png"),
    CONFIG_PANEL("Operator Panel Configuration", "com/prueba/hmipanelsubproject/category/FolderBlue.png");

    private final String displayName;
    private final String iconPath;

    HMICategoryDefinition(String displayName, String iconPath) {
        this.displayName = displayName;
        this.iconPath = iconPath;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconPath() {
        return iconPath;
    }

    public class HMIUtil {

    }
}
