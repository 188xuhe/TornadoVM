package uk.ac.manchester.tornado.drivers.spirv.levelzero;

public class LevelZeroKernel {

    private ZeKernelDesc kernelDesc;
    private ZeKernelHandle kernelHandle;

    public LevelZeroKernel(ZeKernelDesc kernelDesc, ZeKernelHandle kernelHandle) {
        this.kernelDesc = kernelDesc;
        this.kernelHandle = kernelHandle;
    }

    public ZeKernelHandle getKernelHandle() {
        return this.kernelHandle;
    }

    native int zeKernelSuggestGroupSize_native(long ptrZeKernelHandle, int globalSizeX, int globalSizeY, int globalSizeZ, int[] groupSizeX, int[] groupSizeY, int[] groupSizeZ);

    public int zeKernelSuggestGroupSize(long ptrZeKernelHandle, int globalSizeX, int globalSizeY, int globalSizeZ, int[] groupSizeX, int[] groupSizeY, int[] groupSizeZ) {
        int result = zeKernelSuggestGroupSize_native(ptrZeKernelHandle, globalSizeX, globalSizeY, globalSizeZ, groupSizeX, groupSizeY, groupSizeZ);
        return result;
    }

    native int zeKernelSetGroupSize_native(long ptrZeKernelHandle, int groupSizeX, int groupSizeY, int groupSizeZ);

    public int zeKernelSetGroupSize(long ptrZeKernelHandle, int[] groupSizeX, int[] groupSizeY, int[] groupSizeZ) {
        return zeKernelSetGroupSize_native(ptrZeKernelHandle, groupSizeX[0], groupSizeY[0], groupSizeZ[0]);
    }

    native int zeKernelSetArgumentValue_native(long ptrZeKernelHandle, int argIndex, int argSize, LevelZeroBufferInteger argValue);

    public int zeKernelSetArgumentValue(long ptrZeKernelHandle, int argIndex, int argSize, LevelZeroBufferInteger argValue) {
        return zeKernelSetArgumentValue_native(ptrZeKernelHandle, argIndex, argSize, argValue);
    }

    native int zeKernelSetArgumentValue_nativeByteArg(long ptrZeKernelHandle, int argIndex, int argSize, byte[] argValue);

    public int zeKernelSetArgumentValue(long ptrZeKernelHandle, int argIndex, int argSize, byte[] argValue) {
        return zeKernelSetArgumentValue_nativeByteArg(ptrZeKernelHandle, argIndex, argSize, argValue);
    }

    private native int zeKernelSetArgumentValue_nativePtrArg(long ptrZeKernelHandle, int argIndex, int argSize, long ptrBuffer);

    public int zeKernelSetArgumentValue(long ptrZeKernelHandle, int argIndex, int argSize, long ptrBuffer) {
        return zeKernelSetArgumentValue_nativePtrArg(ptrZeKernelHandle, argIndex, argSize, ptrBuffer);
    }
}
