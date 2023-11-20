package edu.alibaba.mpc4j.s2pc.dpsi.circuit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;


import java.nio.ByteBuffer;
import java.util.Set;
/**
 * the client thread in the test of circuit-DPSI
 *
 * @author Yufei Wang
 * @date 2023/9/19
 */
public class CcpsiDpsiClientThread extends Thread{
    /**
     * DPSI客户端
     */
    private final CcpsiDpsiClient<ByteBuffer> client;
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

    CcpsiDpsiClientThread(CcpsiDpsiClient<ByteBuffer> client, Set<ByteBuffer> clientElementSet, int serverElementSize) {
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
            client.init(clientElementSet.size(), serverElementSize);
            intersectionSet = client.dpsi(clientElementSet, serverElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}
