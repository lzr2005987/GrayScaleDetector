package github.lzr.com.noisedetector;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import github.lzr.com.noisedetector.Util.BitmapUtil;
import github.lzr.com.noisedetector.Util.DensityUtil;
import github.lzr.com.noisedetector.Util.SaveUtil;
import github.lzr.com.noisedetector.Util.ValidUtil;
import jxl.write.WriteException;

import static github.lzr.com.noisedetector.Constant.MODE_UNKNOW;

public class MainActivity extends Activity {

    private ImageView pic;
    private int times = 0;
    private TextView tvResult;
    private int size = 0;
    private int lightScale = 0;
    private Bitmap bm;
    private Button btReport;
    private ArrayList<Integer> sizeList = new ArrayList<>();
    private ArrayList<Integer> lightScaleList = new ArrayList<>();

    static {
        if (!OpenCVLoader.initDebug()) { // Handle initialization error }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pic = (ImageView) findViewById(R.id.iv_pic);
        tvResult = (TextView) findViewById(R.id.tv_total);
        btReport = (Button) findViewById(R.id.bt_report);
        btReport.setOnClickListener(new BtReportListener());
        checkPermission();
    }

    class BtReportListener implements View.OnClickListener {

        @Override
        public void onClick(View view) {
            Intent intent = new Intent(MainActivity.this, ReportActivity.class);
            intent.putExtra("sizeList", sizeList);
            intent.putExtra("lightScaleList", lightScaleList);
            startActivity(intent);
        }
    }

    public void change(View view) {
        times++;
        times = times % 5;
        int res = 0;
        switch (times) {
            case 0:
                res = R.drawable.pic1;
                break;
            case 1:
                res = R.drawable.pic2;
                break;
            case 2:
                res = R.drawable.pic3modify;
                break;
            case 3:
                res = R.drawable.pic4modify;
                break;
            case 4:
                res = R.drawable.pic;
                break;
        }
        pic.setImageResource(res);
        bm = ((BitmapDrawable) pic.getDrawable()).getBitmap();
        tvResult.setText("");
        reset();
    }

/*    //亮度计算公式,暂时不用，用hsv颜色空间的v表示亮度
    int red = Color.red(copy.getPixel(i, j));
    int green = Color.green(copy.getPixel(i, j));
    int blue = Color.blue(copy.getPixel(i, j));
    lightScale += red * 0.30 + green * 0.59 + blue * 0.11;*/

    public void detect(View view) {
        bm = ((BitmapDrawable) pic.getDrawable()).getBitmap();
        int width = bm.getWidth();
        int height = bm.getHeight();
        Bitmap copy = bm.copy(Bitmap.Config.ARGB_8888, true);
        //高斯模糊
        //copy = BitmapUtil.doBlur(copy, 1, true);
        boolean[][] lightPixel = new boolean[height][width];
        Log.d("lizheren", "detector: " + width + " " + height);
        int white = 0;
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++) {
                boolean isLightPixel = ValidUtil.isLightPixel(copy, copy.getPixel(i, j));

                if (isLightPixel) {
                    //判断有没有重复计算
                    // 返回confirm代表确定没有重复计算
                    // 返回error代表确定已经重复计算，需舍弃
                    // 返回cancel代表被检测的像素在图像边缘，不能确定附近有没有重复计算
/*                    int isValid = ValidUtil.isValidatePixel(lightPixel, j, i, height);
                    //填充颜色
                    copy.setPixel(i, j, Color.parseColor("#0000ff"));
                    if (isValid == CONFIRM) {
                        copy.setPixel(i, j, Color.parseColor("#ff0000"));
                        white++;
                    }

                    if (isValid != CANCEL) lightPixel[j][i] = true;*/

                    //统计亮点个数的工作放在之后的doNext方法中
                    copy.setPixel(i, j, Color.parseColor("#0000ff"));
                    lightPixel[j][i] = true;
                } else {
                    copy.setPixel(i, j, Color.parseColor("#000000"));
                }
            }
        pic.setImageBitmap(copy);
        //tvResult.setText("count=" + white);
        resetConfig();

        //统计连通区域个数、平均亮度和每个连通区域的大小
        doNext(copy);
    }

    private void doNext(Bitmap copy) {
        int width = copy.getWidth();
        int height = copy.getHeight();
        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                if (Color.blue(copy.getPixel(i, j)) == 255) {
                    parsePoint(copy, i, j);
                    lightScale = lightScale / size;
                    sizeList.add(size);
                    lightScaleList.add(lightScale);
                    Log.d("lizheren", "size: " + size + "  lightscale:" + lightScale);
                    size = 0;
                    lightScale = 0;
                }
        //计算完毕，sizeList存放每个点的大小，lightscaleList存放每个点的平均明度
        btReport.setEnabled(true);
        adjustPoint();
        pic.setImageBitmap(copy);
        try {
            SaveUtil.saveDataToExcel(sizeList, lightScaleList);
        } catch (IOException e) {
            Log.d("lizheren", "io: ");
            e.printStackTrace();
        } catch (WriteException e) {
            Log.d("lizheren", "write: ");
            e.printStackTrace();
        }
        tvResult.setText("count=" + sizeList.size());
    }

