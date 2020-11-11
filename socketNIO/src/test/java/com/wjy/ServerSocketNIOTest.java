package com.wjy;

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
        try(ServerSocketMultiplexingMultithread3 serverSocket = new ServerSocketMultiplexingMultithread3(port)){
            serverSocket.init();
            serverSocket.start();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Test
    public void test(){
        int read = 1;
        int write = 4;
        System.out.println(~1);
        System.out.println(1 | ~1);

        System.out.println(4 & ~4);
    }

}
