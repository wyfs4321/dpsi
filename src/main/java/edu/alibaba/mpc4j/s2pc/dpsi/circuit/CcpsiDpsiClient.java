package edu.alibaba.mpc4j.s2pc.dpsi.circuit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiClient;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiClientOutput;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
/**
 * client for circuit-DPSI
 *
 * @author Yufei Wang
 * @date 2023/8/15
 */
public class CcpsiDpsiClient<T> extends AbstractCcpsiDpsiClient<T> {
    /**
     * psi服务端
     */
    private final CcpsiClient<T> ccpsiClient;
    public CcpsiDpsiClient(Rpc clientRpc, Party serverParty, CcpsiDpsiConfig config) {
        super(CcpsiDpsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        ccpsiClient = CcpsiFactory.createClient(clientRpc, serverParty, config.getCcpsiConfig());
        addSubPtos(ccpsiClient);
    }

    public void init(int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        ccpsiClient.init(maxClientElementSize, maxServerElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

//    @Override
    public Set<T> dpsi(Set<T> clientElementSet, int serverElementSize) throws MpcAbortException {
        setPtoInput(clientElementSet, serverElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        CcpsiClientOutput<T> ccpsiClientOutput;
        ccpsiClientOutput=ccpsiClient.psi(clientElementSet, serverElementSize);
        stopWatch.stop();
        long ccpsiTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, ccpsiTime, "Client runs ccpsi");

        stopWatch.start();
        DataPacketHeader randomizedServerVectorHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), CcpsiDpsiPtoDesc.PtoStep.SERVER_SEND_RANDOMIZED_VECTOR.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> randomizedServerVectorPayload=rpc.receive(randomizedServerVectorHeader).getPayload();
        BitVector randomizedServerVector=handlerandomizedServerVectorPayload(randomizedServerVectorPayload, ccpsiClientOutput.getZ1().bitNum());
        BitVector z=randomizedServerVector.xor(ccpsiClientOutput.getZ1().getBitVector());
        ArrayList<T> table = ccpsiClientOutput.getTable();
        Set<T> intersectionSet=new HashSet<>();
        int beta = ccpsiClientOutput.getBeta();
        for (int i = 0;i<beta;i++) {
            if (table.get(i)!=null && z.get(i)) {
                intersectionSet.add(table.get(i));
            }
        }
        return intersectionSet;
    }
    public BitVector handlerandomizedServerVectorPayload(List<byte[]> randomizedServerVectorPayload,int beta) {
        byte[] randomizedServerList=randomizedServerVectorPayload.get(0);
        return  BitVectorFactory.create(beta, randomizedServerList);
    }

}
