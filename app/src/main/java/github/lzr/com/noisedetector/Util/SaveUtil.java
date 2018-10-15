package github.lzr.com.noisedetector.Util;

import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;
import jxl.write.Label;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;

/**
 * Created by Administrator on 2018/2/10 0010.
 */

public class SaveUtil {
    private static final String FILE_PATH = Environment.getExternalStorageDirectory() + "/light/";

    public static void saveDataToExcel(ArrayList<Integer> sizeList, ArrayList<Integer> lightscaleList) throws IOException, WriteException {
        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        String formatTime = df.format(new Date());

        File fileDir = new File(FILE_PATH);
        if (!fileDir.exists()) fileDir.mkdirs();
        File excelFile = new File(FILE_PATH, formatTime + ".xls");
        if (!excelFile.exists()) excelFile.createNewFile();

        Workbook readWorkbook = null;
        int startRows = 0;
        boolean isExist = false;
        try {
            readWorkbook = Workbook.getWorkbook(new File(FILE_PATH, formatTime + ".xls"));
            Sheet sheet0 = readWorkbook.getSheet(0);
            startRows = sheet0.getRows();
            if (startRows != 0) startRows++;
            isExist = true;
        } catch (BiffException e) {
            e.printStackTrace();
            isExist = false;
        }

        WritableWorkbook writeBook = null;
        WritableSheet sheet = null;
        // 1、创建工作簿(WritableWorkbook)对象，打开excel文件，若文件不存在，则创建文件
        // 2、新建工作表(sheet)对象，并声明其属于第几页
        if (!isExist) {
            writeBook = Workbook.createWorkbook(new File(FILE_PATH, formatTime + ".xls"));
            sheet = writeBook.createSheet("DataSheet", 0);// 第一个参数为工作簿的名称，第二个参数为页数
        } else {
            writeBook = Workbook.createWorkbook(new File(FILE_PATH, formatTime + ".xls"), readWorkbook);
            sheet = writeBook.getSheet(0);
        }

        // 3、创建单元格(Label)对象，
        //Label label1 = new Label(startRows + 1, 0, "name");// 第一个参数指定单元格的列数、第二个参数指定单元格的行数，第三个指定写的字符串内容

        sheet.addCell(new Label(0, startRows, "size"));
        sheet.addCell(new Label(1, startRows, "brightness"));
        for (int i = 0; i < sizeList.size(); i++) {
            sheet.addCell(new Label(0, startRows + i + 1, sizeList.get(i).toString()));
            sheet.addCell(new Label(1, startRows + i + 1, lightscaleList.get(i).toString()));
        }

        // 4、打开流，开始写文件
        writeBook.write();

        // 5、关闭流
        writeBook.close();
    }
}
