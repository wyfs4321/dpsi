package dpsi.main.circuit;

import com.google.common.base.Preconditions;
import dpsi.circuit.CcpsiDpsiClient;
import dpsi.circuit.CcpsiDpsiConfig;
import dpsi.circuit.CcpsiDpsiFactory;
import dpsi.circuit.CcpsiDpsiServer;
import edu.alibaba.mpc4j.common.rpc.MpcAbortException;
import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcPropertiesUtils;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.main.PsoMain;
import org.apache.commons.lang3.time.StopWatch;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Circuit DPSI main.
 *
 * @author anonymous authors
 * @date 2023/10/09
 */
public class CircuitDpsiMain {
    private static final Logger LOGGER = LoggerFactory.getLogger(PsoMain.class);
    /**
     * protocol type name
     */
    public static final String PTO_TYPE_NAME = "Circuit_DPSI";
    /**
     * warmup element byte length
     */
    private static final int WARMUP_ELEMENT_BYTE_LENGTH = 16;
    /**
     * warmup set size
     */
    private static final int WARMUP_SET_SIZE = 1 << 10;
    /**
     * server stop watch
     */
    private final StopWatch serverStopWatch;
    /**
     * client stop watch
     */
    private final StopWatch clientStopWatch;
    /**
     * properties
     */
    private final Properties properties;

    public CircuitDpsiMain(Properties properties) {
        this.properties = properties;
        serverStopWatch = new StopWatch();
        clientStopWatch = new StopWatch();
    }

    public void runNetty() throws Exception {
        Rpc ownRpc = RpcPropertiesUtils.readNettyRpc(properties, "server", "client");
        if (ownRpc.ownParty().getPartyId() == 0) {
            runServer(ownRpc, ownRpc.getParty(1));
        } else if (ownRpc.ownParty().getPartyId() == 1) {
            runClient(ownRpc, ownRpc.getParty(0));
        } else {
            throw new IllegalArgumentException("Invalid PartyID for own_name: " + ownRpc.ownParty().getPartyName());
        }
    }

