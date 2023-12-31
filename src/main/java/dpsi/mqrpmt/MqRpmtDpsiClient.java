package dpsi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.pto.TwoPartyPto;

import java.util.Set;
/**
 * interface for the client in mqRPMT-DPSI protocols
 *
 * @author anonymous authors
 * @date 2023/9/19
 */
public interface MqRpmtDpsiClient<T> extends TwoPartyPto {
    /**
     * 初始化协议。
     *
     * @param clientElementSet     客户端元素集合。
     * @param maxClientElementSize 客户端最大元素数量。
     * @param maxServerElementSize 服务端最大元素数量。
     * @throws MpcAbortException 如果协议异常中止。
     */
    void init(Set<T> clientElementSet, int maxClientElementSize, int maxServerElementSize) throws MpcAbortException;

    /**
     * 执行协议。
     *
     * @return 协议输出结果。
     * @throws MpcAbortException 如果协议异常中止。
     */
    Set<T> dpsi() throws MpcAbortException;
}