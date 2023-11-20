package edu.alibaba.mpc4j.s2pc.dpsi.pid;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import edu.alibaba.mpc4j.s2pc.dpsi.pid.dpsi23.Dpsi23Client;
import edu.alibaba.mpc4j.s2pc.dpsi.pid.dpsi23.Dpsi23Config;
import edu.alibaba.mpc4j.s2pc.dpsi.pid.dpsi23.Dpsi23Server;

/**
 * factory for pid-Dpsi protocol
 *
 * @author Yufei Wang
 * @date 2023/8/2
 */

public class PidDpsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private PidDpsiFactory() {
        // empty
    }

    /**
     * Dpsi协议类型。
     */
    public enum PidDpsiType {
        /**
         * 我们的Dpsi方案
         */
        DPSI23,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @return 服务端。
     */
    public static <X> PidDpsiServer<X> createServer(Rpc serverRpc, Party clientParty, PidDpsiConfig config) {
        PidDpsiFactory.PidDpsiType type = config.getPtoType();
        if (type==PidDpsiType.DPSI23) {
            return new Dpsi23Server<>(serverRpc, clientParty, (Dpsi23Config) config);
        }
        else{
            throw new IllegalArgumentException("Invalid " + PidDpsiFactory.PidDpsiType.class.getSimpleName() + ": " + type.name());
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
    public static <X> PidDpsiClient<X> createClient(Rpc clientRpc, Party serverParty, PidDpsiConfig config) {
        PidDpsiFactory.PidDpsiType type = config.getPtoType();
        if (type==PidDpsiType.DPSI23) {
            return new Dpsi23Client<>(clientRpc, serverParty, (Dpsi23Config) config);
        }
        else{
            throw new IllegalArgumentException("Invalid " + PidDpsiFactory.PidDpsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}
