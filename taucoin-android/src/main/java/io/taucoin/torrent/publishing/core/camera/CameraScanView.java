package io.taucoin.torrent.publishing.core.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.os.HandlerThread;
import android.util.AttributeSet;

import com.google.zxing.qrcode.QRCodeReader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.taucoin.torrent.publishing.core.utils.StringUtil;

@SuppressLint("NewApi")
public class CameraScanView extends CameraView implements Camera.PreviewCallback{
    private HandlerThread backgroundHandlerThread = new HandlerThread("ScanCamera");
    private MyHandler handler;
    private boolean recognized;
    private QRCodeReader qrReader;
    private ScanCallback scanCallback;

    public CameraScanView(@NonNull Context context) {
        super(context);
    }

//    public CameraScanView(@NonNull Context context, @Nullable AttributeSet attrs) {
//        this(context, attrs, 0);
//    }
//
//    public CameraScanView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
//        super(context.getApplicationContext(), attrs, defStyleAttr);
//    }

    public void initAndStartCamera() {
        initAndStartCamera(new QRCodeReader());
    }

    public void initAndStartCamera(QRCodeReader qrReader) {
        this.qrReader = qrReader;
        setUseMaxPreview(true);
        setOptimizeForBarcode(true);
        setDelegate(new CameraView.CameraViewDelegate() {
            @Override
            public void onCameraCreated(Camera camera) {

            }

            @Override
            public void onCameraInit() {
                startRecognizing();
            }
        });

        CameraController.getInstance().initCamera(this::initCamera);
    }

    private void startRecognizing() {
        backgroundHandlerThread.start();
        handler = new MyHandler(backgroundHandlerThread.getLooper());
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (!recognized && getCameraSession() != null) {
                    getCameraSession().setOneShotPreviewCallback(CameraScanView.this);
                    AndroidUtilities.runOnUIThread(this, 500);
                }
            }
        });
    }

    @Override
    public void onPreviewFrame(final byte[] data, final Camera camera) {
        handler.post(() -> {
            try {
                Size size = getPreviewSize();
                int format = camera.getParameters().getPreviewFormat();
                int side = (int) (Math.min(size.getWidth(), size.getHeight()) / 1.5f);
                int x = (size.getWidth() - side) / 2;
                int y = (size.getHeight() - side) / 2;

                String text = Utilities.tryReadQr(qrReader, data, size, x, y, side, null);
                if (StringUtil.isNotEmpty(text)) {
                    recognized = true;
                    camera.stopPreview();

                    AndroidUtilities.runOnUIThread(() -> {
                        if (scanCallback != null) {
                            scanCallback.onScanResult(text);
                        }
                    });
                }
            } catch (Throwable ignore) { }
        });
    }

    public void destroyView(boolean async, final Runnable beforeDestroyRunnable) {
        getCameraSession().setOneShotPreviewCallback(null);
        destroy(async, beforeDestroyRunnable);
//        backgroundHandlerThread.quitSafely();
//        handler.removeCallbacksAndMessages(null);
        backgroundHandlerThread.getLooper().quit();
        handler.removeCallbacks(null); // 防止Handler内存泄露 清空消息队列

    }

    public void stopView() {
        if (getCameraSession() != null) {
            getCameraSession().stop();
        }
    }

    public void resumeView() {
        if (getCameraSession() != null) {
            getCameraSession().resume();
        }
    }

    public void removeScanCallback() {
        this.scanCallback = null;
    }

    public interface ScanCallback {
        void onScanResult(String result);
    }

    public void setScanCallback(ScanCallback callback) {
        this.scanCallback = callback;
    }

    private void handleScanCallback(Object text) {
        if (scanCallback != null) {
            scanCallback.onScanResult(null == text ? null : text.toString());
        }
    }
}