    public void runServer(Rpc serverRpc, Party clientParty) throws MpcAbortException, IOException {
        // 读取协议参数
        LOGGER.info("{} read settings", serverRpc.ownParty().getPartyName());
        // 读取元素字节长度
        int elementByteLength = PropertiesUtils.readInt(properties, "element_byte_length");
        // 读取集合大小
        int[] serverLogSetSizes = PropertiesUtils.readLogIntArray(properties, "server_log_set_size");
        int[] clientLogSetSizes = PropertiesUtils.readLogIntArray(properties, "client_log_set_size");
//        Preconditions.checkArgument(
//                serverLogSetSizes.length == clientLogSetSizes.length,
//                "# of server log_set_size = %s, $ of client log_set_size = %s, they must be equal",
//                serverLogSetSizes.length, clientLogSetSizes.length
//        );
        int setSizeNum = serverLogSetSizes.length;
        int[] serverSetSizes = Arrays.stream(serverLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        int[] clientSetSizes = Arrays.stream(clientLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        // 读取特殊参数
        LOGGER.info("{} read PTO config", serverRpc.ownParty().getPartyName());
        CcpsiDpsiConfig config = CircuitDpsiConfigUtils.createCircuitDpsiConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", serverRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", serverRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0 ; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength);
        }
        LOGGER.info("{} create result file", serverRpc.ownParty().getPartyName());
        // 创建统计结果文件
        String filePath = PTO_TYPE_NAME
                + "_" + config.getPtoType().name()
                + "_" + PropertiesUtils.readString(properties, "circuit_psi_pto_name")
                + PropertiesUtils.readString(properties, "append_string", "")
                + "_" + elementByteLength * Byte.SIZE
                + "_" + serverRpc.ownParty().getPartyId()
                + "_" + ForkJoinPool.getCommonPoolParallelism()
                + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num"
                + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
                + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", serverRpc.ownParty().getPartyName());
        // 建立连接
        serverRpc.connect();
        // 启动测试
        int taskId = 0;
        // 预热
        warmupServer(serverRpc, clientParty, config, taskId);
        taskId++;
        // 正式测试
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            int clientSetSize = clientSetSizes[setSizeIndex];
            Set<ByteBuffer> serverElementSet = readServerElementSet(serverSetSize, elementByteLength);
            runServer(serverRpc, clientParty, config, taskId, true, serverElementSet, clientSetSize,
                    printWriter);
            taskId++;
            runServer(serverRpc, clientParty, config, taskId, false, serverElementSet, clientSetSize,
                    printWriter);
            taskId++;
        }
        // 断开连接
        serverRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }
    private Set<ByteBuffer> readServerElementSet(int setSize, int elementByteLength) throws IOException {
        LOGGER.info("Server read element set");
        InputStreamReader inputStreamReader = new InputStreamReader(
                Files.newInputStream(Paths.get(PsoUtils.getBytesFileName(PsoUtils.BYTES_SERVER_PREFIX, setSize, elementByteLength))),
                CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Set<ByteBuffer> serverElementSet = bufferedReader.lines()
                .map(Hex::decode)
                .map(ByteBuffer::wrap)
                .collect(Collectors.toSet());
        bufferedReader.close();
        inputStreamReader.close();
        return serverElementSet;
    }

    private void warmupServer(Rpc serverRpc, Party clientParty, CcpsiDpsiConfig config, int taskId) throws MpcAbortException, IOException {
        Set<ByteBuffer> serverElementSet = readServerElementSet(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        CcpsiDpsiServer<ByteBuffer> circuitDpsiServer = CcpsiDpsiFactory.createServer(serverRpc, clientParty, config);
        circuitDpsiServer.setTaskId(taskId);
        circuitDpsiServer.setParallel(false);
        circuitDpsiServer.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", circuitDpsiServer.ownParty().getPartyName());
        circuitDpsiServer.init(WARMUP_SET_SIZE, WARMUP_SET_SIZE);
        circuitDpsiServer.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", circuitDpsiServer.ownParty().getPartyName());
        circuitDpsiServer.dpsi(serverElementSet, WARMUP_SET_SIZE);
        circuitDpsiServer.getRpc().synchronize();
        circuitDpsiServer.getRpc().reset();
        LOGGER.info("(warmup) {} finish", circuitDpsiServer.ownParty().getPartyName());
    }

    public void runServer(Rpc serverRpc, Party clientParty, CcpsiDpsiConfig config, int taskId, boolean parallel,
                          Set<ByteBuffer> serverElementSet, int clientSetSize,
                          PrintWriter printWriter) throws MpcAbortException {
        int serverSetSize = serverElementSet.size();
        CcpsiDpsiServer<ByteBuffer> circuitDpsiServer = CcpsiDpsiFactory.createServer(serverRpc, clientParty, config);
        circuitDpsiServer.setTaskId(taskId);
        circuitDpsiServer.setParallel(parallel);
        // 启动测试
        circuitDpsiServer.getRpc().synchronize();
        circuitDpsiServer.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", circuitDpsiServer.ownParty().getPartyName());
        serverStopWatch.start();
        circuitDpsiServer.init(serverSetSize, clientSetSize);
        serverStopWatch.stop();
        long initTime = serverStopWatch.getTime(TimeUnit.MILLISECONDS);
        serverStopWatch.reset();
        long initDataPacketNum = circuitDpsiServer.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = circuitDpsiServer.getRpc().getPayloadByteLength();
        long initSendByteLength = circuitDpsiServer.getRpc().getSendByteLength();
        circuitDpsiServer.getRpc().synchronize();
        circuitDpsiServer.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", circuitDpsiServer.ownParty().getPartyName());
        serverStopWatch.start();
        circuitDpsiServer.dpsi(serverElementSet, clientSetSize);
        serverStopWatch.stop();
        long ptoTime = serverStopWatch.getTime(TimeUnit.MILLISECONDS);
        serverStopWatch.reset();
        long ptoDataPacketNum = circuitDpsiServer.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = circuitDpsiServer.getRpc().getPayloadByteLength();
        long ptoSendByteLength = circuitDpsiServer.getRpc().getSendByteLength();
        // 写入统计结果
        String info = circuitDpsiServer.ownParty().getPartyId()
                + "\t" + serverSetSize
                + "\t" + clientSetSize
                + "\t" + circuitDpsiServer.getParallel()
                + "\t" + ForkJoinPool.getCommonPoolParallelism()
                + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
                + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength;
        printWriter.println(info);
        // 同步
        circuitDpsiServer.getRpc().synchronize();
        circuitDpsiServer.getRpc().reset();
        LOGGER.info("{} finish", circuitDpsiServer.ownParty().getPartyName());
    }

    public void runClient(Rpc clientRpc, Party serverParty) throws MpcAbortException, IOException {
        // 读取协议参数
        LOGGER.info("{} read settings", clientRpc.ownParty().getPartyName());
        // 读取元素字节长度
        int elementByteLength = PropertiesUtils.readInt(properties, "element_byte_length");
        // 读取集合大小
        int[] serverLogSetSizes = PropertiesUtils.readLogIntArray(properties, "server_log_set_size");
        int[] clientLogSetSizes = PropertiesUtils.readLogIntArray(properties, "client_log_set_size");
        Preconditions.checkArgument(
                serverLogSetSizes.length == clientLogSetSizes.length,
                "# of server log_set_size = %s, $ of client log_set_size = %s, they must be equal",
                serverLogSetSizes.length, clientLogSetSizes.length
        );
        int setSizeNum = serverLogSetSizes.length;
        int[] serverSetSizes = Arrays.stream(serverLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        int[] clientSetSizes = Arrays.stream(clientLogSetSizes).map(logSetSize -> 1 << logSetSize).toArray();
        // 读取特殊参数
        LOGGER.info("{} read PTO config", clientRpc.ownParty().getPartyName());
        CcpsiDpsiConfig config = CircuitDpsiConfigUtils.createCircuitDpsiConfig(properties);
        // 生成输入文件
        LOGGER.info("{} generate warm-up element files", clientRpc.ownParty().getPartyName());
        PsoUtils.generateBytesInputFiles(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        LOGGER.info("{} generate element files", clientRpc.ownParty().getPartyName());
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            PsoUtils.generateBytesInputFiles(serverSetSizes[setSizeIndex], clientSetSizes[setSizeIndex], elementByteLength);
        }
        // 创建统计结果文件
        LOGGER.info("{} create result file", clientRpc.ownParty().getPartyName());
        String filePath = PTO_TYPE_NAME
                + "_" + config.getPtoType().name()
                + "_" + PropertiesUtils.readString(properties, "circuit_psi_pto_name")
                + PropertiesUtils.readString(properties, "append_string", "")
                + "_" + elementByteLength * Byte.SIZE
                + "_" + clientRpc.ownParty().getPartyId()
                + "_" + ForkJoinPool.getCommonPoolParallelism()
                + " " + PropertiesUtils.readDouble(properties, "epsilon")
                + ".output";
        FileWriter fileWriter = new FileWriter(filePath);
        PrintWriter printWriter = new PrintWriter(fileWriter, true);
        // 写入统计结果头文件
        String tab = "Party ID\tServer Set Size\tClient Set Size\tIs Parallel\tThread Num"
                + "\tInit Time(ms)\tInit DataPacket Num\tInit Payload Bytes(B)\tInit Send Bytes(B)"
                + "\tPto  Time(ms)\tPto  DataPacket Num\tPto  Payload Bytes(B)\tPto  Send Bytes(B)"
                + "\tPrivacy Budget\tFalse Positive Rate\tFalse Negative Rate";
        printWriter.println(tab);
        LOGGER.info("{} ready for run", clientRpc.ownParty().getPartyName());
        // 建立连接
        clientRpc.connect();
        // 启动测试
        int taskId = 0;
        // 预热
        warmupClient(clientRpc, serverParty, config, taskId);
        taskId++;
        for (int setSizeIndex = 0; setSizeIndex < setSizeNum; setSizeIndex++) {
            int serverSetSize = serverSetSizes[setSizeIndex];
            int clientSetSize = clientSetSizes[setSizeIndex];
            // 读取输入文件
            Set<ByteBuffer> clientElementSet = readClientElementSet(clientSetSize, elementByteLength);
            Set<ByteBuffer> serverElementSet = readServerElementSet(serverSetSize, elementByteLength);
            // 多线程
            runClient(clientRpc, serverParty, config, taskId, true, clientElementSet, serverElementSet, serverSetSize,
                    printWriter);
            taskId++;
            // 单线程
            runClient(clientRpc, serverParty, config, taskId, false, clientElementSet, serverElementSet, serverSetSize,
                    printWriter);
            taskId++;
        }
        // 断开连接
        clientRpc.disconnect();
        printWriter.close();
        fileWriter.close();
    }

    private Set<ByteBuffer> readClientElementSet(int setSize, int elementByteLength) throws IOException {
        LOGGER.info("Client read element set");
        InputStreamReader inputStreamReader = new InputStreamReader(
                Files.newInputStream(Paths.get(PsoUtils.getBytesFileName(PsoUtils.BYTES_CLIENT_PREFIX, setSize, elementByteLength))),
                CommonConstants.DEFAULT_CHARSET
        );
        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
        Set<ByteBuffer> clientElementSet = bufferedReader.lines()
                .map(Hex::decode)
                .map(ByteBuffer::wrap)
                .collect(Collectors.toSet());
        bufferedReader.close();
        inputStreamReader.close();
        return clientElementSet;
    }

    private void warmupClient(Rpc clientRpc, Party serverParty, CcpsiDpsiConfig config, int taskId) throws MpcAbortException, IOException {
        Set<ByteBuffer> clientElementSet = readClientElementSet(WARMUP_SET_SIZE, WARMUP_ELEMENT_BYTE_LENGTH);
        CcpsiDpsiClient<ByteBuffer> circuitDpsiClient = CcpsiDpsiFactory.createClient(clientRpc, serverParty, config);
        circuitDpsiClient.setTaskId(taskId);
        circuitDpsiClient.setParallel(false);
        circuitDpsiClient.getRpc().synchronize();
        // 初始化协议
        LOGGER.info("(warmup) {} init", circuitDpsiClient.ownParty().getPartyName());
        circuitDpsiClient.init(WARMUP_SET_SIZE, WARMUP_SET_SIZE);
        circuitDpsiClient.getRpc().synchronize();
        // 执行协议
        LOGGER.info("(warmup) {} execute", circuitDpsiClient.ownParty().getPartyName());
        circuitDpsiClient.dpsi(clientElementSet, WARMUP_SET_SIZE);
        // 同步并等待5秒钟，保证对方执行完毕
        circuitDpsiClient.getRpc().synchronize();
        circuitDpsiClient.getRpc().reset();
        LOGGER.info("(warmup) {} finish", circuitDpsiClient.ownParty().getPartyName());
    }

    public void runClient(Rpc clientRpc, Party serverParty, CcpsiDpsiConfig config, int taskId, boolean parallel,
                          Set<ByteBuffer> clientElementSet, Set<ByteBuffer> serverElementSet, int serverSetSize,
                          PrintWriter printWriter) throws MpcAbortException {
        int clientSetSize = clientElementSet.size();
        LOGGER.info(
                "{}: serverSetSize = {}, clientSetSize = {}, parallel = {}",
                clientRpc.ownParty().getPartyName(), serverSetSize, clientSetSize, parallel
        );
        CcpsiDpsiClient<ByteBuffer> circuitDpsiClient = CcpsiDpsiFactory.createClient(clientRpc, serverParty, config);
        circuitDpsiClient.setTaskId(taskId);
        circuitDpsiClient.setParallel(parallel);
        // 启动测试
        circuitDpsiClient.getRpc().synchronize();
        circuitDpsiClient.getRpc().reset();
        // 初始化协议
        LOGGER.info("{} init", circuitDpsiClient.ownParty().getPartyName());
        clientStopWatch.start();
        circuitDpsiClient.init(clientSetSize, serverSetSize);
        clientStopWatch.stop();
        long initTime = clientStopWatch.getTime(TimeUnit.MILLISECONDS);
        clientStopWatch.reset();
        long initDataPacketNum = circuitDpsiClient.getRpc().getSendDataPacketNum();
        long initPayloadByteLength = circuitDpsiClient.getRpc().getPayloadByteLength();
        long initSendByteLength = circuitDpsiClient.getRpc().getSendByteLength();
        circuitDpsiClient.getRpc().synchronize();
        circuitDpsiClient.getRpc().reset();
        // 执行协议
        LOGGER.info("{} execute", circuitDpsiClient.ownParty().getPartyName());
        clientStopWatch.start();
        Set<ByteBuffer> intersectionSet=circuitDpsiClient.dpsi(clientElementSet, serverSetSize);
        double[] results=assertOutput(serverElementSet, clientElementSet, intersectionSet);

        clientStopWatch.stop();
        long ptoTime = clientStopWatch.getTime(TimeUnit.MILLISECONDS);
        clientStopWatch.reset();
        long ptoDataPacketNum = circuitDpsiClient.getRpc().getSendDataPacketNum();
        long ptoPayloadByteLength = circuitDpsiClient.getRpc().getPayloadByteLength();
        long ptoSendByteLength = circuitDpsiClient.getRpc().getSendByteLength();
        // 写入统计结果
        String info = circuitDpsiClient.ownParty().getPartyId()
                + "\t" + clientSetSize
                + "\t" + serverSetSize
                + "\t" + circuitDpsiClient.getParallel()
                + "\t" + ForkJoinPool.getCommonPoolParallelism()
                + "\t" + initTime + "\t" + initDataPacketNum + "\t" + initPayloadByteLength + "\t" + initSendByteLength
                + "\t" + ptoTime + "\t" + ptoDataPacketNum + "\t" + ptoPayloadByteLength + "\t" + ptoSendByteLength
                + "\t" + PropertiesUtils.readDouble(properties, "epsilon") + "\t" + results[0] + "\t" + results[1];
        printWriter.println(info);
        circuitDpsiClient.getRpc().synchronize();
        circuitDpsiClient.getRpc().reset();
        LOGGER.info("{} finish", circuitDpsiClient.ownParty().getPartyName());
    }

    private double[] assertOutput(Set<ByteBuffer> serverSet, Set<ByteBuffer> clientSet, Set<ByteBuffer> outputIntersectionSet) {
        double TP=0;
        double FP=0;
        double TN=0;
        double FN=0;
        Set<ByteBuffer> expectIntersectionSet = new HashSet<>(serverSet);
        expectIntersectionSet.retainAll(clientSet);
        for(ByteBuffer element: clientSet) {
            if ((outputIntersectionSet.contains(element)) && (expectIntersectionSet.contains(element))) {
                TP=TP+1.0;
            } else if (!(outputIntersectionSet.contains(element)) && (expectIntersectionSet.contains(element))) {
                FN=FN+1.0;
            } else if ((outputIntersectionSet.contains(element)) && !(expectIntersectionSet.contains(element))) {
                FP=FP+1.0;
            } else if (!(outputIntersectionSet.contains(element)) && !(expectIntersectionSet.contains(element))) {
                TN=TN+1.0;
            }
        }
        double FPR=FP/(FP+TN);
        double FNR=FN/(FN+TP);
        return new double[] {FPR,FNR};
    }
}
