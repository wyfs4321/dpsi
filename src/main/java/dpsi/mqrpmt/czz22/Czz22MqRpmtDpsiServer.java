package dpsi.mqrpmt.czz22;

import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.PtoState;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdp;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdpFactory;
import dpsi.mqrpmt.AbstractMqRpmtDpsiServer;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtClient;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
/**
 * Server for the mqRPMT-DPSI protocols
 *
 * @author anonymous authors
 * @date 2023/9/19
 */
public class Czz22MqRpmtDpsiServer<T> extends AbstractMqRpmtDpsiServer<T> {
    /**
     * mqRPMT客户端
     */
    private final MqRpmtClient mqRPMTServer;
    /**
     * ldp服务端
     */
    private final EncodeLdp ldp;

    /**
     * 客户端输出
     */
    private boolean[] containVector;

    public Czz22MqRpmtDpsiServer(Rpc serverRpc, Party clientParty, Czz22MqRpmtDpsiConfig config) {
        super(Czz22MqRpmtDpsiPtoDesc.getInstance(), serverRpc, clientParty, config);
        mqRPMTServer = MqRpmtFactory.createClient(serverRpc,clientParty,config.getMqRpmtConfig());
        addSubPtos(mqRPMTServer);
        ldp = EncodeLdpFactory.createInstance(config.getLdpConfig());
    }


    @Override
    public void init(Set<T> serverElementSet, int maxServerElementSize, int maxClientElementSize) throws MpcAbortException {
        setInitInput(maxServerElementSize, maxClientElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        mqRPMTServer.init(maxServerElementSize, maxClientElementSize);
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
                encodeTaskId, getPtoDesc().getPtoId(), Czz22MqRpmtDpsiPtoDesc.PtoStep.SERVER_SEND_ELEMENTS_SIZE.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(serverElementsizeHeader, dummyServerElementSizePayload));
        //接收客户端填充数据集规模
        DataPacketHeader clientElementsizeHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Czz22MqRpmtDpsiPtoDesc.PtoStep.CLIENT_SEND_ELEMENTS_SIZE.ordinal(), extraInfo,
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
        containVector = mqRPMTServer.mqRpmt(dummyServerElementSet, dummyClientElementSize);
        stopWatch.stop();
        long mqRPMTTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 3, mqRPMTTime);

        stopWatch.start();
        byte[] noisyContainVector=new byte[containVector.length];
        IntStream.range(0,containVector.length).forEach(index -> {
            String coin=ldp.randomize("1");
            if (coin.equals("0")) {
                noisyContainVector[index]=containVector[index]?(byte)0:(byte)1;
            }
            else {
                noisyContainVector[index]=containVector[index]?(byte)1:(byte)0;
            }
        });
        List<byte[]> vecPayload = new LinkedList<>();
        vecPayload.add(noisyContainVector);
        DataPacketHeader vecHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Czz22MqRpmtDpsiPtoDesc.PtoStep.SERVER_SEND_VECTOR.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(vecHeader, vecPayload));
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
        int dummySize=maxServerElementSize/2;
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
}

