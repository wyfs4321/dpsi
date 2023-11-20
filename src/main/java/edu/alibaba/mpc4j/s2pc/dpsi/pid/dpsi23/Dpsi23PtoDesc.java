package edu.alibaba.mpc4j.s2pc.dpsi.pid.dpsi23;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;


/**
 * Dpsi协议信息。
 *  *此协议首先要求双方在各自数据集中加入随机个数的填充项，其次利用Czz22的pid方案获得双方记录全集的pid，client对所持pid进行扰动并将
 *  扰动结果发送给server进行psi操作，最终server解密pid获得扰动后的交集结果。

 *  *
 *  * @author Yufei Wang
 *  * @date 2023/8/2
 *  */

public class Dpsi23PtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)5736657965321288838L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "23_DPSI";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送集合规模
         */
        SERVER_SEND_ELEMENTS_SIZE,
        /**
         * 客户端发送集合规模
         */
        CLIENT_SEND_ELEMENTS_SIZE,
        /**
         * 客户端发送扰动PID列表
         */
        SERVER_SEND_NOISY_PID_LIST,
    }

    /**
     * 单例模式
     */
    private static final Dpsi23PtoDesc INSTANCE = new Dpsi23PtoDesc();

    /**
     * 私有构造函数
     */
    private Dpsi23PtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}
