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
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.s2pc.dpsi.pid.AbstractPidDpsiClient;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidParty;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidPartyOutput;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
/**
 * client for the pid-Dpsi protocol
 *
 * @author Yufei Wang
 * @date 2023/8/2
 */
public class Dpsi23Client<T> extends AbstractPidDpsiClient<T> {
    /**
     * psi服务端
     */
    private final PidParty<ByteBuffer> pidClient;

    public Dpsi23Client(Rpc clientRpc, Party serverParty, Dpsi23Config config) {
        super(Dpsi23PtoDesc.getInstance(), clientRpc, serverParty, config);
        pidClient = PidFactory.createClient(clientRpc, serverParty,config.getPidConfig());
        addSubPtos(pidClient);
    }
    @Override
    public void init(Set<T> clientElementSet, int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        pidClient.init(maxClientElementSize, maxServerElementSize);
        HashMap<ByteBuffer,T> dummyServerElementMap=createDummyClientElementSet(clientElementSet, maxServerElementSize);
        setClientInfo(clientElementSet,dummyServerElementMap);
        stopWatch.stop();
        long initTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.INIT_STEP, 1, 1, initTime);

        logPhaseInfo(PtoState.INIT_END);
    }

    @Override
    public Set<T> dpsi() throws MpcAbortException {
        logPhaseInfo(PtoState.PTO_BEGIN);

        stopWatch.start();
        //接收服务端填充数据集规模
        DataPacketHeader serverElementsizeHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Dpsi23PtoDesc.PtoStep.SERVER_SEND_ELEMENTS_SIZE.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> dummyServerElementSizePayload = rpc.receive(serverElementsizeHeader).getPayload();
        dummyServerElementSize=handleummyServerElementSizePayload(dummyServerElementSizePayload);
        setdummyServerElementSize(dummyServerElementSize);
        //发送客户端填充数据集规模
        List<byte[]> dummyClientElementSizePayload=new ArrayList<>();
        dummyClientElementSizePayload.add(String.valueOf(dummyClientElementSize).getBytes());
        DataPacketHeader clientElementsizeHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Dpsi23PtoDesc.PtoStep.CLIENT_SEND_ELEMENTS_SIZE.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientElementsizeHeader, dummyClientElementSizePayload));
        stopWatch.stop();
        long initVariableTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 3, initVariableTime);

        stopWatch.start();
        Set<ByteBuffer> dummyClientElementSet= dummyClientElementMap.keySet();
        PidPartyOutput<ByteBuffer> pidClientOutput=pidClient.pid(dummyClientElementSet,dummyServerElementSize);
        stopWatch.stop();
        long pidTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, pidTime);


        //客户端接收服务端的pid集合，并求交集即可
        stopWatch.start();
        Set<ByteBuffer> pidClientSet=pidClientOutput.getPidSet();
        List<ByteBuffer> pidClientList = new ArrayList<>(pidClientSet);
        DataPacketHeader dpsiHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Dpsi23PtoDesc.PtoStep.SERVER_SEND_NOISY_PID_LIST.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> pidServerPayload = rpc.receive(dpsiHeader).getPayload();
        BitVector pidServerVector= BitVectorFactory.create(pidClientList.size(), pidServerPayload.get(0));
        BitVector pidClientVector = BitVectorFactory.createZeros(pidClientList.size());
        IntStream.range(0,pidClientList.size()).forEach(index -> {
            if (pidClientOutput.getId(pidClientList.get(index)) != null) {
                pidClientVector.set(index, true);
            }
        });
        BitVector z=pidServerVector.xor(pidClientVector);

        List<byte[]> intersectionSetPidList = new LinkedList<>();
        IntStream.range(0,z.bitNum()).forEach(index -> {
            if (z.get(index)){
                intersectionSetPidList.add(pidClientList.get(index).array());
            }
        });

        ByteBuffer[] intersectionSetPidBuffer = intersectionSetPidList.stream()
                .map(ObjectUtils::objectToByteArray)
                .map(ByteBuffer::wrap)
                .toArray(ByteBuffer[]::new);

        Set<ByteBuffer> hashIntersectionSet=new HashSet<>();
        IntStream.range(0,intersectionSetPidBuffer.length).forEach(index -> hashIntersectionSet.add(pidClientOutput.getId(intersectionSetPidBuffer[index])));
        stopWatch.stop();
        long psiTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 3, psiTime);

        logPhaseInfo(PtoState.PTO_END);
        Set<T> intersectionSet=new HashSet<>();
        for(ByteBuffer hashElement: hashIntersectionSet) {
            intersectionSet.add(dummyClientElementMap.get(hashElement));
        }
        return intersectionSet.stream().filter(Objects::nonNull).collect(Collectors.toSet());
    }
    public HashMap<ByteBuffer,T> createDummyClientElementSet (Set<T> clientElementSet, int maxServerElementSize){
        //将原始element映射为首位为0的格式并填入dummySet
        Hash hash = HashFactory.createInstance(envType, CommonConstants.BLOCK_BYTE_LENGTH);
        HashMap<ByteBuffer,T> dummyClientElementMap= new HashMap<>();
        clientElementSet.forEach(e ->{
            byte[] element = hash.digestToBytes(ObjectUtils.objectToByteArray(e));
            element[0] = (byte) (element[0] & (byte) 0x7F);
            dummyClientElementMap.put(ByteBuffer.wrap(element),e);
        });
        int dummySize=maxServerElementSize-clientElementSet.size();
        IntStream.range(0,dummySize).forEach(index -> {
                byte[] dummyElement = new byte[CommonConstants.BLOCK_BYTE_LENGTH];
                int intElement = index;
                for (int i = 0; i < CommonConstants.BLOCK_BYTE_LENGTH; i++) {
                    dummyElement[CommonConstants.BLOCK_BYTE_LENGTH - 1 - i] = (byte) (intElement & 0xFF);
                    intElement = intElement >> Byte.SIZE;
                dummyElement[0] = (byte) (dummyElement[0] | (byte) 0x80);
                dummyClientElementMap.put(ByteBuffer.wrap(dummyElement), null);
            }
        });
        return dummyClientElementMap;
    }

    private int handleummyServerElementSizePayload(List<byte[]> dummyServerElementSizePayload) {
        String sizePayload=new String(dummyServerElementSizePayload.get(0));
        return Integer.parseInt(sizePayload);
    }
}
