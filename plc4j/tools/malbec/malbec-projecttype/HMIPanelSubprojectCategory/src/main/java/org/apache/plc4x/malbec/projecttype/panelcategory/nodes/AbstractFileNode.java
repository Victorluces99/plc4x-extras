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
package org.apache.plc4x.malbec.projecttype.panelcategory.nodes;

import java.awt.Component;
import java.awt.Image;
import java.awt.datatransfer.Transferable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import javax.swing.Action;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.Node.PropertySet;
import org.openide.util.HelpCtx;
import org.openide.util.WeakListeners;
import org.openide.util.datatransfer.NewType;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;
import org.openide.util.lookup.ProxyLookup;

/**
 * Node for a file or folder backed by a {@link DataObject}.
 *
 * <p>Unlike {@link org.openide.loaders.DataNode}, this node never calls
 * {@code fireNodeDestroyed()} on itself when the underlying {@link DataObject}
 * becomes invalid. Such an early notification leaves the explorer with a row
 * whose node has already been emptied (the "ghost row" seen after deleting a
 * display or an icon). Here {@link #destroy()} only deletes the data object,
 * so the owning {@code NodeList} can remove the key and let
 * {@link Children.Keys} notify the explorer in the proper order.</p>
 *
 * <p>Everything else (name, icon, actions, sheet, clipboard and paste
 * behaviour) is delegated to the standard node delegate of the
 * {@link DataObject}.</p>
 */
abstract class AbstractFileNode extends AbstractNode {

    private final DataObject dataObject;
    private final FileObject fileObject;
    private final Node delegate;
    private final PropertyChangeListener delegateListener = this::delegatePropertyChange;
    private boolean ownIcon;

    AbstractFileNode(DataObject dataObject, FileObject fileObject, Children children) {
        this(dataObject, fileObject, children, new InstanceContent());
    }

    private AbstractFileNode(DataObject dataObject, FileObject fileObject, Children children,
            InstanceContent content) {
        super(children, new ProxyLookup(new AbstractLookup(content),
                dataObject.getNodeDelegate().getLookup()));
        this.dataObject = dataObject;
        this.fileObject = fileObject;
        this.delegate = dataObject.getNodeDelegate();
        content.add(this);
        delegate.addPropertyChangeListener(WeakListeners.propertyChange(delegateListener, delegate));
    }

    protected final DataObject getDataObject() {
        return dataObject;
    }

    protected final FileObject getFileObject() {
        return fileObject;
    }

    protected final Node getDelegate() {
        return delegate;
    }

    /**
     * Uses the given icon base instead of the icon provided by the data
     * object. An empty or {@code null} base keeps the data object icon.
     */
    protected final void setOwnIconBase(String iconBase) {
        if (iconBase != null && !iconBase.isEmpty()) {
            setIconBaseWithExtension(iconBase);
            ownIcon = true;
        }
    }

    private void delegatePropertyChange(PropertyChangeEvent event) {
        String name = event.getPropertyName();
        if (Node.PROP_NAME.equals(name)) {
            fireNameChange(null, null);
        } else if (Node.PROP_DISPLAY_NAME.equals(name)) {
            fireDisplayNameChange(null, null);
        } else if (Node.PROP_SHORT_DESCRIPTION.equals(name)) {
            fireShortDescriptionChange(null, null);
        } else if (Node.PROP_ICON.equals(name)) {
            fireIconChange();
        } else if (Node.PROP_OPENED_ICON.equals(name)) {
            fireOpenedIconChange();
        } else if (Node.PROP_PROPERTY_SETS.equals(name)) {
            firePropertySetsChange(null, null);
        } else if (Node.PROP_COOKIE.equals(name)) {
            fireCookieChange();
        }
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getDisplayName() {
        return delegate.getDisplayName();
    }

    @Override
    public String getShortDescription() {
        return delegate.getShortDescription();
    }

    @Override
    public String getHtmlDisplayName() {
        return delegate.getHtmlDisplayName();
    }

    @Override
    public Image getIcon(int type) {
        return ownIcon ? super.getIcon(type) : delegate.getIcon(type);
    }

    @Override
    public Image getOpenedIcon(int type) {
        return ownIcon ? super.getOpenedIcon(type) : delegate.getOpenedIcon(type);
    }

    @Override
    public Action[] getActions(boolean context) {
        return delegate.getActions(context);
    }

    @Override
    public Action getPreferredAction() {
        return delegate.getPreferredAction();
    }

    @Override
    public PropertySet[] getPropertySets() {
        return delegate.getPropertySets();
    }

    @Override
    public NewType[] getNewTypes() {
        return delegate.getNewTypes();
    }

    @Override
    public HelpCtx getHelpCtx() {
        return delegate.getHelpCtx();
    }

    @Override
    public boolean hasCustomizer() {
        return delegate.hasCustomizer();
    }

    @Override
    public Component getCustomizer() {
        return delegate.getCustomizer();
    }

    @Override
    public boolean canCopy() {
        return delegate.canCopy();
    }

    @Override
    public boolean canCut() {
        return delegate.canCut();
    }

    @Override
    public boolean canRename() {
        return delegate.canRename();
    }

    @Override
    public boolean canDestroy() {
        return delegate.canDestroy();
    }

    @Override
    public void setName(String name) {
        delegate.setName(name);
    }

    @Override
    public Transferable clipboardCopy() throws IOException {
        return delegate.clipboardCopy();
    }

    @Override
    public Transferable clipboardCut() throws IOException {
        return delegate.clipboardCut();
    }

    @Override
    public Transferable drag() throws IOException {
        return delegate.drag();
    }

    @Override
    protected void createPasteTypes(Transferable transferable, List<PasteType> pasteTypes) {
        pasteTypes.addAll(Arrays.asList(delegate.getPasteTypes(transferable)));
    }

    @Override
    public PasteType getDropType(Transferable transferable, int action, int index) {
        return delegate.getDropType(transferable, action, index);
    }

    @Override
    public Node.Handle getHandle() {
        return delegate.getHandle();
    }

    @Override
    public void destroy() throws IOException {
        if (dataObject.isDeleteAllowed()) {
            dataObject.delete();
        }
    }
}
