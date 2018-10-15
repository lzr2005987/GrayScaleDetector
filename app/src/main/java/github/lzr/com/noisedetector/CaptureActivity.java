package github.lzr.com.noisedetector;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import github.lzr.com.noisedetector.Util.DensityUtil;

/**
 * Created by Administrator on 2018/3/5 0005.
 */

public class CaptureActivity extends Activity implements SurfaceHolder.Callback {
    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    private Camera mCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置无标题
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        //设置全屏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_capture);
        mSurfaceView = (SurfaceView) findViewById(R.id.sv_main);
        mHolder = mSurfaceView.getHolder();
        mHolder.addCallback(this);
    }

    public void capture(View view) {
/*        mCamera.setOneShotPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                Camera.Size size = camera.getParameters().getPreviewSize();
                try {
                    YuvImage image = new YuvImage(bytes, ImageFormat.NV21, size.width, size.height, null);
                    if (image != null) {
                        //获取相机拍摄的图片
                        Bitmap bitmap = takePicture(size, image);
                        finishActivity(bitmap);
                    }
                } catch (Exception ex) {
                    Log.e("lizheren", "Error:" + ex.getMessage());
                }
            }
        });*/


/*        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] bytes, Camera camera) {
                Camera.Size size = camera.getParameters().getPreviewSize();
                try {
                    YuvImage image = new YuvImage(bytes, ImageFormat.NV21, size.width, size.height, null);
                    if (image != null) {
                        //获取相机拍摄的图片
                        Bitmap bitmap = takePicture(size, image);
                        File file = saveImage(bitmap);
                        finishActivity(file);
                    }
                } catch (Exception ex) {
                    Log.e("lizheren", "Error:" + ex.getMessage());
                }
            }
        });*/

        //设置监听
        mCamera.setPreviewCallback(new Camera.PreviewCallback() {

            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                Camera.Size size = camera.getParameters().getPreviewSize();
                try {
                    YuvImage image = new YuvImage(data, ImageFormat.NV21, size.width, size.height, null);
                    if (image != null) {
                        //获取相机拍摄的图片
                        Bitmap bitmap = takePicture(size, image);
                        File file = saveImage(bitmap);
                        finishActivity(file);
                    }
                } catch (Exception ex) {
                    Log.e("Sys", "Error:" + ex.getMessage());
                }
            }
        });
    }

    public File saveImage(Bitmap bmp) {
        File appDir = new File(Environment.getExternalStorageDirectory(), "NoiseDetector");
        Log.d("lizheren", "saveImage: " + appDir.getAbsolutePath());
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = "temp.jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    private void finishActivity(File file) {
        Intent intent = new Intent();
        String absolutePath = file.getAbsolutePath();
        mCamera.setPreviewCallback(null);
        intent.putExtra("bitmap", absolutePath);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private Bitmap takePicture(Camera.Size size, YuvImage image) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compressToJpeg(new Rect(0, 0, size.width, size.height), 80, stream);
        Bitmap srcBmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size());
        //**********************
        //因为图片会发生旋转，因此要对图片进行旋转到和手机在一个方向上
        final Bitmap grayBitmap = rotateMyBitmap(srcBmp);
        //**********************************
        stream.close();
        return grayBitmap;
    }

    public Bitmap rotateMyBitmap(Bitmap bmp) {
        //*****旋转一下
        Matrix matrix = new Matrix();
        matrix.postRotate(90);
        //Bitmap bitmap = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), Bitmap.Config.ARGB_8888);
        Bitmap nbmp2 = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, true);
        Bitmap changedBitmap = changeSize(nbmp2);
        return changedBitmap;
    }

    private Bitmap changeSize(Bitmap bm) {
        // 获得图片的宽高
        int width = bm.getWidth();
        int height = bm.getHeight();
        // 设置想要的大小
        int newWidth = DensityUtil.getScreenWidth(this);
        int newHeight = DensityUtil.getScreenHeight(this);
        // 计算缩放比例
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        // 取得想要缩放的matrix参数
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        // 得到新的图片
        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix,
                true);
        return newbm;
    }

    /**
     * 获得最佳分辨率
     * 注意:因为相机默认是横屏的，所以传参的时候要注意，width和height都是横屏下的
     *
     * @param parameters 相机参数对象
     * @param width      期望宽度
     * @param height     期望高度
     * @return
     */
    private int[] getBestResolution(Camera.Parameters parameters, int width, int height) {
        int[] bestResolution = new int[2];//int数组，用来存储最佳宽度和最佳高度
        int bestResolutionWidth = -1;//最佳宽度
        int bestResolutionHeight = -1;//最佳高度

        List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();//获得设备所支持的分辨率列表
        int difference = 99999;//最小差值，初始化市需要设置成一个很大的数

        //遍历sizeList，找出与期望分辨率差值最小的分辨率
        for (int i = 0; i < sizeList.size(); i++) {
            int differenceWidth = Math.abs(width - sizeList.get(i).width);//求出宽的差值
            int differenceHeight = Math.abs(height - sizeList.get(i).height);//求出高的差值

            //如果它们两的和，小于最小差值
            if ((differenceWidth + differenceHeight) < difference) {
                difference = (differenceWidth + differenceHeight);//更新最小差值
                bestResolutionWidth = sizeList.get(i).width;//赋值给最佳宽度
                bestResolutionHeight = sizeList.get(i).height;//赋值给最佳高度
            }
        }

        //最后将最佳宽度和最佳高度添加到数组中
        bestResolution[0] = bestResolutionWidth;
        bestResolution[1] = bestResolutionHeight;
        return bestResolution;//返回最佳分辨率数组
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        mCamera = Camera.open();
        try {
            mCamera.setPreviewDisplay(mHolder);//设置在surfaceView上显示预览

            mCamera.setDisplayOrientation(90);
            Camera.Parameters parameters = mCamera.getParameters();
            /**获得屏幕分辨率**/
            Display display = this.getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            int screenWidth = size.x;
            int screenHeight = size.y;

            /**获得最佳分辨率，注意此时要传的width和height是指横屏时的,所以要颠倒一下**/
            int[] bestResolution = getBestResolution(parameters, screenHeight, screenWidth);
            parameters.setPreviewSize(bestResolution[0], bestResolution[1]);

            parameters.setRotation(90);
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            //parameters.setAutoWhiteBalanceLock(true);
            mCamera.setParameters(parameters);

            mCamera.startPreview();//开始预览

        } catch (IOException e) {
            //在异常处理里释放camera并置为null
            mCamera.release();
            mCamera = null;
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        //销毁时触发，surfaceView生命周期的结束，在这里关闭相机
        mCamera.stopPreview();
        mCamera.release();
        mCamera = null;
    }

    /**
     * 设置音量键推进焦距
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Camera.Parameters parameters = mCamera.getParameters();
        int maxZoom = parameters.getMaxZoom();
        int zoom = parameters.getZoom();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (zoom + 5 <= maxZoom) parameters.setZoom(zoom + 5);
                else parameters.setZoom(maxZoom);
                mCamera.setParameters(parameters);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (zoom >= 5) parameters.setZoom(zoom - 5);
                else parameters.setZoom(0);
                mCamera.setParameters(parameters);
                return true;
/*            case KeyEvent.KEYCODE_BACK:
                SharedPreferences sp = getSharedPreferences("Info", MODE_PRIVATE);
                SharedPreferences.Editor edit = sp.edit();
                edit.remove("position");
                edit.apply();
                break;*/
        }
        return super.onKeyDown(keyCode, event);
    }
}
