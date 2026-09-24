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

import javax.swing.*;
import java.awt.*;

public class SiemensPanelBuilder implements DeviceDynamicPanelBuilder {
    private JPanel panel;
    private JTextField txtIp;
    private JTextField txtRackSlot;

    public SiemensPanelBuilder() {
        this.panel = new JPanel(new GridBagLayout());
        this.panel.setBorder(BorderFactory.createTitledBorder("Configuración Específica Siemens"));
    }

    @Override
    public void buildParametersUI() {
        txtIp = new JTextField("192.168.0.1");
        txtRackSlot = new JTextField("0/1");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Fila 1
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.3; gbc.weighty = 0.0;
        panel.add(new JLabel("IP Address:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(txtIp, gbc);

        // Fila 2
        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0.3;
        panel.add(new JLabel("Rack / Slot:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(txtRackSlot, gbc);

        // Empujar hacia arriba
        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);
    }

    @Override
    public JPanel getPanel() {
        return panel;
    }

    @Override
    public String getSpecificParametersAsString() {
        return "IP=" + txtIp.getText() + "\nRackSlot=" + txtRackSlot.getText();
    }
}