package korshak.com.screener.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import korshak.com.screener.vo.SignalTilt;
import korshak.com.screener.vo.Trade;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
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
          "PnL", "Duration (days)", "Return %","max possible loss %", "tilt open", "tilt close",
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

        if (trade.getOpen() instanceof SignalTilt) {
          SignalTilt signalTiltOpen = (SignalTilt) trade.getOpen();

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

  /**
   * Creates an Excel report from a map where keys become column names and values become cell values.
   *
   * @param filePath Path where the Excel file will be saved
   * @param sheetName Name for the worksheet
   * @param results Map where keys are column names and values are cell values
   * @throws IOException If there's an error writing the file
   */
  public static void reportForMap(String filePath, String sheetName, Map<String, String> results) throws IOException {
    if (results == null || results.isEmpty()) {
      throw new IllegalArgumentException("Results map cannot be empty");
    }

    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet(sheetName);

      // Create header row
      Row headerRow = sheet.createRow(0);
      CellStyle headerStyle = createHeaderStyle(workbook);

      // Create data row
      Row dataRow = sheet.createRow(1);
      CellStyle dataStyle = createDataStyle(workbook);

      int colNum = 0;
      for (Map.Entry<String, String> entry : results.entrySet()) {
        // Header cell
        Cell headerCell = headerRow.createCell(colNum);
        headerCell.setCellValue(entry.getKey());
        headerCell.setCellStyle(headerStyle);

        // Data cell
        Cell dataCell = dataRow.createCell(colNum);
        dataCell.setCellValue(entry.getValue());
        dataCell.setCellStyle(dataStyle);

        colNum++;
      }

      // Auto-size columns
      for (int i = 0; i < results.size(); i++) {
        sheet.autoSizeColumn(i);
      }

      // Write to file
      try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
        workbook.write(fileOut);
      }
    }
  }

  /**
   * Creates a standard data cell style
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