package uk.ac.manchester.tornado.drivers.ptx.graal.nodes;

import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.Value;
import org.graalvm.compiler.core.common.LIRKind;
import org.graalvm.compiler.core.common.type.StampFactory;
import org.graalvm.compiler.core.common.type.TypeReference;
import org.graalvm.compiler.graph.NodeClass;
import org.graalvm.compiler.lir.Variable;
import org.graalvm.compiler.nodeinfo.NodeInfo;
import org.graalvm.compiler.nodes.ConstantNode;
import org.graalvm.compiler.nodes.FixedNode;
import org.graalvm.compiler.nodes.spi.LIRLowerable;
import org.graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import uk.ac.manchester.tornado.drivers.ptx.graal.PTXArchitecture.PTXMemoryBase;
import uk.ac.manchester.tornado.drivers.ptx.graal.PTXStampFactory;
import uk.ac.manchester.tornado.drivers.ptx.graal.compiler.PTXLIRGenerator;
import uk.ac.manchester.tornado.drivers.ptx.graal.lir.PTXBinary;
import uk.ac.manchester.tornado.drivers.ptx.graal.lir.PTXKind;
import uk.ac.manchester.tornado.drivers.ptx.graal.lir.PTXLIRStmt;

import static uk.ac.manchester.tornado.drivers.ptx.graal.asm.PTXAssembler.PTXBinaryTemplate;

@NodeInfo
public class FixedArrayNode extends FixedNode implements LIRLowerable {

    public static final NodeClass<FixedArrayNode> TYPE = NodeClass.create(FixedArrayNode.class);

    @Input
    protected ConstantNode length;

    protected PTXKind elementKind;
    protected PTXMemoryBase memoryRegister;
    protected ResolvedJavaType elemenType;
    protected PTXBinaryTemplate arrayTemplate;

    public FixedArrayNode(PTXMemoryBase memoryRegister, ResolvedJavaType elementType, ConstantNode length) {
        super(TYPE, StampFactory.objectNonNull(TypeReference.createTrustedWithoutAssumptions(elementType.getArrayClass())));
        this.memoryRegister = memoryRegister;
        this.length = length;
        this.elemenType = elementType;
        this.elementKind = PTXKind.fromResolvedJavaType(elementType);
        this.arrayTemplate = PTXKind.resolvePrivateTemplateType(elementType);
    }

    public FixedArrayNode(PTXMemoryBase memoryRegister, PTXKind ptxKind, PTXBinaryTemplate arrayTemplate, ConstantNode length) {
        super(TYPE, PTXStampFactory.getStampFor(ptxKind));
        this.memoryRegister = memoryRegister;
        this.length = length;
        this.elementKind = ptxKind;
        this.arrayTemplate = arrayTemplate;
    }

    public PTXMemoryBase getMemoryRegister() {
        return memoryRegister;
    }

    public void setMemoryLocation(PTXMemoryBase memoryRegister) {
        this.memoryRegister = memoryRegister;
    }

    public ResolvedJavaType getElementType() {
        return elemenType;
    }

    public ConstantNode getLength() {
        return length;
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {
        /*
         * using as_T reinterprets the data as type T - consider: float x = (float) 1;
         * and int value = 1, float x = &(value);
         */
        final Value lengthValue = gen.operand(length);

        LIRKind lirKind = LIRKind.value(elementKind);
        final Variable variable = ((PTXLIRGenerator)gen.getLIRGeneratorTool()).newVariable(lirKind, true);
        final PTXBinary.Expr declaration = new PTXBinary.Expr(arrayTemplate, lirKind, variable, lengthValue);

        final PTXLIRStmt.ExprStmt expr = new PTXLIRStmt.ExprStmt(declaration);
        gen.getLIRGeneratorTool().append(expr);
        gen.setResult(this, variable);
    }

}
