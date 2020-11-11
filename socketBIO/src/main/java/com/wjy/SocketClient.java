package com.wjy;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SocketClient implements ISocketServer, ILifeCycle {

    private final String ip;

    private final int port;

    private Socket clientSocket;

    public SocketClient(String ip, int port){
        this.ip = ip;
        this.port = port;
    }

    public void init() throws Exception {
        clientSocket = new Socket(ip, port);
    }

    public void stop() throws Exception {
        if (clientSocket != null){
            clientSocket.close();
        }
    }

    public void start() throws Exception {
        OutputStream outputStream = clientSocket.getOutputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        InputStream in = System.in;
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        while (true){
            String line = reader.readLine();
            if (line != null){
                outputStream.write((line + "\r\n").getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
                String readLine = bufferedReader.readLine();
                System.out.println(readLine);
            }
        }
    }

    public void close() {
        try {
            stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
