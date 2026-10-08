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
    IMAGE("Image", "org/apache/plc4x/malbec/projecttype/hmipanelsubprojectcategory/icon2.png"),
    COMUNICATION("Comunicacion", "org/apache/plc4x/malbec/projecttype/hmipanelsubprojectcategory/comm.png"),
    NOTICE_MANAGEMENT("Gestion de Avisos", "org/apache/plc4x/malbec/projecttype/hmipanelsubprojectcategory/icon2.png"),
    RECIPE("Recetas", "org/apache/plc4x/malbec/projecttype/hmipanelsubprojectcategory/icon2.png"),
    HISTORIAL("Historial", "org/apache/plc4x/malbec/projecttype/hmipanelsubprojectcategory/icon2.png"),
    SCRIPTS("Scripts", "org/apache/plc4x/malbec/projecttype/hmipanelsubprojectcategory/icon2.png"),
    REPORT("Informes", "org/apache/plc4x/malbec/projecttype/hmipanelsubprojectcategory/icon2.png"),
    TEXT_GRAPHIC("Texto y Lista de graficos", "org/apache/plc4x/malbec/projecttype/hmipanelsubprojectcategory/icon2.png"),
    ADMIN_USER("Administracion de Usuarios runtime", "org/apache/plc4x/malbec/projecttype/hmipanelsubprojectcategory/icon2.png"),
    CONFIG_PANEL("Configuracion de panel de operador", "org/apache/plc4x/malbec/projecttype/hmipanelsubprojectcategory/icon2.png");

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
