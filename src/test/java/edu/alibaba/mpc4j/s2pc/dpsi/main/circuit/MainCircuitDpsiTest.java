package edu.alibaba.mpc4j.s2pc.dpsi.main.circuit;

import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

/**
 * circuit-DPSI main tests.
 *
 * @author Yufei Wang
 * @date 2023/10/08
 */
@RunWith(Parameterized.class)
public class MainCircuitDpsiTest extends AbstractTwoPartyPtoTest {
    /**
     * sender RPC
     */
    protected final Rpc firstRpc;
    /**
     * receiver RPC
     */
    protected final Rpc secondRpc;

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> configurations() {
        Collection<Object[]> configurations = new ArrayList<>();

        // PSTY19
        configurations.add(new Object[]{
                CcpsiFactory.CcpsiType.PSTY19.name() + "(" + SecurityModel.SEMI_HONEST + ")", "circuit/psty19_semi_honest.txt",
        });
        // RS21
        configurations.add(new Object[]{
                CcpsiFactory.CcpsiType.RS21.name() + "(" + SecurityModel.SEMI_HONEST + ")", "circuit/rs21_semi_honest.txt",
        });
        // CGS22
        configurations.add(new Object[]{
                CcpsiFactory.CcpsiType.CGS22.name() + "(" + SecurityModel.SEMI_HONEST + ")", "circuit/cgs22_semi_honest.txt",
        });

        return configurations;
    }

    /**
     * file name
     */
    private final String filePath;

    public MainCircuitDpsiTest(String name, String filePath) {
        super(name);
        RpcManager rpcManager = new MemoryRpcManager(2);
        firstRpc = rpcManager.getRpc(0);
        secondRpc = rpcManager.getRpc(1);
        this.filePath = filePath;
    }

    @Test
    public void testCircuitDpsi() throws InterruptedException {
        Properties properties = readConfig(filePath);
        runTest(new CircuitDpsiMain(properties));
    }

    private void runTest(CircuitDpsiMain circuitDpsiMain) throws InterruptedException {
        MainCircuitDpsiServerThread serverThread = new MainCircuitDpsiServerThread(firstRpc, secondRpc.ownParty(), circuitDpsiMain);
        MainCircuitDpsiClientThread clientThread = new MainCircuitDpsiClientThread(secondRpc, firstRpc.ownParty(), circuitDpsiMain);
        serverThread.start();
        Thread.sleep(1000);
        clientThread.start();
        serverThread.join();
        clientThread.join();
        Assert.assertTrue(serverThread.getSuccess());
        Assert.assertTrue(clientThread.getSuccess());
    }

    private Properties readConfig(String path) {
        String configPath = Objects.requireNonNull(MainCircuitDpsiTest.class.getClassLoader().getResource(path)).getPath();
        return PropertiesUtils.loadProperties(configPath);
    }
}
