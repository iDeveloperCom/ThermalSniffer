package com.ideveloper.thermalsniffer;

public interface DataPassListener {
    public void passData(long deviceID, float temperature, float wind);
}
