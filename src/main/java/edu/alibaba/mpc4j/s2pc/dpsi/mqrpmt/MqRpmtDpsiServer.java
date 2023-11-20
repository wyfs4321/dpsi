package edu.alibaba.mpc4j.s2pc.dpsi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;

/**
 * interface for theserver of mqRPMT-DPSI protocols
 *
 * @author Yufei Wang
 * @date 2023/9/19
 */
public interface MqRpmtDpsiServer<T> extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param serverElementSet   服务端元素集合。
     * @param maxServerElementSize   服务端最大元素数量。
     * @param maxClientElementSize 客户端最大元素数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(Set<T> serverElementSet, int maxServerElementSize, int maxClientElementSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @throws MpcAbortException 如果协议异常中止。
     */
    void dpsi() throws MpcAbortException;
}
