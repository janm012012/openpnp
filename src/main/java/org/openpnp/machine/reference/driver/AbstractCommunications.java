package org.openpnp.machine.reference.driver;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCommunications;
import org.openpnp.machine.reference.ReferencePasteDispenser;
import org.openpnp.machine.reference.driver.SerialPortCommunications.*;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.driver.wizards.AbstractCommunicationsConfigurationWizard;
import org.openpnp.model.AbstractModelObject;
import org.openpnp.model.Location;
import org.openpnp.spi.PropertySheetHolder;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Element;

import java.io.Closeable;

public abstract class AbstractCommunications extends AbstractModelObject implements ReferenceDriver, Closeable {
    @Element(required = false)
    protected SerialPortCommunications serial = new SerialPortCommunications();

    @Element(required = false)
    protected TcpCommunications tcp = new TcpCommunications();

    @Attribute(required = false)
    protected String communications = "serial";

    protected ReferenceCommunications comms;

    public AbstractCommunications() {
        setCommunications(communications);
    }


    public void dispense(ReferencePasteDispenser dispenser, Location startLocation, Location endLocation, long dispenseTimeMilliseconds) throws Exception {

    }

    public String getCommunications() {
        return communications;
    }

    public void setCommunications(String communications) {
        this.communications = communications;

        switch (communications) {
            case "serial": {
                comms = serial;
                break;
            }
            case "tcp": {
                comms = tcp;
                break;
            }
            default: {
                Logger.error("Invalid communications method attempted to be set. Defaulting to serial.");
                comms = serial;
            }
        }
    }

    public String getPortName() {
        return serial.getPortName();
    }

    public void setPortName(String portName) {
        serial.setPortName(portName);
    }

    public int getBaud() {
        return serial.getBaud();
    }

    public void setBaud(int baud) {
        serial.setBaud(baud);
    }

    public FlowControl getFlowControl() {
        return serial.getFlowControl();
    }

    public void setFlowControl(FlowControl flowControl) {
        serial.setFlowControl(flowControl);
    }

    public DataBits getDataBits() {
        return serial.getDataBits();
    }

    public void setDataBits(DataBits dataBits) {
        serial.setDataBits(dataBits);
    }

    public StopBits getStopBits() {
        return serial.getStopBits();
    }

    public void setStopBits(StopBits stopBits) {
        serial.setStopBits(stopBits);
    }

    public Parity getParity() {
        return serial.getParity();
    }

    public void setParity(Parity parity) {
        serial.setParity(parity);
    }

    public boolean isSetDtr() {
        return serial.isSetDtr();
    }

    public void setSetDtr(boolean setDtr) {
        serial.setSetDtr(setDtr);
    }

    public boolean isSetRts() {
        return serial.isSetRts();
    }

    public void setSetRts(boolean setRts) {
        serial.setSetRts(setRts);
    }

    public String getIpAddress() {
        return tcp.getIpAddress();
    }

    public void setIpAddress(String ipAddress) {
        tcp.setIpAddress(ipAddress);
    }

    public int getPort() {
        return tcp.getPort();
    }

    public void setPort(int port) {
        tcp.setPort(port);
    }


    @Override
    public Icon getPropertySheetHolderIcon() {
        return null;
    }

    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[]{new PropertySheetWizardAdapter(getConfigurationWizard())};
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new AbstractCommunicationsConfigurationWizard(this);
    }

}
