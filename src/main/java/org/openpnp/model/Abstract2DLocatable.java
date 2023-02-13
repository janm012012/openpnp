/*
 * Copyright (C) 2023 Jason von Nieda <jason@vonnieda.org>, Tony Luken <tonyluken62+openpnp@gmail.com>
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

import java.beans.PropertyChangeListener;
import org.openpnp.spi.Definable;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * A class to represent an abstraction of a physical 2D object that has a position and orientation
 * in 3D space
 *
 * @param <T> - the type of the object that extends Abstract2DLocatable
 */
public abstract class Abstract2DLocatable<T extends Abstract2DLocatable<T>> extends AbstractModelObject
        implements Definable<T>, Identifiable, PropertyChangeListener {

    /**
     * An enumeration to identify how the 2D object's surface is oriented with respect to the local
     * 3D space's +Z direction 
     */
    public enum Side {
        Bottom, Top;
        
        public Side flip() {
            if (this.equals(Side.Top)) {
                return Side.Bottom;
            }
            else {
                return Side.Top;
            }
        }
        
        public Side flip(boolean value) {
            if (value) {
                return flip();
            }
            else {
                return this;
            }
        }
    }

    /**
     * Indicates which surface of the object is facing in the direction of its local 3D coordinate
     * system's +Z axis
     */
    @Attribute
    protected Side side = Side.Top;
    
    /**
     * The position and orientation in the local 3D coordinate system
     */
    @Element
    protected Location location;

    /**
     * An identifier for this Abstract2DLocatable
     */
    @Attribute(required = false)
    protected String id;
    
    /**
     * The definition of this Abstract2DLocatable
     */
    protected transient T definition;

    /**
     * A flag to indicate that this Abstract2DLocatable has been modified
     */
    protected transient boolean dirty;

    /**
     * Default Abstract2DLocatable constructor
     */
    @SuppressWarnings("unchecked")
    Abstract2DLocatable() {
        super();
        setDefinition((T) this);
    }
    
    /**
     * Constructs a deep copy of the specified Abstract2DLocatable
     * @param abstract2DLocatableToCopy
     */
    Abstract2DLocatable(Abstract2DLocatable<T> abstract2DLocatableToCopy) {
        super();
        location = abstract2DLocatableToCopy.location;
        setDefinition(abstract2DLocatableToCopy.getDefinition());
        id = abstract2DLocatableToCopy.id;
        addPropertyChangeListener(this);
    }
    
    /**
     * Constructs an Abstract2DLocatable whose position and orientation is given by the specified
     * location
     * @param location - the position and orientation of the new Abstract2DLocatable
     */
    Abstract2DLocatable(Location location) {
        this();
        this.location = location;
    }
    
    /**
     * Cleans-up any property change listeners associated with this Abstract2DLocatable
     */
    @Override
    public void dispose() {
        removePropertyChangeListener(this);
        if (this != definition) {
            definition.removePropertyChangeListener(this);
        }
        super.dispose();
    }
    

    /**
     * @return the side of this Abstract2DLocatable that is facing in the direction of its local 3D
     * coordinate system's +Z axis
     */
    public Side getSide() {
        return side;
    }

    /**
     * Sets the side of this Abstract2DLocatable that is facing in the direction of its local 3D
     * coordinate system's +Z axis
     * @param side - the side to set
     */
    public void setSide(Side side) {
        Object oldValue = this.side;
        this.side = side;
        firePropertyChange("side", oldValue, side); //$NON-NLS-1$
    }
    
    /**
     * 
     * @return the position and orientation of this Abstract2DLocatable in its local 3D coordinate
     * system
     */
    public Location getLocation() {
        return location;
    }

    /**
     * Sets the position and orientation of this Abstract2DLocatable in its local 3D coordinate
     * system
     * @param location - the new position and orientation
     */
    public void setLocation(Location location) {
        Location oldValue = this.location;
        this.location = location;
        firePropertyChange("location", oldValue, location); //$NON-NLS-1$
    }

    /**
     * @return the identifier of this Abstract2DLocatable
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the identifier of this Abstract2DLocatable
     * @param id - the new identifier
     */
    public void setId(String id) {
        String oldValue = this.id;
        this.id = id;
        firePropertyChange("id", oldValue, id); //$NON-NLS-1$
    }

    public T getDefinition() {
        return definition;
    }
    
    public void setDefinition(T definition) {
        T oldValue = this.definition;
        this.definition = definition;
        firePropertyChange("definition", oldValue, definition); //$NON-NLS-1$
        if (oldValue != null) {
            oldValue.removePropertyChangeListener(this);
        }
        if (definition != null) {
            definition.addPropertyChangeListener(this);
        }
    }
    
    public boolean isDefinition(Object potentialDefinition) {
        return this.definition == potentialDefinition;
    };

    /**
     * 
     * @return true if this Abstract2DLocatable has been modified
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * Sets the state of the dirty flag
     * @param dirty - the new state
     */
    public void setDirty(boolean dirty) {
        boolean oldValue = this.dirty;
        this.dirty = dirty;
        firePropertyChange("dirty", oldValue, dirty); //$NON-NLS-1$
    }
}
