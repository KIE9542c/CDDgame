/**
 * ServerPresenter对从ServerTransfer中得到的message进行基础的处理
 * 进行message分类辨识后转发给ServerModel进行具体操作
 * 同时根据ServerModel操作的返回值进行不同的回应信息处理
 * 调用messageReply将message返回给SererTransfer
 *
 * @author Yukino Yukinoshita
 * @version 1.0
 */

public class ServerPresenter implements ServerContract.Presenter {

    /**
     * Transfer
     */
    private final ServerContract.Transfer mServerTransfer;

    /**
     * Model
     */
    private final ServerModel mServerModel;

    /**
     * Constructor
     * initialize transfer and model
     *
     * @param transfer Transfer
     */
    ServerPresenter(ServerContract.Transfer transfer) {
        this.mServerTransfer = transfer;
        mServerModel = ServerModel.getInstance();

        mServerTransfer.setPresenter(this);
    }

    /**
     * 根据端口号转发信息
     * TODO: port is not support in the future version
     *
     * @param port    Transfer传入端口号
     * @param message 客户端信息
     */
    @Override
    public void messageForward(int port, String message) {
        switch (port) {
            case 8080:
                messageLogin(message);
                break;
            case 8081:
                messageRegister(message);
                break;
            case 8082:
                messageWaiting(message);
                break;
            case 8083:
                messageGame(message);
                break;
            default:

        }
    }

    /**
     * 向Transfer返回信息
     *
     * @param message 返回信息
     */
    private void messageReply(String message) {
        mServerTransfer.responseMessage(message);
    }

    /**
     * 登录信息处理
     * 带有5s的用户超时未连接检测
     *
     * @param message 登录信息
     */
    private synchronized void messageLogin(String message) {
        // TODO: check not null for message
        String[] str = message.split("\\$");

        // TODO: check Exception: Index out of range
	System.out.println("str[0]"+str[0]);
        System.out.println("str[1]"+str[1]);

        mServerModel.checkUserTimeout(5 * 1000, str[0]);
        ServerReply reply = mServerModel.userLogin(str[0], str[1]);
	
        if (reply == ServerReply.SUCCESS) {
            messageReply("登录成功");
        } else {
            messageReply("用户名或密码错误");
        }
    }

    /**
     * 注册信息处理
     *
     * @param message 注册信息
     */
    private synchronized void messageRegister(String message) {
        // TODO: check not null for message
        String[] str = message.split("\\$");

        // TODO: check Exception: Index out of range
        ServerReply reply = mServerModel.userRegister(str[0], str[1]);
        if (reply == ServerReply.SUCCESS) {
            // TODO: server should not login for user automatically in the future version
            mServerModel.userLogin(str[0], str[1]);
            messageReply("注册成功");
        } else {
            messageReply("注册失败");
        }
    }

