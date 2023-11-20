package edu.alibaba.mpc4j.s2pc.dpsi;

import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.s2pc.dpsi.pid.PidDpsiClient;
import edu.alibaba.mpc4j.s2pc.dpsi.pid.PidDpsiConfig;
import edu.alibaba.mpc4j.s2pc.dpsi.pid.PidDpsiFactory;
import edu.alibaba.mpc4j.s2pc.dpsi.pid.PidDpsiServer;
import edu.alibaba.mpc4j.s2pc.pso.PsoUtils;
import edu.alibaba.mpc4j.s2pc.dpsi.pid.dpsi23.Dpsi23Config;
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
 * test for pid-DPSI
 *
 * @author Yufei Wang
 * @date 2023/8/8
 */
@RunWith(Parameterized.class)
public class DpsiTest extends AbstractTwoPartyPtoTest {
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
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // DPSI23
        configurations.add(new Object[] {
                PidDpsiFactory.PidDpsiType.DPSI23.name() + " (no-stash)",
                new Dpsi23Config.Builder(epsilon).build(),
        });

        return configurations;
    }

    /**
     * config
     */
    private final PidDpsiConfig config;

    public DpsiTest(String name, PidDpsiConfig config) {
        super(name);
        this.config = config;
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

    private void testPto(int serverSetSize, int clientSetSize, boolean parallel) {
        PidDpsiServer<ByteBuffer> server = PidDpsiFactory.createServer(firstRpc, secondRpc.ownParty(), config);
        PidDpsiClient<ByteBuffer> client = PidDpsiFactory.createClient(secondRpc, firstRpc.ownParty(), config);
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
            DpsiServerThread serverThread = new DpsiServerThread(server, serverSet, clientSet.size());
            DpsiClientThread clientThread = new DpsiClientThread(client, clientSet, serverSet.size());
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
        return new double[]{FPR,FNR};
    }
}
