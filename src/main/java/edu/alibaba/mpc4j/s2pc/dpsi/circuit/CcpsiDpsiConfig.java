package edu.alibaba.mpc4j.s2pc.dpsi.circuit;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.encode.EncodeLdpFactory;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiConfig;
import edu.alibaba.mpc4j.s2pc.pso.cpsi.ccpsi.CcpsiFactory;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * CcpsiDpsi协议配置项。
 *
 * @author Yufei Wang
 * @date 2023/8/15
 */
public class CcpsiDpsiConfig  extends AbstractMultiPartyPtoConfig{
    /**
     * CCPSI协议配置项
     */
    private final CcpsiConfig ccpsiConfig;
    /**
     * LDP协议配置项
     */
    private final EncodeLdpConfig ldpConfig;

    private CcpsiDpsiConfig(CcpsiDpsiConfig.Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.ccpsiConfig);
        ccpsiConfig = builder.ccpsiConfig;
        ldpConfig = builder.ldpConfig;
    }


    public CcpsiDpsiFactory.CcpsiDpsiType getPtoType() {
        return CcpsiDpsiFactory.CcpsiDpsiType.CCPSI;
    }

    public CcpsiConfig getCcpsiConfig(){
        return ccpsiConfig;
    }

    public EncodeLdpConfig getLdpConfig(){
        return ldpConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<CcpsiDpsiConfig> {
        /**
         * CCPSI协议配置项
         */
        private final CcpsiConfig ccpsiConfig;
        /**
         * LDP协议配置项
         */
        private final EncodeLdpConfig ldpConfig;


        public Builder(CcpsiFactory.CcpsiType type, double privacyBudget) {
            ccpsiConfig = CcpsiFactory.createDpsiConfig(type,true);
            ArrayList<String> lableArray=new ArrayList<>(Arrays.asList("0", "1"));
            ldpConfig = EncodeLdpFactory.createDefaultConfig(EncodeLdpFactory.EncodeLdpType.DE, privacyBudget,lableArray);
        }
        @Override
        public CcpsiDpsiConfig build() {
            return new CcpsiDpsiConfig(this);
        }
    }
}
