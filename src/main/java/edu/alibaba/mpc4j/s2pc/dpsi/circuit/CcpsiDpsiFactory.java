package edu.alibaba.mpc4j.s2pc.dpsi.circuit;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;

/**
 * Factory of the circuit-DPSI protocols
 *
 * @author Yufei Wang
 * @date 2023/8/15
 */
public class CcpsiDpsiFactory  implements PtoFactory {
    /**
     * private constructor.
     */
    private CcpsiDpsiFactory() {
        // empty
    }
    /**
     * Dpsi protocol type
     */
    public enum CcpsiDpsiType {
        /**
         * client-payload circuit DPSI protocol
         */
        CCPSI,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @return 服务端。
     */
    public static <X> CcpsiDpsiServer<X> createServer(Rpc serverRpc, Party clientParty, CcpsiDpsiConfig config) {
        CcpsiDpsiFactory.CcpsiDpsiType type = config.getPtoType();
        if (type==CcpsiDpsiType.CCPSI) {
            return new CcpsiDpsiServer<>(serverRpc, clientParty, config);
        }
        else {
            throw new IllegalArgumentException("Invalid " + CcpsiDpsiFactory.CcpsiDpsiType.class.getSimpleName() + ": " + type.name());
        }
    }

    /**
     * 构建客户端。
     *
     * @param clientRpc   客户端通信接口。
     * @param serverParty 服务端信息。
     * @param config      配置项。
     * @return 客户端。
     */
    public static <X> CcpsiDpsiClient<X> createClient(Rpc clientRpc, Party serverParty, CcpsiDpsiConfig config) {
        CcpsiDpsiFactory.CcpsiDpsiType type = config.getPtoType();
        if (type==CcpsiDpsiType.CCPSI) {
            return new CcpsiDpsiClient<>(clientRpc, serverParty, config);
        }
        else{
            throw new IllegalArgumentException("Invalid " + CcpsiDpsiFactory.CcpsiDpsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}
