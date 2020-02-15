package com.example.videoplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.VideoView;

public class MainActivity extends AppCompatActivity {


    private static final String TAG = "MainActivity";

    private static final String videoUrl = "http://vfx.mtime.cn/Video/2019/02/04/mp4/190204084208765161.mp4";

    private int[] res = { R.raw.welcome01,R.raw.welcome02 };
    /**
     * videoView 容器
     */

    private RelativeLayout video_view_container ;

    private VideoView videoView ;
    /**
     * 进度条
     */
    private SeekBar seekBar ;

    /**
     * 播放 ，暂停
     */
    private ImageView   pause_img;


    private ImageView screen_img ;


    private TextView time_current_tv ;

    private TextView time_total_tv ;

    /**
     * 音量img
     */

    private ImageView volume_img ;

    /**
     * 音量 seekBar
     */

    private SeekBar volume_seekBar ;


    private static final int UPDATE_UI = 1024;

    private boolean isFullScreen = false ;

    private AudioManager audioManager ;

    private  final float threshold = 0;
    private boolean isAjust = false ;

    private int screenHeight = 0;

    private int screenWidth = 0;

    float lastX = 0 ,lastY = 0;

    float screenBrightness ;

    private LinearLayout volume_bg_root ;

    private ImageView operation_bg ;
    private SeekBar brightness_seekbar ;

