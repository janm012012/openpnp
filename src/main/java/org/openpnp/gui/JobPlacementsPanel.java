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

package org.openpnp.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.regex.PatternSyntaxException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.openpnp.events.DefinitionStructureChangedEvent;
import org.openpnp.events.PlacementSelectedEvent;
import org.openpnp.gui.components.AutoSelectTextTable;
import org.openpnp.gui.support.ActionGroup;
import org.openpnp.gui.support.CustomBooleanRenderer;
import org.openpnp.gui.support.CustomAlignmentRenderer;
import org.openpnp.gui.support.MonospacedFontTableCellRenderer;
import org.openpnp.gui.support.Helpers;
import org.openpnp.gui.support.Icons;
import org.openpnp.gui.support.IdentifiableListCellRenderer;
import org.openpnp.gui.support.IdentifiableTableCellRenderer;
import org.openpnp.gui.support.LengthCellValue;
import org.openpnp.gui.support.MessageBoxes;
import org.openpnp.gui.support.PartsComboBoxModel;
import org.openpnp.gui.support.RotationCellValue;
import org.openpnp.gui.support.TableUtils;
import org.openpnp.gui.tablemodel.PlacementsTableModel;
import org.openpnp.gui.tablemodel.PlacementsTableModel.Status;
import org.openpnp.model.Board;
import org.openpnp.model.Abstract2DLocatable.Side;
import org.openpnp.model.Configuration;
import org.openpnp.model.Configuration.TablesLinked;
import org.openpnp.model.PlacementsHolderLocation;
import org.openpnp.model.Job;
import org.openpnp.model.Location;
import org.openpnp.model.PanelLocation;
import org.openpnp.model.Part;
import org.openpnp.model.Placement;
import org.openpnp.model.Placement.ErrorHandling;
import org.openpnp.model.Placement.Type;
import org.openpnp.spi.Camera;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.Nozzle;
import org.openpnp.util.MovableUtils;
import org.openpnp.util.UiUtils;
import org.openpnp.util.Utils2D;
import org.pmw.tinylog.Logger;

import com.google.common.eventbus.Subscribe;

@SuppressWarnings("serial")
public class JobPlacementsPanel extends JPanel {
    private JTable table;
    private PlacementsTableModel tableModel;
    private TableRowSorter<PlacementsTableModel> tableSorter;
    private ActionGroup topLevelSingleInstanceActionGroup;
    private ActionGroup singleSelectionActionGroup;
    private ActionGroup multiSelectionActionGroup;
    private ActionGroup positionActionGroup;
    private ActionGroup captureActionGroup;
    private PlacementsHolderLocation<?> boardOrPanelLocation;
    private JobPanel jobPanel;
    private boolean topLevel;
    private boolean singleInstance;
    private ActionGroup editFeederActionGroup;
    private Preferences prefs = Preferences.userNodeForPackage(JobPlacementsPanel.class);


    private static Color typeColorFiducial = new Color(157, 188, 255);
    private static Color typeColorPlacement = new Color(255, 255, 255);
    private static Color statusColorWarning = new Color(252, 255, 157);
    private static Color statusColorReady = new Color(157, 255, 168);
    private static Color statusColorError = new Color(255, 157, 157);
    private static Color statusColorDisabled = new Color(180, 180, 180);

