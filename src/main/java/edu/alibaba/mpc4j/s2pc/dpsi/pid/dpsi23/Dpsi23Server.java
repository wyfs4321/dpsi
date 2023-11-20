package edu.alibaba.mpc4j.s2pc.dpsi.pid.dpsi23;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVector;
import edu.alibaba.mpc4j.common.tool.bitvector.BitVectorFactory;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.crypto.prf.Prf;
import edu.alibaba.mpc4j.common.tool.crypto.prf.PrfFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdp;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdpFactory;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.TruncatedDiscreteLaplaceCdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdp;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdpFactory;
import edu.alibaba.mpc4j.s2pc.dpsi.pid.AbstractPidDpsiServer;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidParty;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidPartyOutput;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * server for the pid-Dpsi protocol
 *
 * @author Yufei Wang
 * @date 2023/8/2
 */
public class Dpsi23Server<T> extends AbstractPidDpsiServer<T> {
    /**
     * psi服务端
     */
    private final PidParty<ByteBuffer> pidServer;
    /**
     * ldp服务端
     */
    private final EncodeLdp ldp;

    TruncatedDiscreteLaplaceCdpConfig dpConfig;
    /**
     * Prf所需的全0密钥
     */
    private static final byte[] ZERO_KEY = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
    public Dpsi23Server(Rpc serverRpc, Party clientParty, Dpsi23Config config) {
        super(Dpsi23PtoDesc.getInstance(), serverRpc, clientParty, config);
        pidServer = PidFactory.createServer(serverRpc,clientParty,config.getPidConfig());
        addSubPtos(pidServer);
        ldp = EncodeLdpFactory.createInstance(config.getLdpConfig());
        dpConfig=(TruncatedDiscreteLaplaceCdpConfig)config.getDpConfig();
    }


    @Override
    public void init(Set<T> serverElementSet, int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        pidServer.init(maxServerElementSize, maxClientElementSize);
        HashMap<ByteBuffer,T> dummyServerElementMap=createDummyServerElementSet(serverElementSet, maxServerElementSize);
        setServerInfo(serverElementSet,dummyServerElementMap);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public void dpsi() throws MpcAbortException {
//        setPtoInput(serverElementSet, clientElementSize);
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        //发送服务端填充数据集规模
        List<byte[]> dummyServerElementSizePayload=new ArrayList<>();
        dummyServerElementSizePayload.add(String.valueOf(dummyServerElementSize).getBytes());
        DataPacketHeader serverElementsizeHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Dpsi23PtoDesc.PtoStep.SERVER_SEND_ELEMENTS_SIZE.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverElementsizeHeader, dummyServerElementSizePayload));
        //接收客户端填充数据集规模
        DataPacketHeader clientElementsizeHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Dpsi23PtoDesc.PtoStep.CLIENT_SEND_ELEMENTS_SIZE.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> dummyClientElementSizePayload = rpc.receive(clientElementsizeHeader).getPayload();
        setdummyClientElementSize(handledummyClientElementSizePayload(dummyClientElementSizePayload));


        stopWatch.stop();
        long initVariableTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, initVariableTime);

        stopWatch.start();
        Set<ByteBuffer> dummyServerElementSet=dummyServerElementMap.keySet();
        PidPartyOutput<ByteBuffer>  pidServerOutput=  pidServer.pid(dummyServerElementSet,dummyClientElementSize);
        stopWatch.stop();
        long pidTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, pidTime);

        stopWatch.start();
        Set<ByteBuffer> pidServerSet=pidServerOutput.getPidSet();
        List<ByteBuffer> pidServerList = new ArrayList<>(pidServerSet);
        BitVector randomizedServerVector = BitVectorFactory.createZeros(pidServerList.size());
        Prf prf = PrfFactory.createInstance(PrfFactory.PrfType.JDK_AES_CBC, CommonConstants.BLOCK_BYTE_LENGTH/2);
        prf.setKey(ZERO_KEY);
        IntStream.range(0,pidServerList.size()).forEach(index -> {
            byte[] prfPid = prf.getBytes(pidServerList.get(index).array());
            double prfPidDouble = convertByteArrayToDouble(prfPid);
            double p = Math.exp(ldp.getEpsilon()) / (Math.exp(ldp.getEpsilon()) + 1);
            if (prfPidDouble<=p) {
                randomizedServerVector.set(index, true);
            }
        });
        List<byte[]> dpsiPayload = new LinkedList<>();
        dpsiPayload.add(randomizedServerVector.getBytes());
        DataPacketHeader dpsiHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Dpsi23PtoDesc.PtoStep.SERVER_SEND_NOISY_PID_LIST.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(dpsiHeader, dpsiPayload));
        stopWatch.stop();
        long ldpTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, ldpTime);

        logPhaseInfo(PtoState.PTO_END);
    }


    public HashMap<ByteBuffer,T> createDummyServerElementSet (Set<T> serverElementSet, int maxServerElementSize){
        //将原始element映射为首位为0的格式并填入dummySet
        Hash hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        HashMap<ByteBuffer,T> dummyServerElementMap= new HashMap<>();
        serverElementSet.forEach(e ->{
            byte[] element = hash.digestToBytes(ObjectUtils.objectToByteArray(e));
            element[0] = (byte) (element[0] & (byte) 0x7F);
            dummyServerElementMap.put(ByteBuffer.wrap(element),e);
        });
        int maxDummySize=maxServerElementSize-serverElementSet.size();
        dpConfig.setParameter((double) 1 /maxServerElementSize,0,maxDummySize);
        BoundIntegralCdp dp = BoundIntegralCdpFactory.createInstance(dpConfig);
        int dummySize= dp.randomize(0);

        IntStream.range(0,dummySize).forEach(index -> {
                byte[] dummyElement = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                int intElement = index;
                for (int i = 0; i < CommonConstants.BLOCK_BYTE_LENGTH; i++) {
                    dummyElement[CommonConstants.BLOCK_BYTE_LENGTH - 1 - i] = (byte) (intElement & 0xFF);
                    intElement = intElement >> Byte.SIZE;
                dummyElement[0] = (byte) (dummyElement[0] | (byte) 0x80);
                dummyServerElementMap.put(ByteBuffer.wrap(dummyElement), null);
            }
        });
        return dummyServerElementMap;
    }

    private int handledummyClientElementSizePayload(List<byte[]> dummyClientElementSizePayload) {
        String sizePayload=new String(dummyClientElementSizePayload.get(0));
        return Integer.parseInt(sizePayload);
    }
    private  double convertByteArrayToDouble(byte[] prfPid){
        ByteBuffer buffer = ByteBuffer.wrap(prfPid);
        long longValue = buffer.getLong();
        double mappedValue = (double) longValue / Long.MAX_VALUE;
        return (mappedValue+1.0)/2;
    }
}
