package com.example.vishal_pc.receiver;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Camera;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.StrictMode;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.example.vishal_pc.receiver.Models.PingStatusModel;
import com.google.gson.Gson;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.spec.ECField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import cz.msebera.android.httpclient.Header;


public class MainService extends Service {

    public static final String ACTION = "com.my.app.MainService";
    public Notification notify;
    public NotificationManager notif;
    public int NOTIFICATION_ID = 1001;

    public LocalBroadcastManager mLocalBroadcastManager;
    public BluetoothManager bluetoothManager;

    public HttpUtils httpUtils;
    private PostSensorData postSensorData;
    private DataSendModel dataSendModel = new DataSendModel();

    private LocationHelper locationHelper;
    private WebSocketClient webSocketClient;
    ServerStatusModel serverStatus;

    CamWebSocketTread camWebSocketTread;

    MediaStream localMediaStream;
    VideoCapturerAndroid vc;

    PeerConnectionFactory pcFactory;
    PeerConnection peerConnection;
    WebSocketClient wsClient;

    PingStatusModel webSocketPing = new PingStatusModel();

    @Override
    public IBinder onBind(Intent intent) {
       return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        flags = START_STICKY;
        return super.onStartCommand(intent, flags, startId);
    }
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("MainService","onCreate");



        httpUtils = new HttpUtils();
        PeerConnectionFactory.initializeAndroidGlobals(
                this,  // Context
                true,  // Audio Enabled
                true,  // Video Enabled
                true,  // Hardware Acceleration Enabled
                null); // Render EGL Context


        notif=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter filter = new IntentFilter(MainService.ACTION);
        mLocalBroadcastManager.registerReceiver(testReceiver,filter);
        mLocalBroadcastManager.registerReceiver(camReceiver,new IntentFilter("cam-event"));

        bluetoothManager = new BluetoothManager(mLocalBroadcastManager);
        bluetoothManager.startConnection();
        updateNotification();

        postSensorData = new PostSensorData();
        postSensorData.start();

        locationHelper = new LocationHelper(MainService.this);
        serverStatus = new ServerStatusModel();
        ThreadManager threadManagere = new ThreadManager();
        threadManagere.start();

