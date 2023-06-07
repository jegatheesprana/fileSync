package com.example.filesynchttp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.filesynchttp.adapters.DeviceItemAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    Button  discoverButton, openFolder, buttonSync, connectionRequestBtn_cancel, connectionRequestBtn_okay;
    ListView deviceList;
    TextView connectionStatus, deviceNameText, connectionRequestMsg;
    Dialog connectionRequestDialog;

    private AndroidWebServer server;
    private String deviceName;
    String myIp;

    ArrayList<ConnectedDevice> availableDevices = new ArrayList<>();
    ServiceDiscovery serviceDiscovery;
    FileManager fileManager;
    Communicator communicator;

    String folderPath;

    List<FileDetail> fileDetails = new ArrayList<>();

    Map<String, String> connectionRequest = new HashMap<>();

    int requests = 0;

    ActivityResultLauncher<Intent> folderActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Uri uri = data.getData();

                        Log.d("Path", uri.getPath());

                        String [] pathsections = uri.getPath().split(":");
                        folderPath  = Environment.getExternalStorageDirectory() + "/"+ pathsections[pathsections.length-1];

                        Log.d("Path", folderPath);
                        fileManager.init();
                    }
                }
            }
    );

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE},
                PackageManager.PERMISSION_GRANTED);

        initiate();
        exqListener();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (server != null)
            server.stop();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void initiate() {
        connectionStatus = findViewById(R.id.connectionStatus);
        deviceNameText = findViewById(R.id.deviceName);
        discoverButton = findViewById(R.id.buttonDiscover);
        deviceList = findViewById(R.id.deviceList);
        openFolder = findViewById(R.id.buttonOpenFolder);
        buttonSync = findViewById(R.id.buttonSync);

        connectionRequestDialog = new Dialog(MainActivity.this);
        connectionRequestDialog.setContentView(R.layout.connection_request_dialog);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectionRequestDialog.getWindow().setBackgroundDrawable(getDrawable(R.drawable.connection_request_background));
        }
        connectionRequestDialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        connectionRequestDialog.setCancelable(false);

        connectionRequestMsg = connectionRequestDialog.findViewById(R.id.connectionRequestMsg);
        connectionRequestBtn_cancel = connectionRequestDialog.findViewById(R.id.connectionRequestBtn_cancel);
        connectionRequestBtn_okay = connectionRequestDialog.findViewById(R.id.connectionRequestBtn_okay);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            deviceName = Settings.Global.getString(getContentResolver(), Settings.Global.DEVICE_NAME);
        } else {
            deviceName = "Unknown";
        }

        deviceNameText.setText("Device Name\n"+deviceName);

        server = new AndroidWebServer(8080, deviceName, this);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        myIp = getIPAddress(true);
        serviceDiscovery = new ServiceDiscovery();
        serviceDiscovery.start();
        fileManager = new FileManager();
        fileManager.start();
        communicator = new Communicator();
        communicator.start();
    }

    private void exqListener() {
        discoverButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View view) {
                connectionStatus.setText("Scanning devices");
                serviceDiscovery.discoverServices();
            }
        });

