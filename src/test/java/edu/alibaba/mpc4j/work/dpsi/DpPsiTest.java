package edu.alibaba.mpc4j.work.dpsi;

import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyMemoryRpcPto;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.utils.BytesUtils;
import edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound.ApacheLaplaceCdpConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound.UnboundRealCdp;
import edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound.UnboundRealCdpConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.real.unbound.UnboundRealCdpFactory;
import edu.alibaba.mpc4j.work.dpsi.DpsiFactory.DpPsiType;
import edu.alibaba.mpc4j.work.dpsi.ccpsi.CcpsiDpsiConfig;
import edu.alibaba.mpc4j.work.dpsi.mqrpmt.MqRpmtDpsiConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory.MqRpmtType;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.czz24.Czz24CwOprfMqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.gmr21.Gmr21MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory.CcpsiType;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.cgs22.Cgs22CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.psty19.Psty19CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.rs21.Rs21CcpsiConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * DP-PSI test.
 */
@RunWith(Parameterized.class)
public class DpPsiTest extends AbstractTwoPartyMemoryRpcPto {
    private static final Logger LOGGER = LoggerFactory.getLogger(DpPsiTest.class);
    /**
     * default size
     */
    private static final int DEFAULT_SIZE = 99;
    /**
     * element byte length
     */
    private static final int ELEMENT_BYTE_LENGTH = CommonConstants.BLOCK_BYTE_LENGTH;
    /**
     * large size
     */
    private static final int LARGE_SIZE = 1 << 8;
    /**
     * privacy budget
     */
    private static final double EPSILON = 2.0;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // MQ_RPMT_BASED (CZZ24_CW_OPRF)
        configurations.add(new Object[]{
            DpPsiType.MQ_RPMT_BASED.name() + " (" + MqRpmtType.CZZ24_CW_OPRF.name() + ")",
            new MqRpmtDpsiConfig.Builder(EPSILON, EPSILON / 2, EPSILON / 2)
                .setMqRpmtConfig(new Czz24CwOprfMqRpmtConfig.Builder().build())
                .build(),
        });
//        // MQ_RPMT_BASED (GMR21)
//        configurations.add(new Object[]{
//            DpPsiType.MQ_RPMT_BASED.name() + " (" + MqRpmtType.GMR21.name() + ")",
//            new MqRpmtDpsiConfig.Builder(EPSILON, EPSILON / 2, EPSILON / 2)
//                .setMqRpmtConfig(new Gmr21MqRpmtConfig.Builder().build())
//                .build(),
//        });
//        // CCPSI_BASED (CGS22)
//        configurations.add(new Object[]{
//            DpPsiType.CCPSI_BASED.name() + " (" + CcpsiType.CGS22.name() + ")",
//            new CcpsiDpsiConfig.Builder(EPSILON)
//                .setCcpsiConfig(new Cgs22CcpsiConfig.Builder(true).build())
//                .build(),
//        });
//        // CCPSI_BASED (PSTY19)
//        configurations.add(new Object[]{
//            DpPsiType.CCPSI_BASED.name() + " (" + CcpsiType.PSTY19.name() + ")",
//            new CcpsiDpsiConfig.Builder(EPSILON)
//                .setCcpsiConfig(new Psty19CcpsiConfig.Builder(true).build())
//                .build(),
//        });
//        // CCPSI_BASED (RS21)
//        configurations.add(new Object[]{
//            DpPsiType.CCPSI_BASED.name() + " (" + CcpsiType.RS21.name() + ")",
//            new CcpsiDpsiConfig.Builder(EPSILON)
//                .setCcpsiConfig(new Rs21CcpsiConfig.Builder(true).build())
//                .build(),
//        });

