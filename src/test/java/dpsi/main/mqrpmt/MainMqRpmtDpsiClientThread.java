package dpsi.main.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;

import java.io.IOException;

/**
 * MqRpmt-DPSI main client thread.
 *
 * @author anonymous authors
 * @date 2023/10/09
 */
public class MainMqRpmtDpsiClientThread extends Thread{
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
    private final MqRpmtDpsiMain mqRpmtDpsiMain;
    /**
     * success
     */
    private boolean success;

    MainMqRpmtDpsiClientThread(Rpc clientRpc, Party serverParty, MqRpmtDpsiMain mqRpmtDpsiMain) {
        this.clientRpc=clientRpc;
        this.serverParty=serverParty;
        this.mqRpmtDpsiMain=mqRpmtDpsiMain;
    }

    boolean getSuccess() {
        return success;
    }

    @Override
    public void run() {
        try {
            mqRpmtDpsiMain.runClient(clientRpc, serverParty);
            success = true;
        } catch (MpcAbortException | IOException e) {
            e.printStackTrace();
        }
    }
}
