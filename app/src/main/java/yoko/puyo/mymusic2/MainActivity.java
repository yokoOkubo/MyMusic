package yoko.puyo.mymusic2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.res.TypedArray;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener{
    private SensorManager sensorManager;
    private Sensor accel;
    private Sensor mag;
    private static final int AZIMUTH_THRESHOLD = 15;
    private static final int MATRIX_SIZE = 16;
    private float[] accelVal = new float[3];    //センサーの値
    private float[] magVal = new float[3];      //センサーの値

    private int nowScale = 0;
    private int oldScale = 9;
    private int nowAzimuth = 0;
    private int oldAzimuth = 0;

    private MediaPlayer[] mplayer;

    TextView text1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);   //画面をスリープにしない

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        text1 = findViewById(R.id.text1);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this, accel);
        sensorManager.unregisterListener(this, mag);
    }

    @Override
    protected void onResume() {
        super.onResume();


        //第3引数はデータ取得間隔　SensorManager.SENSOR_DELAY_FASTEST、NORMALなど　p172でも
        sensorManager.registerListener(this,accel,100000);
        sensorManager.registerListener(this,mag, 100000);

        TypedArray notes = getResources().obtainTypedArray(R.array.notes);
        mplayer = new MediaPlayer[notes.length()];
        for(int i=0; i<notes.length(); i++) {
            mplayer[i] = MediaPlayer.create(this, notes.getResourceId(i, -1));
        }
//        playSound(9);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[]  inR = new float[MATRIX_SIZE];
        float[] outR = new float[MATRIX_SIZE];
        float[]    I = new float[MATRIX_SIZE];
        float[] orVal = new float[3];

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER :
                accelVal = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD :
                magVal = event.values.clone();
                break;
        }
        if (magVal != null && accelVal != null) {
            //加速度センサーの値と地磁気センサーの値からinRとIを計算する
            SensorManager.getRotationMatrix(inR, I, accelVal, magVal);
            //inRをoutRに変換する
            SensorManager.remapCoordinateSystem(inR, SensorManager.AXIS_X,SensorManager.AXIS_Y, outR);
            //方位角、傾斜角、回転角がorValに求まる
            SensorManager.getOrientation(outR, orVal);

            //表示
            StringBuilder str = new StringBuilder();
            str.append("方位角");  //北が0度
            str.append(rad2Deg(orVal[0]));
            str.append("\n");
            str.append("傾斜角");  //上をむいて寝てると0度立てると-90度
            str.append(rad2Deg(orVal[1]));
            str.append("\n");
            str.append("回転角");  //上を向いて寝てると0度TVのように立てると-90度
            str.append(rad2Deg(orVal[2]));
            str.append("\n");

            nowScale = rad2Deg(orVal[1]) / 10;
            str.append("scale:傾斜角方位角/10　");
            str.append(nowScale);
            str.append("\n");
            nowAzimuth = rad2Deg(orVal[0]);
            str.append("方位角　");
            str.append(nowAzimuth);
            text1.setText(str.toString());

            if(nowScale != oldScale) {
                playSound(nowScale);
                oldScale = nowScale;
                oldAzimuth = nowAzimuth;
            } else if(Math.abs(oldAzimuth-nowAzimuth) > AZIMUTH_THRESHOLD) {
                playSound(nowScale);
                oldAzimuth = nowAzimuth;
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }
    int rad2Deg(float rad) {
        return (int)Math.abs(Math.toDegrees(rad));
    }
    void playSound(int scale) {
        mplayer[scale].seekTo(0);
        mplayer[scale].start();
    }
}
