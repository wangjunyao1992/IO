package com.wjy;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * BIO多线程模型
 *
 * java BIO服务端为什么要一个线程对应一个连接，为什么一定要开线程去处理？？？
 *
 * 因为
 *  1)serverSocket.accept();接受客户端连接的方法，必须等到有客户端连接的时候才会解除阻塞；
 *  2)reader.readLine();必须等此客户端发来了数据解会解除阻塞
 * 如果一个客户端连接之后，这个客户端没有发来数据，则代码会一直卡在reader.readLine();此时有
 * 客户端连接，是不会接受连接的，必须要等到第一个连接的客户端发来数据才可以。
 * 同理，当第一个客户端发来了数据，代码将执行到serverSocket.accept();阻塞，那么此时第一个连
 * 接上来的客户端发来数据，reader.readLine();方法是不会调用的，那么就不会读取客户端发来的数据，
 * 客户端发来一条数据，服务器有可能很久不会响应
 *
 * 所有BIO模型一定要开线程去处理
 *
 * 在BIO模型中，服务端线程与客户端线程是1:1的关系，也就是说有多少个客户端，服务端就得创建多数个线程
 * 在java当中，创建线程的代价是很大的。如果有几千个客户端，这个程序是撑不住的
 *
 * @author wangjunyao
 * @version 1.0.0
 * @Description TODO
 * @createTime 2020年11月09日 10:45:00
 */
public class SocketServerMultithreading implements ISocketServer, ILifeCycle{

    private final int port;

    private ServerSocket serverSocket;

    public SocketServerMultithreading(int port){
        this.port = port;
    }

    @Override
    public void init() throws Exception {
        serverSocket = new ServerSocket(port);
    }

    @Override
    public void stop() throws Exception {
        if (serverSocket != null){
            serverSocket.close();
        }
    }

    @Override
    public void start() throws Exception {
        System.out.println("server start...");
        while (true){
            /**
             *此方法会一直阻塞到有客户端连接
             */
            Socket client = serverSocket.accept();
            System.out.println("客户端 " + client.getLocalAddress().getHostAddress() + ":" + client.getPort() + "连接");
            new Thread(()->{
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream()));
                    while (true){
                        /**
                         * 此方法会一直阻塞到有数据输入，此socket对应的客户端发送了数据
                         */
                        String line = reader.readLine();
                        if (line != null){
                            System.out.println(line);
                            writer.write(line + "\r\n");
                            writer.flush();
                        }else {
                            System.out.println("客户端 " + client.getLocalAddress().getHostAddress() + ":" + client.getPort() + "断开连接");
                            client.close();
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }).start();
        }
    }

    @Override
    public void close() {
        try {
            stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
