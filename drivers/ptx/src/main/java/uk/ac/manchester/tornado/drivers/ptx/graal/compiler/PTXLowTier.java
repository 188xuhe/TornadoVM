package uk.ac.manchester.tornado.drivers.ptx.graal.compiler;

import org.graalvm.compiler.nodes.spi.LoweringTool;
import org.graalvm.compiler.options.OptionValues;
import org.graalvm.compiler.phases.common.AddressLoweringPhase;
import org.graalvm.compiler.phases.common.AddressLoweringPhase.AddressLowering;
import org.graalvm.compiler.phases.common.CanonicalizerPhase;
import org.graalvm.compiler.phases.common.DeadCodeEliminationPhase;
import org.graalvm.compiler.phases.common.IterativeConditionalEliminationPhase;
import org.graalvm.compiler.phases.common.LoweringPhase;
import org.graalvm.compiler.phases.common.RemoveValueProxyPhase;
import org.graalvm.compiler.phases.common.UseTrappingNullChecksPhase;
import org.graalvm.compiler.phases.schedule.SchedulePhase;
import uk.ac.manchester.tornado.drivers.common.graal.compiler.DumpLowTierGraph;
import uk.ac.manchester.tornado.drivers.ptx.graal.phases.PTXMulAddPhase;
import uk.ac.manchester.tornado.runtime.common.TornadoOptions;
import uk.ac.manchester.tornado.runtime.graal.compiler.TornadoLowTier;
import uk.ac.manchester.tornado.runtime.graal.phases.TornadoFeatureExtraction;
import uk.ac.manchester.tornado.runtime.graal.phases.TornadoLoopCanonicalization;

import static org.graalvm.compiler.core.common.GraalOptions.ConditionalElimination;
import static org.graalvm.compiler.core.common.GraalOptions.ImmutableCode;
import static org.graalvm.compiler.phases.common.DeadCodeEliminationPhase.Optionality.Required;

public class PTXLowTier extends TornadoLowTier {
    public PTXLowTier(OptionValues options, AddressLowering addressLowering) {
        CanonicalizerPhase canonicalizer;
        if (ImmutableCode.getValue(options)) {
            canonicalizer = CanonicalizerPhase.createWithoutReadCanonicalization();
        } else {
            canonicalizer = CanonicalizerPhase.create();
        }

        appendPhase(new LoweringPhase(canonicalizer, LoweringTool.StandardLoweringStage.LOW_TIER));

        appendPhase(new RemoveValueProxyPhase());

        /*
         * Cleanup IsNull checks resulting from MID_TIER/LOW_TIER lowering and
         * ExpandLogic phase.
         */
        if (ConditionalElimination.getValue(options)) {
            appendPhase(new IterativeConditionalEliminationPhase(canonicalizer, false));
        }

        appendPhase(new AddressLoweringPhase(addressLowering));

        appendPhase(new DeadCodeEliminationPhase(Required));

        appendPhase(new TornadoLoopCanonicalization());

        appendPhase(new PTXMulAddPhase());

        appendPhase(new SchedulePhase(SchedulePhase.SchedulingStrategy.LATEST_OUT_OF_LOOPS));

        if (TornadoOptions.FEATURE_EXTRACTION) {
            appendPhase(new TornadoFeatureExtraction());
        }

        if (TornadoOptions.DUMP_LOW_TIER_WITH_IGV) {
            appendPhase(new DumpLowTierGraph());
        }

    }
}
