package edu.alibaba.mpc4j.s2pc.dpsi.mqrpmt;

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
 * Abstract client for the mqRPMT-DPSI protocols
 *
 * @author Yufei Wang
 * @date 2023/9/19
 */
public abstract class AbstractMqRpmtDpsiClient<T> extends AbstractTwoPartyPto implements MqRpmtDpsiClient<T> {
    /**
     * 客户端最大元素数量
     */
    private int maxClientElementSize;
    /**
     * 服务端最大元素数量
     */
    private int maxServerElementSize;
    /**
     * 客户端元素集合
     */
    protected ArrayList<T> clientElementArrayList;
    /**
     * 客户端元素数量
     */
    protected int clientElementSize;
    /**
     * 客户端填充元素数组
     */
    protected HashMap<ByteBuffer, T> dummyClientElementMap;
    /**
     * 服务端填充元素数量
     */
    protected int dummyServerElementSize;
    /**
     * 客户端填充元素数量
     */
    protected int dummyClientElementSize;

    protected AbstractMqRpmtDpsiClient(PtoDesc ptoDesc, Rpc clientRpc, Party serverParty, MqRpmtDpsiConfig config) {
        super(ptoDesc, clientRpc, serverParty, config);
    }

    protected void setInitInput(int maxClientElementSize, int maxServerElementSize) {
        MathPreconditions.checkPositive("maxClientElementSize", maxClientElementSize);
        this.maxClientElementSize = maxClientElementSize;
        MathPreconditions.checkPositive("maxServerElementSize", maxServerElementSize);
        this.maxServerElementSize = maxServerElementSize;
        initState();
    }


    protected void setClientInfo(Set<T> clientElementSet, HashMap<ByteBuffer, T> dummyElementMap) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("clientElementSize", clientElementSet.size(), maxClientElementSize);
        clientElementSize = clientElementSet.size();
        clientElementArrayList = new ArrayList<>(clientElementSet);
        MathPreconditions.checkPositiveInRangeClosed("dummyClientElementSize", dummyElementMap.size(), maxClientElementSize);
        dummyClientElementSize =  dummyElementMap.size();
        dummyClientElementMap = new HashMap<>(dummyElementMap);
        extraInfo++;
    }

    protected void setdummyServerElementSize(int dummyElementSize) {
        checkInitialized();
        MathPreconditions.checkPositiveInRangeClosed("dummyServerElementSize", dummyElementSize, maxServerElementSize);
        dummyServerElementSize=dummyElementSize;
        //extraInfo++;
    }
}