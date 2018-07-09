import com.sun.net.httpserver.Authenticator;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * ServerModel类处理具体的用户请求
 * 根据不同请求调用不同的数据库操作，并且能对数据库返回的信息进行处理
 *
 * @author Yukino Yukinoshita
 * @version 1.0
 */

public class ServerModel {

    /**
     * Singleton instance
     */
    private static ServerModel instance = null;

    /**
     * Constructor
     */
    private ServerModel() {

    }

    /**
     * Singleton getter
     *
     * @return instance of ServerModel
     */
    public static ServerModel getInstance() {
        if (instance == null) {
            instance = new ServerModel();
        }
        return instance;
    }

    /**
     * Login
     *
     * @param username 用户名
     * @param password 密码
     * @return SUCCESS if login success
     */
    synchronized ServerReply userLogin(String username, String password) {
        try {
            ResultSet resultSet = DatabaseConnector.getInstance().searchUser(username, password);
            // TODO: check not null for resultSet
            if (resultSet.next()) {
                DatabaseConnector.getInstance().userLoginInsert(username);
                return ServerReply.SUCCESS;
            } else return ServerReply.FAIL;
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return ServerReply.LOGINED;
        }
    }


    /**
     * Log out
     *
     * @param username 用户名
     * @return SUCCESS if log out success
     */
    ServerReply userLogout(String username) {
        try {
            DatabaseConnector.getInstance().userLogoutDelete(username);
            return ServerReply.SUCCESS;
        } catch (SQLException e) {
            e.printStackTrace();
            return ServerReply.FAIL;
        }
    }

    /**
     * Register
     *
     * @param username 用户名
     * @param password 密码
     * @return SUCCESS if register success
     */
    synchronized ServerReply userRegister(String username, String password) {
        try {
            DatabaseConnector.getInstance().userRegisterInsert(username, password);
            return ServerReply.SUCCESS;
        } catch (SQLException e) {
            e.printStackTrace();
            return ServerReply.FAIL;
        }
    }

    /**
     * Ready
     *
     * @param username 用户名
     * @return SUCCESS if ready success
     */
    ServerReply userReady(String username) {
        try {
            DatabaseConnector.getInstance().userReadyUpdate(username);
            return ServerReply.SUCCESS;
        } catch (SQLException e) {
            e.printStackTrace();
            return ServerReply.FAIL;
        }
    }

    /**
     * No ready
     *
     * @param username 用户名
     * @return SUCCESS if no ready success
     */
    ServerReply userNoReady(String username) {
        try {
            DatabaseConnector.getInstance().userNoReadyUpdate(username);
            return ServerReply.SUCCESS;
        } catch (SQLException e) {
            e.printStackTrace();
            return ServerReply.FAIL;
        }
    }

    /**
     * 检测用户是否超过一定时间没有响应
     * 若超时则将用户从在线列表中除去
     *
     * @param timeout 超时时间限制
     * @param name    指定用户名
     * @return SUCCESS if check success EVEN user is NOT time out
     */
    ServerReply checkUserTimeout(final int timeout, String name) {
        // TODO: there may be an easier way to get time
        // TODO: duplicate code for get time can declare a new method
        java.util.Date date = new java.util.Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.HOUR, 8);
        date = cal.getTime();
        Timestamp NowTime = new Timestamp(date.getTime());

