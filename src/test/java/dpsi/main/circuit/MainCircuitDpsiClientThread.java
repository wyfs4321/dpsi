package dpsi.main.circuit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

import java.io.IOException;

/**
 * Circuit-DPSI main client thread.
 *
 * @author anonymous authors
 * @date 2023/10/08
 */
public class MainCircuitDpsiClientThread extends Thread {
    /**
     * client RPC
     */
    private final Rpc clientRpc;
    /**
     * server party
     */
    private final Party serverParty;
    /**
     * main DPSI
     */
    private final CircuitDpsiMain circuitDpsiMain;
    /**
     * success
     */
    private boolean success;

    MainCircuitDpsiClientThread(Rpc clientRpc, Party serverParty, CircuitDpsiMain circuitDpsiMain) {
        this.clientRpc=clientRpc;
        this.serverParty=serverParty;
        this.circuitDpsiMain=circuitDpsiMain;
    }

    boolean getSuccess() {
        return success;
    }

    @Override
    public void run() {
        try {
            circuitDpsiMain.runClient(clientRpc, serverParty);
            success = true;
        } catch (MpcAbortException | IOException e) {
            e.printStackTrace();
        }
    }
}
