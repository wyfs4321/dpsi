package dpsi.main.mqrpmt;

import dpsi.mqrpmt.MqRpmtDpsiFactory;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.RpcManager;
import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.impl.memory.MemoryRpcManager;
import edu.alibaba.mpc4j.common.rpc.test.AbstractTwoPartyPtoTest;
import edu.alibaba.mpc4j.common.tool.utils.PropertiesUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;

/**
 * mqRPMT-DPSI main tests.
 *
 * @author anonymous authors
 * @date 2023/10/09
 */
@RunWith(Parameterized.class)
public class MainMqRpmtDpsiTest extends AbstractTwoPartyPtoTest {
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

        // CZZ22
        configurations.add(new Object[]{
                MqRpmtDpsiFactory.MqRpmtDpsiType.CZZ22.name() + "(" + SecurityModel.SEMI_HONEST + ")", "mqrpmt/czz22_semi_honest.txt",
        });

        return configurations;
    }

    /**
     * file name
     */
    private final String filePath;

    public MainMqRpmtDpsiTest(String name, String filePath) {
        super(name);
        RpcManager rpcManager = new MemoryRpcManager(2);
        firstRpc = rpcManager.getRpc(0);
        secondRpc = rpcManager.getRpc(1);
        this.filePath = filePath;
    }

    @Test
    public void testMqRpmtDpsi() throws InterruptedException {
        Properties properties = readConfig(filePath);
        runTest(new MqRpmtDpsiMain(properties));
    }

    private void runTest(MqRpmtDpsiMain mqRpmtDpsiMain) throws InterruptedException {
        MainMqRpmtDpsiServerThread serverThread = new MainMqRpmtDpsiServerThread(firstRpc, secondRpc.ownParty(), mqRpmtDpsiMain);
        MainMqRpmtDpsiClientThread clientThread = new MainMqRpmtDpsiClientThread(secondRpc, firstRpc.ownParty(), mqRpmtDpsiMain);
        serverThread.start();
        Thread.sleep(1000);
        clientThread.start();
        serverThread.join();
        clientThread.join();
        Assert.assertTrue(serverThread.getSuccess());
        Assert.assertTrue(clientThread.getSuccess());
    }

    private Properties readConfig(String path) {
        String configPath = Objects.requireNonNull(MainMqRpmtDpsiTest.class.getClassLoader().getResource(path)).getPath();
        return PropertiesUtils.loadProperties(configPath);
    }
}