        return configurations;
    }

    /**
     * config
     */
    private final DpsiConfig config;

    public DpPsiTest(String name, DpsiConfig config) {
        super(name);
        this.config = config;
    }
    @Test
    public void testLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, false);
    }

    @Test
    public void test2() {
        testPto(2, 2, false);
    }

    @Test
    public void test10() {
        testPto(10, 10, false);
    }

    @Test
    public void testLargeServerSize() {
        testPto(DEFAULT_SIZE, 10, false);
    }

    @Test
    public void testLargeClientSize() {
        testPto(10, DEFAULT_SIZE, false);
    }

    @Test
    public void testDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, false);
    }

    @Test
    public void testParallelDefault() {
        testPto(DEFAULT_SIZE, DEFAULT_SIZE, true);
    }



    @Test
    public void testParallelLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, true);
    }

    @Test
    public void testPsiSum() {
        testPsisumPto(LARGE_SIZE, LARGE_SIZE, true);
    }

    private void testPto(int serverSetSize, int clientSetSize, boolean parallel) {
        DpsiServer<ByteBuffer> server = DpsiFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        DpsiClient<ByteBuffer> client = DpsiFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----test {}，server_size = {}，client_size = {}-----",
                server.getPtoDesc().getPtoName(), serverSetSize, clientSetSize
            );
            // generate sets
            ArrayList<Set<ByteBuffer>> sets = PsoUtils.generateBytesSets(serverSetSize, clientSetSize, ELEMENT_BYTE_LENGTH);
            Set<ByteBuffer> serverSet = sets.get(0);
            Set<ByteBuffer> clientSet = sets.get(1);
            DpPsiServerThread serverThread = new DpPsiServerThread(server, serverSet, clientSet.size());
            DpPsiClientThread clientThread = new DpPsiClientThread(client, clientSet, serverSet.size());
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            serverThread.start();
            clientThread.start();
            // stop
            serverThread.join();
            clientThread.join();
            stopWatch.stop();
            long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
            stopWatch.reset();
            // verify
            assertOutput(serverSet, clientSet, clientThread.getClientOutput());
            printAndResetRpc(time);
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void testPsisumPto(int serverSetSize, int clientSetSize, boolean parallel) {
        DpsiServer<ByteBuffer> server = DpsiFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        DpsiClient<ByteBuffer> client = DpsiFactory.createClient(secondRpc, firstRpc.ownParty(), config);
        server.setParallel(parallel);
        client.setParallel(parallel);
        int randomTaskId = Math.abs(SECURE_RANDOM.nextInt());
        server.setTaskId(randomTaskId);
        client.setTaskId(randomTaskId);
        try {
            LOGGER.info("-----PSI Sum test {}，server_size = {}，client_size = {}-----",
                    server.getPtoDesc().getPtoName(), serverSetSize, clientSetSize
            );
            // generate sets
            ArrayList<DataContainer<ByteBuffer, Double>> containers = loadBytesSets(serverSetSize, clientSetSize, ELEMENT_BYTE_LENGTH);
            Set<ByteBuffer> serverSet = containers.get(0).getSet();
            Set<ByteBuffer> clientSet = containers.get(1).getSet();

            ArrayList<Double> errorList = new ArrayList<Double>(10);
            ArrayList<Double> baselineErrorList = new ArrayList<Double>(10);

            for (int i=0; i<5; i++){
                DpPsiServerThread serverThread = new DpPsiServerThread(server, serverSet, clientSet.size());
                DpPsiClientThread clientThread = new DpPsiClientThread(client, clientSet, serverSet.size());
                StopWatch stopWatch = new StopWatch();
                // start
                stopWatch.start();
                serverThread.start();
                clientThread.start();
                // stop
                serverThread.join();
                clientThread.join();
                stopWatch.stop();
                long time = stopWatch.getTime(TimeUnit.MILLISECONDS);
                stopWatch.reset();
                // verify
                ArrayList<Double> resultList = new ArrayList<Double>(3);
                resultList = assertOutputSum(containers.get(0), containers.get(1), clientThread.getClientOutput());
                errorList.add(Math.abs(resultList.get(1)-resultList.get(0))/ resultList.get(0));
                baselineErrorList.add(Math.abs(resultList.get(2)-resultList.get(0))/ resultList.get(0));
                printAndResetRpc(time);
                // destroy
//                new Thread(server::destroy).start();
//                new Thread(client::destroy).start();
            }

            LOGGER.info("actualError = {}, baselineError = {}",  calculateMean(errorList), calculateMean(baselineErrorList));
            LOGGER.info("actualStdError = {}, baselineStdError = {}",  calculateStandardDeviation(errorList), calculateStandardDeviation(baselineErrorList));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static double calculateMean(ArrayList<Double> numbers) {
        double mean = 0;
        for (double number : numbers) {
            mean += number;
        }
        mean /= numbers.size();
        return mean;
    }
    public static double calculateStandardDeviation(ArrayList<Double> numbers) {
        double mean = 0;
        double sumOfSquaredDeviation = 0;

        // 计算平均值
        mean = calculateMean(numbers);

        // 计算每个数与平均值的差的平方
        for (double number : numbers) {
            sumOfSquaredDeviation += Math.pow(number - mean, 2);
        }

        // 计算标准差
        double variance = sumOfSquaredDeviation / numbers.size();
        return Math.sqrt(variance);
    }

    public class DataContainer<ByteBuffer, Double> {
        private Map<ByteBuffer, Double> map;
        private Set<ByteBuffer> set;

        public DataContainer(Map<ByteBuffer, Double>map, Set<ByteBuffer> set) {
            this.map = map;
            this.set = set;
        }

        public Map<ByteBuffer, Double> getMap() {
            return map;
        }
        public Set<ByteBuffer> getSet() {
            return set;
        }
    }
    /**
     * 生成参与方的测试集合。
     * lineitem.csv数据格式为ORDERKEY, PARTKEY, SUPPKEY, LINENUMBER, QUANTITY, EXTENDEDPRICE, DISCOUNT
     *
     * @param serverSize 服务端集合大小。
     * @param clientSize 客户端集合大小。
     * @param elementByteLength 元素字节长度。
     * @return 各个参与方的集合。
     */
    public ArrayList<DataContainer<ByteBuffer,Double>> loadBytesSets(int serverSize, int clientSize, int elementByteLength) {
        assert serverSize >= 1 : "server must have at least 2 elements";
        assert clientSize >= 1 : "client must have at least 2 elements";
        assert elementByteLength >= CommonConstants.STATS_BYTE_LENGTH;
        //打开TPC-H数据表
        String path = "/Users/yufei/Documents/code/git/tpch-kit/database/";
        String filename = "lineitem";
        try{
            // 放置各个参与方的映射,集合
            Map<ByteBuffer, Double> serverMap = new HashMap<ByteBuffer, Double>(serverSize);
            Map<ByteBuffer, Double> clientMap = new HashMap<ByteBuffer, Double>(clientSize);
            Set<ByteBuffer> serverSet = new HashSet<>(serverSize);
            Set<ByteBuffer> clientSet = new HashSet<>(clientSize);

            BufferedReader serverReader = new BufferedReader(new FileReader(path+filename+"_server.csv"));
            CSVParser serverCSVParser = new CSVParser(serverReader, CSVFormat.DEFAULT);
            for (CSVRecord record : serverCSVParser) {
                ByteBuffer serverByteBuffer = ByteBuffer.allocate(elementByteLength);
                serverByteBuffer.putInt(elementByteLength - Integer.BYTES * 2, Integer.parseInt(record.get(0)));
                serverByteBuffer.putInt(elementByteLength - Integer.BYTES, Integer.parseInt(record.get(3)));
                serverSet.add(serverByteBuffer);
                serverMap.put(serverByteBuffer,Double.parseDouble(record.get(6)));
            }
            serverCSVParser.close();
            serverReader.close();

            BufferedReader clientReader = new BufferedReader(new FileReader(path+filename+"_client.csv"));
            CSVParser clientCSVParser = new CSVParser(clientReader, CSVFormat.DEFAULT);
            for (CSVRecord record : clientCSVParser) {
                ByteBuffer clientByteBuffer = ByteBuffer.allocate(elementByteLength);
                clientByteBuffer.putInt(elementByteLength - Integer.BYTES * 2, Integer.parseInt(record.get(0)));
                clientByteBuffer.putInt(elementByteLength - Integer.BYTES, Integer.parseInt(record.get(3)));
                clientSet.add(clientByteBuffer);
                clientMap.put(clientByteBuffer,Double.parseDouble(record.get(6)));
            }
            clientCSVParser.close();
            clientReader.close();

            // 构建返回结果
            ArrayList<DataContainer<ByteBuffer, Double>> dataContainerArrayList = new ArrayList<>(2);
            DataContainer<ByteBuffer, Double> serverContainer = new DataContainer<ByteBuffer, Double>(serverMap, serverSet);
            dataContainerArrayList.add(serverContainer);
            DataContainer<ByteBuffer, Double> clientContainer = new DataContainer<ByteBuffer, Double>(clientMap, clientSet);
            dataContainerArrayList.add(clientContainer);

            return dataContainerArrayList;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ArrayList<Double> assertOutputSum(DataContainer<ByteBuffer, Double> serverContainer, DataContainer<ByteBuffer, Double> clientContainer, Set<ByteBuffer> actualIntersection) {
        Set<ByteBuffer> serverElementSet = serverContainer.getSet();
        Set<ByteBuffer> clientElementSet = clientContainer.getSet();
        Map<ByteBuffer, Double> serverElementValue = serverContainer.getMap();
        Map<ByteBuffer, Double> clientElementValue = clientContainer.getMap();

        Assert.assertTrue(actualIntersection.size() <= clientElementSet.size());
        Assert.assertTrue(clientElementSet.containsAll(actualIntersection));

        double sum = 0.0;
        for (ByteBuffer element : actualIntersection) {
//            sum = sum + clientElementValue.get(element);
            sum = sum + 1.0;
        }

        Set<ByteBuffer> expectIntersection = new HashSet<>(serverElementSet);
        expectIntersection.retainAll(clientElementSet);
        double expectSum = 0.0;
        for (ByteBuffer element : expectIntersection) {
//            expectSum = expectSum + clientElementValue.get(element);
            expectSum = expectSum + 1.0;
        }

        double baselineSum = expectSum;

        UnboundRealCdpConfig realCdpConfig = new ApacheLaplaceCdpConfig
                .Builder(EPSILON, 1.0)
                .setDelta(0.0)
                .build();
        UnboundRealCdp mechanism = UnboundRealCdpFactory.createInstance(realCdpConfig);
        baselineSum = mechanism.randomize(baselineSum);

        LOGGER.info("expectSum ={}, actualSum = {}, baselineSum = {}, size = {}", expectSum, sum, baselineSum, actualIntersection.size());
        ArrayList<Double> resultList = new ArrayList<Double>(3);
        resultList.add(expectSum);
        resultList.add(sum);
        resultList.add(baselineSum);
        return resultList;

    }
    private void assertOutput(Set<ByteBuffer> serverElementSet, Set<ByteBuffer> clientElementSet,
                                  Set<ByteBuffer> actualIntersection) {
        // it is hard to verify the result, we only know that the intersection should be the subset of client set.
        Assert.assertTrue(actualIntersection.size() <= clientElementSet.size());
        Assert.assertTrue(clientElementSet.containsAll(actualIntersection));
        // compute some measurements.
        int tp = 0;
        int fp = 0;
        int tn = 0;
        int fn = 0;
        Set<ByteBuffer> expectIntersection = new HashSet<>(serverElementSet);
        expectIntersection.retainAll(clientElementSet);
        for (ByteBuffer element : clientElementSet) {
            if ((actualIntersection.contains(element)) && (expectIntersection.contains(element))) {
                tp = tp + 1;
            } else if (!(actualIntersection.contains(element)) && (expectIntersection.contains(element))) {
                fn = fn + 1;
            } else if ((actualIntersection.contains(element)) && !(expectIntersection.contains(element))) {
                fp = fp + 1;
            } else if (!(actualIntersection.contains(element)) && !(expectIntersection.contains(element))) {
                tn = tn + 1;
            }
        }
        double fpr = (double) fp /(fp+tn);
        double fnr = (double) fn /(fn+tp);
        LOGGER.info("FNR = {}, FNR = {}", fpr, fnr);
    }
}
