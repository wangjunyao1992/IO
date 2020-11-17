package com.wjy;

import com.wjy.multithreading.ServerSocketMultiplexingMultithreading;
import org.junit.Test;

/**
 * @author wangjunyao
 * @version 1.0.0
 * @Description TODO
 * @createTime 2020年11月09日 15:13:00
 */
public class ServerSocketNIOTest {

    private int port = 8090;

    @Test
    public void socketNIOTest(){
        try(ServerSocketNIO serverSocketNIO = new ServerSocketNIO(port)){
            serverSocketNIO.init();
            serverSocketNIO.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void serverSocketMultiplexingSingleThread1Test(){
        try(ServerSocketMultiplexingSingleThread1 serverSocket = new ServerSocketMultiplexingSingleThread1(port)){
            serverSocket.init();
            serverSocket.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void serverSocketMultiplexingSingleThread2Test(){
        try(ServerSocketMultiplexingSingleThread2 serverSocket = new ServerSocketMultiplexingSingleThread2(port)){
            serverSocket.init();
            serverSocket.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void serverSocketMultiplexingMultithread3Test(){
        try(ServerSocketMultiplexingSingleThread3 serverSocket = new ServerSocketMultiplexingSingleThread3(port)){
            serverSocket.init();
            serverSocket.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 测试方法   在另外一个线程中死循环，会随着主线程的结束而结束
     */
    @Test
    public void multithreadingTest(){
        try(ServerSocketMultiplexingMultithreading server = new ServerSocketMultiplexingMultithreading(port)){
            server.init();
            server.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //在try中，随着方法结束会关闭
        try(ServerSocketMultiplexingMultithreading server = new ServerSocketMultiplexingMultithreading(8090)){
            server.init();
            server.start();
            //这里加个阻塞
            System.in.read();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void test(){
        Aa aa = new Aa();
        new Thread(aa).start();
    }



}

class  Aa implements Runnable{

    @Override
    public void run() {
        while (true) {
            System.out.println("11111111");
        }
    }

}
