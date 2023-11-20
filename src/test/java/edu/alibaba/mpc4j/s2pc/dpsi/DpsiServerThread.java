package edu.alibaba.mpc4j.s2pc.dpsi;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.s2pc.dpsi.pid.PidDpsiServer;

import java.nio.ByteBuffer;
import java.util.Set;

/**
 * DPSI协议服务端线程。
 *
 * @author Yufei Wang
 * @date 2023/8/8
 */
public class DpsiServerThread extends Thread{
    /**
     * PSI服务端
     */
    private final PidDpsiServer<ByteBuffer> server;
    /**
     * 服务端集合
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * 客户端元素数量
     */
    private final int clientElementSize;

    DpsiServerThread(PidDpsiServer<ByteBuffer> server, Set<ByteBuffer> serverElementSet, int clientElementSize) {
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