        try {
            Timestamp LastTime = DatabaseConnector.getInstance().searchUserLastVisit(name);
            // TODO: check not null for time
            if (NowTime.getTime() - LastTime.getTime() > timeout) {
                DatabaseConnector.getInstance().userTimeout(name);
            }
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return ServerReply.FAIL;
        }
        return ServerReply.SUCCESS;

    }

    /**
     * 检测用户是否超过一定时间没有响应
     * 若超时则将用户从在线列表中除去
     * 该重载方法不指定用户名，对所有在线列表用户执行检查
     *
     * @param timeout 超时时间限制
     * @return SUCCESS if check success EVEN NO user time out
     */
    ServerReply checkUserTimeout(final int timeout) {
        try {
            ResultSet resultSet = DatabaseConnector.getInstance().searchOnlineUser();
            // TODO: check not null for resultSet
            while (resultSet.next()) {
                checkUserTimeout(timeout, resultSet.getString("name"));
            }
            return ServerReply.SUCCESS;
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return ServerReply.FAIL;
        }
    }

    /**
     * 获取具有指定状态的用户数量
     *
     * @param status 用户状态
     * @return 用户数量
     */
    private int getNumberOfUserByState(String status) {
        try {
            return DatabaseConnector.getInstance().searchNumberOfUserByState(status);
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取指定用户的状态
     *
     * @param name 用户名
     * @return 状态
     */
    String getUserState(String name) {
        try {
            return DatabaseConnector.getInstance().searchUserState(name);
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 设置用户状态
     *
     * @param name   用户
     * @param status 状态
     * @return SUCCESS if set success
     */
    private ServerReply setUserState(String name, String status) {
        try {
            DatabaseConnector.getInstance().setUserState(name, status);
            return ServerReply.SUCCESS;
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return ServerReply.FAIL;
        }
    }

    /**
     * 为用户分配房间号
     *
     * @param name 用户名
     * @return SUCCESS if distribute successfully
     */
    synchronized ServerReply distributeRoomNumberForUser(String name) {
        // TODO: check user room number is 0 ?
        int countRoom = (getNumberOfUserByState("ready") + getNumberOfUserByState("game")) / 4;
        for (int i = 1; i <= countRoom; ++i) {
            if (getNumberOfPlayerInRoom(i) < 4) {
                if (setUserRoomNumber(name, i) == ServerReply.SUCCESS) {
                    setUserState(name, "game");
                    return ServerReply.SUCCESS;
                }
            }
        }
        return ServerReply.FAIL;
    }

    /**
     * 设置用户所在房间号
     *
     * @param name       用户名
     * @param roomNumber 房间号
     * @return SUCCESS if set success
     */
    private ServerReply setUserRoomNumber(String name, int roomNumber) {
        try {
            DatabaseConnector.getInstance().setUserRoomNumber(name, roomNumber);
            //TODO：下面是将用户放进playing表中，临时方案，暂时没想好具体放进去的时机
            DatabaseConnector.getInstance().setUserInPlaying(name,roomNumber);

            return ServerReply.SUCCESS;
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return ServerReply.FAIL;
        }
    }

    /**
     * 获取用户所在房间号
     *
     * @param name 用户名
     * @return 房间号
     */
    int getUserRoomNumber(String name) {
        try {
            return DatabaseConnector.getInstance().searchUserRoomNumber(name);
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * 获取房间内人数
     *
     * @param roomNumber 房间号
     * @return 房间内人数
     */
    int getNumberOfPlayerInRoom(int roomNumber) {
        try {
            return DatabaseConnector.getInstance().searchNumberOfUserInRoom(roomNumber);
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return 0;
        }
    }
    /**
     * 获取房间内人数Playing表
     *
     * @param roomNumber 房间号
     * @return 房间内人数
     */
    int getNumberOfPlayerInPlayingRoom(int roomNumber) {
        try {
            return DatabaseConnector.getInstance().searchNumberOfUserInPlayingRoom(roomNumber);
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return 0;
        }
    }
    /**
     * 获取房间中所有用户
     * Format: name1$name2$
     *
     * @param roomNumber 房间号
     * @return 符合format的String
     */
    String getAllUsersInRoom(int roomNumber) {
        try {
            ResultSet resultSet = DatabaseConnector.getInstance().searchUserInRoom(roomNumber);
            StringBuilder str = new StringBuilder();
            // TODO: check not null for resultSet
            while (resultSet.next()) {
                str.append(resultSet.getString("name")).append("$");
            }
            return str.toString();
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 更新用户最后访问时间
     *
     * @param username 用户名
     * @return SUCCESS if update successfully
     */
    ServerReply updateUserLastVisit(String username) {
        try {
            DatabaseConnector.getInstance().updateUserLastVisit(username);
            return ServerReply.SUCCESS;
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return ServerReply.FAIL;
        }
    }

    /**
     * 获取在线玩家
     * Format: name1$state1$name2$state2$
     *
     * @return 符合format的String
     */
    String requestForOnlineUser() {
        try {
            ResultSet resultSet = DatabaseConnector.getInstance().searchOnlineUser();
            StringBuilder str = new StringBuilder();
            // TODO: check not null for resultSet
            while (resultSet.next()) {
                str.append(resultSet.getString("name")).append("$").append(resultSet.getString("status")).append("$");
            }
            return str.toString();
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 获取在线玩家人数
     *
     * @return 在线人数
     */
    int requestForNumberOfOnlineUser() {
        try {
            return DatabaseConnector.getInstance().searchNumberOfOnlineUser();
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return 0;
        }
    }
    /**
    * 获取玩家积分
    *
    * @param username 玩家用户名
    * @return score 该玩家积分
    */
    int requestForUserScore(String username){
        try{
            return DatabaseConnector.getInstance().searchUserScore(username);
        }catch (SQLException e){
            // TODO: Exception handling
            e.printStackTrace();
            return 0;
        }

    }

    /**
     * 获取房间中其他玩家的手牌数
     * @param username 用户名
     * @return 以name$number格式的String
     */
    String requestForCardNUmberInRoom(String username){
        try {
            ResultSet resultSet = DatabaseConnector.getInstance().searchNumberOfOtherUserCardInRoom(username);
            StringBuilder str = new StringBuilder();
            // TODO: check not null for resultSet
            while (resultSet.next()) {
                str.append(resultSet.getString("name")).append("$").append(resultSet.getString("CardNumber")).append("$");
            }
            return str.toString();
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 获取和该用户房间中的首位出牌玩家
     *
     * @param username 用户名
     * @return */
    String requestForFirstPlayUser(String username){
        try{
            int roomnumber=DatabaseConnector.getInstance().searchUserRoomNumber(username);
            return DatabaseConnector.getInstance().searchForFistPlayUser(roomnumber);
        }
        catch (SQLException e){
            e.printStackTrace();
            return "";
        }
    }
    /**
     * 获取玩家手牌
     *
     * @param username 用户名
     * @return card 字符串形式的玩家手牌
     * */
    String requestForUserCard(String username){
        try{
            return DatabaseConnector.getInstance().searchUserCard(username);
        }
        catch (SQLException e){
            e.printStackTrace();
            return "";
        }
    }
    /**
     * 获取玩家手牌数
     *
     * @param username 用户名
     * @return cardnumber 现有的手牌数
     *
     */
     int requestForCardNumber(String username){
         try {
             return DatabaseConnector.getInstance().searchCardNumber(username);
         }
         catch (SQLException e){
             e.printStackTrace();
             return 13;
         }
     }
    /**
    * 获取一组乱序的52张牌
    *
    * @return cardset 一个乱序的牌组
     */

     String []getDisorderCard()throws Exception{
        ArrayList<String> list = new ArrayList<String>();
        String [] cardset=new String[52];
        for(int i = 0;i < 52;i++){
            list.add(i+"");
        }

        Collections.shuffle(list);

        Iterator<String> ite = list.iterator();

        for(int j=0;j<52;j++) {

                ite.hasNext();
                cardset[j] = ite.next().toString();

        }
        return cardset;
    }


    /**
     * 为某个房间中的所有用户发牌并设置出牌玩家标志
     * TODO:用getDiorderCard获取乱序牌组然后进行循环发牌,  不安全的操作的warning在这个函数
     *
     * @param roomnumber 需要发牌的房间号
     * @return ServerReply
     */
    ServerReply updateCardOfRoom(int roomnumber){

        try {


                String[] usercard=new String[]{"","","",""};
                String[] user=new String[4];
                String[] cardset = getDisorderCard();
                int first=0;
                for(int i=0;i<52;i++){
                    usercard[i/13]=usercard[i/13]+cardset[i]+"$";

                    if(cardset[i].equals("0")){
                        first=i/13;
                    }
                }

                ResultSet resultSet=DatabaseConnector.getInstance().searchUserInRoom(roomnumber);
                for(int j=0;j<4;j++){
                    resultSet.next();
                    user[j] = resultSet.getString("name");
                }

                for(int i=0;i<4;i++){
                     DatabaseConnector.getInstance().setUserCard(user[i],usercard[i]);
                }
                DatabaseConnector.getInstance().setFirstPlayUser(user[first]);

                return ServerReply.SUCCESS;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return ServerReply.FAIL;
            }
    }

    /**
    * 记录房间中最近出的牌
     *
     * @param username 出牌的用户名
     * @param playcard 出牌的信息
     * @return ServerReply
     * */
    ServerReply updateLastPlayCard(String username,String playcard,int playcardnumber){
        try{
                DatabaseConnector.getInstance().updatePlayCardNumber(username,playcardnumber);
                DatabaseConnector.getInstance().updatePlayCard(username,playcard);
                updateNextUserPlayCard(username);
                return ServerReply.SUCCESS;
        }
        catch (SQLException e){
            e.printStackTrace();
            return ServerReply.FAIL;
        }
    }
   /**
    * 查询最近出的牌
    *
    * @param username 查询的用户名
    * @return card 出的牌的信息*/
    String requestForLastPlayCard(String username){
        try{
            ResultSet resultSet=DatabaseConnector.getInstance().searchForLastPlayCard(username);
            resultSet.next();
            return resultSet.getString("LastPlayCard");
        }
        catch (SQLException e){
            e.printStackTrace();
            return null;
        }
    }
    /**
    * TODO：清理下家的上次出牌
     * @param username 这个玩家的用户名*/
    ServerReply updateNextUserPlayCard(String username){
            try{
                    int roomnumber=DatabaseConnector.getInstance().searchUserRoomNumber(username);
                    ResultSet resultSet=DatabaseConnector.getInstance().searchUserInRoom(roomnumber);
                    String [] user=new String[4];
                    String nextuser;
                    int position=0;
                    for(int i=0;i<4;i++){
                            resultSet.next();
                            user[i]=resultSet.getString("name");
                            if(user[i].equals(username)) position=i;
                    }
                    nextuser=user[(position+1)%4];
                    DatabaseConnector.getInstance().updatePlayCardUser(nextuser);
                    return ServerReply.SUCCESS;

            }catch (SQLException e){
                e.printStackTrace();
                return ServerReply.FAIL;
            }
    }
    /**
    *增加用户积分
     *
     * @param username 用户名
     * @param score 要增加的积分
     * @return ServerReply
     * @throws SQLException
     * */
    ServerReply updateUserScore(String username,int score){
            try{
                int newscore=DatabaseConnector.getInstance().searchUserScore(username)+score;
                DatabaseConnector.getInstance().updateUserScore(username,newscore);
                return ServerReply.SUCCESS;
            }catch(SQLException e)
        {
            e.printStackTrace();
            return ServerReply.FAIL;
        }
    }

    ServerReply updateUserBack(String username) {
        try{
            DatabaseConnector.getInstance().userLogoutDelete(username);
            DatabaseConnector.getInstance().userLoginInsert(username);

            return ServerReply.SUCCESS;
        }catch (SQLException e){
            e.printStackTrace();
            return ServerReply.FAIL;
        }
    }
}
