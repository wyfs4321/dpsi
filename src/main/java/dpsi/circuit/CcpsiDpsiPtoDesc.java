package dpsi.circuit;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;



/**
 * Cgs22Dpsi协议描述项。
 *
 * @author anonymous authors
 * @date 2023/8/15
 */
public class CcpsiDpsiPtoDesc implements PtoDesc{

    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)2761310131839337505L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "CCPSI_DPSI";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送扰动秘密分享向量
         */
        SERVER_SEND_RANDOMIZED_VECTOR,
    }

    /**
     * 单例模式
     */
    private static final CcpsiDpsiPtoDesc INSTANCE = new CcpsiDpsiPtoDesc();

    /**
     * 私有构造函数
     */
    private CcpsiDpsiPtoDesc() {
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