package edu.alibaba.mpc4j.s2pc.dpsi.pid;

import edu.alibaba.mpc4j.common.rpc.pto.MultiPartyPtoConfig;

/**
 * interface for the config of pid-Dpsi protocol
 *
 * @author Yufei Wang
 * @date 2023/8/2
 */

public interface PidDpsiConfig extends MultiPartyPtoConfig {
    /**
     * 返回协议类型。
     *
     * @return 协议类型。
     */
    PidDpsiFactory.PidDpsiType getPtoType();
}