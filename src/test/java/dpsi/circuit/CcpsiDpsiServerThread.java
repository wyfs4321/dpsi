package dpsi.circuit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;

import java.nio.ByteBuffer;
import java.util.Set;
/**
 * the server thread in the test of circuit-DPSI
 *
 * @author anonymous authors
 * @date 2023/9/19
 */
public class CcpsiDpsiServerThread extends Thread{
    /**
     * PSI服务端
     */
    private final CcpsiDpsiServer<ByteBuffer> server;
    /**
     * 服务端集合
     */
    private final Set<ByteBuffer> serverElementSet;
    /**
     * 客户端元素数量
     */
    private final int clientElementSize;

    CcpsiDpsiServerThread(CcpsiDpsiServer<ByteBuffer> server, Set<ByteBuffer> serverElementSet, int clientElementSize) {
        this.server = server;
        this.serverElementSet = serverElementSet;
        this.clientElementSize = clientElementSize;
    }

    @Override
    public void run() {
        try {
            server.init( serverElementSet.size(), clientElementSize);
            server.dpsi(serverElementSet,clientElementSize);
        } catch (MpcAbortException e) {
            e.printStackTrace();
        }
    }
}