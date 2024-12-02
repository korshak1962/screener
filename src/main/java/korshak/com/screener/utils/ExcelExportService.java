package korshak.com.screener.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import korshak.com.screener.vo.SignalTilt;
import korshak.com.screener.vo.Trade;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
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
          "PnL", "Duration (days)", "Return %", "tilt open", "tilt close"
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
}