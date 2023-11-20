package edu.alibaba.mpc4j.s2pc.dpsi.mqrpmt.czz22;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdpConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdpFactory;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdpFactory;
import edu.alibaba.mpc4j.s2pc.dpsi.mqrpmt.MqRpmtDpsiConfig;

import edu.alibaba.mpc4j.s2pc.dpsi.mqrpmt.MqRpmtDpsiFactory;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;


import java.util.ArrayList;
import java.util.Arrays;
/**
 * config for the mqRPMT-DPSI protocols
 *
 * @author Yufei Wang
 * @date 2023/9/19
 */
public class Czz22MqRpmtDpsiConfig extends AbstractMultiPartyPtoConfig implements MqRpmtDpsiConfig {
    /**
     * PID协议配置项
     */
    private final MqRpmtConfig mqRpmtConfig;
    /**
     * LDP协议配置项
     */
    private final EncodeLdpConfig ldpConfig;
    /**
     * DP协议配置项
     */
    private final BoundIntegralCdpConfig dpConfig;

    private Czz22MqRpmtDpsiConfig(Czz22MqRpmtDpsiConfig.Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.mqRpmtConfig);
        mqRpmtConfig = builder.mqRpmtConfig;
        ldpConfig = builder.ldpConfig;
        dpConfig = builder.dpConfig;
    }
    @Override
    public MqRpmtDpsiFactory.MqRpmtDpsiType getPtoType() {
        return MqRpmtDpsiFactory.MqRpmtDpsiType.CZZ22;
    }

    public MqRpmtConfig getMqRpmtConfig(){
        return mqRpmtConfig;
    }

    public EncodeLdpConfig getLdpConfig(){
        return ldpConfig;
    }

    public BoundIntegralCdpConfig getDpConfig(){
        return dpConfig;
    }
    public static class Builder implements org.apache.commons.lang3.builder.Builder<Czz22MqRpmtDpsiConfig> {
        /**
         * mqRPMT协议配置项
         */
        private final MqRpmtConfig mqRpmtConfig;
        /**
         * LDP协议配置项
         */
        private final EncodeLdpConfig ldpConfig;
        /**
         * DP协议配置项
         */
        private final BoundIntegralCdpConfig dpConfig;


        public Builder(double privacyBudget) {
            mqRpmtConfig = MqRpmtFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            ArrayList<String> lableArray=new ArrayList<>(Arrays.asList("0", "1"));
            ldpConfig = EncodeLdpFactory.createDefaultConfig(EncodeLdpFactory.EncodeLdpType.DE, privacyBudget,lableArray);
            dpConfig = BoundIntegralCdpFactory.createDefaultConfig(privacyBudget, 0.000001,1,0,1000);
        }


        @Override
        public Czz22MqRpmtDpsiConfig build() {
            return new Czz22MqRpmtDpsiConfig(this);
        }
    }
}