    /**
     * 尽可能去除异常的点
     */
    private void adjustPoint() {
        int avg = 0;
        int avgTemp = 0;
        int removeCount = 0;
        //平均值
        for (int i = 0; i < sizeList.size(); i++)
            avg += sizeList.get(i);
        avg = avg / sizeList.size();

        Log.d("lizheren", "adjustPoint: " + avg);

        //平均每个点的离散度
        for (int i = 0; i < sizeList.size(); i++)
            avgTemp += Math.abs(sizeList.get(i) - avg);
        avgTemp = avgTemp / sizeList.size();

        Log.d("lizheren", "adjustPoint1: " + avgTemp);
        //离散度大于平均离散度5倍的点舍去
        for (int i = 0; i < sizeList.size(); i++)
            if (Math.abs(sizeList.get(i) - avg) > 5 * avgTemp) {
                Log.d("lizheren", "i: " + i + "  size:" + sizeList.get(i));
                sizeList.remove(i);
                lightScaleList.remove(i);
                removeCount++;

            }
        Log.d("lizheren", "remove: " + removeCount);

    }

    private void parsePoint(Bitmap copy, int i, int j) {
        if (i < 0 || j < 0 || i >= copy.getWidth() || j >= copy.getHeight()) return;
        if (Color.blue(copy.getPixel(i, j)) == 255 || Color.red(copy.getPixel(i, j)) == 255) {
            size++;
            float[] hsv = new float[3];
            Color.RGBToHSV(Color.red(bm.getPixel(i, j)),
                    Color.green(bm.getPixel(i, j)),
                    Color.blue(bm.getPixel(i, j)),
                    hsv);
/*            lightScale += Color.red(bm.getPixel(i, j)) * 0.30 +
                    Color.green(bm.getPixel(i, j)) * 0.59
                    + Color.blue(bm.getPixel(i, j)) * 0.11;*/
            lightScale += (int) (hsv[2] * 10000);
            copy.setPixel(i, j, Color.parseColor("#00ff00"));
            parsePoint(copy, i + 1, j);
            parsePoint(copy, i, j + 1);
            parsePoint(copy, i + 1, j + 1);
            parsePoint(copy, i + 1, j - 1);
            parsePoint(copy, i - 1, j);
            parsePoint(copy, i, j - 1);
            parsePoint(copy, i - 1, j - 1);
            parsePoint(copy, i - 1, j + 1);
        }
    }

    public void clear(View view) {
        reset();
    }

    public void take(View view) {
        startActivityForResult(new Intent(this, CaptureActivity.class), 0);
        //openTakePhoto();
    }

    private void openTakePhoto() {
        /**
         * 在启动拍照之前最好先判断一下sdcard是否可用
         */
        String state = Environment.getExternalStorageState(); //拿到sdcard是否可用的状态码
        if (state.equals(Environment.MEDIA_MOUNTED)) {   //如果可用
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            startActivityForResult(intent, 0);
        } else {
            Toast.makeText(MainActivity.this, "sdcard不可用", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            String path = data.getStringExtra("bitmap");
            //Bitmap bitmap = BitmapFactory.decodeFile(path);
            Bitmap bitmap = BitmapUtil.compressBitmap(new File(path));

            pic.setImageBitmap(bitmap);
        }
/*        Bitmap photo = null;
        if (data.getData() != null || data.getExtras() != null) { //防止没有返回结果
            Uri uri = data.getData();
            if (uri != null) {
                photo = BitmapFactory.decodeFile(uri.getPath());//拿到图片
            }
            if (photo == null) {
                Bundle bundle = data.getExtras();
                if (bundle != null) {
                    photo = (Bitmap) bundle.get("data");
                } else {
                    Toast.makeText(getApplicationContext(), "找不到图片", Toast.LENGTH_SHORT).show();
                }
            }

            pic.setImageBitmap(photo);

        }*/
    }

    /**
     * 将彩色图转换为灰度图
     *
     * @param img 位图
     * @return 返回转换好的位图
     */
    public Bitmap convertGreyImg(Bitmap img) {
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

    private void reset() {
        sizeList.clear();
        lightScaleList.clear();
        btReport.setEnabled(false);
        tvResult.setText("");
        pic.setImageBitmap(bm);
    }

    private void resetConfig() {
        ValidUtil.threshold = 0;
        ValidUtil.mode = MODE_UNKNOW;
        ValidUtil.isEroded = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        //load OpenCV engine and init OpenCV library
        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, getApplicationContext(), mLoaderCallback);

        ViewGroup.LayoutParams layoutParams = pic.getLayoutParams();
        layoutParams.width = DensityUtil.getScreenWidth(this) / 2;
        layoutParams.height = DensityUtil.getScreenHeight(this) / 2;
        pic.setLayoutParams(layoutParams);
    }

    //OpenCV库加载并初始化成功后的回调函数
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {

        @Override
        public void onManagerConnected(int status) {
            // TODO Auto-generated method stub
            switch (status) {
                case BaseLoaderCallback.SUCCESS:
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };


    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    public void checkPermission() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }
}
