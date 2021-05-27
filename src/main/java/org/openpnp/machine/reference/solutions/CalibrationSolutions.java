/*
 * Copyright (C) 2021 <mark@makr.zone>
 * inspired and based on work
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

package org.openpnp.machine.reference.solutions;

import java.awt.Toolkit;

import org.openpnp.gui.MainFrame;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis;
import org.openpnp.machine.reference.axis.ReferenceControllerAxis.BacklashCompensationMethod;
import org.openpnp.machine.reference.feeder.ReferenceTubeFeeder;
import org.openpnp.model.AxesLocation;
import org.openpnp.model.Length;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Package;
import org.openpnp.model.Part;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Milestone;
import org.openpnp.model.Solutions.State;
import org.openpnp.spi.Axis.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.CoordinateAxis;
import org.openpnp.spi.Head;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.VisionUtils;
import org.openpnp.vision.pipeline.CvStage.Result.Circle;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;

/**
 * This helper class implements the Issues & Solutions for the Calibration Milestone. 
 */
public class CalibrationSolutions implements Solutions.Subject {

    @Attribute(required = false)
    private int backlashCalibrationPasses = 4;

    @Attribute(required = false)
    private double errorDampening = 0.8;

    @Attribute(required = false)
    private double backlashTestMoveMm = 20.0;

    @Attribute(required = false)
    private double oneSidedBacklashSafetyFactor = 1.1;

    @Attribute(required = false)
    private int nozzleOffsetAngles = 6;

    @Attribute(required = false)
    private long extraVacuumDwellMs = 300;


    public CalibrationSolutions setMachine(ReferenceMachine machine) {
        this.machine = machine;
        return this;
    }

    private ReferenceMachine machine;


