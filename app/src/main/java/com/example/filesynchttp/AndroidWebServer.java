package com.example.filesynchttp;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class AndroidWebServer extends NanoHTTPD {

    private String deviceName;
    private MainActivity activity;

    public AndroidWebServer(int port, String deviceName, MainActivity activity) {
        super(port);
        this.deviceName = deviceName;
        this.activity = activity;
    }

    public AndroidWebServer(String hostname, int port, String deviceName) {
        super(hostname, port);
        this.deviceName = deviceName;
    }

    @Override
    public NanoHTTPD.Response serve(String uri, Method method,
                                    Map<String, String> header,
                                    Map<String, String> parameters,
                                    Map<String, String> files) {
        String IP = header.get("remote-addr");
        switch(uri) {
//            case "/":
//                return newFixedLengthResponse( "Hi" );
//            case "/ip":
//                return newFixedLengthResponse( activity.myIp );
            case "/name":
                return newFixedLengthResponse( deviceName );


            case "/files":
                if (!activity.isDeviceAuthorized(IP)) {
                    return newFixedLengthResponse( Response.Status.UNAUTHORIZED, "text/plain", "You don't have access");
                }
                List<Map<String, String>> fileDetailsArr = new ArrayList<>();
                for(FileDetail fileDetail: activity.fileDetails) {
                    Map<String, String> fileMap = new HashMap<>();
                    fileMap.put("fileName", fileDetail.fileName);
                    fileMap.put("relativePath", fileDetail.relativePath);
                    fileMap.put("lastModified", fileDetail.lastModified.toString());
                    fileDetailsArr.add(fileMap);
                }
                JSONArray fileDetails = new JSONArray(fileDetailsArr);
                return newFixedLengthResponse( Response.Status.OK, "application/json", fileDetails.toString() );
            case "/file":
                if (!activity.isDeviceAuthorized(IP)) {
                    return newFixedLengthResponse( Response.Status.UNAUTHORIZED, "text/plain", "You don't have access");
                }
                String filePath = parameters.get("filePath");
                String foundFilePath=null;
                for (FileDetail file: activity.fileDetails) {
                    if (file.relativePath.equals(filePath)) {
                        foundFilePath = file.path;
                    }
                }
                if (foundFilePath==null) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "File not found");
                }
                try {
                    File foundFile = new File(foundFilePath);
                    FileInputStream fis = new FileInputStream(foundFile);
                    NanoHTTPD.Response res = newChunkedResponse(Response.Status.OK, "text/plain", fis);
//                    res.addHeader("Content-Disposition", "attachment; filename=\""+foundFile.getName()+"\"");
                    return res;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Internal Server error");
                }
            case "/requestToConnect":
                String requestingDeviceName = parameters.get("deviceName");
                activity.incomingRequestToConnect(IP, requestingDeviceName);
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "ack");
            case "/requestToConnect/accept":
                activity.connectionAccpeted(IP);
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "ack");
            case "/requestToConnect/refuse":
                activity.connectionRefused(IP);
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "ack");
            default:
                return newFixedLengthResponse( uri + " " +deviceName );
        }
    }
}
