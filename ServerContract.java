/**
 * ServerContract约束Transfer与Presenter之间的交流
 *
 * @author Yukino Yukinoshita
 * @version 1.0
 */

interface ServerContract {

    /**
     * Transfer接口
     */
    interface Transfer {

        /**
         * setPresenter方法用于为Transfer设置对应的Presenter
         *
         * @param presenter Presenter
         */
        void setPresenter(Presenter presenter);

        /**
         * 用于向Transfer传递要向客户端返回的信息
         *
         * @param message 返回信息
         */
        void responseMessage(String message);

    }

    /**
     * Presenter接口
     */
    interface Presenter {

        /**
         * 用于向Presenter传递客户端发来的信息
         *
         * @param port    端口号
         * @param message 客户端信息
         */
        void messageForward(int port, String message);

    }

}
