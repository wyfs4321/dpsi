package dpsi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.Party;
import edu.alibaba.mpc4j.common.rpc.Rpc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractTwoPartyPto;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
/**
 * Abstract server for the mqRPMT-DPSI protocols
 *
 * @author anonymous authors
 * @date 2023/9/19
 */
public abstract class AbstractMqRpmtDpsiServer<T> extends AbstractTwoPartyPto implements MqRpmtDpsiServer<T> {
    /**
     * 服务端最大元素数量
     */
    private int maxServerElementSize;
    /**
     * 客户端最大元素数量
     */
    private int maxClientElementSize;
    /**
     * 服务端元素数组
     */
    protected ArrayList<T> serverElementArrayList;
    /**
     * 服务端元素数量
     */
    protected int serverElementSize;
    /**
     * 服务端填充元素数组
     */
    protected HashMap<ByteBuffer, T> dummyServerElementMap;
    /**
     * 服务端填充元素数量
     */
    protected int dummyServerElementSize;
    /**
     * 客户端填充元素数量
     */
    protected int dummyClientElementSize;

    protected AbstractMqRpmtDpsiServer(PtoDesc ptoDesc, Rpc serverRpc, Party clientParty, MqRpmtDpsiConfig config) {
        super(ptoDesc, serverRpc, clientParty, config);
    }

    protected void setInitInput(int maxServerElementSize, int maxClientElementSize) {
        MathPreconditions.checkPositive("maxServerElementSize", maxServerElementSize);
        this.maxServerElementSize = maxServerElementSize;
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        initState();
    }


    protected void setServerInfo(Set<T> serverElementSet, HashMap<ByteBuffer, T> dummyElementMap) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("serverElementSize", serverElementSet.size(), maxServerElementSize);
        serverElementSize = serverElementSet.size();
        serverElementArrayList = new ArrayList<>(serverElementSet);
        MathPreconditions.checkPositiveInRangeClosed("dummyServerElementSize", dummyElementMap.size(), maxServerElementSize);
        dummyServerElementSize =  dummyElementMap.size();
        dummyServerElementMap = new HashMap<>( dummyElementMap);
        extraInfo++;
    }
    protected void setdummyClientElementSize(int dummyElementSize) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("dummyClientElementSize", dummyElementSize, maxClientElementSize);
        dummyClientElementSize = dummyElementSize;
        //extraInfo++;
    }
}