        StrictMode.ThreadPolicy policy =  new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
    }


    @Override
    public void onDestroy() {
        notif.cancel(0);
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_LONG).show();
        locationHelper.stopListener();

        postSensorData = null;
        serverStatus.status = 0;
        serverConnectionManager.apiCall();

        super.onDestroy();
    }

    private BroadcastReceiver testReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("command");
            Log.i("testReceiver", result);
            if(result == "update-notification")
            {
                updateNotification();
            }
            else if(result == "update-controls")
            {
               bluetoothManager.sendToArduino();
            }
        }
    };

    private BroadcastReceiver camReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String result = intent.getStringExtra("message");
            Log.d("camReceiver", result);
        }
    };

    public void updateNotification()
    {
        Intent resultIntent = new Intent(this, StopService.class);
        PendingIntent resultPendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        String notificationContent = bluetoothManager.bluetoothConnectionStatus;
        notify = new Notification.Builder(getApplicationContext())
                .setContentTitle("Receiver is running")
                .setContentText(notificationContent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .addAction(R.drawable.ic_stop, "Stop", resultPendingIntent)
                .build();
        notify.flags |= Notification.FLAG_ONGOING_EVENT;
        notif.notify(NOTIFICATION_ID, notify);
    }

    private class ThreadManager extends Thread {
        public boolean stopThread = false;

        public CameraThread cameraThread;
        StartWebSocketTread webSocketTread;

        public void exit()
        {
            stopThread = true;
        }

        @Override
        public void run() {
            cameraThread = new CameraThread();
            cameraThread.start();
//            stopThread=true;
            webSocketTread = new StartWebSocketTread();
            webSocketTread.start();


            while (!stopThread) {
                try {
                    Thread.sleep(1000);
                    if(!webSocketPing.pingSent) {
                        try {
                            webSocketClient.send("ping");
                            webSocketPing.pingSent = true;
                        } catch (Exception e)
                        {
                            Log.e("ThreadHandler","send ping error "+ e.getMessage());
                            webSocketPing.time += 1;
                        }
                    } else if(!webSocketPing.pingReceived) {
                        webSocketPing.time += 1;
                    }
                    if(webSocketPing.time > 5){
                        webSocketPing.reset();
                        bluetoothManager.arduinoDataSendModel.TriggerFailSafeMode();
                        bluetoothManager.sendToArduino();
//                        webSocketTread.exit();
//                        webSocketTread = new StartWebSocketTread();
//                        webSocketTread.start();
                        Log.e("ThreadHandler", "Restarting websocket");
                    }
                } catch (Exception e) {
                    ConnectWebSocket();
                    Log.e("ThreadHandler", e.getMessage());
                }
            }
        }
    }

    private class PostSensorData extends Thread {
        public boolean stopThread = false;

        @Override
        public void run() {
            while (!stopThread) {
                try {
//                    PostDataToDB();
                    Thread.sleep(20000);
                    webSocketClient.send("ping");
                } catch (Exception e) {
                    ConnectWebSocket();
                    Log.e("postSensorData", e.getMessage());
                }
            }
        }
    }

    private class CameraThread extends Thread {
        @Override
        public void run() {
            pcFactory = new PeerConnectionFactory();

            vc = VideoCapturerAndroid.create("Camera 0, Facing front, Orientation 270", null);

            MediaConstraints videoConstraints = new MediaConstraints();
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minHeight", Integer.toString(720)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(720)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minWidth", Integer.toString(1280)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(1280)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(20)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(20)));


            VideoSource localVideoSource = pcFactory.createVideoSource(vc, videoConstraints);
            VideoTrack localVideoTrack = pcFactory.createVideoTrack("VIDEO_TRACK", localVideoSource);
            localVideoTrack.setEnabled(true);

            localMediaStream = pcFactory.createLocalMediaStream("LOCAL_STREAM");
            localMediaStream.addTrack(localVideoTrack);

            camWebSocketTread = new CamWebSocketTread();
            camWebSocketTread.start();

        }
    }


    private class StartWebSocketTread extends Thread
    {
        public boolean stopThread = false;

        public void exit()
        {
            stopThread = true;
            webSocketClient = null;
        }
        @Override
        public void run() {
//            StartWebSocketServer();
            ConnectWebSocket();
            try{
                Thread.sleep(10000);
            }
            catch (Exception e)
            {
                Log.e("Thread Sleep",e.toString());
            }

            while(!stopThread){
                try
                {
                    PostDataToWebSocketClients();
//                    Log.d("cameraBuffer", mPreview.mFrameBuffer.size() + "");
                    Thread.sleep(500);
                }
                catch (Exception ex)
                {
                    Log.e("CAM",ex.toString());
                }

            }

        }
    }
    private class ServerConnectionManager extends Thread {
        public boolean stopThread = false;
        @Override
        public void run() {
            while (!stopThread) {
                try {
                    if(serverStatus.status != 1)
                    {
//                        StartWebSocketServer();
                    }
                    apiCall();
                    Thread.sleep(5000);
                } catch (Exception e) {
                    Log.e("postSensorData", e.getMessage());
                }
            }
        }
        public void apiCall()
        {
            RequestParams rp = new RequestParams();
            rp.add("status", serverStatus.status + "");
            rp.add("ip", serverStatus.ip + "");
            rp.add("port", serverStatus.port + "");

            httpUtils.get("update-server-status.php", rp, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        JSONArray responseObject = response.getJSONArray("responseObject");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                    // Pull out the first event on the public timeline
                    Log.d("ServerConnectionManager", "---------------- this is response : " + timeline.toString());
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String error, Throwable ex)
                {
                    Log.d("ServerConnectionManager", error + ex.getMessage() + " code:"+ statusCode);
                }
            });
        }
    }

    private void PostDataToWebSocketClients()
    {
        String payLoad = "";
        try
        {
            dataSendModel.bluetoothConnectionStatus = bluetoothManager.bluetoothConnectionStatus;
            dataSendModel.arduinoResponseModel = bluetoothManager.arduinoResponseModel;
            dataSendModel.timeStamp = new Date();


            payLoad = new Gson().toJson(dataSendModel, DataSendModel.class);
            if(payLoad != "" && webSocketClient != null && webSocketClient.getConnection().isOpen()) {
                webSocketClient.send(payLoad);
                Log.d("Sending Data", payLoad);
            }
        }
        catch (Exception e)
        {
            Log.e("Json Covert1", e + payLoad);
        }
    }
    private void ConnectWebSocket()
    {
        URI uri;
        try {
            uri = new URI(HttpUtils.WEB_SOCKET_URL);
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }
        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake arg1) {
                Log.i("WebSocket", "Session is starting");
            }
            @Override
            public void onMessage(String message) {
                try{
                    if(message != "" && !message.equalsIgnoreCase("ping-success")) {
                        ServerResponseModel response = new Gson().fromJson(message, ServerResponseModel.class);
                        bluetoothManager.arduinoDataSendModel = response;
                        bluetoothManager.sendToArduino();
                        if(response.command.equalsIgnoreCase("sendOffer"))
                        {
                            camWebSocketTread.sendOffer(response.commandPayload);
                        }
                    } else if (message.equalsIgnoreCase("ping-success")){
                        webSocketPing.reset();
                        Log.d("WebSocket", "ping success");
                    }
                }
                catch (Exception e)
                {
                    Log.d("ServerResponse",e.getMessage());
                }
            }
            @Override
            public void onError(Exception e) {
                Log.d("conn", e.getMessage());
            }
            @Override
            public void onClose(int arg1, String arg2, boolean arg3) {
                Log.i("WebSocket", "Closed ");
            }
        };
        webSocketClient.connect();
    }

    private class CamWebSocketTread extends Thread
    {
        @Override
        public void run() {
            try{
                startPeerConnection();
                Thread.sleep(2000);
                URI uri;
                try {
                    uri = new URI(HttpUtils.CAM_WEB_SOCKET_URL);
                }
                catch (URISyntaxException e) {
                    e.printStackTrace();
                    return;
                }
                wsClient = new WebSocketClient(uri) {
                    @Override
                    public void onOpen(ServerHandshake arg1) {
                        Log.i("WebSocket", "Session is starting");
                    }
                    @Override
                    public void onMessage(String message) {
                        try{
                            Log.d("Message FromServer",message);
                            WebRTCResponseModel data = new Gson().fromJson(message, WebRTCResponseModel.class);
                            switch(data.type) {
                                case "offer":
                                    Log.d("offer",data.offer.sdp);
                                    SessionDescription sdp = new SessionDescription(
                                            SessionDescription.Type.fromCanonicalForm(data.offer.type),
                                            data.offer.sdp
                                    );

                                    peerConnection.setRemoteDescription(new SimpleSdpObserver(),sdp);
                                    //create an answer to an offer
                                    peerConnection.createAnswer(new SimpleSdpObserver() {
                                        @Override
                                        public void onCreateSuccess(SessionDescription sessionDescription) {
                                            peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                                            JSONObject message = new JSONObject();
                                            JSONObject answer = new JSONObject();
                                            try {
                                                message.put("name", "B");
                                                message.put("type", "answer");
                                                answer.put("sdp", sessionDescription.description);
                                                answer.put("type", "answer");
                                                message.put("answer", answer);
                                                sendMessage(message);
                                                vc.changeCaptureFormat(1280,720,20);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }, new MediaConstraints());
                                    break;
                                case "candidate":
                                    try{
                                        peerConnection.addIceCandidate(new IceCandidate(
                                                data.candidate.sdpMid,
                                                data.candidate.sdpMLineIndex,
                                                data.candidate.candidate));
                                    } catch (Exception e)
                                    {
                                        Log.e("candidate-01", e.getMessage());
                                    }
                                    break;
                                case "answer":
                                    try{
                                        SessionDescription temSdp = new SessionDescription(
                                                SessionDescription.Type.fromCanonicalForm(data.answer.type),
                                                data.answer.sdp
                                        );
                                        peerConnection.setRemoteDescription(new SimpleSdpObserver(),temSdp);
                                    } catch (Exception e)
                                    {
                                        Log.e("answer-01", e.getMessage());
                                    }
                                    break;
                                case "leave":
                                {
                                    peerConnection.close();
                                    Thread.sleep(2000);
                                    startPeerConnection();
                                }
                                default:
                                    break;
                            }

                        }
                        catch (Exception e)
                        {
                            Log.d("ServerResponse",e.getMessage());
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        Log.d("conn", e.getMessage());
                    }
                    @Override
                    public void onClose(int arg1, String arg2, boolean arg3) {
                        Log.i("WebSocket", "Closed ");
                    }
                };
                wsClient.connect();

                Thread.sleep(2000);
                JSONObject message = new JSONObject();
                try {
                    message.put("type", "login");
                    message.put("name","A");
                    sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            catch (Exception e)
            {
                Log.e("Thread Sleep",e.toString());
            }
        }
        public void sendMessage(JSONObject message)
        {
            Log.d("Sending message", message.toString());
            wsClient.send(message.toString());
        }

        public void sendOffer(final String userName)
        {
            peerConnection.createOffer(new SimpleSdpObserver() {
                @Override
                public void onCreateSuccess(SessionDescription sessionDescription) {
                    peerConnection.setLocalDescription(new SimpleSdpObserver(), sessionDescription);
                    JSONObject message = new JSONObject();
                    JSONObject offer = new JSONObject();
                    try {
                        message.put("name", userName);
                        message.put("type", "offer");
                        offer.put("type","offer");
                        offer.put("sdp", sessionDescription.description);
                        message.put("offer", offer);
                        sendMessage(message);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, new MediaConstraints());
        }

        public void startPeerConnection() {
            ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
            iceServers.add(new PeerConnection.IceServer("stun:stun.l.google.com:19302?transport=udp"));

            peerConnection = pcFactory.createPeerConnection(
                    iceServers,
                    new MediaConstraints(),
                    new PeerConnection.Observer() {
                        @Override
                        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

                        }

                        @Override
                        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

                        }

                        @Override
                        public void onIceConnectionReceivingChange(boolean b) {

                        }

                        @Override
                        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

                        }

                        @Override
                        public void onIceCandidate(IceCandidate iceCandidate) {
                            JSONObject message = new JSONObject();
                            JSONObject offer = new JSONObject();
                            try {
                                message.put("name", "B");
                                message.put("type", "candidate");
                                offer.put("sdpMid",iceCandidate.sdpMid);
                                offer.put("candidate", iceCandidate.sdp);
                                offer.put("sdpMLineIndex", iceCandidate.sdpMLineIndex);
                                message.put("candidate", offer);
                                Log.d("Sending message", message.toString());
                                camWebSocketTread.sendMessage(message);
                            } catch (Exception e)
                            {
                                Log.e("IceCandidate",e.getMessage());
                            }

                        }

                        @Override
                        public void onAddStream(MediaStream mediaStream) {
                        }

                        @Override
                        public void onRemoveStream(MediaStream mediaStream) {

                        }

                        @Override
                        public void onDataChannel(DataChannel dataChannel) {

                        }

                        @Override
                        public void onRenegotiationNeeded() {

                        }
                    }
            );
            peerConnection.addStream(localMediaStream);
            final DataChannel localDataChannel = peerConnection.createDataChannel("sendDataChannel", new DataChannel.Init());
            localDataChannel.registerObserver(new DataChannel.Observer() {
                @Override
                public void onBufferedAmountChange(long l) {
                }
                @Override
                public void onStateChange() {
                    Log.d("DC", "onStateChange: " + localDataChannel.state().toString());
                    /*runOnUiThread(() -> {

                     */
                }
                @Override
                public void onMessage(DataChannel.Buffer buffer) {
                    String message = bb_to_str(buffer.data,Charset.defaultCharset());
                    Log.d("DC", "Message" + message);
                    try{
                        if(message != "" && !message.equalsIgnoreCase("ping-success")) {
                            ServerResponseModel response = new Gson().fromJson(message, ServerResponseModel.class);
                            bluetoothManager.arduinoDataSendModel = response;
                            bluetoothManager.sendToArduino();
                            if(response.command.equalsIgnoreCase("sendOffer"))
                            {
                                camWebSocketTread.sendOffer(response.commandPayload);
                            }
                        } else if (message.equalsIgnoreCase("ping-success")){
                            webSocketPing.reset();
                            Log.d("WebSocket", "ping success");
                        }
                    }
                    catch (Exception e)
                    {
                        Log.d("ServerResponse",e.getMessage());
                    }

                }
            });
        }
    }
    public static ByteBuffer str_to_bb(String msg, Charset charset){
        return ByteBuffer.wrap(msg.getBytes(charset));
    }
    public static String bb_to_str(ByteBuffer buffer, Charset charset){
        byte[] bytes;
        if(buffer.hasArray()) {
            bytes = buffer.array();
        } else {
            bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
        }
        return new String(bytes, charset);
    }

}
