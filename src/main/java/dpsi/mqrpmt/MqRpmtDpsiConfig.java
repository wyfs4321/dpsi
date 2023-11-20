package dpsi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;
/**
 * interface for the config in mqRPMT-DPSI protocols
 *
 * @author anonymous authors
 * @date 2023/9/19
 */
public interface MqRpmtDpsiConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    MqRpmtDpsiFactory.MqRpmtDpsiType getPtoType();
}