    public JobPlacementsPanel(JobPanel jobPanel) {
    	this.jobPanel = jobPanel;
        createUi();
    }
    private void createUi() {
        setBorder(new TitledBorder(null, "Placements", TitledBorder.LEADING, TitledBorder.TOP, null, null));
        
        Configuration configuration = Configuration.get();
        
        topLevelSingleInstanceActionGroup = new ActionGroup(newAction, removeAction, 
                setTypeAction, setSideAction);
        topLevelSingleInstanceActionGroup.setEnabled(false);

        editFeederActionGroup = new ActionGroup(editPlacementFeederAction);
        editFeederActionGroup.setEnabled(false);
        
        singleSelectionActionGroup = new ActionGroup(setPlacedAction, setErrorHandlingAction, 
                setEnabledAction);
        singleSelectionActionGroup.setEnabled(false);

        multiSelectionActionGroup = new ActionGroup(setPlacedAction, setErrorHandlingAction, 
                setEnabledAction);
        multiSelectionActionGroup.setEnabled(false);

        positionActionGroup = new ActionGroup(moveCameraToPlacementLocation,
                moveCameraToPlacementLocationNext, moveToolToPlacementLocation);
        positionActionGroup.setEnabled(false);

        captureActionGroup = new ActionGroup(captureCameraPlacementLocation,
                captureToolPlacementLocation);
        captureActionGroup.setEnabled(false);

        // Suppress because adding the type specifiers breaks WindowBuilder.
        @SuppressWarnings({"unchecked", "rawtypes"})
        JComboBox<PartsComboBoxModel> partsComboBox = new JComboBox(new PartsComboBoxModel());
        partsComboBox.setMaximumRowCount(20);
        partsComboBox.setRenderer(new IdentifiableListCellRenderer<Part>());
        @SuppressWarnings({"unchecked", "rawtypes"})
        JComboBox<Side> sidesComboBox = new JComboBox(Side.values());
        // Note we don't use Type.values() here because there are a couple Types that are only
        // there for backwards compatibility and we don't want them in the list.
        @SuppressWarnings({"unchecked", "rawtypes"})
        JComboBox<Type> typesComboBox = new JComboBox(new Type[] { Type.Placement, Type.Fiducial });
        @SuppressWarnings({"unchecked", "rawtypes"})
        JComboBox<Type> errorHandlingComboBox = new JComboBox(ErrorHandling.values());
        
        setLayout(new BorderLayout(0, 0));
        tableModel = new PlacementsTableModel(configuration) {
            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return (columnIndex != 1 && columnIndex != 9) && ((topLevel && singleInstance && !(boardOrPanelLocation instanceof PanelLocation)) || 
                        (!(topLevel && singleInstance&& !(boardOrPanelLocation instanceof PanelLocation)) && (columnIndex == 0 || columnIndex == 8 || columnIndex == 10)));
            }
        };
        tableSorter = new TableRowSorter<>(tableModel);
        
        table = new AutoSelectTextTable(tableModel);
        table.setRowSorter(tableSorter);
        table.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setDefaultEditor(Side.class, new DefaultCellEditor(sidesComboBox));
        table.setDefaultEditor(Part.class, new DefaultCellEditor(partsComboBox));
        table.setDefaultEditor(Type.class, new DefaultCellEditor(typesComboBox));
        table.setDefaultEditor(ErrorHandling.class, new DefaultCellEditor(errorHandlingComboBox));
        table.setDefaultRenderer(Part.class, new IdentifiableTableCellRenderer<Part>());
        table.setDefaultRenderer(PlacementsTableModel.Status.class, new StatusRenderer());
        table.setDefaultRenderer(Placement.Type.class, new TypeRenderer());
        table.setDefaultRenderer(Boolean.class, new CustomBooleanRenderer());
        table.setDefaultRenderer(LengthCellValue.class, new MonospacedFontTableCellRenderer());
        table.setDefaultRenderer(RotationCellValue.class, new MonospacedFontTableCellRenderer());
        table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        
        TableUtils.setColumnAlignment(tableModel, table);
        
        TableUtils.installColumnWidthSavers(table, prefs, "JobPanel.jobPlacementsTable.columnWidth");
        
