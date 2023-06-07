package com.example.filesynchttp;

public class ConnectedDevice {
    public String IP;
    public String deviceName;
    public Boolean selectedForPairing;
    public Boolean requestedForPairing;
    public Boolean inSync;
    public ConnectionStatus status;

    public ConnectedDevice(String IP, String deviceName) {
        this.IP = IP;
        this.deviceName = deviceName;
        this.selectedForPairing = false;
        this.requestedForPairing = false;
        this.inSync = false;
        this.status = ConnectionStatus.NONE;
    }
}