//        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
//
//            }
//        });

        openFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                chooseFolder(null);
            }
        });

        buttonSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (folderPath == null) {
                    chooseFolder("Choose directory to sync");
                } else {
                    communicator.requestToConnect();
                    communicator.initiateInterval();
                }
            }
        });

        connectionRequestBtn_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String IP = connectionRequest.get("IP");
                connectionRequestDialog.dismiss();
                ConnectedDevice peerDevice = findDeviceByIP(IP);
                peerDevice.status = ConnectionStatus.PEER_REQUEST_REFUSED;
                updateDeviceListUI();
                communicator.refuseConnection(IP);
            }
        });

        connectionRequestBtn_okay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (folderPath == null) {
                    chooseFolder("Choose directory to sync");
                } else {
                    String IP = connectionRequest.get("IP");
                    connectionRequestDialog.dismiss();
                    ConnectedDevice peerDevice = findDeviceByIP(IP);
                    peerDevice.status = ConnectionStatus.PEER_REQUEST_ACCEPTED;
                    peerDevice.inSync = true;
                    updateDeviceListUI();
                    communicator.acceptConnection(IP);
                }
            }
        });
    }

    public void updateDeviceListUI() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceItemAdapter deviceItemAdapter = new DeviceItemAdapter(getApplicationContext(), availableDevices);
                deviceList.setAdapter(deviceItemAdapter);
            }
        });
    }

    public void showModal(String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionRequestMsg.setText(message);
                connectionRequestDialog.show();
            }
        });
    }

    public void chooseFolder(@Nullable String message) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP){
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            i = Intent.createChooser(i, (message != null && message.isEmpty())? "Choose directory":message);
            folderActivityResultLauncher.launch(i);
        }
    }

    public ConnectedDevice findDeviceByIP(String IP) {
        ConnectedDevice foundDevice=null;
        for(ConnectedDevice connectedDevice: availableDevices) {
            if (connectedDevice.IP.equals(IP)) {
                foundDevice = connectedDevice;
            }
        }
        return foundDevice;
    }

    public void incomingRequestToConnect(String IP, String requestingDeviceName) {
        ConnectedDevice requestingDevice = findDeviceByIP(IP);
        if (requestingDevice==null) {
            requestingDevice = new ConnectedDevice(IP, requestingDeviceName);
            availableDevices.add(requestingDevice);
        }

        Log.d("Status", requestingDevice.deviceName);

        connectionRequest.put("IP", IP);

        requestingDevice.status = ConnectionStatus.PEER_REQUEST_PENDING;
        updateDeviceListUI();

        showModal("Device " + requestingDevice.deviceName + " wants to sync with your folder");

        Log.d("Status2", requestingDevice.deviceName);
    }

    public Boolean connectionAccpeted(String IP) {
        ConnectedDevice peerDevice = findDeviceByIP(IP);
        if (peerDevice != null && peerDevice.requestedForPairing ) {
            peerDevice.status = ConnectionStatus.PEER_REQUEST_ACCEPTED;
            peerDevice.inSync = true;
            updateDeviceListUI();
            communicator.getFileDetails(IP);
            communicator.initiateInterval();
        } else {
            return false;
        }
        return true;
    }

    public Boolean connectionRefused(String IP) {
        ConnectedDevice peerDevice = findDeviceByIP(IP);
        if (peerDevice != null && peerDevice.requestedForPairing ) {
            peerDevice.status = ConnectionStatus.OUR_REQUEST_REJECTED;
            updateDeviceListUI();
            return true;
        } else {
            return false;
        }
    }

    public Boolean isDeviceAuthorized(String IP) {
        ConnectedDevice peerDevice = findDeviceByIP(IP);
        return peerDevice != null && peerDevice.inSync;
    }

    private void sendRequest(String url) {

        // getting a new volley request queue for making new requests
        RequestQueue volleyQueue = Volley.newRequestQueue(MainActivity.this);

        RequestFuture<JSONObject> requestFuture = RequestFuture.newFuture();

        // since the response we get from the api is in JSON, we
        // need to use `JsonObjectRequest` for parsing the
        // request response
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                // we are using GET HTTP request method
                Request.Method.GET,
                // url we want to send the HTTP request to
                url,
                // this parameter is used to send a JSON object to the
                // server, since this is not required in our case,
                // we are keeping it `null`
                null,
                requestFuture,
                requestFuture
        );

        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                500,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        // add the json request object created above
        // to the Volley request queue
        volleyQueue.add(jsonObjectRequest);

        try {
            // Wait for the request to complete synchronously
            JSONObject response = requestFuture.get();

            // Handle the case when the HTTP request succeeds
            String dogImageUrl = response.getString("message");

            // Load the image into the ImageView using Glide
            // (Code for loading the image goes here)

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }

    class ServiceDiscovery extends Thread {
        @Override
        public void run() {

        }

        public void discoverServices() {

            availableDevices = new ArrayList<>();
            updateDeviceListUI();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    String[] myIPArray = myIp.split("\\.");
                    InetAddress currentPingAddr;

                    for (int i = 0; i <= 255; i++) {
                        try {

                            // build the next IP address
                            currentPingAddr = InetAddress.getByName(myIPArray[0] + "." +
                                    myIPArray[1] + "." +
                                    myIPArray[2] + "." +
                                    Integer.toString(i));

                            // 50ms Timeout for the "ping"
                            if (!currentPingAddr.equals(InetAddress.getByName(myIp)) && currentPingAddr.isReachable(50)) {
                                String finalCurrentPingAddr = currentPingAddr.getHostAddress();
                                String deviceName = sendRequest("http://"+finalCurrentPingAddr+":8080/name");
                                if (deviceName != null && !deviceName.isEmpty()) {
                                    Log.d("IP", "Got response from "+finalCurrentPingAddr);
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            ConnectedDevice device = new ConnectedDevice(
                                                    finalCurrentPingAddr,
                                                    deviceName
                                            );


                                            .add(device);

//                                            String[] namesList = new String[availableDevices.size()];
//                                            int i=0;
//                                            for (ConnectedDevice map : availableDevices) {
//                                                if (!map.deviceName.isEmpty()) {
//                                                    namesList[i] = (map.deviceName);
//                                                    i++;
//                                                }
//                                            }
//                                            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, namesList);
//                                            DeviceItemAdapter deviceItemAdapter = new DeviceItemAdapter(getApplicationContext(), availableDevices);
//                                            deviceList.setAdapter(deviceItemAdapter);
                                            updateDeviceListUI();
                                        }
                                    });
                                } else {
                                    Log.d("IP", "No response from "+finalCurrentPingAddr);
                                }

                            }
                        } catch (UnknownHostException ex) {
                        } catch (IOException ex) {
                        }
                    }

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            connectionStatus.setText("Scanning completed");
                        }
                    });
                }
            });
        }

        private String sendRequest(String url) {
            RequestQueue volleyQueue = Volley.newRequestQueue(MainActivity.this);
            RequestFuture<String> requestFuture = RequestFuture.newFuture();

            StringRequest jsonObjectRequest = new StringRequest(
                    Request.Method.GET,
                    url,
                    requestFuture,
                    requestFuture
            );

            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    500,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            volleyQueue.add(jsonObjectRequest);

            String response = "";

            try {
                response = requestFuture.get();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            return response;
        }
    }

    public class FileManager extends Thread {

        public void init() {
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    fileDetails = readFiles(folderPath, "");
                    Log.d("files", "Reading files");
                }
            }, 0, 5000);
        }

        public List<FileDetail> readFiles(String folderPath, String root) {
            List<FileDetail> localFileDetails = new ArrayList<>();

            File directory = new File(folderPath);

            File[] files = directory.listFiles();
            if (files == null) {
                Log.d("Files", "Empty File");
                return localFileDetails;
            }

            for(File file: files) {
                if (file.isDirectory()) {
                    Log.d("Directory", file.getAbsolutePath());
                    List<FileDetail> directoryFileDetails = readFiles(file.getPath(), root + file.getName() + "/");
                    localFileDetails.addAll(directoryFileDetails);
                } else {
                    FileDetail fileDetail = new FileDetail(
                            file.getName(),
                            file.getPath(),
                            root+file.getName(),
                            new Date(file.lastModified())
                    );
                    Log.d("Files", fileDetail.toString());
                    localFileDetails.add(fileDetail);
                }
            }

            return localFileDetails;
        }
    }

    class Communicator extends Thread {

        public void requestToConnect() {


            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

//            connectionStatus.setText("Sync Starting");

            executor.execute(new Runnable() {
                @Override
                public void run() {

                    for(ConnectedDevice connectedDevice: availableDevices) {
                        if (connectedDevice.selectedForPairing) {
                            connectedDevice.requestedForPairing = true;
                            sendRequest("http://"+connectedDevice.IP+":8080/requestToConnect?deviceName="+deviceName.replace(" ", "%20"));
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    connectedDevice.status = ConnectionStatus.OUR_REQUEST_PENDING;
                                    DeviceItemAdapter deviceItemAdapter = new DeviceItemAdapter(getApplicationContext(), availableDevices);
                                    deviceList.setAdapter(deviceItemAdapter);
                                }
                            });
                        }
                    }

//                    handler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            connectionStatus.setText("Syncing");
//                        }
//                    });
                }
            });
        }

        public void initiateInterval() {
            new Timer().scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    Log.d("interval", "Sending requested");
                    for (ConnectedDevice device: availableDevices) {
                        if (device.inSync) {
                            getFileDetails(device.IP);
                        }
                    }
                }
            }, 2000, 2000);
        }

        public void refuseConnection(String IP) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    sendRequest("http://"+IP+":8080/requestToConnect/refuse");
                }
            });
        }

        public void acceptConnection(String IP) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    sendRequest("http://"+IP+":8080/requestToConnect/accept");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            getFileDetails(IP);
                        }
                    });
                }
            });
        }

        public void getFileDetails(String IP) {

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());

            executor.execute(new Runnable() {
                @Override
                public void run() {
                    String filesStr = sendRequest("http://"+IP+":8080/files");
                    try {
                        JSONArray fileJsonArr = new JSONArray(filesStr);

                        for(int i = 0, size = fileJsonArr.length(); i<size; i++) {
                            JSONObject fileJson = fileJsonArr.getJSONObject(i);
                            String fileName = fileJson.getString("fileName");
                            String relativePath = fileJson.getString("relativePath");
                            Date lastModified = new Date(fileJson.getString("lastModified"));

                            Log.d("getFileDetails", fileName);

                            Boolean present= false;

                            for (FileDetail fileDetail: fileDetails) {
                                if (fileDetail.relativePath.equals(relativePath) && (fileDetail.lastModified.equals(lastModified) || fileDetail.lastModified.after(lastModified))) {
                                    present = true;
                                }
                            }

                            if (!present) {
                                requests++;
                                connectionStatus.setText("Sync starting");
                                RequestQueue volleyQueue = Volley.newRequestQueue(MainActivity.this);
                                String URL = "http://"+IP+":8080/file?filePath="+relativePath.replace(" ", "%20");
                                InputStreamVolleyRequest request = new InputStreamVolleyRequest(Request.Method.POST, URL, new Response.Listener<byte[]>() {
                                    @Override
                                    public void onResponse(byte[] response) {
                                        HashMap<String, Object> map = new HashMap<String, Object>();
                                        try {
                                            if (response!=null) {
                                                try{

                                                    long lenghtOfFile = response.length;

                                                    //covert reponse to input stream
                                                    InputStream input = new ByteArrayInputStream(response);
                                                    File file = new File(folderPath + "/" + relativePath);

                                                    File directory = new File(file.getParent());

                                                    if (!directory.exists()) {
                                                        directory.mkdir();
                                                    }

                                                    file.setLastModified(lastModified.getTime());
                                                    map.put("resume_path", file.toString());
                                                    BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
                                                    byte data[] = new byte[1024];

                                                    long total = 0;
                                                    int count;

                                                    while ((count = input.read(data)) != -1) {
                                                        total += count;
                                                        output.write(data, 0, count);
                                                    }

                                                    output.flush();

                                                    output.close();
                                                    input.close();

                                                    FileDetail newFile = new FileDetail(fileName, (folderPath + "/" + relativePath), relativePath, lastModified);
                                                    fileDetails.add(newFile);

                                                    requests--;
                                                    if (requests==0) {
                                                        connectionStatus.setText("Sync completed");
                                                    }
                                                }catch(IOException e){
                                                    e.printStackTrace();

                                                }
                                            }
                                        } catch (Exception e) {
                                            // TODO Auto-generated catch block
                                            Log.d("KEY_ERROR", "UNABLE TO DOWNLOAD FILE");
                                            e.printStackTrace();
                                        }
                                    }
                                }, new Response.ErrorListener() {
                                    @Override
                                    public void onErrorResponse(VolleyError error) {

                                    }
                                }, null);
                                volleyQueue.add(request);
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });

        }

        private String sendRequest(String url) {
            RequestQueue volleyQueue = Volley.newRequestQueue(MainActivity.this);
            RequestFuture<String> requestFuture = RequestFuture.newFuture();

            StringRequest jsonObjectRequest = new StringRequest(
                    Request.Method.GET,
                    url,
                    requestFuture,
                    requestFuture
            );

            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    500,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            volleyQueue.add(jsonObjectRequest);

            String response = "";

            try {
                response = requestFuture.get();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            return response;
        }
    }
}