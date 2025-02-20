package korshak.com.screener.serviceImpl.chart;

import java.awt.event.ActionEvent;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

public class ChartMenuBar extends JMenuBar {
  private final ChartController controller;

  public ChartMenuBar(ChartController controller) {
    this.controller = controller;
    createMenus();
  }

  private void createMenus() {
    add(createViewMenu());
  }

  private JMenu createViewMenu() {
    JMenu viewMenu = new JMenu("View");

    // Zoom controls
    JMenuItem zoomInItem = new JMenuItem("Zoom In");
    zoomInItem.addActionListener((ActionEvent e) -> controller.zoomIn());

    JMenuItem zoomOutItem = new JMenuItem("Zoom Out");
    zoomOutItem.addActionListener((ActionEvent e) -> controller.zoomOut());

    JMenuItem resetZoomItem = new JMenuItem("Reset Zoom");
    resetZoomItem.addActionListener((ActionEvent e) -> controller.resetZoom());

    // Crosshair toggle
    JCheckBoxMenuItem toggleCrosshairItem = new JCheckBoxMenuItem("Show Crosshair", true);
    toggleCrosshairItem.addActionListener((ActionEvent e) ->
        controller.toggleCrosshair(toggleCrosshairItem.isSelected()));

    // Add items to View menu
    viewMenu.add(zoomInItem);
    viewMenu.add(zoomOutItem);
    viewMenu.add(resetZoomItem);
    viewMenu.addSeparator();
    viewMenu.add(toggleCrosshairItem);

    return viewMenu;
  }
}