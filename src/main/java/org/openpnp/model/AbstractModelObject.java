/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
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
import java.beans.PropertyChangeListenerProxy;
import java.beans.PropertyChangeSupport;

import org.pmw.tinylog.Logger;

public abstract class AbstractModelObject {
    protected final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }
    
    /**
     * Removes all property change listeners from this object and logs a warning message for each
     */
    public void dispose() {
       for (PropertyChangeListener listener : propertyChangeSupport.getPropertyChangeListeners()) {
           if (listener instanceof PropertyChangeListenerProxy) {
               PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listener;
               Logger.warn(String.format("During disposal of %s @%08x - removed listener: %s @%08x", this.getClass().getSimpleName(), this.hashCode(), proxy.getListener(), proxy.getListener().hashCode()));
           }
           else {
               Logger.warn(String.format("During disposal of %s @%08x - removed listener: %s @%08x", this.getClass().getSimpleName(), this.hashCode(), listener, listener.hashCode()));
           }
           propertyChangeSupport.removePropertyChangeListener(listener);
        }
    }
    
    /**
     * Dumps all registered listeners to the log file
     */
    public void dumpListeners() {
        Logger.trace(String.format("Dump of %s @%08x listeners:", this.getClass().getSimpleName(), this.hashCode()));
        for (PropertyChangeListener listener : propertyChangeSupport.getPropertyChangeListeners()) {
            if (listener instanceof PropertyChangeListenerProxy) {
                PropertyChangeListenerProxy proxy = (PropertyChangeListenerProxy) listener;
                Logger.trace(String.format("    +-- prop:%s %s @%08x", proxy.getPropertyName(), proxy.getListener(), proxy.getListener().hashCode()));
            }
            else {
                Logger.trace(String.format("    +-- %s @%08x", listener, listener.hashCode()));
            }
        }        
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
    
    protected void fireIndexedPropertyChange(String propertyName, int index, Object oldValue, Object newValue) {
        propertyChangeSupport.fireIndexedPropertyChange(propertyName, index, oldValue, newValue);
    }
}
