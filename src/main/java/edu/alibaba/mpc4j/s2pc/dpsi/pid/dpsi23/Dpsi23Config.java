package edu.alibaba.mpc4j.s2pc.dpsi.pid.dpsi23;


import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdpConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.bound.BoundIntegralCdpFactory;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdpFactory;
import edu.alibaba.mpc4j.s2pc.dpsi.pid.PidDpsiConfig;
import edu.alibaba.mpc4j.s2pc.dpsi.pid.PidDpsiFactory;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidConfig;
import edu.alibaba.mpc4j.s2pc.pjc.pid.PidFactory;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * config for the pid-Dpsi protocol
 *
 * @author Yufei Wang
 * @date 2023/8/2
 */
public class Dpsi23Config extends AbstractMultiPartyPtoConfig implements PidDpsiConfig {
    /**
     * PID协议配置项
     */
    private final PidConfig pidConfig;
    /**
     * LDP协议配置项
     */
    private final EncodeLdpConfig ldpConfig;
    /**
     * DP协议配置项
     */
    private final BoundIntegralCdpConfig dpConfig;

    private Dpsi23Config(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.pidConfig);
        pidConfig = builder.pidConfig;
        ldpConfig = builder.ldpConfig;
        dpConfig = builder.dpConfig;
    }
    @Override
    public final PidDpsiFactory.PidDpsiType getPtoType() {
        return PidDpsiFactory.PidDpsiType.DPSI23;
    }

    public final PidConfig getPidConfig(){
        return pidConfig;
    }

    public final EncodeLdpConfig getLdpConfig(){
        return ldpConfig;
    }

    public BoundIntegralCdpConfig getDpConfig(){
        return dpConfig;
    }
    public static class Builder implements org.apache.commons.lang3.builder.Builder<Dpsi23Config> {
        /**
         * PID协议配置项
         */
        private final PidConfig pidConfig;
        /**
         * LDP协议配置项
         */
        private final EncodeLdpConfig ldpConfig;
        /**
         * DP协议配置项
         */
        private final BoundIntegralCdpConfig dpConfig;


        public Builder(double privacyBudget) {
            pidConfig = PidFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
            ArrayList<String> lableArray=new ArrayList<>(Arrays.asList("0", "1"));
            ldpConfig = EncodeLdpFactory.createDefaultConfig(EncodeLdpFactory.EncodeLdpType.DE, privacyBudget,lableArray);
            dpConfig = BoundIntegralCdpFactory.createDefaultConfig(privacyBudget, 0.001,1,0,1000);
        }

        @Override
        public Dpsi23Config build() {
            return new Dpsi23Config(this);
        }
    }
}
