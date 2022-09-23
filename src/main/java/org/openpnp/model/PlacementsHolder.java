/*
 * Copyright (C) 2022 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.model;

import java.beans.IndexedPropertyChangeEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import org.apache.commons.beanutils.BeanUtils;
import org.openpnp.spi.Definable;
import org.openpnp.util.IdentifiableList;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;

/**
 * A PlacementsHolder is an abstraction of an object that has physical 2D extent and contains
 * Placements
 */
public abstract class PlacementsHolder<T extends PlacementsHolder<T>> 
        extends AbstractModelObject implements Definable<T>, PropertyChangeListener {
    
    /**
     * The name of this PlacementsHolder
     */
    @Attribute(required = false)
    protected String name;
    
    /**
     * The physical extent of this PlacementsHolder
     */
    @Element(required = false)
    protected Location dimensions = new Location(LengthUnit.Millimeters);

    /**
     * The list of Placements contained by this PlacementsHolder
     */
    @ElementList(required = false)
    protected IdentifiableList<Placement> placements = new IdentifiableList<>();

    protected transient T definition;
    protected transient File file;
    protected transient boolean dirty;

    @Commit
    protected void commit() {
        for (Placement placement : placements) {
            placement.addPropertyChangeListener(this);
        }
    }
    
    /**
     * Constructs a new PlacementsHolder
     */
    @SuppressWarnings("unchecked")
    public PlacementsHolder() {
        definition = (T) this;
        addPropertyChangeListener(this);
    }
    
    /**
     * Constructs a deep copy of the specified PlacementsHolder
     * @param holderToCopy
     */
    public PlacementsHolder(PlacementsHolder<T> holderToCopy) {
        name = holderToCopy.name;
        dimensions = holderToCopy.dimensions;
        placements = new IdentifiableList<>();
        for (Placement placement : holderToCopy.placements) {
            placements.add(new Placement(placement));
        }
        file = holderToCopy.file;
        dirty = holderToCopy.dirty;
        setDefinition(holderToCopy.definition);
        addPropertyChangeListener(this);
    }
    
    /**
     * Cleans-up property change listeners associated with this PlacementsHolder
     */
    @Override
    public void dispose() {
        removePropertyChangeListener(this);
        for (Placement placement : placements) {
            placement.dispose();
        }
        setDefinition(null);
        super.dispose();
    }
    
    /**
     * 
     * @return the name of this PlacementsHolder
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of this PlacementsHolder
     * @param name - the name to set
     */
    public void setName(String name) {
        Object oldValue = this.name;
        this.name = name;
        firePropertyChange("name", oldValue, name);
    }

    /**
     * 
     * @return the dimensions (physical extent) of this PlacementsHolder (contained in the X and Y 
     * fields)
     */
    public Location getDimensions() {
        return dimensions;
    }

    /**
     * Sets the dimensions (physical extent) of this PlacementsHolder (contained in the X and Y 
     * fields)
     * @param dimensions
     */
    public void setDimensions(Location dimensions) {
        Location oldValue = this.dimensions;
        this.dimensions = dimensions.derive(null, null, 0.0, 0.0);
        firePropertyChange("dimensions", oldValue, dimensions);
    }

    /**
     * 
     * @return a list of placements contained by this PlacementsHolder
     */
    public IdentifiableList<Placement> getPlacements() {
        return new IdentifiableList<>(placements);
    }

    /**
     * Sets the list of placements contained by this PlacementsHolder
     * @param placements - the list of placements to set
     */
    public void setPlacements(IdentifiableList<Placement> placements) {
        this.placements = placements;
    }

    /**
     * Sets the placement at the specified index
     * @param index - the index of the placement to set
     * @param placement - the placement to set
     */
    public void setPlacements(int index, Placement placement) {
        placements.add(index, placement);
    }

    /**
     * Adds a placement to the list of placements
     * @param placement - the placement to add
     */
    public void addPlacement(Placement placement) {
        if (placement != null) {
            placements.add(placement);
            fireIndexedPropertyChange("placements", placements.indexOf(placement), null, placement);
            placement.addPropertyChangeListener(this);
            placement.getDefinition().addPropertyChangeListener(placement);
        }
        
    }
    
    /**
     * Removes the specified placement from the list of placements
     * @param placement - the placement to remove
     */
    public void removePlacement(Placement placement) {
        if (placement != null) {
            int index = placements.indexOf(placement);
            if (index >= 0) {
                placements.remove(placement);
                fireIndexedPropertyChange("placements", index, placement, null);
                placement.dispose();
            }
        }
    }
    
    /**
     * Removes the placement at the specified index 
     * @param index - the index of the placement to remove
     */
    public void removePlacement(int index) {
        if (index >= 0 && index < placements.size()) {
            Placement placement = placements.get(index);
            placements.remove(index);
            fireIndexedPropertyChange("placements", index, placement, null);
            placement.dispose();
        }
    }
    
    /**
     * Removes all placements
     */
    public void removeAllPlacements() {
        Object oldValue = new IdentifiableList<>(placements);
        for (Placement placement : placements) {
            placement.dispose();
        }
        placements = new IdentifiableList<>();
        firePropertyChange("placements", oldValue, placements);
    }
    
    /**
     * Gets the file where this PlacementsHolder is stored
     * @return
     */
    public File getFile() {
        return file;
    }

    /**
     * Sets the file where this PlacementsHolder will be stored
     * @param file
     */
    public void setFile(File file) {
        Object oldValue = this.file;
        this.file = file;
        firePropertyChange("file", oldValue, file);
    }

    /**
     * @return true if this PlacementsHolder has been modified
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Sets the state of the dirty flag (used to indicate that this PlacementsHolder has been 
     * modified) to the specified value
     * @param dirty - the state to set the flag
     */
    public void setDirty(boolean dirty) {
        boolean oldValue = this.dirty;
        this.dirty = dirty;
        firePropertyChange("dirty", oldValue, dirty);
    }

    @Override
    public T getDefinition() {
        return definition;
    }

    @Override
    public void setDefinition(T definedBy) {
        PlacementsHolder<T> oldValue = this.definition;
        this.definition = definedBy;
        firePropertyChange("definedBy", oldValue, definedBy);
        if (oldValue != null) {
            oldValue.removePropertyChangeListener(this);
        }
        if (definedBy != null) {
            definedBy.addPropertyChangeListener(this);
        }
    }

    @Override
    public boolean isDefinedBy(Object definedBy) {
        return this.definition == definedBy;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
//        Logger.trace(String.format("PropertyChangeEvent handled by AbstractBoard @%08x = %s", this.hashCode(), evt));
        if (evt.getSource() != PlacementsHolder.this || !evt.getPropertyName().equals("dirty")) {
            dirty = true;
            if (PlacementsHolder.this != definition && evt.getSource() == definition) {
                Logger.trace(String.format("Attempting to set %s @%08x property %s = %s", this.getClass().getSimpleName(), this.hashCode(), evt.getPropertyName(), evt.getNewValue()));
                if (evt instanceof IndexedPropertyChangeEvent) {
                    if (evt.getPropertyName() == "placements") {
                        int index = ((IndexedPropertyChangeEvent) evt).getIndex();
                        if (evt.getNewValue() instanceof Placement) {
                            addPlacement(new Placement((Placement) evt.getNewValue()));
                        }
                        else if (evt.getOldValue() instanceof Placement) {
                            removePlacement(index);
                            ((Placement) evt.getOldValue()).dispose();
                        }
                    }
                }
                else {
                    try {
                        BeanUtils.setProperty(this, evt.getPropertyName(), evt.getNewValue());
                    }
                    catch (IllegalAccessException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    catch (InvocationTargetException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
        
    }

}
