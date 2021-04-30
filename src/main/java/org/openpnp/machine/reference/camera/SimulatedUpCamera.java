package org.openpnp.machine.reference.camera;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.SimulationModeMachine;
import org.openpnp.machine.reference.camera.wizards.SimulatedUpCameraConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.model.Footprint;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Part;
import org.openpnp.model.Solutions;
import org.openpnp.model.Solutions.Severity;
import org.openpnp.spi.Head;
import org.openpnp.spi.Nozzle;
import org.openpnp.spi.PropertySheetHolder;
import org.openpnp.util.Utils2D;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;


@Root
public class SimulatedUpCamera extends ReferenceCamera {
    @Attribute(required=false)
    protected int width = 640;

    @Attribute(required=false)
    protected int height = 480;

    @Element(required=false)
    private Location errorOffsets = new Location(LengthUnit.Millimeters);

    public SimulatedUpCamera() {
        setUnitsPerPixel(new Location(LengthUnit.Millimeters, 0.0234375D, 0.0234375D, 0, 0));
        setLooking(Looking.Up);
    }

    @Override
    public BufferedImage internalCapture() {
        if (!ensureOpen()) {
            return null;
        }
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) image.getGraphics();
        AffineTransform tx = g.getTransform();
        // invert the image in Y so that Y+ is up
        g.translate(0, height);
        g.scale(1, -1);
        g.translate(width / 2, height / 2);

        g.setColor(Color.black);
        g.fillRect(0, 0, width, height);

        // figure out our physical viewport size
        Location phySize = getUnitsPerPixel().convertToUnits(LengthUnit.Millimeters)
                                             .multiply(width, height, 0, 0);
        double phyWidth = phySize.getX();
        double phyHeight = phySize.getY();

        // and bounds
        Location location = getLocation().convertToUnits(LengthUnit.Millimeters);
        Rectangle2D.Double phyBounds = new Rectangle2D.Double(location.getX() - phyWidth / 2,
                location.getY() - phyHeight / 2, phyWidth, phyHeight);

        // determine if there are any nozzles within our bounds and if so render them
        try {
            for (Head head :  Configuration.get()
                    .getMachine().getHeads()) {
                for (Nozzle nozzle : head.getNozzles()) {
                    Location l = SimulationModeMachine.getSimulatedPhysicalLocation(nozzle, getLooking());
                    if (phyBounds.contains(l.getX(), l.getY())) {
                        drawNozzle(g, nozzle, l);
                    }
                }
            }
        }
        catch (Exception e) {
            // This can throw a concurrrent modification exception if the nozzles 
            // array is modified. 
            e.printStackTrace();
        }

        g.setTransform(tx);

        SimulationModeMachine.simulateCameraExposure(this, g, width, height);

        g.dispose();

        return image;
    }

    private void drawNozzle(Graphics2D g, Nozzle nozzle, Location l) {
        g.setStroke(new BasicStroke(2f));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        LengthUnit units = LengthUnit.Millimeters;
        Location unitsPerPixel = getUnitsPerPixel().convertToUnits(units);
        
        // Draw the nozzle
        // Get nozzle offsets from camera
        Location offsets = l.subtractWithRotation(getLocation());
        
        // Create a nozzle shape
        fillShape(g, new Ellipse2D.Double(-0.5, -0.5, 1, 1), Color.green, unitsPerPixel, offsets, false);

        // Draw the part
        Part part = nozzle.getPart();
        if (part == null) {
            return;
        }

        org.openpnp.model.Package pkg = part.getPackage();
        Footprint footprint = pkg.getFootprint();
        if (footprint == null) {
            return;
        }

        if (footprint.getUnits() != units) {
            throw new Error("Not yet supported.");
        }
        
        // First draw the body in dark grey.
        fillShape(g, footprint.getBodyShape(), new Color(60, 60, 60), unitsPerPixel, offsets, true);
        
        // Then draw the pads in white
        fillShape(g, footprint.getPadsShape(), Color.white, unitsPerPixel, offsets, true);
    }
    
    private void fillShape(Graphics2D g, Shape shape, Color color, Location unitsPerPixel, Location offsets, boolean addError) {
        AffineTransform tx = new AffineTransform();
        // Scale to pixels
        tx.scale(1.0 / unitsPerPixel.getX(), 1.0 / unitsPerPixel.getY());
        // Translate and rotate to offsets
        tx.translate(offsets.getX(), offsets.getY());
        tx.rotate(Math.toRadians(Utils2D.normalizeAngle(offsets.getRotation())));
        if (addError) {
            // Translate and rotate to error offsets
            tx.translate(errorOffsets.getX(), errorOffsets.getY());
            tx.rotate(Math.toRadians(Utils2D.normalizeAngle(errorOffsets.getRotation())));
        }
        // Transform
        shape = tx.createTransformedShape(shape);
        // Draw
        g.setColor(color);
        g.fill(shape);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Location getErrorOffsets() {
        return errorOffsets;
    }

    public void setErrorOffsets(Location errorOffsets) {
        this.errorOffsets = errorOffsets;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new SimulatedUpCameraConfigurationWizard(this);
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }


    @Override
    public void findIssues(Solutions solutions) {
        super.findIssues(solutions);
        solutions.add(new Solutions.Issue(
                this, 
                "The SimulatedUpCamera can be replaced with a OpenPnpCaptureCamera to connect to a real USB camera.", 
                "Replace with OpenPnpCaptureCamera.", 
                Severity.Fundamental,
                "https://github.com/openpnp/openpnp/wiki/OpenPnpCaptureCamera") {

            @Override
            public void setState(Solutions.State state) throws Exception {
                if (state == Solutions.State.Solved) {
                    OpenPnpCaptureCamera camera = createReplacementCamera();
                    replaceCamera(camera);
                }
                else if (getState() == Solutions.State.Solved) {
                    // Place the old one back (from the captured SimulatedUpCamera.this).
                    replaceCamera(SimulatedUpCamera.this);
                }
                super.setState(state);
            }
        });
    }
}
