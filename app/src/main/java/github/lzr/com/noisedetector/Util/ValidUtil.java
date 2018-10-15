package github.lzr.com.noisedetector.Util;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import static github.lzr.com.noisedetector.Constant.CANCEL;
import static github.lzr.com.noisedetector.Constant.CONFIRM;
import static github.lzr.com.noisedetector.Constant.ERROR;
import static github.lzr.com.noisedetector.Constant.MODE_COLOR;
import static github.lzr.com.noisedetector.Constant.MODE_GRAY;
import static github.lzr.com.noisedetector.Constant.MODE_UNKNOW;
import static github.lzr.com.noisedetector.Constant.RANGE;

/**
 * Created by Administrator on 2018/1/24 0024.
 */

public class ValidUtil {
    public static int threshold = 0;
    public static String mode = MODE_UNKNOW;
    public static boolean isEroded = false;


    public static boolean isLightPixel(Bitmap bm, int pixel) {
        int red = Color.red(pixel);
        int green = Color.green(pixel);
        int blue = Color.blue(pixel);
        int max, min;

        //先检测是灰度图还是彩色图
        if (mode.equals(MODE_UNKNOW)) {
            mode = checkMode(bm);
            Log.d("lizheren", "mode: " + mode);
        }

        if (mode.equals(MODE_GRAY)) {
            if (threshold == 0) {
                Bitmap grayBm = convertGrayImg(bm);
                threshold = OTSUThreshold(grayBm);
                Log.d("lizheren", "isLightPixel: " + threshold);
            }
            if (red > threshold && green > threshold && blue > threshold) return true;
        } else if (mode.equals(MODE_COLOR)) {
            if (!isEroded) {
                BitmapUtil.erode(bm);
                isEroded = true;
            }
            max = red > green ? (red > blue ? red : blue) : (green > blue ? green : blue);
            min = red < green ? (red < blue ? red : blue) : (green < blue ? green : blue);
            if (max - min > 40) return true;
        }
        return false;
    }

    private static String checkMode(Bitmap bm) {
        int pixel, red, green, blue, max, min;
        for (int i = 0; i < bm.getWidth(); i++)
            for (int j = 0; j < bm.getHeight(); j++) {
                pixel = bm.getPixel(i, j);
                red = Color.red(pixel);
                green = Color.green(pixel);
                blue = Color.blue(pixel);
                max = red > green ? (red > blue ? red : blue) : (green > blue ? green : blue);
                min = red < green ? (red < blue ? red : blue) : (green < blue ? green : blue);
                if (max - min > 50) return MODE_COLOR;
            }
        return MODE_GRAY;
    }

    private static Bitmap convertGrayImg(Bitmap img) {
        int width = img.getWidth();         //获取位图的宽
        int height = img.getHeight();       //获取位图的高

        int[] pixels = new int[width * height]; //通过位图的大小创建像素点数组

        img.getPixels(pixels, 0, width, 0, 0, width, height);
        int alpha = 0xFF << 24;
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int grey = pixels[width * i + j];

                int red = ((grey & 0x00FF0000) >> 16);
                int green = ((grey & 0x0000FF00) >> 8);
                int blue = (grey & 0x000000FF);

                grey = (int) ((float) red * 0.3 + (float) green * 0.59 + (float) blue * 0.11);
                grey = alpha | (grey << 16) | (grey << 8) | grey;
                pixels[width * i + j] = grey;
            }
        }
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        result.setPixels(pixels, 0, width, 0, 0, width, height);
        return result;
    }

    //OTSU大津法自适应阈值
    private static int OTSUThreshold(Bitmap bm) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int histogram[] = new int[256];
        int graySum = 0;
        int ftNum = 0;
        int bgNum = 0;
        int ftSum = 0;
        int bgSum = 0;
        double w0, w1, u0, u1, g, temp = 0;
        int T = 0;
        int totalPixel = bm.getWidth() * bm.getHeight();
        //灰度直方图
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int red = Color.red(bm.getPixel(i, j));
                int green = Color.green(bm.getPixel(i, j));
                int blue = Color.blue(bm.getPixel(i, j));
                histogram[(red + green + blue) / 3]++;
            }
        }
        //求总灰度值
        for (int i = 0; i < 256; i++) {
            graySum += histogram[i] * i;
        }

        for (int i = 0; i < 256; i++) {
            ftNum += histogram[i];  //阈值为i时前景个数
            bgNum = totalPixel - ftNum;      //阈值为i时背景个数
            w0 = (double) ftNum / totalPixel; //前景像素占总数比
            w1 = (double) bgNum / totalPixel; //背景像素占总数比
            if (ftNum == 0) continue;
            if (bgNum == 0) break;
            //前景平均灰度
            ftSum += i * histogram[i];
            u0 = ftSum / ftNum;

            //背景平均灰度
            bgSum = graySum - ftSum;
            u1 = bgSum / bgNum;

            g = w0 * w1 * (u0 - u1) * (u0 - u1);
            if (g > temp) {
                temp = g;
                T = i;
            }
        }

        return T;
    }

    /**
     * confirm：确认亮点
     * error：重复亮点
     * cancel：边缘亮点
     *
     * @param pixel
     * @param x
     * @param y
     * @param height
     * @return
     */
    public static int isValidatePixel(boolean[][] pixel, int x, int y, int height) {
        int rate = 2;
        if (x < RANGE || y < RANGE) return CANCEL;
        if (x + RANGE > height) return CANCEL;
        if (x < rate * RANGE || x + rate * RANGE > height) rate = 1;
        // if (x + rate * RANGE > height) return CANCEL;
        for (int i = 0; i < RANGE; i++)
            for (int j = 0; j < RANGE; j++) {
                for (int k = 0; k < rate * i; k++)
                    if (pixel[x - k][y - j]) return ERROR;
                for (int k = 0; k < rate * i; k++)
                    if (pixel[x + k][y - j]) return ERROR;
            }
        //Log.d("lizheren", "i: " + x + " j:" + y);
        return CONFIRM;
    }
}
