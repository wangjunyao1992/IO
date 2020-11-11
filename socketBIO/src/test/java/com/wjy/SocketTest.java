package com.wjy;

import org.junit.Test;

public class SocketTest {

    public static void main(String[] args) {
        try(SocketClient client = new SocketClient("127.0.0.1", 8081);){
            client.init();
            client.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void testClientSocket() {
        try(SocketClient client = new SocketClient("127.0.0.1", 8081);){
            client.init();
            client.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testServerSocketSingleThread(){
        try(SocketServerSingleThread socketServer = new SocketServerSingleThread(8081)){
            socketServer.init();
            socketServer.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void testServerSocketMultithreading(){
        try (SocketServerMultithreading socketServer = new SocketServerMultithreading(8081)){
            socketServer.init();
            socketServer.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
