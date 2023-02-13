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

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.util.ArrayList;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.core.Commit;
import org.simpleframework.xml.core.Persist;

/**
 * Extends java.awt.geom.Area to be serializable and have length units
 */
public class GeometricArea extends java.awt.geom.Area {
    @Attribute
    protected LengthUnit units = LengthUnit.Millimeters;
    
    @Attribute
    protected Integer windingRule = null;
    
    @ElementList
    protected ArrayList<Integer> segmentTypes;
    
    @ElementList
    protected ArrayList<Double> segmentPoints;
    
    /**
     * Default constructor which creates an empty area with units of Millimeters
     */
    GeometricArea() {
        super();
    }

    /**
     * Constructs a GeometricArea with units of Millimeters from the specified Shape object. The 
     * geometry is explicitly closed, if the Shape is not already closed. The fill rule (even-odd 
     * or winding) specified by the geometry of the Shape is used to determine the resulting 
     * enclosed area.
     * @param shape - the Shape from which the area is constructed
     */
    GeometricArea(Shape shape) {
        super(shape);
    }

    /**
     * Constructs a GeometricArea from the specified Shape object with the specified units. The 
     * geometry is explicitly closed, if the Shape is not already closed. The fill rule (even-odd 
     * or winding) specified by the geometry of the Shape is used to determine the resulting 
     * enclosed area.
     * @param shape - the Shape from which the area is constructed
     * @param units - the units
     */
    GeometricArea(Shape shape, LengthUnit units) {
        super(shape);
        this.units = units;
    }

    /**
     * Constructs a new copy of the specified GeometricArea
     * @param geometricArea - the area to copy
     */
    GeometricArea(GeometricArea geometricArea) {
        super(geometricArea);
        units = geometricArea.getUnits();
    }

    /**
     * Restores the area from the serializable elements immediately following their deserialization
     */
    @Commit
    public void commit() {
        if (windingRule != null && segmentTypes != null && segmentPoints != null) {
            Path2D path = new Path2D.Double(windingRule);
            int idx = 0;
            for (int segType : segmentTypes) {
                switch (segType) {
                    case PathIterator.SEG_MOVETO:
                        path.moveTo(segmentPoints.get(idx), segmentPoints.get(idx+1));
                        idx += 2;
                    case PathIterator.SEG_LINETO:
                        path.lineTo(segmentPoints.get(idx), segmentPoints.get(idx+1));
                        idx += 2;
                        break;
                    case PathIterator.SEG_QUADTO:
                        path.quadTo(segmentPoints.get(idx), segmentPoints.get(idx+1), 
                                segmentPoints.get(idx+2), segmentPoints.get(idx+3));
                        idx += 4;
                        break;
                    case PathIterator.SEG_CUBICTO:
                        path.curveTo(segmentPoints.get(idx), segmentPoints.get(idx+1), 
                                segmentPoints.get(idx+2), segmentPoints.get(idx+3), 
                                segmentPoints.get(idx+4), segmentPoints.get(idx+5));
                        idx += 6;
                        break;
                    case PathIterator.SEG_CLOSE:
                        path.closePath();
                        break;
                }
            }
            reset();
            if (idx > 0) {
                add(new Area(path));
            }
        }
    }
    
    /**
     * Sets the serializable elements from the area just prior to serialization
     */
    @Persist
    public void persist() {
        PathIterator pathIter = getPathIterator(null);
        if (!pathIter.isDone()) {
            windingRule = pathIter.getWindingRule();
            segmentTypes = new ArrayList<>();
            segmentPoints = new ArrayList<>();
        }
        double[] coords = new double[6];
        while (!pathIter.isDone()) {
            int segType = pathIter.currentSegment(coords);
            segmentTypes.add(segType);
            switch (segType) {
                case PathIterator.SEG_MOVETO:
                case PathIterator.SEG_LINETO:
                    segmentPoints.add(coords[0]);
                    segmentPoints.add(coords[1]);
                    break;
                case PathIterator.SEG_QUADTO:
                    segmentPoints.add(coords[0]);
                    segmentPoints.add(coords[1]);
                    segmentPoints.add(coords[2]);
                    segmentPoints.add(coords[3]);
                    break;
                case PathIterator.SEG_CUBICTO:
                    segmentPoints.add(coords[0]);
                    segmentPoints.add(coords[1]);
                    segmentPoints.add(coords[2]);
                    segmentPoints.add(coords[3]);
                    segmentPoints.add(coords[4]);
                    segmentPoints.add(coords[5]);
                    break;
                case PathIterator.SEG_CLOSE:
                    break;
            }
            pathIter.next();
        }
    }
    
    /**
     * 
     * @return the units of this
     */
    public LengthUnit getUnits() {
        return units;
    }

    /**
     * Sets the units of this
     * @param units - the units to set
     */
    public void setUnits(LengthUnit units) {
        this.units = units;
    }

    /**
     * Returns a new GeometricArea converted to the specified units
     * @param units - the units of the new path
     * @return the new GeometricArea
     */
    public GeometricArea convertToUnits(LengthUnit units) {
        double scale = (new Length(1, this.units)).divide(new Length(1, units));
        AffineTransform at = new AffineTransform();
        at.scale(scale, scale);
        GeometricArea ret = new GeometricArea(this.createTransformedArea(at));
        ret.setUnits(units);
        return ret;
    }
}