    private Handler UIHandler = new Handler()
    {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if(msg.what == UPDATE_UI) {
                int currentPosition = videoView.getCurrentPosition();
                int totalDuration = videoView.getDuration();

                updateTv(time_current_tv, currentPosition);
                updateTv(time_total_tv, totalDuration);
                seekBar.setMax(totalDuration);
                seekBar.setProgress(currentPosition);
                UIHandler.sendEmptyMessageDelayed(UPDATE_UI, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        play();
        initEvent();
    }
    private void initView()
    {
        video_view_container = findViewById(R.id.video_view_container);
        videoView = findViewById(R.id.video_view);
        seekBar = findViewById(R.id.play_seek);
        pause_img = findViewById(R.id.pause_img);
        screen_img = findViewById(R.id.screen_img);
        time_current_tv = findViewById(R.id.time_current_tv);
        time_total_tv = findViewById(R.id.time_total_tv);
        volume_img = findViewById(R.id.volume_img);
        volume_seekBar = findViewById(R.id.volume_seekBar);
        volume_bg_root = findViewById(R.id.volume_bg_root);
        brightness_seekbar = findViewById(R.id.brightness_seekbar);
        operation_bg = findViewById(R.id.operation_bg);
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume_seekBar.setMax(max);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volume_seekBar.setProgress(current);
    }

    private void initEvent()
    {
        pause_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(videoView.isPlaying())
                {
                    pause_img.setImageResource(R.drawable.play_btn_style);
                    videoView.pause();
                    UIHandler.removeMessages(UPDATE_UI);

                }else
                {
                    pause_img.setImageResource(R.drawable.pause_btn_style);
                    videoView.start();
                    UIHandler.sendEmptyMessageDelayed(UPDATE_UI, 500);
                }
            }
        });
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {



            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

                UIHandler.removeMessages(UPDATE_UI);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                int progress = seekBar.getProgress();
                videoView.seekTo(progress);
                UIHandler.sendEmptyMessage(UPDATE_UI);
            }
        });
        screen_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               // Configuration configuration = getResources().getConfiguration();
                if(!isFullScreen)
                {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            }
        });
        videoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                float x = motionEvent.getX();
                float y = motionEvent.getY();

                switch (motionEvent.getAction())
                {
                    case MotionEvent.ACTION_DOWN:
                        lastX = x;
                        lastY = y;

                        break;

                    case MotionEvent.ACTION_MOVE:

                       float detlaX = x-lastX ;
                       float detlaY = y-lastY ;

                       float absDetlaX = Math.abs(detlaX);
                       float absDetlaY = Math.abs(detlaY);
                       if(absDetlaX > threshold && absDetlaY > threshold)
                       {
                           if(absDetlaX < absDetlaY)
                           {
                               isAjust = true ;
                           }else
                           {
                               isAjust = false ;
                           }
                       }
                       if(isAjust)
                       {
                          if(x > screenWidth /2)
                          {
                              changeVolume(-detlaY);
                          }else
                          {
                              changeBrightness(-detlaY);
                          }
                       }


                        //lastX = x;
                        //lastY = y;
                        break;

                    case MotionEvent.ACTION_UP:
                        volume_bg_root.setVisibility(View.GONE);
                        break;

                }
                return true;
            }
        });
    }

    /**
     * 改变音量
     * @param detlaY
     */
    private void changeVolume(float detlaY)
    {

        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        volume_seekBar.setMax(max);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        volume_seekBar.setProgress(current);
        Log.e(TAG,"detlaY::"+detlaY);
        Log.e(TAG,"screenHeight::"+screenHeight);
        int index = (int)(detlaY / screenHeight * max );

        int volume = Math.max(current +index,0);
        operation_bg.setImageResource(R.drawable.volume);
        volume_bg_root.setVisibility(View.VISIBLE);
        Log.e(TAG,"volume::"+volume);
        brightness_seekbar.setMax(max);
        brightness_seekbar.setProgress(Math.min(volume,max));
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,Math.min(volume,max),0);
    }

    private void changeBrightness(float detlaY)
    {
         WindowManager.LayoutParams params = getWindow().getAttributes();
         screenBrightness = params.screenBrightness;
         float index = detlaY/screenHeight/3;
         Log.e(TAG,"screenBrightness index::"+index);
         screenBrightness +=index;
         if(screenBrightness>1.0f)
         {
             screenBrightness = 1.0f;
         }
         if(screenBrightness <0.0f)
         {
             screenBrightness = 0.0f;
         }
         params.screenBrightness = screenBrightness;
         getWindow().setAttributes(params);
        operation_bg.setImageResource(R.drawable.screen_brightness);
        volume_bg_root.setVisibility(View.VISIBLE);
        //Log.e(TAG,"screenBrightness "+(int)screenBrightness*100);
        brightness_seekbar.setProgress((int)(screenBrightness*100));

    }
    private void updateTv(TextView tv  ,int milliSec)
    {

        int sec = milliSec / 1000;
        int hh = sec /3600;
        int mm = sec%3600/60;
        int ss = sec % 60 ;
        String  str = null;
        if(hh != 0)
        {
          str = String.format("%02d:%02d:%02d",hh,mm,ss);
        }else
        {
            str = String.format("%02d:%02d",mm,ss);
        }
        tv.setText(str);
    }
    private void play()
    {
        Uri uri = (Uri.parse("android.resource://" + getPackageName() + "/" +res[0]));
        videoView.setVideoURI(Uri.parse(videoUrl));//Uri.parse(videoUrl)
       // videoView.start();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            setVideoScale(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
            isFullScreen = true ;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            volume_img.setVisibility(View.VISIBLE);
            volume_seekBar.setVisibility(View.VISIBLE);
        }else
        {
            setVideoScale(ViewGroup.LayoutParams.MATCH_PARENT,dp2px(240f));
            isFullScreen = false ;
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            volume_img.setVisibility(View.GONE);
            volume_seekBar.setVisibility(View.GONE);
        }
    }

    private void setVideoScale(int width,int height)
    {
        ViewGroup.LayoutParams layoutParams = videoView.getLayoutParams();
        layoutParams.height = height;
        layoutParams.width = width;
        videoView.setLayoutParams(layoutParams);

        ViewGroup.LayoutParams layoutParams1 = video_view_container.getLayoutParams();

        layoutParams1.height = height;
        layoutParams1.width = width;
        video_view_container.setLayoutParams(layoutParams1);
    }

    private int  dp2px(float value)
    {
      float scale = this.getResources().getDisplayMetrics().densityDpi;
      return (int)(value*(scale/160)+0.5f);
    }
}
