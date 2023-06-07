package com.example.filesynchttp;

public enum ConnectionStatus {
    NONE,
    PEER_REQUEST_PENDING,
    PEER_REQUEST_ACCEPTED,
    PEER_REQUEST_REFUSED,
    OUR_REQUEST_PENDING,
    OUR_REQUEST_ACCEPTED,
    OUR_REQUEST_REJECTED
}
