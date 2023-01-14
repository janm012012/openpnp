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

package org.openpnp.gui.tablemodel;

import java.util.List;
import java.util.function.Supplier;

import org.openpnp.Translations;
import org.openpnp.events.PlacementsHolderChangedEvent;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.model.Board;
import org.openpnp.model.Configuration;
import org.openpnp.model.PlacementsHolder;
import org.openpnp.model.Length;
import org.openpnp.model.Location;
import org.openpnp.model.Panel;

@SuppressWarnings("serial")
public class PlacementsHolderTableModel extends AbstractObjectTableModel implements ColumnAlignable, ColumnWidthSaveable {
    private final Configuration configuration;

    private String[] columnNames = new String[] {"PlacementsHolder Name", //$NON-NLS-1$
            Translations.getString("PlacementsHolderTableModel.ColumnName.Width"), //$NON-NLS-1$
            Translations.getString("PlacementsHolderTableModel.ColumnName.Length")}; //$NON-NLS-1$

    @SuppressWarnings("rawtypes")
    private Class[] columnTypes = new Class[] {String.class, LengthCellValue.class,
            LengthCellValue.class};
    
    private int[] columnAlignments = new int[] {LEFT, CENTER, CENTER};
    
    private int[] columnWidthTypes = new int[] {PROPORTIONAL, FIXED, FIXED};

    private Supplier<List<? extends PlacementsHolder<?>>> placementsHolders;

    private Class<? extends PlacementsHolder<?>> classType;

    public PlacementsHolderTableModel(Configuration configuration, 
            Supplier<List<? extends PlacementsHolder<?>>> placementsHolders, 
            Class<? extends PlacementsHolder<?>> classType) {
        this.configuration = configuration;
        this.placementsHolders = placementsHolders;
        this.classType = classType;
    }

    @Override
    public String getColumnName(int column) {
        if (column == 0) {
            if (classType == Board.class) {
                return Translations.getString("PlacementsHolderTableModel.ColumnName.BoardName"); //$NON-NLS-1$
            }
            else if (classType == Panel.class) {
                return Translations.getString("PlacementsHolderTableModel.ColumnName.PanelName"); //$NON-NLS-1$
            }
        }
        return columnNames[column];
    }

    public int getColumnCount() {
        return columnNames.length;
    }

    public int getRowCount() {
        if (placementsHolders.get() == null) {
            return 0;
        }
        return placementsHolders.get().size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex == -1) {
            return false;
        }
        return true;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            PlacementsHolder<?> placementsHolder = placementsHolders.get().get(rowIndex);
            if (columnIndex == 0) {
                Object oldValue = placementsHolder.getName();
                placementsHolder.setName((String) aValue);
                fireTableCellUpdated(rowIndex, columnIndex);
                Configuration.get().getBus().post(
                        new PlacementsHolderChangedEvent(placementsHolder, "name", oldValue, aValue, this)); //$NON-NLS-1$
            }
            else if (columnIndex == 1) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location oldValue = placementsHolder.getDimensions();
                Location dims = Length.setLocationField(configuration, oldValue, length, Length.Field.X);
                placementsHolder.setDimensions(dims);
                fireTableCellUpdated(rowIndex, columnIndex);
                Configuration.get().getBus().post(
                        new PlacementsHolderChangedEvent(placementsHolder, "dimensions", oldValue, dims, this)); //$NON-NLS-1$
            }
            else if (columnIndex == 2) {
                LengthCellValue value = (LengthCellValue) aValue;
                Length length = value.getLength();
                Location oldValue = placementsHolder.getDimensions();
                Location dims = Length.setLocationField(configuration, oldValue, length, Length.Field.Y);
                placementsHolder.setDimensions(dims);
                fireTableCellUpdated(rowIndex, columnIndex);
                Configuration.get().getBus().post(
                        new PlacementsHolderChangedEvent(placementsHolder, "dimensions", oldValue, dims, this)); //$NON-NLS-1$
            }
        }
        catch (Exception e) {
            // TODO: dialog, bad input
        }
    }

    public Object getValueAt(int row, int col) {
        PlacementsHolder<?> placementsHolder = placementsHolders.get().get(row);
        Location dim = placementsHolder.getDimensions();
        switch (col) {
            case 0:
                return placementsHolder.getName();
            case 1:
                return new LengthCellValue(dim.getLengthX(), true, true);
            case 2:
                return new LengthCellValue(dim.getLengthY(), true, true);
            default:
                return null;
        }
    }

    @Override
    public Object getRowObjectAt(int index) {
        return placementsHolders.get().get(index);
    }

    @Override
    public int[] getColumnAlignments() {
        return columnAlignments;
    }

    @Override
    public int[] getColumnWidthTypes() {
        return columnWidthTypes;
    }
}
