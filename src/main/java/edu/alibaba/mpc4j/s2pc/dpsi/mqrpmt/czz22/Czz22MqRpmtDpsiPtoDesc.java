package edu.alibaba.mpc4j.s2pc.dpsi.mqrpmt.czz22;

import edu.alibaba.mpc4j.common.rpc.desc.PtoDesc;
import edu.alibaba.mpc4j.common.rpc.desc.PtoDescManager;


/**
 * Mqrpmt-Dpsi协议信息。
 *  *此协议首先要求双方在各自数据集中加入随机个数的填充项，其次利用Czz22的rqRpmt方案使Server获得向量e并对向量进行扰动，
 *  最后双方基于扰动向量e计算PSI

 *  *
 *  * @author Yufei Wang
 *  * @date 2023/9/18
 *  */
public class Czz22MqRpmtDpsiPtoDesc implements PtoDesc {
    /**
     * 协议ID
     */
    private static final int PTO_ID = Math.abs((int)-731131751523952969L);
    /**
     * 协议名称
     */
    private static final String PTO_NAME = "CZZ22_DPSI";

    /**
     * 协议步骤
     */
    enum PtoStep {
        /**
         * 服务端发送集合规模
         */
        SERVER_SEND_ELEMENTS_SIZE,
        /**
         * 客户端发送集合规模
         */
        CLIENT_SEND_ELEMENTS_SIZE,
        /**
         * 服务端发送向量
         */
        SERVER_SEND_VECTOR
    }

    /**
     * 单例模式
     */
    private static final edu.alibaba.mpc4j.s2pc.dpsi.mqrpmt.czz22.Czz22MqRpmtDpsiPtoDesc INSTANCE = new edu.alibaba.mpc4j.s2pc.dpsi.mqrpmt.czz22.Czz22MqRpmtDpsiPtoDesc();

    /**
     * 私有构造函数
     */
    private Czz22MqRpmtDpsiPtoDesc() {
        // empty
    }

    public static PtoDesc getInstance() {
        return INSTANCE;
    }

    static {
        PtoDescManager.registerPtoDesc(getInstance());
    }

    @Override
    public int getPtoId() {
        return PTO_ID;
    }

    @Override
    public String getPtoName() {
        return PTO_NAME;
    }
}
