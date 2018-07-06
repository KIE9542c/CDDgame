import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 服务器入口点
 * ServerProvide类负责打开端口，接受客户端的请求，创建一个socket并使用线程池
 * 中的一条线程调用ServerTransfer对象开始工作
 *
 * @author Yukino Yukinoshita
 * @version 1.0
 */

class ServerProvider implements Runnable {

    /**
     * 服务器端口号
     */
    private final int port;

    /**
     * 服务端socket 用于监听端口
     */
    private ServerSocket serverSocket = null;

    /**
     * 指定ThreadPoolExecutor中的核心线程数
     */
    private final int coreThread = 2;

    /**
     * 指定ThreadPoolExecutor中的最大线程数
     */
    private final int maxThread = 4;

    /**
     * 指定ThreadPoolExecutor中的线程最大生存时间
     * 单位默认为Millisecond
     */
    private final int aliveTime = 240;

    /**
     * 指定当ThreadPoolExecutor中线程不足时，允许最大排队请求数
     */
    private final int queueLength = 2;

    /**
     * ThreadPoolExecutor线程池对象
     */
    private final ThreadPoolExecutor threadPoolExecutor =
            new ThreadPoolExecutor(coreThread, maxThread, aliveTime,
                    TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(queueLength));

    /**
     * 构造方法 初始化端口号
     *
     * @param port 服务端端口号
     */
    private ServerProvider(int port) {
        this.port = port;
    }

    /**
     * 实现Runnable接口的run方法
     * 打开服务端socket并进行监听，当收到客户端请求时，调用线程池中
     * 一条线程进行处理请求
     */
    @Override
    public void run() {
        //TODO:新加的测试能否防止崩溃
        threadPoolExecutor.allowCoreThreadTimeOut(true);

        openServerSocket();
        while (true) {
            Socket socket = null;
            try {
                // TODO: check null for serverSocket
                socket = this.serverSocket.accept();
            } catch (IOException e) {
                // TODO: Exception handling
                e.printStackTrace();
            }
            // TODO: check null for socket
            this.threadPoolExecutor.execute(new ServerTransfer(port, socket));
        }
    }

    /**
     * 程序的入口点，新建3条线程，分别监听不同端口
     * TODO: merge 3 ports into only one port.
     *
     * @param args 程序入参
     */
    public static void main(String[] args) {
        new Thread(new ServerProvider(8080)).start();
        new Thread(new ServerProvider(8081)).start();
        new Thread(new ServerProvider(8082)).start();
        new Thread(new ServerProvider(8083)).start();
    }

    /**
     * 打开服务端socket
     */
    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            // TODO: Exception handling
            e.printStackTrace();
        }
    }
}
