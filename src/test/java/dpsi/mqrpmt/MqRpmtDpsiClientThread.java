package dpsi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * the client thread in the test of mqRPMT-DPSI
 *
 * @author anonymous authors
 * @date 2023/9/19
 */

public class MqRpmtDpsiClientThread extends Thread {
    /**
     * DPSI客户端
     */
    private final MqRpmtDpsiClient<ByteBuffer> client;
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

    MqRpmtDpsiClientThread(MqRpmtDpsiClient<ByteBuffer> client, Set<ByteBuffer> clientElementSet, int serverElementSize) {
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