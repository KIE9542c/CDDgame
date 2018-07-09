import java.io.*;
import java.net.Socket;

/**
 * ServerTransfer类不负责具体message处理操作，只负责转发到ServerPresenter对象中
 * 并让其处理message
 * 该类开始执行时，先从socket中读取message并转发给ServerPresenter
 * 随后等待ServerPresenter调用responseMessage向客户端返回message
 * 在responseMessage方法末端执行关闭socket
 *
 * @author Yukino Yukinoshita
 * @version 1.0
 */

public class ServerTransfer implements ServerContract.Transfer, Runnable {

    /**
     * Presenter
     */
    private ServerContract.Presenter mServerPresenter;

    /**
     * 服务端端口号
     */
    private final int port;

    /**
     * socket
     */
    private final Socket socket;

    /**
     * DataInputStream对象，用于从socket读取客户端发来的信息
     */
    private DataInputStream in = null;

    /**
     * DataOutputStream对象，用于向socket写入信息
     */
    //TODO:暂时不用private DataOutputStream out = null;

    /**
     * 构造方法 初始化端口号和socket
     *
     * @param port   服务端端口号
     * @param socket 传入的socket
     */
    ServerTransfer(int port, Socket socket) {
        this.port = port;
        this.socket = socket;
        new ServerPresenter(this);
    }

    /**
     * 向客户端回应信息并关闭该socket
     *
     * @param message 向客户端回应的信息
     */
    @Override
    public void responseMessage(String message) {
        try {
            socket.getOutputStream().write(message.getBytes("UTF-8"));
        } catch (IOException e) {
            // TODO: Exception handling
            e.printStackTrace();
        } finally {
            closeSocket();
        }
    }

    /**
     * setter for Presenter
     *
     * @param presenter Presenter
     */
    @Override
    public void setPresenter(ServerContract.Presenter presenter) {
        this.mServerPresenter = presenter;
    }

    /**
     * 实现Runnable接口的run方法
     * 向Presenter转发信息并说明端口号
     */
    @Override
    public void run() {
        mServerPresenter.messageForward(port, receiveMessage());
    }

    /**
     * 从客户端读入信息
     *
     * @return 从客户端读入的信息
     */
   /*
    private String receiveMessage() {
        try {
            in = new DataInputStream(socket.getInputStream());
            return in.readUTF();
        } catch (IOException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return e.getMessage();
        }
    }
    last version
    */
    /*private String receiveMessage() {
        try {
            in = new DataInputStream(socket.getInputStream());
            return in.readUTF();
        } catch (IOException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return e.getMessage();
        } finally {
            try {
                socket.shutdownInput();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    TODO:各种测试
    */
    private static final int BUFFER_SIZE = 1024;

    private String receiveMessage() {

        try {

            in = new DataInputStream(socket.getInputStream());
            char[] data = new char[BUFFER_SIZE];
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            int len = br.read(data);

            String get = String.valueOf(data, 0, len);//接收一个字符串数据
            String[] str = get.split("\\$");
            System.out.println("接收到的$前字符串：" + str[0]);
            System.out.println("接收到的$后字符串：" + str[1]);

            return get;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                if (socket.isConnected())
                    socket.close();
            } catch (IOException e2) {
                e2.getMessage();
            }
            return "";
        }

    }


    /**
     * close socket
     */
    private void closeSocket() {
        try {
            in.close();
        } catch (IOException e) {
            // TODO: Exception handling
            e.printStackTrace();
        }
    }

}