    @Override
    public void findIssues(Solutions solutions) {
        if (solutions.isTargeting(Milestone.Calibration)) {
            for (Head h : machine.getHeads()) {
                if (h instanceof ReferenceHead) {
                    ReferenceHead head = (ReferenceHead) h;
                    ReferenceCamera defaultCamera = null;
                    try {
                        defaultCamera = (ReferenceCamera)head.getDefaultCamera();
                    }
                    catch (Exception e) {
                        // Ignore missing camera.
                    }
                    Nozzle defaultNozzle = null;
                    try {
                        defaultNozzle = head.getDefaultNozzle();
                    }
                    catch (Exception e1) {
                    }
                    if (defaultCamera != null) {
                        for (Camera camera : head.getCameras()) {
                            if (camera instanceof ReferenceCamera) {
                                perDownLookingCameraSolutions(solutions, head, defaultCamera, defaultNozzle, (ReferenceCamera) camera);
                            }
                        }
                        for (Nozzle nozzle : head.getNozzles()) {
                            if (nozzle instanceof ReferenceNozzle) {
                                perNozzleSolutions(solutions, head, defaultCamera, defaultNozzle, (ReferenceNozzle) nozzle);
                            }
                        }
                        perHeadSolutions(solutions, head, defaultCamera);
                    }
                }
            } 

            Camera defaultCamera = null;
            Nozzle defaultNozzle = null;
            try {
                defaultCamera = VisionUtils.getBottomVisionCamera();
                Head head = machine.getDefaultHead();
                defaultNozzle = head.getDefaultNozzle();
            }
            catch (Exception e1) {
            }
            if (defaultCamera != null && defaultNozzle  != null) {
                for (Camera camera : machine.getCameras()) {
                    if (camera instanceof ReferenceCamera) {
                        perUpLookingCameraSolutions(solutions, defaultCamera, defaultNozzle, (ReferenceCamera) camera);
                    }
                }
            }
        }
    }
    private void perHeadSolutions(Solutions solutions, ReferenceHead head, ReferenceCamera defaultCamera) {
        // TODO Auto-generated method stub

    }
    private void perDownLookingCameraSolutions(Solutions solutions, ReferenceHead head,
            ReferenceCamera defaultCamera, Nozzle defaultNozzle, ReferenceCamera camera) {
        // TODO Auto-generated method stub

        // Calibrate backlash.
        if (camera == defaultCamera) {
            CoordinateAxis rawAxisX = HeadSolutions.getRawAxis(machine, camera.getAxisX());
            CoordinateAxis rawAxisY = HeadSolutions.getRawAxis(machine, camera.getAxisY());
            for (CoordinateAxis rawAxis : new CoordinateAxis[] {rawAxisX, rawAxisY}) {
                if (rawAxis instanceof ReferenceControllerAxis) {
                    ReferenceControllerAxis axis = (ReferenceControllerAxis)rawAxis;
                    BacklashCompensationMethod oldMethod = axis.getBacklashCompensationMethod();
                    Length oldOffset = axis.getBacklashOffset();
                    double oldSpeed = axis.getBacklashSpeedFactor();

                    solutions.add(new Solutions.Issue(
                            camera, 
                            "Calibrate backlash compensation for axis "+axis.getName()+".", 
                            "Automatically calibrates the backlash compensation for "+axis.getName()+" using the primary calibration fiducial.", 
                            Solutions.Severity.Fundamental,
                            "https://github.com/openpnp/openpnp/wiki/Calibration-Solutions#calibrating-backlash-compensation") {

                        @Override 
                        public void activate() throws Exception {
                            MainFrame.get().getMachineControls().setSelectedTool(camera);
                            camera.ensureCameraVisible();
                        }

                        @Override 
                        public String getExtendedDescription() {
                            return "<html>"
                                    + "<p>Backlash compensation is used to avoid the effects of any looseness or play in the mechanical "
                                    + "linkages of machine axes. More information can be found in the Wiki (press the blue Info button).</p><br/>"
                                    + "<p><span color=\"red\">CAUTION</span>: The camera "+camera.getName()+" will move over the fiducial "
                                    + "and then perform a "+(backlashTestMoveMm*2)+" mm calibration motion pattern moving axis "
                                    + axis.getName()+".</p><br/>"
                                    + "<p>When ready, press Accept.</p>"
                                    + (getState() == State.Solved ? 
                                            "<br/><h4>Results:</h4>"
                                            + "<table>"
                                            + "<tr><td align=\"right\">Detected Backlash:</td>"
                                            + "<td>"+axis.getBacklashOffset()+"</td></tr>"
                                            + "<tr><td align=\"right\">Selected Method:</td>"
                                            + "<td>"+axis.getBacklashCompensationMethod().toString()+"</td></tr>"
                                            + "<tr><td align=\"right\">Speed Factor:</td>"
                                            + "<td>"+axis.getBacklashSpeedFactor()+"</td></tr>"
                                            + "<tr><td align=\"right\">Applicable Tolerance:</td>"
                                            + "<td>"+String.format("%.4f", getAxisCalibrationTolerance(camera, axis))+" mm</td></tr>"
                                            + "</table>" 
                                            : "")
                                    + "</html>";
                        }

                        @Override
                        public void setState(Solutions.State state) throws Exception {
                            if (state == State.Solved) {
                                final State oldState = getState();
                                UiUtils.submitUiMachineTask(
                                        () -> {
                                            calibrateAxisBacklash(head, camera, camera, axis);
                                            return true;
                                        },
                                        (result) -> {
                                            UiUtils.messageBoxOnException(() -> super.setState(state));
                                            // Persist this solved state.
                                            solutions.setSolutionsIssueSolved(this, true);
                                        },
                                        (t) -> {
                                            UiUtils.showError(t);
                                            // restore old state
                                            UiUtils.messageBoxOnException(() -> setState(oldState));
                                        });
                            }
                            else {
                                axis.setBacklashCompensationMethod(oldMethod);
                                axis.setBacklashOffset(oldOffset);
                                axis.setBacklashSpeedFactor(oldSpeed);
                                // Persist this unsolved state.
                                solutions.setSolutionsIssueSolved(this, false);
                                super.setState(state);
                            }
                        }
                    });
                }
            }

        }
    }

