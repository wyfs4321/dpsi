package edu.alibaba.mpc4j.s2pc.dpsi.circuit;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdp;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdpFactory;
import edu.alibaba.mpc4j.s2pc.aby.basics.z2.SquareZ2Vector;

import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Server of the circuit-DPSI protocols
 *
 * @author Yufei Wang
 * @date 2023/8/15
 */
public class CcpsiDpsiServer<T> extends AbstractCcpsiDpsiServer<T> {
    /**
     * psi服务端
     */
    private final CcpsiServer<T> ccpsiServer;
    /**
     * ldp服务端
     */
    private final EncodeLdp ldp;

    public CcpsiDpsiServer(Rpc serverRpc, Party clientParty, CcpsiDpsiConfig config) {
        super(CcpsiDpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        ccpsiServer = CcpsiFactory.createServer(serverRpc,clientParty,config.getCcpsiConfig());
        addSubPtos(ccpsiServer);
        ldp = EncodeLdpFactory.createInstance(config.getLdpConfig());
    }

    public void init(int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        ccpsiServer.init(maxServerElementSize, maxClientElementSize);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

//    @Override
    public void dpsi(Set<T> serverElementSet, int clientElementSize) throws MpcAbortException {
        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        SquareZ2Vector ccpsiServerOutput=ccpsiServer.psi(serverElementSet, clientElementSize);
        stopWatch.stop();
        long ccpsiTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 2, ccpsiTime, "Server runs ccpsi");

        stopWatch.start();
        byte[] randomizedServerList=handlerandomizedccpsiServerList(ccpsiServerOutput);
        List<byte[]> randomizedServerPayload=new ArrayList<>();
        randomizedServerPayload.add(randomizedServerList);
        DataPacketHeader randomizedServerVectorHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), CcpsiDpsiPtoDesc.PtoStep.SERVER_SEND_RANDOMIZED_VECTOR.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(randomizedServerVectorHeader, randomizedServerPayload));
        stopWatch.stop();
        long ldpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 2, ldpTime);

        logPhaseInfo(PtoState.PTO_END);

    }

    public byte[] handlerandomizedccpsiServerList (SquareZ2Vector ccpsiServerOutput) {
        BitVector ccpsiServerVector=ccpsiServerOutput.getBitVector();
        BitVector randomizedServerList= BitVectorFactory.createZeros(ccpsiServerVector.bitNum());
        IntStream.range(0,ccpsiServerVector.bitNum()).forEach(index ->{
            String coin=ldp.randomize(ccpsiServerVector.get(index)?"1":"0");
            if (coin.equals("1")) {randomizedServerList.set(index,true);}
        });
        return randomizedServerList.getBytes();
    }


}
