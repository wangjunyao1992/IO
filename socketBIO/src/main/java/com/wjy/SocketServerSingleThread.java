package com.wjy;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * BIO 单线程server
 * 在BIO单线程模型下，serverSocket.accept();和reader.readLine();
 * 都会造成阻塞，因此在该模型下，服务端每次只能处理一个客户端请求
 */
public class SocketServerSingleThread implements ISocketServer, ILifeCycle {

    private final int port;

    private ServerSocket serverSocket;

    public SocketServerSingleThread(int port){
        this.port = port;
    }

    public void init() throws Exception{
        serverSocket = new ServerSocket(port);
    }

    public void stop() throws Exception{
        if (serverSocket != null){
            serverSocket.close();
        }
    }

    public void start() throws Exception{
        System.out.println("server start...");
        while (true){
            /**
             *此方法会一直阻塞到有客户端连接
             */
            Socket client = serverSocket.accept();
            System.out.println("客户端 " + client.getLocalAddress().getHostAddress() + ":" + client.getPort() + "连接");
            BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
            while (true){
                /**
                 * 此方法会一直阻塞到有数据输入，此socket对应的客户端发送了数据
                 */
                String line = reader.readLine();
                if (null != line){
                    System.out.println(line);
                    writer.write(line + "\r\n");
                    writer.flush();
                }else {
                    client.close();
                    System.out.println("客户端断开连接~~~");
                    break;
                }
            }
            System.out.println("客户端断开连接");
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