        tableModel.setJobPlacementsPanel(this);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting()) {
                    return;
                }

                boolean updateLinkedTables = MainFrame.get().getTabs().getSelectedComponent() == MainFrame.get().getJobTab() 
                        && Configuration.get().getTablesLinked() == TablesLinked.Linked;
                
                if (getSelections().size() > 1) {
                    // multi select
                    singleSelectionActionGroup.setEnabled(false);
                    editFeederActionGroup.setEnabled(false);
                    positionActionGroup.setEnabled(false);
                    captureActionGroup.setEnabled(false);
                    multiSelectionActionGroup.setEnabled(true);
                    if (updateLinkedTables) {
                        Configuration.get().getBus().post(new PlacementSelectedEvent(null,
                                boardOrPanelLocation, JobPlacementsPanel.this));
                    }
                }
                else {
                    // single select, or no select
                    multiSelectionActionGroup.setEnabled(false);
                    singleSelectionActionGroup.setEnabled(getSelection() != null);
                    editFeederActionGroup.setEnabled(getSelection() != null && getSelection().getType() == Placement.Type.Placement);
                    positionActionGroup.setEnabled(getSelection() != null
                            && getSelection().getSide() == boardOrPanelLocation.getGlobalSide());
                    captureActionGroup.setEnabled(topLevel && singleInstance && getSelection() != null
                            && getSelection().getSide() == boardOrPanelLocation.getGlobalSide());
                    if (updateLinkedTables) {
                        Configuration.get().getBus().post(new PlacementSelectedEvent(getSelection(),
                                boardOrPanelLocation, JobPlacementsPanel.this));
                    }
                    MainFrame mainFrame = MainFrame.get();
                    if (getSelection() != null && updateLinkedTables) {
                        Part selectedPart = getSelection().getPart();
                        if (selectedPart != null) {
                            mainFrame.getPartsTab().selectPartInTable(selectedPart);
                            mainFrame.getPackagesTab().selectPackageInTable(selectedPart.getPackage());
                            mainFrame.getFeedersTab().selectFeederForPart(selectedPart);
                            mainFrame.getVisionSettingsTab().selectVisionSettingsInTable(selectedPart);
                        }
                    }
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() != 2) {
                    return;
                }
                int row = table.rowAtPoint(new Point(mouseEvent.getX(), mouseEvent.getY()));
                int col = table.columnAtPoint(new Point(mouseEvent.getX(), mouseEvent.getY()));
                if (tableModel.getColumnClass(col) == Status.class) {
                    Status status = (Status) tableModel.getValueAt(row, col);
                    // TODO: This is some sample code for handling the user
                    // wishing to do something with the status. Not using it
                    // right now but leaving it here for the future.
                    System.out.println(status);
                }
            }
        });
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == ' ') {
                    Placement placement = getSelection();
                    placement.setEnabled(!placement.isEnabled());
                    refreshSelectedRow();
                    updateActivePlacements();
                }
                else {
                    super.keyTyped(e);
                }
            }
        });
        
        JPopupMenu popupMenu = new JPopupMenu();

        JMenu setTypeMenu = new JMenu(setTypeAction);
        setTypeMenu.add(new SetTypeAction(Placement.Type.Placement));
        setTypeMenu.add(new SetTypeAction(Placement.Type.Fiducial));
        popupMenu.add(setTypeMenu);

        JMenu setSideMenu = new JMenu(setSideAction);
        for (Side side : Side.values()) {
            setSideMenu.add(new SetSideAction(side));
        }
        popupMenu.add(setSideMenu);

        JMenu setPlacedMenu = new JMenu(setPlacedAction);
        setPlacedMenu.add(new SetPlacedAction(true));
        setPlacedMenu.add(new SetPlacedAction(false));
        popupMenu.add(setPlacedMenu);

        JMenu setEnabledMenu = new JMenu(setEnabledAction);
        setEnabledMenu.add(new SetEnabledAction(true));
        setEnabledMenu.add(new SetEnabledAction(false));
        popupMenu.add(setEnabledMenu);

        JMenu setErrorHandlingMenu = new JMenu(setErrorHandlingAction);
        setErrorHandlingMenu.add(new SetErrorHandlingAction(ErrorHandling.Alert));
        setErrorHandlingMenu.add(new SetErrorHandlingAction(ErrorHandling.Defer));
        popupMenu.add(setErrorHandlingMenu);

        table.setComponentPopupMenu(popupMenu);

        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane, BorderLayout.CENTER);
        
        JPanel panel = new JPanel();
        add(panel, BorderLayout.NORTH);
        panel.setLayout(new BorderLayout(0, 0));
        JToolBar toolBarPlacements = new JToolBar();
        panel.add(toolBarPlacements);
        
        toolBarPlacements.setFloatable(false);
        JButton btnNewPlacement = new JButton(newAction);
        btnNewPlacement.setHideActionText(true);
        toolBarPlacements.add(btnNewPlacement);
        JButton btnRemovePlacement = new JButton(removeAction);
        btnRemovePlacement.setHideActionText(true);
        toolBarPlacements.add(btnRemovePlacement);
        toolBarPlacements.addSeparator();
        
        JButton btnPositionCameraPositionLocation = new JButton(moveCameraToPlacementLocation);
        btnPositionCameraPositionLocation.setHideActionText(true);
        toolBarPlacements.add(btnPositionCameraPositionLocation);
        
        JButton btnPositionCameraPositionNextLocation =
                new JButton(moveCameraToPlacementLocationNext);
        btnPositionCameraPositionNextLocation.setHideActionText(true);
        toolBarPlacements.add(btnPositionCameraPositionNextLocation);

        JButton btnPositionToolPositionLocation = new JButton(moveToolToPlacementLocation);
        btnPositionToolPositionLocation.setHideActionText(true);
        toolBarPlacements.add(btnPositionToolPositionLocation);

        toolBarPlacements.addSeparator();

        JButton btnCaptureCameraPlacementLocation = new JButton(captureCameraPlacementLocation);
        btnCaptureCameraPlacementLocation.setHideActionText(true);
        toolBarPlacements.add(btnCaptureCameraPlacementLocation);

        JButton btnCaptureToolPlacementLocation = new JButton(captureToolPlacementLocation);
        btnCaptureToolPlacementLocation.setHideActionText(true);
        toolBarPlacements.add(btnCaptureToolPlacementLocation);

        toolBarPlacements.addSeparator();

        JButton btnEditFeeder = new JButton(editPlacementFeederAction);
        btnEditFeeder.setHideActionText(true);
        toolBarPlacements.add(btnEditFeeder);

        JPanel panel_1 = new JPanel();
        panel.add(panel_1, BorderLayout.EAST);

        JLabel lblNewLabel = new JLabel("Search");
        panel_1.add(lblNewLabel);

        searchTextField = new JTextField();
        searchTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                search();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                search();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                search();
            }
        });
        panel_1.add(searchTextField);
        searchTextField.setColumns(15);
        
        Configuration.get().getBus().register(this);
    }
    
    @Subscribe
    public void boardOrPanelDefinitionStructureChanged(DefinitionStructureChangedEvent event) {
        Logger.trace("DefinitionStructureChangedEvent = " + event);
        if (event.source != this && boardOrPanelLocation != null && 
                boardOrPanelLocation.getPlacementsHolder() != null &&
                /*event.definition == boardOrPanelLocation.getPlacementsHolder().getDefinition() &&*/
                event.changedName.equals("placements")) {
            SwingUtilities.invokeLater(() -> {
                refresh();
            });
        }
    }

    @Subscribe
    public void placementSelected(PlacementSelectedEvent event) {
        if (event.source == this) {
            return;
        }
        
        SwingUtilities.invokeLater(() -> {
            selectPlacement(event.placement);
        });
    }

    private void search() {
        updateRowFilter();
    }
    
    public void refresh() {
        tableModel.fireTableDataChanged();
        updateActivePlacements();
    }

    public void refreshSelectedRow() {
        int index = table.convertRowIndexToModel(table.getSelectedRow());
        tableModel.fireTableRowsUpdated(index, index);
    }

    public void selectPlacement(Placement placement) {
        if (placement == null) {
            table.getSelectionModel().clearSelection();
            return;
        }
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getRowObjectAt(i).getDefinition() == placement) {
                int index = table.convertRowIndexToView(i);
                table.getSelectionModel().setSelectionInterval(index, index);
                table.scrollRectToVisible(new Rectangle(table.getCellRect(index, 0, true)));
                break;
            }
        }
    }
    
    // TODO STOPSHIP This is called all over the place and it's likely to rot - need to find
    // a listener or something it can use.
    public void updateActivePlacements() {
        Job job = jobPanel.getJob();
        int activePlacements = job.getActivePlacements(job.getRootPanelLocation());
        int totalActivePlacements = job.getTotalActivePlacements(job.getRootPanelLocation());
        int blActivePlacements = job.getActivePlacements(boardOrPanelLocation);
        int blTotalActivePlacements = job.getTotalActivePlacements(boardOrPanelLocation);

        MainFrame.get().setPlacementCompletionStatus(totalActivePlacements - activePlacements, 
                totalActivePlacements, 
                blTotalActivePlacements - blActivePlacements, 
                blTotalActivePlacements);
    }
    
    private void updateRowFilter() {
        List<RowFilter<PlacementsTableModel, Integer>> filters = new ArrayList<>();
        
        RowFilter<PlacementsTableModel, Integer> sideFilter = new RowFilter<PlacementsTableModel, Integer>() {
            public boolean include(Entry<? extends PlacementsTableModel, ? extends Integer> entry) {
                if (boardOrPanelLocation == null) {
                    return false;
                }
                PlacementsTableModel model = entry.getModel();
                Placement placement = model.getRowObjectAt(entry.getIdentifier());
                return placement.getSide() == boardOrPanelLocation.getGlobalSide();
            }
        };
        filters.add(sideFilter);
        
        try {
            RowFilter<PlacementsTableModel, Integer> searchFilter = RowFilter.regexFilter("(?i)" + searchTextField.getText().trim());
            filters.add(searchFilter);
        }
        catch (PatternSyntaxException e) {
        }
        
        tableSorter.setRowFilter(RowFilter.andFilter(filters));
    }
    
    
    public void setBoardOrPanelLocation(PlacementsHolderLocation<?> boardOrPanelLocation) {
        this.boardOrPanelLocation = boardOrPanelLocation;
        if (boardOrPanelLocation == null) {
            tableModel.setPlacementsHolderLocation(null, false);
            topLevelSingleInstanceActionGroup.setEnabled(false);
        }
        else {
            topLevel = boardOrPanelLocation != null && 
                    boardOrPanelLocation.getParent() == jobPanel.getJob().getRootPanelLocation();
            singleInstance = boardOrPanelLocation != null && 
                    1 == jobPanel.getJob().instanceCount(boardOrPanelLocation.getPlacementsHolder());
            tableModel.setPlacementsHolderLocation(boardOrPanelLocation, topLevel && singleInstance);
            topLevelSingleInstanceActionGroup.setEnabled(topLevel && singleInstance);

            updateRowFilter();
        }
        updateActivePlacements();
    }

    public Placement getSelection() {
        List<Placement> selectedPlacements = getSelections();
        if (selectedPlacements.isEmpty()) {
            return null;
        }
        return selectedPlacements.get(0);
    }

    public List<Placement> getSelections() {
        ArrayList<Placement> placements = new ArrayList<>();
        if (boardOrPanelLocation == null) {
            return placements;
        }
        int[] selectedRows = table.getSelectedRows();
        for (int selectedRow : selectedRows) {
            selectedRow = table.convertRowIndexToModel(selectedRow);
            placements.add(tableModel.getRowObjectAt(selectedRow));
        }
        return placements;
    }

    /**
     * @return the jobPanel
     */
    public JobPanel getJobPanel() {
        return jobPanel;
    }

    public final Action newAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.add);
            putValue(NAME, "New Placement");
            putValue(SHORT_DESCRIPTION, "Create a new placement and add it to the selected board or panel.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            if (Configuration.get().getParts().size() == 0) {
                MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                        "There are currently no parts defined in the system. Please create at least one part before creating a placement.");
                return;
            }

            String id = JOptionPane.showInputDialog(getTopLevelAncestor(),
                    "Please enter an ID for the new placement.");
            if (id == null) {
                return;
            }
            
            // Check if the new placement ID is unique
            for(Placement compareplacement : boardOrPanelLocation.getPlacementsHolder().getPlacements()) {
            	if (compareplacement.getId().equals(id)) {
            		MessageBoxes.errorBox(getTopLevelAncestor(), "Error",
                            "The ID for the new placement already exists");
                    return;
            	}
            }
            
            Placement placement = new Placement(id);

            placement.setPart(Configuration.get().getParts().get(0));
            placement.setLocation(new Location(Configuration.get().getSystemUnits()));
            placement.setSide(boardOrPanelLocation.getGlobalSide());

            if (boardOrPanelLocation instanceof PanelLocation) {
                placement.setType(Type.Fiducial);
            }
            boardOrPanelLocation.getPlacementsHolder().getDefinition().addPlacement(placement);
            Configuration.get().getBus()
                .post(new DefinitionStructureChangedEvent(boardOrPanelLocation.getPlacementsHolder().getDefinition(), "placements", JobPlacementsPanel.this));
            jobPanel.getJob().removePlaced(boardOrPanelLocation, placement.getId());
            tableModel.fireTableDataChanged();
            Helpers.selectLastTableRow(table);
            
            updateActivePlacements();
        }
    };

    public final Action removeAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.delete);
            putValue(NAME, "Remove Placement(s)");
            putValue(SHORT_DESCRIPTION, "Remove the currently selected placement(s).");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                boardOrPanelLocation.getPlacementsHolder().getDefinition().removePlacement((Placement) placement.getDefinition());
            }
            Configuration.get().getBus()
                .post(new DefinitionStructureChangedEvent(boardOrPanelLocation.getPlacementsHolder().getDefinition(), "placements", JobPlacementsPanel.this));
            tableModel.fireTableDataChanged();
            updateActivePlacements();
        }
    };

    public final Action moveCameraToPlacementLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerCamera);
            putValue(NAME, "Move Camera To Placement Location");
            putValue(SHORT_DESCRIPTION, "Position the camera at the placement's location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                Location location = Utils2D.calculateBoardPlacementLocation(boardOrPanelLocation,
                        getSelection().getLocation());

                Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                        .getDefaultCamera();
                MovableUtils.moveToLocationAtSafeZ(camera, location);
                MovableUtils.fireTargetedUserAction(camera);

                Map<String, Object> globals = new HashMap<>();
                globals.put("camera", camera);
                Configuration.get().getScripting().on("Camera.AfterPosition", globals);
            });
        }
    };
    public final Action moveCameraToPlacementLocationNext = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerCameraMoveNext);
            putValue(NAME, "Move Camera To Next Placement Location ");
            putValue(SHORT_DESCRIPTION,
                    "Position the camera at the next placements location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.submitUiMachineTask(() -> {
                // Need to keep current focus owner so that the space bar can be
                // used after the initial click. Otherwise, button focus is lost
                // when table is updated
                Component comp = MainFrame.get().getFocusOwner();
                Helpers.selectNextTableRow(table);
                comp.requestFocus();
                Location location = Utils2D.calculateBoardPlacementLocation(boardOrPanelLocation,
                        getSelection().getLocation());
                Camera camera = MainFrame.get().getMachineControls().getSelectedTool().getHead()
                        .getDefaultCamera();
                MovableUtils.moveToLocationAtSafeZ(camera, location);
                MovableUtils.fireTargetedUserAction(camera);

                Map<String, Object> globals = new HashMap<>();
                globals.put("camera", camera);
                Configuration.get().getScripting().on("Camera.AfterPosition", globals);
            });
        };
    };

    public final Action moveToolToPlacementLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.centerTool);
            putValue(NAME, "Move Tool To Placement Location");
            putValue(SHORT_DESCRIPTION, "Position the tool at the placement's location.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Location location = Utils2D.calculateBoardPlacementLocation(boardOrPanelLocation,
                    getSelection().getLocation());

            Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
            UiUtils.submitUiMachineTask(() -> {
                MovableUtils.moveToLocationAtSafeZ(nozzle, location);
                MovableUtils.fireTargetedUserAction(nozzle);
            });
        }
    };

    public final Action captureCameraPlacementLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.captureCamera);
            putValue(NAME, "Capture Camera Placement Location");
            putValue(SHORT_DESCRIPTION,
                    "Set the placement's location to the camera's current position.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                HeadMountable tool = MainFrame.get().getMachineControls().getSelectedTool();
                Camera camera = tool.getHead().getDefaultCamera();
                Location placementLocation = Utils2D.calculateBoardPlacementLocationInverse(
                        boardOrPanelLocation, camera.getLocation());
                getSelection().getDefinition().setLocation(placementLocation.derive(null, null, 0.0, null));
                table.repaint();
            });
        }
    };

    public final Action captureToolPlacementLocation = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.captureTool);
            putValue(NAME, "Capture Tool Placement Location");
            putValue(SHORT_DESCRIPTION,
                    "Set the placement's location to the tool's current position.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            UiUtils.messageBoxOnException(() -> {
                Nozzle nozzle = MainFrame.get().getMachineControls().getSelectedNozzle();
                Location placementLocation = Utils2D
                        .calculateBoardPlacementLocationInverse(boardOrPanelLocation, nozzle.getLocation());
                getSelection().getDefinition().setLocation(placementLocation.derive(null, null, 0.0, null));
                table.repaint();
            });
        }
    };

    public final Action editPlacementFeederAction = new AbstractAction() {
        {
            putValue(SMALL_ICON, Icons.editFeeder);
            putValue(NAME, "Edit Placement Feeder");
            putValue(SHORT_DESCRIPTION, "Edit the placement's associated feeder definition.");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            Placement placement = getSelection();
            MainFrame.get().getFeedersTab().showFeederForPart(placement.getPart());
        }
    };

    public final Action setTypeAction = new AbstractAction() {
        {
            putValue(NAME, "Set Type");
            putValue(SHORT_DESCRIPTION, "Set placement type(s) to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetTypeAction extends AbstractAction {
        final Placement.Type type;

        public SetTypeAction(Placement.Type type) {
            this.type = type;
            putValue(NAME, type.toString());
            putValue(SHORT_DESCRIPTION, "Set placement type(s) to " + type.toString());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setType(type);
                tableModel.fireTableDataChanged();
                updateActivePlacements();
            }
        }
    };

    public final Action setSideAction = new AbstractAction() {
        {
            putValue(NAME, "Set Side");
            putValue(SHORT_DESCRIPTION, "Set placement(s) side to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetSideAction extends AbstractAction {
        final Side side;

        public SetSideAction(Side side) {
            this.side = side;
            putValue(NAME, side.toString());
            putValue(SHORT_DESCRIPTION, "Set placement(s) side to " + side.toString());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setSide(side);
                tableModel.fireTableDataChanged();
                updateActivePlacements();
            }
        }
    };
    
    public final Action setErrorHandlingAction = new AbstractAction() {
        {
            putValue(NAME, "Set Error Handling");
            putValue(SHORT_DESCRIPTION, "Set placement(s) error handling to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetErrorHandlingAction extends AbstractAction {
        Placement.ErrorHandling errorHandling;

        public SetErrorHandlingAction(Placement.ErrorHandling errorHandling) {
            this.errorHandling = errorHandling;
            putValue(NAME, errorHandling.toString());
            putValue(SHORT_DESCRIPTION, "Set placement(s) error handling to " + errorHandling.toString());
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setErrorHandling(errorHandling);
                tableModel.fireTableDataChanged();
                updateActivePlacements();
            }
        }
    };
    
    public final Action setPlacedAction = new AbstractAction() {
        {
            putValue(NAME, "Set Placed");
            putValue(SHORT_DESCRIPTION, "Set placed to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };

    class SetPlacedAction extends AbstractAction {
        final Boolean placed;

        public SetPlacedAction(Boolean placed) {
            this.placed = placed;
            String name = placed ? "Placed" : "Not Placed";
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, "Set placed to " + name);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                jobPanel.getJob().setPlaced(boardOrPanelLocation, placement.getId(), placed);
                tableModel.fireTableDataChanged();   
                updateActivePlacements();
            }
        }
    };

    public final Action setEnabledAction = new AbstractAction() {
        {
            putValue(NAME, "Set Enabled");
            putValue(SHORT_DESCRIPTION, "Set placement(s) enabled to...");
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {}
    };
    private JTextField searchTextField;

    class SetEnabledAction extends AbstractAction {
        final Boolean enabled;

        public SetEnabledAction(Boolean enabled) {
            this.enabled = enabled;
            String name = enabled ? "Enabled" : "Disabled";
            putValue(NAME, name);
            putValue(SHORT_DESCRIPTION, "Set placement(s) enabled to " + name);
        }

        @Override
        public void actionPerformed(ActionEvent arg0) {
            for (Placement placement : getSelections()) {
                placement.setEnabled(enabled);
                tableModel.fireTableDataChanged();   
                updateActivePlacements();
            }
        }
    };

    static class TypeRenderer extends DefaultTableCellRenderer {
        @Override
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            Type type = (Type) value;
            setText(type.name());
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            Color alternateRowColor = UIManager.getColor("Table.alternateRowColor");
            if (value == Type.Fiducial) {
                c.setForeground(Color.black);
                c.setBackground(typeColorFiducial);
            } else if (isSelected) {
                c.setForeground(table.getSelectionForeground());
                c.setBackground(table.getSelectionBackground());
            } else {
                c.setForeground(table.getForeground());
                c.setBackground(row%2==0 ? table.getBackground() : alternateRowColor);
            }

            return c;
        }
    }

    static class StatusRenderer extends DefaultTableCellRenderer {
        public void setValue(Object value) {
            if (value == null) {
                return;
            }
            Status status = (Status) value; 
            if (status == Status.Ready) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(statusColorReady);
                setText("Ready");
            }
            else if (status == Status.MissingFeeder) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(statusColorError);
                setText("Missing Feeder");
            }
            else if (status == Status.ZeroPartHeight) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(statusColorWarning);
                setText("Part Height");
            }
            else if (status == Status.MissingPart) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(statusColorError);
                setText("Missing Part");
            }
            else if (status == Status.Disabled) {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(statusColorDisabled);
                setText("Disabled");
            }
            else {
                setBorder(new LineBorder(getBackground()));
                setForeground(Color.black);
                setBackground(statusColorError);
                setText(status.toString());
            }
        }
    }
}
