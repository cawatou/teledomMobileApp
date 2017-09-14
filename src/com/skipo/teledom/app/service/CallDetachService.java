package com.skipo.teledom.app.service;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.pubnub.api.Callback;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;
import com.skipo.teledom.MainActivity;
import com.skipo.teledom.R;
import com.skipo.teledom.app.util.Constants;
import com.skipo.teledom.app.util.LogRTCListener;
import com.skipo.teledom.app.widget.*;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


import me.kevingleason.pnwebrtc.PnPeer;
import me.kevingleason.pnwebrtc.PnRTCClient;
import me.kevingleason.pnwebrtc.PnSignalingParams;


public class CallDetachService extends Service {
    private static final String TAG = "[MyApp-CDS]";

    public static final String VIDEO_TRACK_ID = "videoPN";
    public static final String AUDIO_TRACK_ID = "audioPN";
    public static final String LOCAL_MEDIA_STREAM_ID = "localStreamPN";

    private ViewGroup view = null;
    private WindowManager wm;
    private WindowManager.LayoutParams lp;
    private LayoutInflater infl;

    private Pubnub mPubNub;
    private String PubKey;
    private String SubKey;
    private String SecKey;

    private SharedPreferences Pref;
    private String domofonName;
    private String clientName;
    private String pubChannel;
    private String subChannel;

    private PnRTCClient pnRTCClient;
    private VideoSource localVideoSource;
    private VideoRenderer.Callbacks localRender;
    private VideoRenderer.Callbacks remoteRender;

    private WebView webView;
    private GLSurfaceView videoView;
    private ImageView image;
    private SlideButton btnOpenDoor;
    private SlideButton btnCallDispatch;
    private TextView textResolve;
    private TextView textReject;
    private TextView textDoor;

    private Ringtone ringtone;
    private String photoImage;

    private boolean onButtonPush = false;

    Handler serviceHandler;


    public CallDetachService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate");

        serviceHandler = new Handler(this.getMainLooper());
        Pref = getSharedPreferences(Constants.SHARED_PREFS, MODE_PRIVATE);

        domofonName = Pref.getString(Constants.DOMOFON_PHONE_NUMBER, "");
        PubKey = Pref.getString(Constants.PUBNUB_PUBLISH_KEY, "");
        SubKey = Pref.getString(Constants.PUBNUB_SUBSCRIBE_KEY, "");
        SecKey = Pref.getString(Constants.PUBNUB_SECRET_KEY, "");


        initPubNub();

        //sendDetachIncommingCall();

        if ( isDomofonTimeout(10000) ) {
            Log.i(TAG, "Domofon not answer. Timeout");
            stopSelf();
            return;
        }

        lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 0,
                WindowManager.LayoutParams.TYPE_SYSTEM_ERROR,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
        );

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        infl = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
//        view = (ViewGroup) infl.inflate(R.layout.dailer_layout, null);
        view = (ViewGroup) infl.inflate(R.layout.activity_video_call, null);

        videoView = (GLSurfaceView) view.findViewById(R.id.gl_surface);
        VideoRendererGui.setView(videoView, null);
       // webView = (WebView) view.findViewById(R.id.webview);

        textResolve = (TextView) view.findViewById(R.id.txtResolve);
        textReject = (TextView) view.findViewById(R.id.txtReject);
        textDoor = (TextView) view.findViewById(R.id.txtDoor);

        InputStream stream = new ByteArrayInputStream(Base64.decode(photoImage.getBytes(), Base64.DEFAULT));
        Bitmap bitmap =  BitmapFactory.decodeStream(stream);

        image = (ImageView) view.findViewById(R.id.imageView);
        image.setImageBitmap(bitmap);

        btnOpenDoor = (SlideButton) view.findViewById(R.id.btnDoor);
        btnOpenDoor.setSlideButtonListener(new SlideButtonListener() {
            @Override
            public void handleSlide(int progress) {
                if (progress < 50) return;
                Log.i(TAG,"Button Open Door Click");
                onButtonPush = true;
                sendMessage("open");
                hangup();
            }
        });

        btnCallDispatch = (SlideButton) view.findViewById(R.id.btnCall);