    /**
     * 玩家在大厅等待时信息处理
     * 用户超时检测设置为30s
     * <p>
     * 客户端online表明希望得到当前在线用户信息，
     * 在该版本中，对online信息的处理不仅返回在线用户信息，
     * 还进行ready状态是否能进入房间的判断。
     * <p>
     * 在收到online信息时执行更新用户最后访问时间，判断用户是否在房间当中
     * 若用户不在房间中，试图为处在ready状态的用户分配房间，随后只返回在线用户信息
     * 若用户在房间中，检测房间内人数是否够4人并作出对应处理
     *
     * @param message 等待信息
     */
    private void messageWaiting(String message) {
        mServerModel.checkUserTimeout(30 * 1000);

        // TODO: check not null for message
        String[] str = message.split("\\$");
        // TODO: check Exception: Index out of range
        String action = str[0];
        // TODO: switch and if-else block is bad design in OOP
        switch (action) {
            case "online": {
                // TODO: check Exception: Index out of range
                String username = str[1];

                mServerModel.updateUserLastVisit(username);
                if (mServerModel.getUserRoomNumber(username) == 0) {
                    if ("ready".equals(mServerModel.getUserState(username))) {
                        mServerModel.distributeRoomNumberForUser(username);
                    }
                    int numberOnline = mServerModel.requestForNumberOfOnlineUser();
                    String onlineUser = mServerModel.requestForOnlineUser();
                    messageReply(String.valueOf(numberOnline) + "$" + onlineUser);
                } else {
                    if (mServerModel.getNumberOfPlayerInRoom(mServerModel.getUserRoomNumber(username)) < 4) {
                        int numberOnline = mServerModel.requestForNumberOfOnlineUser();
                        String onlineUser = mServerModel.requestForOnlineUser();
                        messageReply(String.valueOf(numberOnline) + "$" + onlineUser);
                    } else {
                        int roomnumber=mServerModel.getUserRoomNumber(username);
                       
			//TODO:在这里进行发牌的话会进行四次发牌，导致了些相关问题，目前是在每次发牌都设置ISFirst=False以防标记多次出牌者
                        mServerModel.updateCardOfRoom(roomnumber);

			 messageReply("game$" + mServerModel.getAllUsersInRoom(roomnumber));

                        
                    }
                }

                break;
            }
            case "offline": {
                // TODO: check Exception: Index out of range
                String username = str[1];
                ServerReply reply = mServerModel.userLogout(username);
                if (reply == ServerReply.SUCCESS) {
                    messageReply("offline$" + username);
                }
                break;
            }
            case "ready": {
                // TODO: check Exception: Index out of range
                String username = str[1];
                ServerReply reply = mServerModel.userReady(username);
                if (reply == ServerReply.SUCCESS) {
                    messageReply("ready$1");
                }
                break;
            }
            case "noready": {
                // TODO: check Exception: Index out of range
                String username = str[1];
                ServerReply reply = mServerModel.userNoReady(username);
                if (reply == ServerReply.SUCCESS) {
                    messageReply("online$1");
                }
                break;
            }
            case "back":{
                String username=str[1];
                mServerModel.updateUserBack(username);
                messageReply("back"+"$"+"0");
                break;
            }
            default:

                break;
        }
    }

    /**
     *
     * 收到"score"时 返回"score"和该玩家的积分
     *
     * @param message game message
     */
    private void messageGame(String message) {
        String[] str = message.split("\\$");
        // TODO: check Exception: Index out of range
        String action = str[0];
        // TODO: switch and if-else block is bad design in OOP
        switch (action){
            case "score":{
                String username=str[1];
                int score=mServerModel.requestForUserScore(username);
                messageReply("score" + "$" + score);

                break;
            }
            case "other":{
                String username=str[1];
                messageReply("other"+"$"+mServerModel.requestForCardNUmberInRoom(username));
                break;
            }
            case "first":{
                //TODO:人数不够时强制退出
                String username=str[1];
                int roomnumber=mServerModel.getUserRoomNumber(username);
                int roomusernumber=mServerModel.getNumberOfPlayerInRoom(roomnumber);
                if(roomusernumber<4){
                    mServerModel.updateUserBack(username);
                    messageReply("back"+"$"+"1");
                    break;
                }

                messageReply("first"+"$"+mServerModel.requestForFirstPlayUser(username));
                break;
            }
            case "card": {
                String username=str[1];
                messageReply("card"+"$"+mServerModel.requestForUserCard(username));
                break;
            }
            case "play":{
                String username=str[1];
                int roomnumber=mServerModel.getUserRoomNumber(username);
                int usernumber=mServerModel.getNumberOfPlayerInPlayingRoom(roomnumber);
                if(usernumber < 4){
                    mServerModel.updateUserBack(username);
                    messageReply("back"+"$"+"-1");
                    break;
                }
                int playcardnumber=Integer.parseInt(str[2]);

                String cardinformation=message.substring(5);

                int nowcardnumber=mServerModel.requestForCardNumber(username);
                int newcardnumber=nowcardnumber-playcardnumber;

                mServerModel.updateLastPlayCard(username,cardinformation,newcardnumber);
                messageReply("play"+"$"+mServerModel.requestForLastPlayCard(username));

                break;
            }
            case "otherplay": {
                String username=str[1];
                int roomnumber=mServerModel.getUserRoomNumber(username);
                int usernumber=mServerModel.getNumberOfPlayerInPlayingRoom(roomnumber);
                if(usernumber < 4){
                    mServerModel.updateUserBack(username);
                    messageReply("back"+"$"+"-1");
                    break;
                }
                messageReply("otherplay"+"$"+mServerModel.requestForLastPlayCard(username));
                break;
            }
            case "gainscore":{
                String username=str[1];
                int Score=Integer.parseInt(str[2]);
                //TODO:用户状态的各种改变
                mServerModel.updateUserScore(username,Score);
                messageReply("game is over");
            }
            default:
                break;
        }
    }




}
