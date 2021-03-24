package uk.ac.manchester.tornado.drivers.spirv.graal;

import java.util.Arrays;

import uk.ac.manchester.tornado.api.mm.ObjectBuffer;
import uk.ac.manchester.tornado.drivers.spirv.SPIRVDeviceContext;
import uk.ac.manchester.tornado.drivers.spirv.SPIRVLevelZeroCommandQueue;
import uk.ac.manchester.tornado.drivers.spirv.SPIRVLevelZeroModule;
import uk.ac.manchester.tornado.drivers.spirv.SPIRVModule;
import uk.ac.manchester.tornado.drivers.spirv.levelzero.LevelZeroCommandList;
import uk.ac.manchester.tornado.drivers.spirv.levelzero.LevelZeroKernel;
import uk.ac.manchester.tornado.drivers.spirv.levelzero.ZeGroupDispatch;
import uk.ac.manchester.tornado.drivers.spirv.levelzero.ZeKernelHandle;
import uk.ac.manchester.tornado.drivers.spirv.levelzero.samples.LevelZeroUtils;
import uk.ac.manchester.tornado.drivers.spirv.mm.SPIRVByteBuffer;
import uk.ac.manchester.tornado.runtime.common.CallStack;
import uk.ac.manchester.tornado.runtime.tasks.meta.TaskMetaData;

public class SPIRVLevelZeroInstalledCode extends SPIRVInstalledCode {

    public SPIRVLevelZeroInstalledCode(String name, SPIRVModule spirvModule, SPIRVDeviceContext deviceContext) {
        super(name, spirvModule, deviceContext);
    }

    @Override
    public int launchWithDependencies(CallStack stack, ObjectBuffer atomicSpace, TaskMetaData meta, long batchThreads, int[] waitEvents) {
        return 0;
    }

    private void setKernelArgs(final SPIRVByteBuffer stack, final ObjectBuffer atomicSpace, TaskMetaData meta) {

        SPIRVLevelZeroModule module = (SPIRVLevelZeroModule) spirvModule;
        LevelZeroKernel levelZeroKernel = module.getKernel();
        ZeKernelHandle kernel = levelZeroKernel.getKernelHandle();

        int index = 0;
        // heap (global memory)
        buffer.clear();
        buffer.putLong(stack.toBuffer());
        // int result =
        // levelZeroKernel.zeKernelSetArgumentValue(kernel.getPtrZeKernelHandle(), 0,
        // Sizeof.POINTER.getNumBytes(), buffer);
        // LevelZeroUtils.errorLog("zeKernelSetArgumentValue", result);
        index++;
    }

    @Override
    public int launchWithoutDependencies(CallStack stack, ObjectBuffer atomicSpace, TaskMetaData meta, long batchThreads) {
        System.out.println("Running CODE!!!!!!!!!! ");

        SPIRVLevelZeroModule module = (SPIRVLevelZeroModule) spirvModule;
        LevelZeroKernel levelZeroKernel = module.getKernel();
        ZeKernelHandle kernel = levelZeroKernel.getKernelHandle();

        long[] globalWork = new long[3];
        Arrays.fill(globalWork, 1);
        int dims = meta.getDims();
        System.arraycopy(meta.getGlobalWork(), 0, globalWork, 0, dims);

        // Prepare kernel for launch
        // A) Suggest scheduling parameters to level-zero
        int[] groupSizeX = new int[] { (int) globalWork[0] };
        int[] groupSizeY = new int[] { (int) globalWork[1] };
        int[] groupSizeZ = new int[] { (int) globalWork[2] };
        int result = levelZeroKernel.zeKernelSuggestGroupSize(kernel.getPtrZeKernelHandle(), (int) globalWork[0], (int) globalWork[1], (int) globalWork[2], groupSizeX, groupSizeY, groupSizeZ);
        LevelZeroUtils.errorLog("zeKernelSuggestGroupSize", result);

        result = levelZeroKernel.zeKernelSetGroupSize(kernel.getPtrZeKernelHandle(), groupSizeX, groupSizeY, groupSizeZ);
        LevelZeroUtils.errorLog("zeKernelSetGroupSize", result);

        // FIXME <PENDING> Set arguments
        // result =
        // levelZeroKernel.zeKernelSetArgumentValue(kernel.getPtrZeKernelHandle(), 0,
        // Sizeof.POINTER.getNumBytes(), bufferA);
        // LevelZeroUtils.errorLog("zeKernelSetArgumentValue", result);

        // Dispatch SPIR-V Kernel
        ZeGroupDispatch dispatch = new ZeGroupDispatch();
        dispatch.setGroupCountX((int) globalWork[0] / groupSizeX[0]);
        dispatch.setGroupCountY((int) globalWork[1] / groupSizeY[0]);
        dispatch.setGroupCountZ((int) globalWork[2] / groupSizeZ[0]);

        SPIRVLevelZeroCommandQueue commandQueue = (SPIRVLevelZeroCommandQueue) deviceContext.getSpirvContext().getCommandQueueForDevice(0);
        LevelZeroCommandList commandList = commandQueue.getCommandList();

        // Launch the kernel on the Intel Integrated GPU
        result = commandList.zeCommandListAppendLaunchKernel(commandList.getCommandListHandlerPtr(), kernel.getPtrZeKernelHandle(), dispatch, null, 0, null);
        LevelZeroUtils.errorLog("zeCommandListAppendLaunchKernel", result);

        return 0;
    }
}
