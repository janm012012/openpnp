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

package org.openpnp.gui.tablemodel;

import java.util.List;
import java.util.Locale;

import javax.swing.table.AbstractTableModel;

import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.model.Board.Side;
import org.pmw.tinylog.Logger;
import org.openpnp.model.Configuration;
import org.openpnp.model.FiducialLocatableLocation;
import org.openpnp.model.Job;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.PanelLocation;

@SuppressWarnings("serial")
public class FiducialLocatableLocationsTableModel extends AbstractTableModel {
    private final Configuration configuration;

    private String[] columnNames = new String[] {"Panel/Board", "Width", "Length", "Side", "X", "Y", "Z",
            "Rot.", "Enabled?", "Check Fids?"};

    @SuppressWarnings("rawtypes")
    private Class[] columnTypes = new Class[] {String.class, LengthCellValue.class,
            LengthCellValue.class, Side.class, LengthCellValue.class, LengthCellValue.class,
            LengthCellValue.class, String.class, Boolean.class, Boolean.class};

//    private Job job;

    private PanelLocation rootPanelLocation;

    private List<FiducialLocatableLocation> fiducialLocatableLocations;

    public FiducialLocatableLocationsTableModel(Configuration configuration) {
        this.configuration = configuration;
//        this.rootPanelLocation = rootPanelLocation;
//        this.fiducialLocatableLocations = fiducialLocatableLocations;
    }

//    public void setJob(Job job) {
//        this.job = job;
//        fireTableDataChanged();
//    }
//
//    public Job getJob() {
//        return job;
//    }

    public PanelLocation getRootPanelLocation() {
        return rootPanelLocation;
    }

    public void setRootPanelLocation(PanelLocation rootPanelLocation) {
        this.rootPanelLocation = rootPanelLocation;
        fireTableDataChanged();
    }

    public List<FiducialLocatableLocation> getFiducialLocatableLocations() {
        return fiducialLocatableLocations;
    }

    public void setFiducialLocatableLocations(List<FiducialLocatableLocation> fiducialLocatableLocations) {
        this.fiducialLocatableLocations = fiducialLocatableLocations;
        fireTableDataChanged();
    }

    public FiducialLocatableLocation getFiducialLocatableLocation(int index) {
        return fiducialLocatableLocations.get(index);
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        if (fiducialLocatableLocations == null) {
            return 0;
        }
        return fiducialLocatableLocations.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return false;
        }
        if ((fiducialLocatableLocations.get(rowIndex).getParent() == rootPanelLocation) ||
                (columnIndex == 8) || (columnIndex == 9)) {
            return true;
        }
        return false;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            FiducialLocatableLocation fiducialLocatableLocation = fiducialLocatableLocations.get(rowIndex);
            if (columnIndex == 0) {
                fiducialLocatableLocation.getFiducialLocatable().setName((String) aValue);
            }
            else if (columnIndex == 1) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location location = fiducialLocatableLocation.getFiducialLocatable().getDimensions();
                location = Length.setLocationField(configuration, location, length, Length.Field.X);
                fiducialLocatableLocation.getFiducialLocatable().setDimensions(location);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 2) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location location = fiducialLocatableLocation.getFiducialLocatable().getDimensions();
                location = Length.setLocationField(configuration, location, length, Length.Field.Y);
                fiducialLocatableLocation.getFiducialLocatable().setDimensions(location);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 3) {
                Side oldSide = fiducialLocatableLocation.getSide();
                Side newSide = (Side) aValue;
                if (newSide != oldSide) {
                    Location savedLocation = fiducialLocatableLocation.getGlobalLocation();
                    fiducialLocatableLocation.setSide(newSide);
                    if (fiducialLocatableLocation.getParent() == rootPanelLocation) {
                        fiducialLocatableLocation.setGlobalLocation(savedLocation);
                    }
                    fiducialLocatableLocation.setLocalToParentTransform(null);
                }
                fireTableDataChanged();
            }
            else if (columnIndex == 4) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location location = fiducialLocatableLocation.getGlobalLocation();
                location = Length.setLocationField(configuration, location, length, Length.Field.X);
                fiducialLocatableLocation.setGlobalLocation(location);
                fireTableDataChanged();
            }
            else if (columnIndex == 5) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location location = fiducialLocatableLocation.getGlobalLocation();
                location = Length.setLocationField(configuration, location, length, Length.Field.Y);
                fiducialLocatableLocation.setGlobalLocation(location);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
            else if (columnIndex == 6) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location location = fiducialLocatableLocation.getGlobalLocation();
                location = Length.setLocationField(configuration, location, length, Length.Field.Z);
                fiducialLocatableLocation.setGlobalLocation(location);
                fireTableDataChanged();
            }
            else if (columnIndex == 7) {
                fiducialLocatableLocation.setGlobalLocation(fiducialLocatableLocation.getGlobalLocation().derive(null, null, null,
                        Double.parseDouble(aValue.toString())));
                fireTableDataChanged();
            }
            else if (columnIndex == 8) {
                fiducialLocatableLocation.setLocallyEnabled((Boolean) aValue);
                fireTableDataChanged();
            }
            else if (columnIndex == 9) {
                fiducialLocatableLocation.setCheckFiducials((Boolean) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    public Object getValueAt(int row, int col) {
        FiducialLocatableLocation fiducialLocatableLocation = fiducialLocatableLocations.get(row);
        Location loc = fiducialLocatableLocation.getGlobalLocation();
        Location dim = fiducialLocatableLocation.getFiducialLocatable().getDimensions();
        switch (col) {
            case 0:
                return fiducialLocatableLocation.getFiducialLocatable().getName();
            case 1:
                return new LengthCellValue(dim.getLengthX());
            case 2:
                return new LengthCellValue(dim.getLengthY());
            case 3:
                return fiducialLocatableLocation.getSide();
            case 4:
                return new LengthCellValue(loc.getLengthX());
            case 5:
                return new LengthCellValue(loc.getLengthY());
            case 6:
                return new LengthCellValue(loc.getLengthZ());
            case 7:
                return String.format(Locale.US, configuration.getLengthDisplayFormat(),
                        loc.getRotation(), "");
            case 8:
                return fiducialLocatableLocation.isEnabled();
            case 9:
                return fiducialLocatableLocation.isCheckFiducials();
            default:
                return null;
        }
    }
}
