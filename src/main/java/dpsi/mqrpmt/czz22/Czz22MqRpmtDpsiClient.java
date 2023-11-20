package dpsi.mqrpmt.czz22;

import dpsi.mqrpmt.AbstractMqRpmtDpsiClient;
import edu.alibaba.mpc4j.common.rpc.*;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacket;
import edu.alibaba.mpc4j.common.rpc.utils.DataPacketHeader;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import edu.alibaba.mpc4j.common.tool.utils.ObjectUtils;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdp;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdpFactory;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.TruncatedDiscreteLaplaceCdpConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtServer;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * Client for the mqRPMT-DPSI protocols
 *
 * @author anonymous authors
 * @date 2023/9/19
 */
public class Czz22MqRpmtDpsiClient<T> extends AbstractMqRpmtDpsiClient<T> {
    /**
     * mqRPMT服务端，之所以在DpsiServer中调用mqRPMTServer是因为dpsi需要由server端拿到vector以进行扰动
     */
    private final MqRpmtServer mqRPMTClient;
    /**
     * 服务端输出向量
     */
    private ByteBuffer[] clientVector;

    TruncatedDiscreteLaplaceCdpConfig dpConfig;
    /**
     * 用于生成dummySize的dp服务端
     */
    BoundIntegralCdp dp;

    public Czz22MqRpmtDpsiClient(Rpc clientRpc, Party serverParty, Czz22MqRpmtDpsiConfig config) {
        super(Czz22MqRpmtDpsiPtoDesc.getInstance(), clientRpc, serverParty, config);
        mqRPMTClient = MqRpmtFactory.createServer(clientRpc, serverParty,config.getMqRpmtConfig());
        addSubPtos(mqRPMTClient);
        dpConfig=(TruncatedDiscreteLaplaceCdpConfig)config.getDpConfig();
    }
    @Override
    public void init(Set<T> clientElementSet, int maxClientElementSize, int maxServerElementSize) throws MpcAbortException {
        setInitInput(maxClientElementSize, maxServerElementSize);
        logPhaseInfo(PtoState.INIT_BEGIN);

        stopWatch.start();
        mqRPMTClient.init(maxClientElementSize, maxServerElementSize);
        HashMap<ByteBuffer,T> dummyClientElementMap=createDummyClientElementSet(clientElementSet, maxServerElementSize);
        setClientInfo(clientElementSet,dummyClientElementMap);
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
                encodeTaskId, getPtoDesc().getPtoId(), Czz22MqRpmtDpsiPtoDesc.PtoStep.SERVER_SEND_ELEMENTS_SIZE.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        List<byte[]> dummyServerElementSizePayload = rpc.receive(serverElementsizeHeader).getPayload();
        dummyServerElementSize=handleummyServerElementSizePayload(dummyServerElementSizePayload);
        setdummyServerElementSize(dummyServerElementSize);

        //发送客户端填充数据集规模
        List<byte[]> dummyClientElementSizePayload=new ArrayList<>();
        dummyClientElementSizePayload.add(String.valueOf(dummyClientElementSize).getBytes());
        DataPacketHeader clientElementsizeHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Czz22MqRpmtDpsiPtoDesc.PtoStep.CLIENT_SEND_ELEMENTS_SIZE.ordinal(), extraInfo,
                ownParty().getPartyId(), otherParty().getPartyId()
        );
        rpc.send(DataPacket.fromByteArrayList(clientElementsizeHeader, dummyClientElementSizePayload));
        stopWatch.stop();
        long initVariableTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 1, 4, initVariableTime);

        //计算mqRPMT
        stopWatch.start();
        Set<ByteBuffer> dummyClientElementSet=dummyClientElementMap.keySet();
        clientVector=mqRPMTClient.mqRpmt(dummyClientElementSet,dummyServerElementSize);
        stopWatch.stop();
        long mqRPMTTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 2, 4, mqRPMTTime);


        //客户端接收来自服务端的扰动向量
        stopWatch.start();

        DataPacketHeader vecHeader = new DataPacketHeader(
                encodeTaskId, getPtoDesc().getPtoId(), Czz22MqRpmtDpsiPtoDesc.PtoStep.SERVER_SEND_VECTOR.ordinal(), extraInfo,
                otherParty().getPartyId(), ownParty().getPartyId()
        );
        byte[] containVector = rpc.receive(vecHeader).getPayload().get(0);
        stopWatch.stop();
        long encTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 3, 4, encTime, "Server handles union");



        stopWatch.start();
        IntStream intStream = IntStream.range(0, dummyClientElementSize);
        intStream = parallel ? intStream.parallel() : intStream;
        List<ByteBuffer> intersection = new ArrayList<>();
        intStream.forEach(index -> {
                    if (containVector[index]==1) {
                        intersection.add(clientVector[index]);
                    }
        });

        Set<T> output = new HashSet<>();
        IntStream.range(0,intersection.size()).forEach(index -> {
//            T originElement=dummyClientElementMap.get(intersection.get(index));

            output.add(dummyClientElementMap.get(intersection.get(index)));
        });
        stopWatch.stop();
        long interTime = stopWatch.getTime(TimeUnit.MILLISECONDS);
        stopWatch.reset();
        logStepInfo(PtoState.PTO_STEP, 4, 4, interTime);

        logPhaseInfo(PtoState.PTO_END);
        return output;
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
        int maxDummySize=maxServerElementSize/2;
        dpConfig.setParameter((float)1/maxServerElementSize,0,maxDummySize);
        dp = BoundIntegralCdpFactory.createInstance(dpConfig);
        int dummySize=dp.randomize(0);
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