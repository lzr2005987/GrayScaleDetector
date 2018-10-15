package github.lzr.com.noisedetector.Util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created by Administrator on 2018/1/24 0024.
 */

public class BitmapUtil {

    public static Bitmap doBlur(Bitmap sentBitmap, int radius, boolean canReuseInBitmap) {
        Bitmap bitmap;
        if (canReuseInBitmap) {
            bitmap = sentBitmap;
        } else {
            bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);
        }

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }
        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = (0xff000000 & pix[yi]) | (dv[rsum] << 16) | (dv[gsum] << 8) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return (bitmap);
    }

    ///结构元素
    private static int sData[] = {
            0, 0, 0,
            0, 1, 0,
            0, 1, 1
    };

    /**
     * 腐蚀运算
     *
     * @param threshold 当灰度值大于阈值（小于阈值）时并且结构元素为1（0）时，才认为对应位置匹配上；
     * @return
     */
    public static Bitmap correde(Bitmap bm, int threshold) {
        int width = bm.getWidth();
        int height = bm.getHeight();
        int source[][] = new int[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                ///边缘不进行操作，边缘内才操作
                if (i > 0 && j > 0 && i < height - 1 && j < width - 1) {
                    int max = 0;

                    ///对结构元素进行遍历
                    for (int k = 0; k < sData.length; k++) {
                        int x = k / 3;///商表示x偏移量
                        int y = k % 3;///余数表示y偏移量

                        if (sData[k] != 0) {
                            ///不为0时，必须全部大于阈值，否则就设置为0并结束遍历
                            if (source[i - 1 + x][j - 1 + y] >= threshold) {
                                if (source[i - 1 + x][j - 1 + y] > max) {
                                    max = source[i - 1 + x][j - 1 + y];
                                }
                            } else {
                                ////与结构元素不匹配,赋值0,结束遍历
                                max = 0;
                                break;
                            }
                        }
                    }

                    ////此处可以设置阈值，当max小于阈值的时候就赋为0
                    bm.setPixel(i, j, max);

                } else {
                    ///直接赋值
                    bm.setPixel(i, j, source[i][j]);

                }///end of the most out if-else clause .

            }
        }///end of outer for clause

        return bm;
    }

/*    public static Bitmap correde(Bitmap bm) {
        ArrayList<Integer> pixel = new ArrayList<>();
        int width = bm.getWidth();
        int height = bm.getHeight();
        for (int i = 1; i < width - 1; i++)
            for (int j = 1; j < height - 1; j++) {
                pixel.add(bm.getPixel(i - 1, j - 1));
                pixel.add(bm.getPixel(i - 1, j));
                pixel.add(bm.getPixel(i - 1, j + 1));
                pixel.add(bm.getPixel(i, j - 1));
                pixel.add(bm.getPixel(i, j - 1));
                pixel.add(bm.getPixel(i, j + 1));
                pixel.add(bm.getPixel(i + 1, j - 1));
                pixel.add(bm.getPixel(i + 1, j));
                pixel.add(bm.getPixel(i + 1, j + 1));
                bm.setPixel(i, j, Collections.min(pixel));
            }
        return bm;
    }*/

    /**
     * opencv腐蚀方法
     *
     * @param bm
     * @return
     */
    public static Bitmap erode(Bitmap bm) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bm, mat);
        Mat kernelErode = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_ELLIPSE, new Size(3, 3));
        Imgproc.erode(mat, mat, kernelErode);
        Utils.matToBitmap(mat, bm);
        return bm;
    }

    public static Bitmap compressBitmap(File file) {
        try {

            // BitmapFactory options to downsize the image
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            o.inSampleSize = 6;
            // factor of downsizing the image

            FileInputStream inputStream = new FileInputStream(file);
            //Bitmap selectedBitmap = null;
            BitmapFactory.decodeStream(inputStream, null, o);
            inputStream.close();

            // The new size we want to scale to
            final int REQUIRED_SIZE = 75;

            // Find the correct scale value. It should be the power of 2.
            int scale = 1;
            while (o.outWidth / scale / 2 >= REQUIRED_SIZE &&
                    o.outHeight / scale / 2 >= REQUIRED_SIZE) {
                scale *= 2;
            }

            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            inputStream = new FileInputStream(file);

            Bitmap selectedBitmap = BitmapFactory.decodeStream(inputStream, null, o2);
            inputStream.close();

            // here i override the original image file
//            file.createNewFile();
//
//
//            FileOutputStream outputStream = new FileOutputStream(file);
//
//            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100 , outputStream);


            //File aa = new File(newpath);

            //FileOutputStream outputStream = new FileOutputStream(aa);

            //choose another format if PNG doesn't suit you

            //selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);


            //String filepath = aa.getAbsolutePath();
            //Log.e("getAbsolutePath", aa.getAbsolutePath());

            return selectedBitmap;
        } catch (Exception e) {
            return null;
        }
    }
}
