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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.beanutils.BeanUtils;
import org.openpnp.spi.Definable;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

/**
 * A class to represent an abstraction of a physical 2D object that has a position and orientation
 * in 3D space
 *
 * @param <T> - the type of the object that extends AbstractLocatable
 */
public abstract class AbstractLocatable<T extends AbstractLocatable<T>> extends AbstractModelObject
        implements Definable<T>, Identifiable, PropertyChangeListener {

    /**
     * An enumeration to identify how the 2D object's surface is oriented with respect to the 3D
     * space's +Z direction 
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

    @Attribute
    protected Side side = Side.Top;
    
    @Element
    protected Location location;

    @Attribute(required = false)
    protected String id;
    
    protected transient T definedBy;

    protected transient boolean dirty;

    @SuppressWarnings("unchecked")
    AbstractLocatable() {
        super();
        definedBy = (T) this;
        addPropertyChangeListener(this);
    }
    
    AbstractLocatable(AbstractLocatable<T> abstractLocatable) {
        super();
        location = abstractLocatable.location;
        setDefinedBy(abstractLocatable.getDefinedBy());
        id = abstractLocatable.id;
        addPropertyChangeListener(this);
    }
    
    AbstractLocatable(Location location) {
        this();
        this.location = location;
    }
    
    @Override
    public void dispose() {
        removePropertyChangeListener(this);
        setDefinedBy(null);
        super.dispose();
    }
    

    /**
     * @return the side
     */
    public Side getSide() {
        return side;
    }

    /**
     * Sets the side
     * @param side - the side to set
     */
    public void setSide(Side side) {
        Object oldValue = this.side;
        this.side = side;
        firePropertyChange("side", oldValue, side);
    }
    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        Location oldValue = this.location;
        this.location = location;
        firePropertyChange("location", oldValue, location);
    }

    public T getDefinedBy() {
        return definedBy;
    }
    
    public void setDefinedBy(T definedBy) {
        T oldValue = this.definedBy;
        this.definedBy = definedBy;
        firePropertyChange("definedBy", oldValue, definedBy);
        if (oldValue != null) {
            oldValue.removePropertyChangeListener(this);
        }
        if (definedBy != null && definedBy != this) {
            definedBy.addPropertyChangeListener(this);
        }
    }
    
    public boolean isDefinedBy(Object definedBy) {
        return this.definedBy == definedBy;
    };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        String oldValue = this.id;
        this.id = id;
        firePropertyChange("id", oldValue, id);
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        boolean oldValue = this.dirty;
        this.dirty = dirty;
        firePropertyChange("dirty", oldValue, dirty);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() != AbstractLocatable.this || !evt.getPropertyName().equals("dirty")) {
            dirty = true;
            if (AbstractLocatable.this != definedBy && evt.getSource() == definedBy) {
                try {
                    Logger.trace(String.format("Setting %s %s @%08x property %s = %s", 
                            this.getClass().getSimpleName(), this.getId(), this.hashCode(), 
                            evt.getPropertyName(), evt.getNewValue()));
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
