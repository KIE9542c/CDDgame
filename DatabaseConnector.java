import java.sql.*;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * DatabaseConnector连接数据库并执行数据库语句
 *
 * @author Yukino Yukinoshita
 * @version 1.0
 */

public class DatabaseConnector {

    /**
     * Singleton instance
     */
    private static DatabaseConnector instance = null;

    /**
     * connection for database
     */
    private Connection connection;

    /**
     * Constructor
     */
    private DatabaseConnector() {
        connectToDatabase();
    }

    /**
     * Singleton getter for instance
     *
     * @return DatabaseConnector instance
     */
    public static DatabaseConnector getInstance() {
        if (instance == null) {
            instance = new DatabaseConnector();
        }
        try {
            if (instance.connection.isClosed())
                instance.connectToDatabase();
        }catch (SQLException e) {
        }
        return instance;

    }

    /**
     * search user by username and password
     *
     * @param username 用户名
     * @param password 密码
     * @return SQL ResultSet 用户名单
     * @throws SQLException
     */
    synchronized ResultSet searchUser(String username, String password) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("select * from user1 where name=? and password=?");
        pStmt.setString(1, username);
        pStmt.setString(2, password);
        return pStmt.executeQuery();
    }

    /**
     * 用户注册时数据库插入
     *
     * @param name     用户名
     * @param password 密码
     * @throws SQLException
     */
    synchronized void userRegisterInsert(String name, String password) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("INSERT INTO user1(name,password) VALUES (?,?)");
        pStmt.setString(1, name);
        pStmt.setString(2, password);
        pStmt.executeUpdate();
    }

    /**
     * 用户登录时数据库插入
     *
     * @param username 用户名
     * @return SUCCESS if login success
     * @throws SQLException
     */
    synchronized ServerReply userLoginInsert(String username) throws SQLException {
        // TODO: there may be an easier way to get time
        // TODO: duplicate code for get time can declare a new method
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        java.util.Date date = new java.util.Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.HOUR, 8);
        date = cal.getTime();
        Timestamp timeStamp = new Timestamp(date.getTime());

        PreparedStatement pStmt = connection.prepareStatement("INSERT INTO onlineuser(name,logintime) VALUES (?,?)");
        pStmt.setString(1, username);
        pStmt.setTimestamp(2, timeStamp);
        pStmt.executeUpdate();
        return ServerReply.SUCCESS;
    }

    /**
     * 用户退出时数据库删除
     *
     * @param username 用户名
     * @throws SQLException
     */
    void userLogoutDelete(String username) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("delete from onlineuser where name=? ");
        pStmt.setString(1, username);
        pStmt.executeUpdate();
    }

    /**
     * 用户准备时更新数据库
     *
     * @param username 用户名
     * @throws SQLException
     */
    void userReadyUpdate(String username) throws SQLException {
        setUserState(username, "ready");
    }

    /**
     * 用户取消准备时更新数据库
     *
     * @param username 用户名
     * @throws SQLException
     */
    void userNoReadyUpdate(String username) throws SQLException {
        setUserState(username, "online");
    }

    /**
     * 设置用户状态
     *
     * @param username 用户名
     * @param status   状态
     * @throws SQLException
     */
    void setUserState(String username, String status) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("update onlineuser set status=? where name =? ");
        pStmt.setString(1, status);
        pStmt.setString(2, username);
        pStmt.executeUpdate();
    }

    /**
     * 获取用户状态
     *
     * @param username 用户名
     * @return 状态
     * @throws SQLException
     */
    String searchUserState(String username) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("select status from onlineuser where name =? ");
        pStmt.setString(1, username);
        ResultSet resultSet = pStmt.executeQuery();
        // TODO: check not null for resultSet
        resultSet.next();
        // TODO: check not null for getString method
        return resultSet.getString("status");
    }

    /**
     * 获取用户最后访问时间
     *
     * @param username 用户名
     * @return Timestamp object record the time user last visit server
     * @throws SQLException
     */
    Timestamp searchUserLastVisit(String username) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("select logintime from onlineuser where name =? ");
        pStmt.setString(1, username);
        ResultSet resultSet = pStmt.executeQuery();
        // TODO: check not null for resultSet
        resultSet.next();
        // TODO: check not null for getTimestamp method
        return resultSet.getTimestamp("logintime");
    }

    /**
     * 更新用户最后访问时间
     *
     * @param username 用户名
     * @throws SQLException
     */
    void updateUserLastVisit(String username) throws SQLException {
        // TODO: there may be an easier way to get time
        // TODO: duplicate code for get time can declare a new method
        TimeZone tz = TimeZone.getTimeZone("Asia/Shanghai");
        TimeZone.setDefault(tz);
        java.util.Date date = new java.util.Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.HOUR, 8);
        date = cal.getTime();
        Timestamp timeStamp = new Timestamp(date.getTime());

        PreparedStatement pStmt = connection.prepareStatement("update onlineuser set logintime=? where name =? ");
        pStmt.setTimestamp(1, timeStamp);
        pStmt.setString(2, username);
        pStmt.executeUpdate();
    }

    /**
     * 用户超时未访问
     *
     * @param username 用户名
     * @throws SQLException
     */
    void userTimeout(String username) throws SQLException {
        userLogoutDelete(username);
    }

    /**
     * 查询在线用户名单
     *
     * @return SQL ResultSet 在线用户名单
     * @throws SQLException
     */
    ResultSet searchOnlineUser() throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("select * from onlineuser");
        return pStmt.executeQuery();
    }

    /**
     * 获取在线用户人数
     *
     * @return 在线用户人数
     * @throws SQLException
     */
    int searchNumberOfOnlineUser() throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("select count(*) as numbers from onlineuser");
        ResultSet resultSet = pStmt.executeQuery();
        // TODO: check not null for resultSet
        resultSet.next();
        // TODO: check not null for getInt method
        return resultSet.getInt("numbers");
    }

    /**
     * 获取指定状态下的用户的数量
     *
     * @param status 状态
     * @return 指定状态用户数量
     * @throws SQLException
     */
    int searchNumberOfUserByState(String status) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("select count(*) as numbers from onlineuser where status = ?");
        pStmt.setString(1, status);
        ResultSet resultSet = pStmt.executeQuery();
        // TODO: check not null for resultSet
        resultSet.next();
        // TODO: check not null for getInt method
        return resultSet.getInt("numbers");
    }

    /**
     * 为用户设置房间号
     *
     * @param username   用户名
     * @param roomNumber 房间号
     * @throws SQLException
     */
    void setUserRoomNumber(String username, int roomNumber) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("update onlineuser set roomnumber=? where name = ?");
        pStmt.setInt(1, roomNumber);
        pStmt.setString(2, username);
        pStmt.executeUpdate();
    }
    /**
     * 将用户添加到playing表中
     *
     * @param username 用户名
     * @throws SQLException*/
    void setUserInPlaying(String username)throws SQLException{
        PreparedStatement pStmt=connection.prepareStatement("insert into playing(name) value (?)");
        pStmt.setString(1,username);

        pStmt.executeUpdate();
    }
    /**
     * 获取用户房间号
     *
     * @param username 用户名
     * @return 用户所在房间号
     * @throws SQLException
     */
    int searchUserRoomNumber(String username) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("select roomnumber from onlineuser where name = ?");
        pStmt.setString(1, username);
        ResultSet resultSet = pStmt.executeQuery();
        // TODO: check not null for resultSet
        resultSet.next();
        // TODO: check not null for getInt method
        return resultSet.getInt("roomnumber");
    }

    /**
     * 获取用户积分
     *
     * @param username 用户名
     * @return 该用户积分
     * @throws SQLException
     */
    int searchUserScore(String username) throws SQLException{
        PreparedStatement pStmt = connection.prepareStatement("select Score from user1 where name = ?");
        pStmt.setString(1,username);
        ResultSet resultSet=pStmt.executeQuery();

        resultSet.next();

        return resultSet.getInt("score");
    }

    /**
     * 更新用户积分
     *
     * @param username 用户名
     * @param score 新的积分
     * @throws SQLException*/
    void updateUserScore(String username,int score) throws SQLException{
        PreparedStatement pStmt = connection.prepareStatement("update user1 set Score=? where name=?");
        pStmt.setInt(1,score);
        pStmt.setString(2,username);

        pStmt.executeUpdate();
    }

    /**
     * 获取用户所在房间中其他玩家的手牌数
     *
     * @param username 用户名
     * @return SQL ResultSet 玩家和对应手牌数
     * @throws SQLException
     */
    ResultSet searchNumberOfOtherUserCardInRoom(String username) throws SQLException{
        int roomNumber=searchUserRoomNumber(username);
        PreparedStatement pStmt=connection.prepareStatement("select playing.Name,playing.CardNumber from playing join onlineuser where onlineuser.roomnumber= ? and playing.Name != ?" );
        pStmt.setInt(1,roomNumber);
        pStmt.setString(2,username);
        //TODO: 感觉playing里面不太稳
        return pStmt.executeQuery();
    }

    /**
    * 为某个玩家发牌
    *
    * @param username 玩家用户名
    * @param card 一组13张牌
     * @throws SQLException
     */
    void setUserCard(String username,String card) throws SQLException{
        //TODO:IsFirst有待改
        PreparedStatement pStmt=connection.prepareStatement("update playing set Card= ? ,IsFirst= FALSE where name = ?" );
        pStmt.setString(1,card);
        pStmt.setString(2,username);

        pStmt.executeUpdate();
    }

    /**
     * 查询初始玩家手牌
     *
     * @param username 玩家用户名
     * @return card 字符串的玩家手牌
     * @throws SQLException
     */
    String searchUserCard(String username)throws SQLException{
        PreparedStatement pStmt=connection.prepareStatement("select card from playing where name = ?");
        pStmt.setString(1,username);

        ResultSet resultSet=pStmt.executeQuery();
        resultSet.next();

        return resultSet.getString("card");
    }

    /**
     * 设置某位玩家为首次出牌者
     *
     * @parm username 玩家用户名
     * @throws SQLException*/
    void setFirstPlayUser(String username) throws SQLException{
        PreparedStatement pStmt=connection.prepareStatement("update playing set IsFirst= true where name = ?" );
        pStmt.setString(1,username);

        pStmt.executeUpdate();
    }

    /**
     * 查询玩家当前手牌数
     *
     * @param username
     * @return cardnumber
     * @throws SQLException
     */
    int searchCardNumber(String username) throws SQLException{
        PreparedStatement pStmt=connection.prepareStatement("select CardNumber from playing where name = ?");
        pStmt.setString(1,username);

        ResultSet resultSet=pStmt.executeQuery();
        resultSet.next();

        return resultSet.getInt("CardNumber");
    }
    /**
     * 更新出牌玩家的手牌数
     *
     * @param username  出牌的玩家
     * @param cardnumber 出牌后手牌的数量
     * @throws SQLException
     */

    void updatePlayCardNumber(String username,int cardnumber)throws SQLException{
        PreparedStatement pStmt=connection.prepareStatement("update playing set CardNumber=? where name = ?");
        pStmt.setInt(1,cardnumber);
        pStmt.setString(2,username);

        pStmt.executeUpdate();
    }

    /**
     * 记录某个玩家最近出的牌
     *
     * @param username 用户名
     * @param nameandcard play&最近出牌的玩家$最近出的牌$
     * @throws SQLException
     */
    void updatePlayCard(String username,String nameandcard) throws SQLException{
        PreparedStatement pStmt=connection.prepareStatement("update playing set LastPlayCard=? where name=?");
        pStmt.setString(1,nameandcard);
        pStmt.setString(2,username);

        pStmt.executeUpdate();
    }
    /**
     * 查询牌局里最近出的牌
     *TODO
     * @param username 用户名
     * @throws SQLException
     * */
    ResultSet searchForLastPlayCard(String username) throws SQLException{
            PreparedStatement pStmt=connection.prepareStatement("select LastPlayCard from playing where name = ?");
            pStmt.setString(1,username);

            return pStmt.executeQuery();
    }

    /**
     * 清理该玩家的出牌情况
     *
     * @param username 用户名
     * @throws SQLException
     * */
    void updatePlayCardUser(String username)throws SQLException{
        PreparedStatement pStmt=connection.prepareStatement("update playing set LastPlayCard = NULL where name =?");
        pStmt.setString(1,username);
        pStmt.executeUpdate();
    }

    /**
    * 查询该房间中的首位出牌者
    *
    * @param roomnumber 房间号
     *@return username 出牌者用户名
     * @throws  SQLException
     */
    String searchForFistPlayUser(int roomnumber) throws SQLException{
        PreparedStatement pStm=connection.prepareStatement("select playing.name from playing join onlineuser where onlineuser.roomnumber=? and playing.IsFirst=true ");
        pStm.setInt(1,roomnumber);
        ResultSet resultSet= pStm.executeQuery();
        resultSet.next();

        return resultSet.getString("name");

    }

    /**
     * 获取某个房间中的人数
     *
     * @param roomNumber 房间号
     * @return 房间中的人数
     * @throws SQLException
     */
    synchronized int searchNumberOfUserInRoom(int roomNumber) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("select count(*) as numbers from onlineuser where roomnumber = ?");
        pStmt.setInt(1, roomNumber);
        ResultSet resultSet = pStmt.executeQuery();
        // TODO: check not null for resultSet
        resultSet.next();
        // TODO: check not null for getInt method
        return resultSet.getInt("numbers");
    }

    /**
     * 获取某个房间中的所有人
     *
     * @param roomNumber 房间号
     * @return SQL ResultSet 房间中的人
     * @throws SQLException
     */
    ResultSet searchUserInRoom(int roomNumber) throws SQLException {
        PreparedStatement pStmt = connection.prepareStatement("select name from onlineuser where roomnumber = ?");
        pStmt.setInt(1, roomNumber);
        // TODO: check not null for return value ResultSet
        return pStmt.executeQuery();
    }

    /**
     * Create connection to database
     * TODO: check if fail to connect
     */
    private void connectToDatabase() {

        // parameter list
        final String ip = "127.0.0.1";
        final String port = "3306";
        final String database = "CDDuser";
        final String user = "root";
        final String password = "CDDgame123";
        final boolean isUseSSL = false;
        final String serverTimezone = "GMT";
        final boolean isUseUnicode = true;
        final String encode = "utf-8";

        String connectionString = "jdbc:mysql://" + ip + ":" + port
                + "/" + database + "?"
                + "user=" + user
                + "&password=" + password
                + "&useSSL=" + String.valueOf(isUseSSL)
                + "&serverTimezone=" + serverTimezone
                + "&useUnicode=" + String.valueOf(isUseUnicode)
                + "&characterEncoding=" + encode;

        try {
            connection = DriverManager.getConnection(connectionString);
        } catch (SQLException e) {
            // TODO: Exception handling
            e.printStackTrace();
        }
    }

    /**
     * Close the connection
     * TODO: find a way to close
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                // TODO: Exception handling
                e.printStackTrace();
            }
        }
    }
}

