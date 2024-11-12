package korshak.com.screener.serviceImpl.chart;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.ChartPanel;

public class CrosshairOverlayPanel extends JPanel {
  private final ChartPanel chartPanel;
  private Point currentPoint;
  private String valueLabel = "";
  private String dateLabel = "";
  private boolean enabled = true;

  public CrosshairOverlayPanel(ChartPanel chartPanel) {
    this.chartPanel = chartPanel;
    setOpaque(false);
  }

  @Override
  protected void paintComponent(Graphics g) {
    if (!enabled || currentPoint == null) return;

    Point chartLocation = chartPanel.getLocationOnScreen();
    Point overlayLocation = getLocationOnScreen();
    int xOffset = chartLocation.x - overlayLocation.x;
    int yOffset = chartLocation.y - overlayLocation.y;

    Graphics2D g2 = (Graphics2D) g;
    Rectangle2D dataArea = chartPanel.getScreenDataArea();
    if (dataArea == null || !dataArea.contains(
        new Point(currentPoint.x - xOffset, currentPoint.y - yOffset))) return;

    // Translate graphics to match chart position
    g2.translate(xOffset, yOffset);

    // Enable anti-aliasing
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    // Draw crosshair lines
    g2.setColor(new Color(100, 100, 100, 180));
    g2.setStroke(new BasicStroke(1.0f));

    // Draw lines
    g2.drawLine(currentPoint.x, (int)dataArea.getMinY(),
        currentPoint.x, (int)dataArea.getMaxY());
    g2.drawLine((int)dataArea.getMinX(), currentPoint.y,
        (int)dataArea.getMaxX(), currentPoint.y);

    // Draw labels
    drawLabels(g2, dataArea);

    // Reset translation
    g2.translate(-xOffset, -yOffset);
  }

  private void drawLabels(Graphics2D g2, Rectangle2D dataArea) {
    g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
    FontMetrics fm = g2.getFontMetrics();

    int labelPadding = 2;
    int dateWidth = fm.stringWidth(dateLabel);
    int dateHeight = fm.getHeight();

    // Date label
    g2.setColor(new Color(255, 255, 255, 220));
    g2.fillRect(currentPoint.x - dateWidth/2 - labelPadding,
        (int)dataArea.getMaxY() + labelPadding,
        dateWidth + 2*labelPadding,
        dateHeight);

    g2.setColor(Color.BLACK);
    g2.drawString(dateLabel,
        currentPoint.x - dateWidth/2,
        (int)dataArea.getMaxY() + labelPadding + fm.getAscent());

    if (!valueLabel.isEmpty()) {
      int valueWidth = fm.stringWidth(valueLabel);

      g2.setColor(new Color(255, 255, 255, 220));
      g2.fillRect((int)dataArea.getMaxX() - valueWidth - 2*labelPadding,
          currentPoint.y - dateHeight/2 - labelPadding,
          valueWidth + 2*labelPadding,
          dateHeight + 2*labelPadding);

      g2.setColor(Color.BLACK);
      g2.drawString(valueLabel,
          (int)dataArea.getMaxX() - valueWidth - labelPadding,
          currentPoint.y + fm.getAscent()/2);
    }
  }

  public void updateLabels(Point point, String date, String value) {
    this.currentPoint = point;
    this.dateLabel = date;
    this.valueLabel = value;
    repaint();
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
    if (!enabled) {
      currentPoint = null;
      valueLabel = "";
      dateLabel = "";
    }
    repaint();
  }
}