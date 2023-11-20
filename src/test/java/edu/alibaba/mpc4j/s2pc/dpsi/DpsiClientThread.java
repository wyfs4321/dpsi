package edu.alibaba.mpc4j.s2pc.dpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.dpsi.pid.PidDpsiClient;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * DPSI协议客户端线程。
 *
 * @author Yufei Wang
 * @date 2023/8/8
 */
public class DpsiClientThread extends Thread {
    /**
     * DPSI客户端
     */
    private final PidDpsiClient<ByteBuffer> client;
    /**
     * 客户端集合
     */
    private final Set<ByteBuffer> clientElementSet;
    /**
     * 服务端元素数量
     */
    private final int serverElementSize;
    /**
     * 客户端交集
     */
    private Set<ByteBuffer> intersectionSet;

    DpsiClientThread(PidDpsiClient<ByteBuffer> client, Set<ByteBuffer> clientElementSet, int serverElementSize) {
        this.client = client;
        this.clientElementSet = clientElementSet;
        this.serverElementSize = serverElementSize;
    }

    Set<ByteBuffer> getIntersectionSet() {
        return intersectionSet;
    }

    @Override
    public void run() {
        try {
            client.init(clientElementSet, 2*clientElementSet.size(), 2*serverElementSize);
            intersectionSet = client.dpsi();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}