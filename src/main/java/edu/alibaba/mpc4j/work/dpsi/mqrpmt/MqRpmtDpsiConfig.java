package edu.alibaba.mpc4j.work.dpsi.mqrpmt;

import edu.alibaba.mpc4j.common.rpc.desc.SecurityModel;
import edu.alibaba.mpc4j.common.rpc.pto.AbstractMultiPartyPtoConfig;
import edu.alibaba.mpc4j.common.tool.CommonConstants;
import edu.alibaba.mpc4j.common.tool.MathPreconditions;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.UnboundIntegralCdpConfig;
import edu.alibaba.mpc4j.dp.cdp.numeric.integral.unbound.geometric.ApacheGeometricCdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.binary.BinaryLdpConfig;
import edu.alibaba.mpc4j.dp.ldp.nominal.binary.BinaryLdpFactory;

import edu.alibaba.mpc4j.work.dpsi.DpsiConfig;
import edu.alibaba.mpc4j.work.dpsi.DpsiFactory.DpPsiType;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtConfig;
import edu.alibaba.mpc4j.s2pc.opf.mqrpmt.MqRpmtFactory;


public class MqRpmtDpsiConfig extends AbstractMultiPartyPtoConfig implements DpsiConfig {
    /**
     * ε_c
     */
    private final double epsilon;
    /**
     * binary LDP
     */
    private final BinaryLdpConfig binaryLdpConfig;
    /**
     * max PSI-CA dummy size
     */
    private final int maxPsicaDummySize;
    /**
     * PSI-CA CDP config
     */
    private final UnboundIntegralCdpConfig psicaCdpConfig;
    /**
     * max PSD-CA dummy size
     */
    private final int maxPsdcaDummySize;
    /**
     * PSD-CA CDP config
     */
    private final UnboundIntegralCdpConfig psdcaCdpConfig;
    /**
     * mqRPMT
     */
    private final MqRpmtConfig mqRpmtConfig;

    private MqRpmtDpsiConfig(Builder builder) {
        super(SecurityModel.SEMI_HONEST, builder.mqRpmtConfig);
        epsilon = builder.outputEpsilon;
        binaryLdpConfig = builder.binaryLdpConfig;
        maxPsicaDummySize = builder.maxPsicaDummySize;
        psicaCdpConfig = builder.psicaCdpConfig;
        maxPsdcaDummySize = builder.maxPsdcaDummySize;
        psdcaCdpConfig = builder.psdcaCdpConfig;
        mqRpmtConfig = builder.mqRpmtConfig;
    }

    @Override
    public DpPsiType getPtoType() {
        return DpPsiType.MQ_RPMT_BASED;
    }

    @Override
    public double getEpsilon() {
        return epsilon;
    }

    public BinaryLdpConfig getBinaryLdpConfig() {
        return binaryLdpConfig;
    }

    public int getMaxPsicaDummySize() {
        return maxPsicaDummySize;
    }

    public UnboundIntegralCdpConfig getPsicaCdpConfig() {
        return psicaCdpConfig;
    }

    public int getMaxPsdcaDummySize() {
        return maxPsdcaDummySize;
    }

    public UnboundIntegralCdpConfig getPsdcaCdpConfig() {
        return psdcaCdpConfig;
    }

    public MqRpmtConfig getMqRpmtConfig() {
        return mqRpmtConfig;
    }

    public static class Builder implements org.apache.commons.lang3.builder.Builder<MqRpmtDpsiConfig> {
        /**
         * ε_c
         */
        private final double outputEpsilon;
        /**
         * binary LDP
         */
        private final BinaryLdpConfig binaryLdpConfig;
        /**
         * max PSI-CA dummy size
         */
        private final int maxPsicaDummySize;
        /**
         * PSI-CA CDP config
         */
        private final UnboundIntegralCdpConfig psicaCdpConfig;
        /**
         * max PSD-CA dummy size
         */
        private final int maxPsdcaDummySize;
        /**
         * PSD-CA CDP config
         */
        private final UnboundIntegralCdpConfig psdcaCdpConfig;
        /**
         * mqRPMT
         */
        private MqRpmtConfig mqRpmtConfig;

        public Builder(double outputEpsilon, double psicaEpsilon, double psdcaEpsilon) {
            MathPreconditions.checkPositive("ε_c", outputEpsilon);
            this.outputEpsilon = outputEpsilon;
            binaryLdpConfig = BinaryLdpFactory.createDefaultConfig(outputEpsilon);
            MathPreconditions.checkPositive("ε_psica", psicaEpsilon);
            maxPsicaDummySize = getMaxDummySize(psicaEpsilon);
            psicaCdpConfig = new ApacheGeometricCdpConfig.Builder(psicaEpsilon, 1).build();
            MathPreconditions.checkPositive("ε_psdca", psdcaEpsilon);
            maxPsdcaDummySize = getMaxDummySize(psdcaEpsilon);
            psdcaCdpConfig = new ApacheGeometricCdpConfig.Builder(psdcaEpsilon, 1).build();
            mqRpmtConfig = MqRpmtFactory.createDefaultConfig(SecurityModel.SEMI_HONEST);
        }

        private int getMaxDummySize(double epsilon) {
            double expEpsilon = Math.exp(epsilon);
            // η^0 = ln(e^ε + 1) / ε
            double eta0 = Math.log((expEpsilon + 1)) / expEpsilon;
            // Pr[η > m] = (e^{-ε(m - η^0 - 1)}) / (e^ε + 1) = 2^{-σ}
            int formula = (int) Math.ceil(
                (-1 * Math.log(Math.pow(2, -1 * CommonConstants.STATS_BIT_LENGTH) * (expEpsilon + 1)) / epsilon) + 1 + eta0
            );
            return (formula <= 0) ? 1 : formula;
        }

        public Builder setMqRpmtConfig(MqRpmtConfig mqRpmtConfig) {
            this.mqRpmtConfig = mqRpmtConfig;
            return this;
        }

        @Override
        public MqRpmtDpsiConfig build() {
            return new MqRpmtDpsiConfig(this);
        }
    }
}