/*        final ObjectAnimator obj  = ObjectAnimator.ofInt(btnCallDispatch, "progress", 55, 45, 55, 45, 54, 46, 54, 46, 53, 47, 53, 47, 52, 48, 52, 48, 51, 49, 51, 49, 51, 49, 51, 49, 50);
        obj.setDuration(500);
        obj.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {}
            @Override
            public void onAnimationEnd(Animator animation) {
                obj.setStartDelay(2000);
                obj.start();
            }
            @Override
            public void onAnimationCancel(Animator animation) {}
            @Override
            public void onAnimationRepeat(Animator animation) {}
        });
        obj.start();
*/
        btnCallDispatch.setSlideButtonListener(new SlideButtonListener() {
            @Override
            public void handleSlide(int progress) {
                Log.i(TAG,"SlideButton position: " + progress);
                if ( progress < 40 ) {
                    Log.i(TAG, "Get accept state");
                    if ( !onButtonPush ) {
                        onButtonPush = true;
                        ringtoneStop();
                        image.setVisibility(View.GONE);
                        btnOpenDoor.setVisibility(View.GONE);
                        textDoor.setVisibility(View.GONE);
                        textResolve.setText("ВПУСТИТЬ");
                        textReject.setText("ОТМЕНА");
                    } else {
                        sendMessage("open");
                        hangup();
                    }
                } else if ( progress > 60 ) {
                    Log.i(TAG, "Get decline button state");
                    onButtonPush = true;
                    sendMessage("decline");
                    hangup();
                } else {
                    //obj.cancel();
                }
            }
        });


        wm.addView(view, lp);

        startPeerConnection();

        ringtoneStart();

        notificationTimeout(10000);

    }

    private void stopDialerView() {
        if ( null != view ) {
            wm.removeView(view);
            view = null;
        }
    }

    private boolean isDomofonTimeout(int timeout) {
        Log.i(TAG, "Start Domofon timeout");
        while ( true ) {
            try {
                Thread.sleep(100);
                timeout -= 100;
                if ( (timeout <= 0) || (null != photoImage) ) break;
            } catch(InterruptedException e){
                Log.i(TAG, "Thread sleep InterruptedException: " + e.getMessage());
            }
        }
        if ( timeout <= 0 ) {
            Log.i(TAG, "Timeout to Domofon");
            return true;
        }
        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        ringtoneStop();

        stopDialerView();

        closePubNub();

        if (localVideoSource != null) {
            localVideoSource.stop();
        }
        if (pnRTCClient != null) {
            pnRTCClient.onDestroy();
        }

        Log.i(TAG, "onDestroy");
    }

    public void ringtoneStart() {
        Log.i(TAG, "ringtoneStart");
        Uri currentRintoneUri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(getApplicationContext(), currentRintoneUri);
        ringtone.play();
    }

    public void ringtoneStop() {
        Log.i(TAG, "ringtoneStop");
        if ( null != ringtone ) {
            ringtone.stop();
            ringtone = null;
        }
    }

    private void notificationTimeout(int timeout) {
        Log.i(TAG, "Start notification timeout");
        new CountDownTimer(timeout, 100) {

            public void onTick(long millisUntilFinished) {
                if ( CallDetachService.this.onButtonPush ) this.cancel();
            }

            public void onFinish() {
                Log.i(TAG, "Timeout to notification");
                Intent notificationIntent = new Intent(CallDetachService.this, MainActivity.class);
                PendingIntent contentIntent = PendingIntent.getActivity(CallDetachService.this, 0, notificationIntent, PendingIntent.FLAG_CANCEL_CURRENT);
                NotificationCompat.Builder builder = new NotificationCompat.Builder(CallDetachService.this);
                builder.setContentIntent(contentIntent)
                        .setSmallIcon(R.mipmap.icon)
                        .setWhen(System.currentTimeMillis())
                        .setAutoCancel(true)
                        .setContentTitle("TELEDOM")
                        .setContentText("Пропущенный посетитель");
                Notification notification = builder.build();
                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(CallDetachService.this);
                notificationManager.notify(133, notification);
                CallDetachService.this.stopSelf();
            }
        }.start();
    }

    public void initPubNub(){
        Log.i(TAG, "initPubNub");

        mPubNub  = new Pubnub(PubKey, SubKey ,SecKey/*, true*/);

        clientName = mPubNub.getUUID();
        subChannel = clientName;// + Constants.STDBY_SUFFIX;

        pubChannel = domofonName;

        subscribeStdBy();
    }

    private void closePubNub() {
        Log.i(TAG, "Close PubNub");
        try {Thread.sleep(2000);} catch (InterruptedException e){e.printStackTrace();}
        mPubNub.unsubscribeAll();
        mPubNub.shutdown();
    }

    private void subscribeStdBy(){
        Log.i(TAG, "Subscribe to StdBy");
        try {
            mPubNub.subscribe(subChannel, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.i(TAG, "MESSAGE: " + message.toString());
                    if (!(message instanceof JSONObject)) return; // Ignore if not JSONObject
                    JSONObject jsonMsg = (JSONObject) message;
                    try {
                        if (!jsonMsg.has(Constants.JSON_EVENT)) return;     //Ignore Signaling messages.
                        photoImage = jsonMsg.getString(Constants.JSON_PHOTO).replace("\\", "");
                        Log.i(TAG, "PHOTO SOURCE: " + photoImage);
                    } catch (JSONException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void connectCallback(String channel, Object message) {
                    Log.i(TAG, "CONNECTED: " + message.toString());
                    sendDetachIncommingCall();
                }

                @Override
                public void errorCallback(String channel, PubnubError error) {
                    Log.i(TAG, "ERROR: " + error.toString());
                }
            });
        } catch (PubnubException e){
            Log.i(TAG, "HEREEEE");
            e.printStackTrace();
        }
    }

    public void sendDetachIncommingCall(){
        Log.i(TAG, "sendDetachIncommingCall");
        this.mPubNub.hereNow(pubChannel, new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                Log.i(TAG, "HERE_NOW: " +" CH - " + pubChannel + " " + message.toString());
                try {
                    int occupancy = ((JSONObject) message).getInt(Constants.JSON_OCCUPANCY);
                    if (occupancy == 0) {
                        Log.i(TAG, "User is not online!");
                        return;
                    }
                    sendMessage("detach");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void sendMessage(String msg) {
        Log.i(TAG, "sendMessage");
        try {
            JSONObject jsonCall = new JSONObject();
            jsonCall.put(Constants.JSON_EVENT, "mobile_call");
            jsonCall.put(Constants.JSON_USER, clientName);
            jsonCall.put(Constants.JSON_TIME, System.currentTimeMillis());
            jsonCall.put(Constants.JSON_RESPONSE, "done");
            jsonCall.put(Constants.JSON_MESSAGE, msg);
            mPubNub.publish(pubChannel, jsonCall, new Callback() {
                @Override
                public void successCallback(String channel, Object message) {
                    Log.i(TAG, "SUCCESS to [" + pubChannel + "]:\n" + message.toString());
                }
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void startPeerConnection() {
        Log.i(TAG, "startPeerConnection");
        // First, we initiate the PeerConnectionFactory with our application context and some options.
        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true,  // Hardware Acceleration Enabled
                null); // Render EGL Context

        PeerConnectionFactory pcFactory = new PeerConnectionFactory();
        pnRTCClient = new PnRTCClient(PubKey, SubKey, subChannel+"-stdby");
        pnRTCClient.setSignalParams(new PnSignalingParams());
        // Returns the number of cams & front/back face device name
        String frontFacingCam = VideoCapturerAndroid.getNameOfFrontFacingDevice();

        // Creates a VideoCapturerAndroid instance for the device name
        VideoCapturer capturer = VideoCapturerAndroid.create(frontFacingCam);

        // First create a Video Source, then we can make a Video Track
        localVideoSource = pcFactory.createVideoSource(capturer, pnRTCClient.videoConstraints());
        VideoTrack localVideoTrack = pcFactory.createVideoTrack(VIDEO_TRACK_ID, localVideoSource);

        // First we create an AudioSource then we can create our AudioTrack
        AudioSource audioSource = pcFactory.createAudioSource(this.pnRTCClient.audioConstraints());
        AudioTrack localAudioTrack = pcFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);

        // Now that VideoRendererGui is ready, we can get our VideoRenderer.
        // IN THIS ORDER. Effects which is on top or bottom
        remoteRender = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
        localRender = VideoRendererGui.create(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);

        // We start out with an empty MediaStream object, created with help from our PeerConnectionFactory
        //  Note that LOCAL_MEDIA_STREAM_ID can be any string
        MediaStream mediaStream = pcFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);

        // Now we can add our tracks.
        mediaStream.addTrack(localVideoTrack);
        mediaStream.addTrack(localAudioTrack);

        // First attach the RTC Listener so that callback events will be triggered
        pnRTCClient.attachRTCListener(new DemoRTCListener());

        // Then attach your local media stream to the PnRTCClient.
        //  This will trigger the onLocalStream callback.
        pnRTCClient.attachLocalMediaStream(mediaStream);

        // Listen on a channel. This is your "phone number," also set the max chat users.
        pnRTCClient.listenOn(subChannel+"-stdby");
        pnRTCClient.setMaxConnections(1);

        // If the intent contains a number to dial, call it now that you are connected.
        //  Else, remain listening for a call.
        pnRTCClient.connect(pubChannel+"-stdby");
    }

    public void hangup() {
        Log.i(TAG, "hangup");
        if ( null != pnRTCClient ) {
            pnRTCClient.closeAllConnections();
            pnRTCClient = null;
            //return;
        }
        stopSelf();
    }

   /**
     * LogRTCListener is used for debugging purposes, it prints all RTC messages.
     * DemoRTC is just a Log Listener with the added functionality to append screens.
     */
    private class DemoRTCListener extends LogRTCListener {
        @Override
        public void onLocalStream(final MediaStream localStream) {
            super.onLocalStream(localStream); // Will log values
            Log.i(TAG, "Local stream add");
            Runnable thread = new Runnable() {
                @Override
                public void run() {
                    if(localStream.videoTracks.size()==0) return;
                    localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
                }
            };
        }

        @Override
        public void onAddRemoteStream(final MediaStream remoteStream, final PnPeer peer) {
            super.onAddRemoteStream(remoteStream, peer); // Will log values
            Log.i(TAG, "Remote stream add");
            Runnable thread = new Runnable() {
                @Override
                public void run() {
                    try {
                        if(remoteStream.audioTracks.size()==0 || remoteStream.videoTracks.size()==0) {
                            Log.i(TAG, "Remote stream is empty");
                            return;
                        }
                        remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
                        VideoRendererGui.update(remoteRender, 0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, false);
                        //VideoRendererGui.update(localRender, 72, 65, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FIT, true);
                        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
                        am.setSpeakerphoneOn(true);
                        sendMessage("ready");
                    }
                    catch (Exception e){ e.printStackTrace(); }
                }
            };
            serviceHandler.post(thread);
        }

        @Override
        public void onMessage(PnPeer peer, Object message) {
            super.onMessage(peer, message);  // Will log values
            if (!(message instanceof JSONObject)) return; //Ignore if not JSONObject
            JSONObject jsonMsg = (JSONObject) message;
            Log.i(TAG, "MESSAGE: " + jsonMsg.toString());
            try {
                long   time = jsonMsg.getLong(Constants.JSON_TIME);
            } catch (JSONException e){
                e.printStackTrace();
            }
        }

        @Override
        public void onPeerConnectionClosed(PnPeer peer) {
            super.onPeerConnectionClosed(peer);
            try {Thread.sleep(1500);} catch (InterruptedException e){e.printStackTrace();}
            CallDetachService.this.stopSelf();
        }
    }
}
