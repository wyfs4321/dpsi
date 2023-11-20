package dpsi.mqrpmt;

import dpsi.mqrpmt.czz22.Czz22MqRpmtDpsiConfig;
import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * mqRPMT-DPSI协议测试。
 *
 * @author anonymous authors
 * @date 2023/9/19
 */
@RunWith(Parameterized.class)
public class MqRpmtDpsiTest extends AbstractTwoPartyPtoTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DpsiTest.class);
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
    private static final int LARGE_SIZE = 1 << 10;
    /**
     * privacy budget
     */
    private static final double epsilon=1.0;

    private double relative_error;
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // CZZ22
        configurations.add(new Object[] {
                MqRpmtDpsiFactory.MqRpmtDpsiType.CZZ22.name() + " (no-stash)",
                new Czz22MqRpmtDpsiConfig.Builder(epsilon).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final MqRpmtDpsiConfig config;

    public MqRpmtDpsiTest(String name, MqRpmtDpsiConfig config) {
        super(name);
        this.config = config;
    }

    @Test
    public void testARE() {
        ArrayList<Double> ARE=new ArrayList<>();
        double RE;
        for (int i=0;i<100;i++) {
            RE=testRE();
//            testPto(1,1,false);
            ARE.add(RE);
        }
        double sum = 0;
        for (int k=0;k<ARE.size();k++){
            sum+=ARE.get(k);
        }
        double avg = sum/ARE.size();
        double variance = 0;
        for (int k=0;k<ARE.size();k++){
            variance +=(ARE.get(k)-avg)*(ARE.get(k)-avg);
        }
        double f=variance/ARE.size();

        LOGGER.info("-----Average Relative Error = {}, The variance of Relative Error = {}-----",
                avg, f
        );
    }
    @Test
    public void test1() {
        testPto(1, 1, false);
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
    public void testLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, false);
    }

    @Test
    public void testParallelLarge() {
        testPto(LARGE_SIZE, LARGE_SIZE, true);
    }

    private double testRE() {
        int serverSetSize=LARGE_SIZE;
        int clientSetSize=LARGE_SIZE;
        boolean parallel=false;
        MqRpmtDpsiServer<ByteBuffer> server = MqRpmtDpsiFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        MqRpmtDpsiClient<ByteBuffer> client = MqRpmtDpsiFactory.createClient(secondRpc, firstRpc.ownParty(), config);
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
            MqRpmtDpsiServerThread serverThread = new MqRpmtDpsiServerThread(server, serverSet, clientSet.size());
            MqRpmtDpsiClientThread clientThread = new MqRpmtDpsiClientThread(client, clientSet, serverSet.size());
            StopWatch stopWatch = new StopWatch();
            // start
            stopWatch.start();
            serverThread.start();
            clientThread.start();
            // stop
            serverThread.join();
            clientThread.join();
            stopWatch.stop();
            stopWatch.reset();
            // verify
            relative_error=assertPsica(serverSet, clientSet, clientThread.getIntersectionSet());
            LOGGER.info("-----Relative Error = {}-----",
                    relative_error
            );
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return relative_error;
    }

    private void testPto(int serverSetSize, int clientSetSize, boolean parallel) {
        MqRpmtDpsiServer<ByteBuffer> server = MqRpmtDpsiFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        MqRpmtDpsiClient<ByteBuffer> client = MqRpmtDpsiFactory.createClient(secondRpc, firstRpc.ownParty(), config);
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
            MqRpmtDpsiServerThread serverThread = new MqRpmtDpsiServerThread(server, serverSet, clientSet.size());
            MqRpmtDpsiClientThread clientThread = new MqRpmtDpsiClientThread(client, clientSet, serverSet.size());
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
            double[] results=assertOutput(serverSet, clientSet, clientThread.getIntersectionSet());
            printAndResetRpc(time);
            LOGGER.info("-----FPR = {}, FNR = {}-----",
                    results[0], results[1]
            );
            double relative_error=assertPsica(serverSet, clientSet, clientThread.getIntersectionSet());
            LOGGER.info("-----Relative Error = {}-----",
                    relative_error
            );
            // destroy
            new Thread(server::destroy).start();
            new Thread(client::destroy).start();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
    private double assertPsica(Set<ByteBuffer> serverSet, Set<ByteBuffer> clientSet, Set<ByteBuffer> outputIntersectionSet) {
        Set<ByteBuffer> expectIntersectionSet = new HashSet<>(serverSet);
        expectIntersectionSet.retainAll(clientSet);
        MathPreconditions.checkPositive("expectPsica", expectIntersectionSet.size());
        return (double)Math.abs(expectIntersectionSet.size()-outputIntersectionSet.size())/expectIntersectionSet.size();
    }
    }

