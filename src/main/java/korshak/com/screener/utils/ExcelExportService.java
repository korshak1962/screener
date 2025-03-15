package korshak.com.screener.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import korshak.com.screener.vo.SignalTilt;
import korshak.com.screener.vo.Trade;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class ExcelExportService {
  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private ExcelExportService() {
  } // Prevent instantiation

  public static void exportTradesToExcel(List<Trade> trades, String filePath) throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Trades");

      Row headerRow = sheet.createRow(0);
      String[] headers = {
          "Open Date", "Open Price", "Close Date", "Close Price",
          "PnL", "Duration (days)", "Return %", "max possible loss %", "tilt open", "tilt close",
          "trend tilt open", "trend tilt close"
      };

      CellStyle headerStyle = createHeaderStyle(workbook);
      for (int i = 0; i < headers.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      CellStyle dateStyle = createDateStyle(workbook);
      CellStyle numberStyle = createNumberStyle(workbook);
      CellStyle percentStyle = createPercentStyle(workbook);

      int rowNum = 1;
      for (Trade trade : trades) {
        Row row = sheet.createRow(rowNum++);

        Cell openDateCell = row.createCell(0);
        openDateCell.setCellValue(trade.getOpen().getDate().format(DATE_FORMATTER));
        openDateCell.setCellStyle(dateStyle);

        Cell openPriceCell = row.createCell(1);
        openPriceCell.setCellValue(trade.getOpen().getPrice());
        openPriceCell.setCellStyle(numberStyle);

        Cell closeDateCell = row.createCell(2);
        closeDateCell.setCellValue(trade.getClose().getDate().format(DATE_FORMATTER));
        closeDateCell.setCellStyle(dateStyle);

        Cell closePriceCell = row.createCell(3);
        closePriceCell.setCellValue(trade.getClose().getPrice());
        closePriceCell.setCellStyle(numberStyle);

        Cell pnlCell = row.createCell(4);
        pnlCell.setCellValue(trade.getPnl());
        pnlCell.setCellStyle(numberStyle);

        Cell durationCell = row.createCell(5);
        long days = java.time.temporal.ChronoUnit.DAYS.between(
            trade.getOpen().getDate(), trade.getClose().getDate()
        );
        durationCell.setCellValue(days);
        durationCell.setCellStyle(numberStyle);

        Cell returnCell = row.createCell(6);
        double returnPercent = trade.getPnl() / trade.getOpen().getPrice();
        returnCell.setCellValue(returnPercent);
        returnCell.setCellStyle(percentStyle);

        Cell posLossCell = row.createCell(7);
        posLossCell.setCellValue(trade.getMaxPainPercent());
        posLossCell.setCellStyle(numberStyle);

        if (trade.getOpen() instanceof SignalTilt signalTiltOpen) {

          Cell tiltOpenCell = row.createCell(7);
          double tiltOpenValue = signalTiltOpen.getTilt();
          tiltOpenCell.setCellValue(tiltOpenValue);
          tiltOpenCell.setCellStyle(numberStyle);

          SignalTilt signalTiltClose = (SignalTilt) trade.getClose();
          Cell tiltCloseCell = row.createCell(8);
          double tiltCloseValue = signalTiltClose.getTilt();
          tiltCloseCell.setCellValue(tiltCloseValue);
          tiltCloseCell.setCellStyle(numberStyle);

          Cell tiltTrendOpenCell = row.createCell(9);
          double tiltTrendOpenValue = signalTiltOpen.getTrendTilt();
          tiltTrendOpenCell.setCellValue(tiltTrendOpenValue);
          tiltTrendOpenCell.setCellStyle(numberStyle);


          Cell tiltTrendCloseCell = row.createCell(10);
          double tiltTrendCloseValue = signalTiltClose.getTrendTilt();
          tiltTrendCloseCell.setCellValue(tiltTrendCloseValue);
          tiltTrendCloseCell.setCellStyle(numberStyle);
        }
      }

      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
        workbook.write(fileOut);
      }
    }
  }

  private static CellStyle createHeaderStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    return style;
  }

  private static CellStyle createDateStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
    return style;
  }

  private static CellStyle createNumberStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
    return style;
  }

  private static CellStyle createPercentStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setDataFormat(workbook.createDataFormat().getFormat("0.00%"));
    return style;
  }

  public static void reportForMap(String filePath, String sheetName,
                                  Map<String, List<String>> colNameToValues, Set<String> urlColumns)
      throws IOException {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet(sheetName);
      CreationHelper createHelper = workbook.getCreationHelper();

      // Get column names and find indices for CLOSE and priceToSell
      String[] columnNames = colNameToValues.keySet().toArray(new String[0]);
      int closeIndex = -1;
      int priceToSellIndex = -1;

      for (int i = 0; i < columnNames.length; i++) {
        if ("CLOSE".equals(columnNames[i])) {
          closeIndex = i;
        } else if ("priceToSell".equals(columnNames[i])) {
          priceToSellIndex = i;
        }
      }

      // Create basic styles
      CellStyle headerStyle = createHeaderStyle(workbook);

      // Create normal and red fonts
      Font normalFont = workbook.createFont();
      normalFont.setFontName("Arial");

      Font redFont = workbook.createFont();
      redFont.setFontName("Arial");
      redFont.setColor(IndexedColors.RED.getIndex());

      Font urlFont = workbook.createFont();
      urlFont.setUnderline(Font.U_SINGLE);
      urlFont.setColor(IndexedColors.BLUE.getIndex());

      Font redUrlFont = workbook.createFont();
      redUrlFont.setUnderline(Font.U_SINGLE);
      redUrlFont.setColor(IndexedColors.RED.getIndex());

      // Create header row
      Row headerRow = sheet.createRow(0);
      for (int i = 0; i < columnNames.length; i++) {
        Cell cell = headerRow.createCell(i);
        cell.setCellValue(columnNames[i]);
        cell.setCellStyle(headerStyle);
      }

      // Get maximum row count
      int maxRows = 0;
      for (List<String> values : colNameToValues.values()) {
        maxRows = Math.max(maxRows, values.size());
      }

      // Process data rows
      for (int rowIdx = 0; rowIdx < maxRows; rowIdx++) {
        Row row = sheet.createRow(rowIdx + 1);

        // Check if this row should be highlighted
        boolean useRedFont = false;
        if (closeIndex >= 0 && priceToSellIndex >= 0) {
          List<String> closeValues = colNameToValues.get(columnNames[closeIndex]);
          List<String> priceToSellValues = colNameToValues.get(columnNames[priceToSellIndex]);

          if (rowIdx < closeValues.size() && rowIdx < priceToSellValues.size()) {
            try {
              // Clean up strings and parse as doubles for numeric comparison
              String closeStr = closeValues.get(rowIdx).replace(",", "").trim();
              String priceToSellStr = priceToSellValues.get(rowIdx).replace(",", "").trim();

              double closeValue = Double.parseDouble(closeStr);
              double priceToSellValue = Double.parseDouble(priceToSellStr);

              if (closeValue < priceToSellValue) {
                useRedFont = true;
              }
            } catch (NumberFormatException e) {
              // Continue without highlighting if numbers can't be parsed
            }
          }
        }

        // Create cells for this row
        for (int colIdx = 0; colIdx < columnNames.length; colIdx++) {
          Cell cell = row.createCell(colIdx);
          String columnName = columnNames[colIdx];
          List<String> columnValues = colNameToValues.get(columnName);

          if (rowIdx < columnValues.size()) {
            String value = columnValues.get(rowIdx);
            cell.setCellValue(value);

            // Basic cell style
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setBorderBottom(BorderStyle.THIN);
            cellStyle.setBorderLeft(BorderStyle.THIN);
            cellStyle.setBorderRight(BorderStyle.THIN);
            cellStyle.setBorderTop(BorderStyle.THIN);

            // Apply appropriate font based on need for red highlight and cell type
            if (urlColumns != null && urlColumns.contains(columnName)) {
              // URL styling
              Hyperlink link = createHelper.createHyperlink(HyperlinkType.URL);
              link.setAddress(value);
              cell.setHyperlink(link);
              cellStyle.setFont(useRedFont ? redUrlFont : urlFont);
            } else {
              // Normal cell styling
              cellStyle.setFont(useRedFont ? redFont : normalFont);
            }

            cell.setCellStyle(cellStyle);
          }
        }
      }

      // Auto-size columns
      for (int i = 0; i < columnNames.length; i++) {
        sheet.autoSizeColumn(i);
      }

      // Write to file
      try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
        workbook.write(fileOut);
      }
    }
  }

  private static CellStyle createUrlStyle(Workbook workbook) {
    CellStyle linkStyle = workbook.createCellStyle();
    Font linkFont = workbook.createFont();
    linkFont.setUnderline(Font.U_SINGLE);
    linkFont.setColor(IndexedColors.BLUE.getIndex());
    linkStyle.setFont(linkFont);
    return linkStyle;
  }

  private static void setUrlCell(Workbook workbook, Cell cell, String url) {
    CreationHelper createHelper = workbook.getCreationHelper();
    Hyperlink link = createHelper.createHyperlink(HyperlinkType.URL);
    link.setAddress(url);

    cell.setCellValue(url);
    cell.setHyperlink(link);
    cell.setCellStyle(createUrlStyle(workbook));
  }

  /**
   * Creates a standard data cell style
   *
   * @param workbook The workbook to create the style in
   * @return CellStyle configured for data cells
   */
  private static CellStyle createDataStyle(Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
    style.setBorderTop(BorderStyle.THIN);
    style.setAlignment(HorizontalAlignment.LEFT);
    style.setVerticalAlignment(VerticalAlignment.CENTER);

    Font font = workbook.createFont();
    font.setFontName("Arial");
    font.setFontHeightInPoints((short) 11);
    style.setFont(font);

    return style;
  }
}