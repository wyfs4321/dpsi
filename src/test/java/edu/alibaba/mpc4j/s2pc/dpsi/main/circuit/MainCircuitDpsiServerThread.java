package edu.alibaba.mpc4j.s2pc.dpsi.main.circuit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import java.io.IOException;

/**
 * Circuit-DPSI main server thread.
 *
 * @author Yufei Wang
 * @date 2023/10/08
 */
public class MainCircuitDpsiServerThread extends Thread{
    /**
     * server RPC
     */
    private final Rpc serverRpc;
    /**
     * client party
     */
    private final Party clientParty;
    /**
     * main DPSI
     */
    private final CircuitDpsiMain circuitDpsiMain;
    /**
     * success
     */
    private boolean success;

    MainCircuitDpsiServerThread(Rpc serverRpc, Party clientParty, CircuitDpsiMain circuitDpsiMain) {
        this.serverRpc = serverRpc;
        this.clientParty = clientParty;
        this.circuitDpsiMain = circuitDpsiMain;
    }

    boolean getSuccess() {
        return success;
    }

    @Override
    public void run() {
        try {
            circuitDpsiMain.runServer(serverRpc, clientParty);
            success = true;
        } catch (MpcAbortException | IOException e) {
            e.printStackTrace();
        }
    }
}
