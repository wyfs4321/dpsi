package edu.alibaba.mpc4j.s2pc.dpsi.main.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.s2pc.dpsi.main.circuit.CircuitDpsiMain;

import java.io.IOException;

/**
 * mqRPMT-DPSI main server thread.
 *
 * @author Yufei Wang
 * @date 2023/10/09
 */
public class MainMqRpmtDpsiServerThread extends Thread{
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
    private final MqRpmtDpsiMain mqRpmtDpsiMain;
    /**
     * success
     */
    private boolean success;

    MainMqRpmtDpsiServerThread(Rpc serverRpc, Party clientParty, MqRpmtDpsiMain mqRpmtDpsiMain) {
        this.serverRpc = serverRpc;
        this.clientParty = clientParty;
        this.mqRpmtDpsiMain = mqRpmtDpsiMain;
    }
    boolean getSuccess() {
        return success;
    }

    @Override
    public void run() {
        try {
            mqRpmtDpsiMain.runServer(serverRpc, clientParty);
            success = true;
        } catch (MpcAbortException | IOException e) {
            e.printStackTrace();
        }
    }
}
