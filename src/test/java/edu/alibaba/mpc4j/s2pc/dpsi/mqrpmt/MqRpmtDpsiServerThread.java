package edu.alibaba.mpc4j.s2pc.dpsi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;
/**
 * the server thread in the test of mqRPMT-DPSI
 *
 * @author Yufei Wang
 * @date 2023/9/19
 */
public class MqRpmtDpsiServerThread extends Thread{
    /**
     * PSI服务端
     */
    private final MqRpmtDpsiServer<ByteBuffer> server;
    /**
     * 服务端集合
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * 客户端元素数量
     */
    private final int clientElementSize;

    MqRpmtDpsiServerThread(MqRpmtDpsiServer<ByteBuffer> server, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.server = server;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    @Override
    public void run() {
        try {
            server.init(serverElementSet, 2*serverElementSet.size(), 2*clientElementSize);
            server.dpsi();
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}