package dpsi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.pto.PtoFactory;
import dpsi.mqrpmt.czz22.Czz22MqRpmtDpsiClient;
import dpsi.mqrpmt.czz22.Czz22MqRpmtDpsiConfig;
import dpsi.mqrpmt.czz22.Czz22MqRpmtDpsiServer;
import dpsi.pid.PidDpsiFactory;

/**
 * factory for the mqRPMT-DPSI protocols
 *
 * @author anonymous authors
 * @date 2023/9/19
 */
public class MqRpmtDpsiFactory implements PtoFactory {
    /**
     * private constructor.
     */
    private MqRpmtDpsiFactory() {
        // empty
    }

    /**
     * Dpsi协议类型。
     */
    public enum MqRpmtDpsiType {
        /**
         * Our mqRPMT-DPSI protocol
         */
        CZZ22,
    }

    /**
     * 构建服务端。
     *
     * @param serverRpc   服务端通信接口。
     * @param clientParty 客户端信息。
     * @param config      配置项。
     * @return 服务端。
     */
    public static <X> MqRpmtDpsiServer<X> createServer(Rpc serverRpc, Party clientParty, MqRpmtDpsiConfig config) {
        MqRpmtDpsiFactory.MqRpmtDpsiType type = config.getPtoType();
        switch (type) {
            case CZZ22:
                return new Czz22MqRpmtDpsiServer<>(serverRpc, clientParty, (Czz22MqRpmtDpsiConfig) config);
            default:
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
    public static <X> MqRpmtDpsiClient<X> createClient(Rpc clientRpc, Party serverParty, MqRpmtDpsiConfig config) {
        MqRpmtDpsiFactory.MqRpmtDpsiType type = config.getPtoType();
        switch (type) {
            case CZZ22:
                return new Czz22MqRpmtDpsiClient<>(clientRpc, serverParty, (Czz22MqRpmtDpsiConfig) config);
            default:
                throw new IllegalArgumentException("Invalid " + PidDpsiFactory.PidDpsiType.class.getSimpleName() + ": " + type.name());
        }
    }
}