    private void perNozzleSolutions(Solutions solutions, ReferenceHead head, ReferenceCamera defaultCamera,
            Nozzle defaultNozzle, ReferenceNozzle nozzle) {
        final Location oldNozzleOffsets = nozzle.getHeadOffsets();
        final Length oldTestObjectDiameter = head.getCalibrationTestObjectDiameter(); 
        // Get the test subject diameter.
        solutions.add(new Solutions.Issue(
                nozzle, 
                "Calibrate precise camera ↔ nozzle "+nozzle.getName()+" offsets.", 
                "Use a test object to perform the precision camera ↔ nozzle "+nozzle.getName()+" offsets calibration.", 
                Solutions.Severity.Fundamental,
                "https://github.com/openpnp/openpnp/wiki/Calibration-Solutions#calibrating-precision-camera-to-nozzle-offsets") {

            private int featureDiameter;

            {
                featureDiameter = 40;
                if (head.getCalibrationTestObjectDiameter() != null
                        && head.getCalibrationTestObjectDiameter().getValue() != 0) {
                    // Existing diameter setting.
                    featureDiameter = (int) Math.round(head.getCalibrationTestObjectDiameter().divide(defaultCamera.getUnitsPerPixel().getLengthX()));
                }
            }

            @Override 
            public void activate() throws Exception {
                MainFrame.get().getMachineControls().setSelectedTool(defaultCamera);
                defaultCamera.ensureCameraVisible();
            }

            @Override
            public Solutions.Issue.CustomProperty[] getProperties() {
                return new Solutions.Issue.CustomProperty[] {
                        new Solutions.Issue.IntegerProperty(
                                "Detected feature diameter",
                                "Adjust the nozzle tip feature diameter that should be detected.",
                                3, 1000) {
                            @Override
                            public int get() {
                                return featureDiameter;
                            }
                            @Override
                            public void set(int value) {
                                featureDiameter = value;
                                UiUtils.submitUiMachineTask(() -> {
                                    try {
                                        // This show a diagnostic detection image in the camera view for 2000ms.
                                        machine.getVisualSolutions().getSubjectPixelLocation(defaultCamera, null, new Circle(0, 0, value), 0, 2000);
                                    }
                                    catch (Exception e) {
                                        Toolkit.getDefaultToolkit().beep();
                                    }
                                });
                            }
                        },
                };
            }
            @Override 
            public String getExtendedDescription() {
                return "<html>"
                        + "<p>To calibrate precision camera ↔ nozzle offsets, we let the nozzle pick, rotate and place a small "
                        + "test object and then measure the resulting offsets using the camera.</p><br/>"
                        + "<p>Instructions about suitable test objects etc. must be obtained in the OpenPnP "
                        + "Wiki. Press the blue Info button to open the Wiki.</p><br/>"
                        + "<p>Place the calibration test object onto the calibration primary fiducial.</p><br/>"
                        + "<p>Jog camera " + defaultCamera.getName()
                        + " over the test object. Target it with the cross-hairs.</p><br/>"
                        + "<p>Adjust the <strong>Detected feature diameter</strong> up and down and see if it is detected right in the "
                        + "camera view.</p><br/>"
                        + "<p><strong color=\"red\">Caution:</strong> The nozzle "+nozzle.getName()+" will now move to the test object "
                        + "and perform the calibration pick & place pattern. Make sure to load the right nozzle tip and "
                        + "ready the vacuum system.</p><br/>"
                        + "<p>When ready, press Accept.</p>"
                        + (getState() == State.Solved && !nozzle.getHeadOffsets().equals(oldNozzleOffsets) ? 
                                "<br/><h4>Results:</h4>"
                                + "<table>"
                                + "<tr><td align=\"right\">Detected Nozzle Head Offsets:</td>"
                                + "<td>"+nozzle.getHeadOffsets()+"</td></tr>"
                                + "<tr><td align=\"right\">Previous Nozzle Head Offsets:</td>"
                                + "<td>"+oldNozzleOffsets+"</td></tr>"
                                + "<tr><td align=\"right\">Difference:</td>"
                                + "<td>"+nozzle.getHeadOffsets().subtract(oldNozzleOffsets)+"</td></tr>"
                                + "</table>" 
                                : "")
                        + "</html>";
            }

            @Override
            public void setState(Solutions.State state) throws Exception {
                if (state == State.Solved) {
                    final State oldState = getState();
                    UiUtils.submitUiMachineTask(
                            () -> {
                                Circle testObject = machine.getVisualSolutions()
                                        .getSubjectPixelLocation(defaultCamera, null, new Circle(0, 0, featureDiameter), 0, 0);
                                head.setCalibrationTestObjectDiameter(
                                        new Length(testObject.getDiameter()*defaultCamera.getUnitsPerPixel().getX(), 
                                                defaultCamera.getUnitsPerPixel().getUnits()));
                                calibrateNozzleOffsets(head, defaultCamera, nozzle);
                                return true;
                            },
                            (result) -> {
                                UiUtils.messageBoxOnException(() -> super.setState(state));
                                // Persist this solved state.
                                solutions.setSolutionsIssueSolved(this, true);
                            },
                            (t) -> {
                                UiUtils.showError(t);
                                // restore old state
                                UiUtils.messageBoxOnException(() -> setState(oldState));
                            });
                }
                else {
                    // Restore the camera offset
                    nozzle.setHeadOffsets(oldNozzleOffsets);
                    head.setCalibrationTestObjectDiameter(oldTestObjectDiameter);
                    // Persist this unsolved state.
                    solutions.setSolutionsIssueSolved(this, false);
                    super.setState(state);
                }
            }
        });
    }

    private void perUpLookingCameraSolutions(Solutions solutions, Camera defaultCamera,
            Nozzle defaultNozzle, ReferenceCamera camera) {
        // TODO Auto-generated method stub

    }

    private void calibrateAxisBacklash(ReferenceHead head, ReferenceCamera camera,
            HeadMountable movable, ReferenceControllerAxis axis) throws Exception {
        // Use the primary calibration fiducial for calibration.
        Location location = head.getCalibrationPrimaryFiducialLocation();
        Length fiducialDiameter = head.getCalibrationPrimaryFiducialDiameter();
        // General note: We always use mm.
        // Calculate the unit vector for the axis in both logical and axis coordinates. 
        Location unit = new Location(LengthUnit.Millimeters, 
                (axis.getType() == Type.X ? 1 : 0), 
                (axis.getType() == Type.Y ? 1 : 0),
                0, 0);
        AxesLocation axesLocation0 = movable.toRaw(location);
        AxesLocation axesLocation1 = movable.toRaw(location.add(unit));
        double mmAxis = axesLocation1.getCoordinate(axis) - axesLocation0.getCoordinate(axis); 

        double toleranceMm = getAxisCalibrationTolerance(camera, axis);

        // Determine the needed backlash compensation at various speed factors. 
        double[] speeds = new double [] { 0.25, 0.5, 0.75, 1 };
        double[] backlashOffsetBySpeed = new double [speeds.length];
        int iSpeed = 0;
        for (double speed : speeds) {
            // Reset the config.
            axis.setBacklashCompensationMethod(BacklashCompensationMethod.DirectionalCompensation);
            axis.setBacklashOffset(new Length(0, LengthUnit.Millimeters));
            axis.setBacklashSpeedFactor(1.0);
            // Find the right backlash offset.
            double offsetMm = 0;
            for (int pass = 0; pass < backlashCalibrationPasses; pass++) {
                // Approach from minus.
                MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, -backlashTestMoveMm*mmAxis));
                movable.moveTo(location, speed);
                Location effective0 = machine.getVisualSolutions().getDetectedLocation(camera, camera, 
                        location, fiducialDiameter, 2000);

                // Approach from plus.
                MovableUtils.moveToLocationAtSafeZ(movable, displacedAxisLocation(movable, axis, location, backlashTestMoveMm*mmAxis));
                movable.moveTo(location, speed);
                Location effective1 = machine.getVisualSolutions().getDetectedLocation(camera, camera, 
                        location, fiducialDiameter, 2000);

                double mmError = effective1.subtract(effective0).dotProduct(unit).getValue();
                if (movable == camera) {
                    // If the camera moves (and not the subject) then the subject's 
                    // displacement is negative.
                    mmError = -mmError;
                }
                offsetMm += mmError*mmAxis*errorDampening;
                if (pass == 0 && mmError <= -toleranceMm) {
                    // Overshoot - cannot compensate.
                    break;
                }
                axis.setBacklashOffset(new Length(offsetMm, LengthUnit.Millimeters)
                        .convertToUnits(axis.getDriver().getUnits()));
                if (Math.abs(mmError) < toleranceMm) {
                    break;
                }
            }
            // Record this for the speed. 
            backlashOffsetBySpeed[iSpeed++] = offsetMm;
        }
        // Determine consistency over speed.
        int consistent = 0;
        double offsetMmSum = 0;
        for (double offsetMm : backlashOffsetBySpeed) {
            if (offsetMm <= -toleranceMm
                    || Math.abs(offsetMm - backlashOffsetBySpeed[0]) > toleranceMm) {
                // inconsistent
                break;
            }
            offsetMmSum += offsetMm;
            consistent++;
        }
        double offsetMmAvg = offsetMmSum/consistent;
        double offsetMmMax = 0;
        iSpeed = 0;
        for (double offsetMm : backlashOffsetBySpeed) {
            offsetMmMax = Math.max(offsetMmMax, Math.abs(offsetMm));
            Logger.debug("Axis "+axis.getName()+" backlash offsets at speed factor "+speeds[iSpeed++]+" is "+offsetMm);
        }
        Logger.debug("Axis "+axis.getName()+" backlash offsets analysis, consistent: "+consistent+", avg offset: "+offsetMmAvg+", max offset: "+offsetMmMax);
        // Set the backlash method according to consistency.
        if (consistent == speeds.length) {
            // We got consistent backlash over all the speeds.
            if (offsetMmAvg < toleranceMm) {
                // Smaller than resolution, no need for compensation.
                axis.setBacklashCompensationMethod(BacklashCompensationMethod.None);
            }
            else {
                // Consistent over speed, can be compensated by directional method. 
                axis.setBacklashCompensationMethod(BacklashCompensationMethod.DirectionalCompensation);
            }
            axis.setBacklashOffset(new Length(offsetMmAvg, LengthUnit.Millimeters));
            axis.setBacklashSpeedFactor(speeds[consistent-1]);
        }
        else if (consistent > 0) {
            // Not consistent over speed.
            axis.setBacklashCompensationMethod(BacklashCompensationMethod.OneSidedPositioning);
            axis.setBacklashOffset(new Length(offsetMmMax*oneSidedBacklashSafetyFactor, LengthUnit.Millimeters));
            axis.setBacklashSpeedFactor(speeds[consistent-1]);
        }
        else {
            throw new Exception("Axis "+axis.getName()+" seems to overshoot, even at the lowest speed factor. "
                    + "Make sure OpenPnP has effective acceleration/jerk control. "
                    + "Automatic compensation not possible.");
        }
    }
    private double getAxisCalibrationTolerance(ReferenceCamera camera,
            ReferenceControllerAxis axis) {
        // Get the axis resolution, but it might still be at the default 0.0001.
        Length resolution = new Length(axis.getResolution(), axis.getDriver().getUnits());
        // Get a minimal pixel step size. 
        Length pixelStep = (axis.getType() == Type.X  
                ? camera.getUnitsPerPixel().getLengthX() 
                        : camera.getUnitsPerPixel().getLengthY()).multiply(1.1);
        if (pixelStep.compareTo(resolution) > 0) {
            // If the pixel step is coarser than the set axis resolution or if 
            // the axis resolution is not (yet) set properly, take the pixel step. 
            resolution = pixelStep;
        }
        resolution = resolution.convertToUnits(LengthUnit.Millimeters);
        return resolution.getValue();
    }

    private Location displacedAxisLocation(HeadMountable movable, ReferenceControllerAxis axis,
            Location location, double displacement) throws Exception {
        AxesLocation axesLocation = movable.toRaw(location);
        axesLocation = axesLocation.add(new AxesLocation(axis, displacement));
        Location newLocation = movable.toTransformed(axesLocation);
        return newLocation;
    }

    private void calibrateNozzleOffsets(ReferenceHead head, ReferenceCamera defaultCamera, ReferenceNozzle nozzle)
            throws Exception {
        try {
            // Create a pseudo part and feeder to enable pick and place.
            Part testPart = new Part("TEST-OBJECT");
            testPart.setHeight(new Length(0.01, LengthUnit.Millimeters));
            Package packag = new Package("TEST-OBJECT-PACKAGE");
            testPart.setPackage(packag);
            ReferenceTubeFeeder feeder = new ReferenceTubeFeeder();
            feeder.setPart(testPart);
            // Get the initial precise test object location.
            Location location = machine.getVisualSolutions()
                    .centerInOnSubjectLocation(defaultCamera, defaultCamera,
                            head.getCalibrationTestObjectDiameter(), 2000);
            // We accumulate all the detected differences and only calculate the centroid in the end. 
            int accumulated = 0;
            Location offsetsDiff = new Location(LengthUnit.Millimeters);
            double da = 360.0 / nozzleOffsetAngles;
            for (double angle = -180 + da / 2; angle < 180; angle += da) {
                // Subtract from accumulation.
                offsetsDiff = offsetsDiff.subtract(location);
                // Replace Z.
                location = location.derive(head.getCalibrationPrimaryFiducialLocation(), false,
                        false, true, false);
                // Pick the test object at the location.
                feeder.setLocation(location.derive(null, null, null, angle));
                nozzle.moveToPickLocation(feeder);
                nozzle.pick(testPart);
                // Extra wait time.
                Thread.sleep(extraVacuumDwellMs);
                // Place the part 180° rotated. This way we will detect the true nozzle rotation axis, which is 
                // the true nozzle location, namely in the center of the two detected locations. Note that run-out 
                // is cancelled out too, so run-out compensation is no prerequisite. 
                Location placementLocation = location.derive(null, null, null, angle + 180.0);
                nozzle.moveToPlacementLocation(placementLocation, testPart);
                nozzle.place();
                // Extra wait time.
                Thread.sleep(extraVacuumDwellMs);
                // Look where it is now.
                MovableUtils.moveToLocationAtSafeZ(defaultCamera, location);
                Location newlocation = machine.getVisualSolutions()
                        .centerInOnSubjectLocation(defaultCamera, defaultCamera,
                                head.getCalibrationTestObjectDiameter(), 2000);
                // Add to accumulation.
                offsetsDiff = offsetsDiff.add(newlocation);
                accumulated += 2;
                Logger.debug("Nozzle "+nozzle.getName()+" has placed at offsets "
                        +newlocation.subtract(location)+ " at angle "+angle);
                // Next
                location = newlocation;
            }
            // Compute the average of the accumulated offsets differences. Take only X, Y.
            offsetsDiff = offsetsDiff.multiply(1.0 / accumulated)
                    .multiply(1, 1, 0, 0);
            Location headOffsets = nozzle.getHeadOffsets()
                    .add(offsetsDiff);
            Logger.info("Set nozzle " + nozzle.getName() + " head offsets to " + headOffsets
                    + " (previously " + nozzle.getHeadOffsets() + ")");
            nozzle.setHeadOffsets(headOffsets);
        }
        finally {
            if (nozzle.getPart() != null) {
                nozzle.place();
            }
        }
    }
}
