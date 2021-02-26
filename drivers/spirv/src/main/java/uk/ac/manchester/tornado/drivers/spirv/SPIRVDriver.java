package uk.ac.manchester.tornado.drivers.spirv;

import jdk.vm.ci.hotspot.HotSpotJVMCIRuntime;
import org.graalvm.compiler.options.OptionValues;
import org.graalvm.compiler.phases.util.Providers;
import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.enums.TornadoDeviceType;
import uk.ac.manchester.tornado.api.exceptions.TornadoBailoutRuntimeException;
import uk.ac.manchester.tornado.api.exceptions.TornadoRuntimeException;
import uk.ac.manchester.tornado.drivers.spirv.graal.SPIRVHotSpotBackendFactory;
import uk.ac.manchester.tornado.runtime.TornadoAcceleratorDriver;
import uk.ac.manchester.tornado.runtime.TornadoVMConfig;
import uk.ac.manchester.tornado.runtime.common.TornadoLogger;
import uk.ac.manchester.tornado.runtime.graal.backend.TornadoBackend;
import uk.ac.manchester.tornado.runtime.graal.compiler.TornadoSuitesProvider;

public final class SPIRVDriver extends TornadoLogger implements TornadoAcceleratorDriver {

    private final SPIRVBackend[][] backends;
    private final SPIRVBackend[] flatBackends;
    private int deviceCount;

    public SPIRVDriver(OptionValues options, HotSpotJVMCIRuntime vmRuntime, TornadoVMConfig vmCon) {
        int numSPIRVPlatforms = SPIRVProxy.getNumPlatforms();
        info("[SPIRV] Found %d platforms", numSPIRVPlatforms);

        if (numSPIRVPlatforms < 1) {
            throw new TornadoBailoutRuntimeException("[Warning] No SPIRV platforms found. Deoptimizing to sequential execution");
        }

        backends = new SPIRVBackend[numSPIRVPlatforms][];
        discoverDevices(options, vmRuntime, vmCon, numSPIRVPlatforms);
        flatBackends = new SPIRVBackend[deviceCount];
        int index = 0;
        for (int i = 0; i < getNumPlatforms(); i++) {
            for (int j = 0; j < getNumDevicesForPlatform(i); j++, index++) {
                flatBackends[index] = backends[i][j];
            }
        }
    }

    private void discoverDevices(OptionValues options, HotSpotJVMCIRuntime vmRuntime, TornadoVMConfig vmCon, int numPlatforms) {
        for (int platformIndex = 0; platformIndex < numPlatforms; platformIndex++) {
            SPIRVPlatform platform = SPIRVProxy.getPlatform(platformIndex);
            SPIRVContext context = platform.createContext();
            int numDevices = platform.getNumDevices();
            backends[platformIndex] = new SPIRVBackend[numDevices];
            for (int deviceIndex = 0; deviceIndex < numDevices; deviceIndex++) {
                SPIRVDevice device = platform.getDevice(deviceIndex);
                backends[platformIndex][deviceIndex] = createSPIRVBackend(options, vmRuntime, vmCon, device, context);
            }
        }
        deviceCount = getNumDevices();
    }

    private SPIRVBackend createSPIRVBackend(OptionValues options, HotSpotJVMCIRuntime vmRuntime, TornadoVMConfig vmConfig, SPIRVDevice device, SPIRVContext context) {
        return SPIRVHotSpotBackendFactory.createBackend(options, vmRuntime, vmConfig, device, context);
    }

    @Override
    public SPIRVBackend getDefaultBackend() {
        return null;
    }

    @Override
    public Providers getProviders() {
        return getDefaultBackend().getProviders();
    }

    @Override
    public TornadoSuitesProvider getSuitesProvider() {
        return getDefaultBackend().getTornadoSuites();
    }

    @Override
    public TornadoDevice getDefaultDevice() {
        return getDefaultBackend().getDeviceContext().asMapping();
    }

    @Override
    public void setDefaultDevice(int index) {

    }

    private int getNumDevicesForPlatform(int platform) {
        try {
            return backends[platform].length;
        } catch (NullPointerException e) {
            return 0;
        }
    }

    private int getNumDevices() {
        int count = 0;
        for (int i = 0; i < getNumPlatforms(); i++) {
            count += getNumDevicesForPlatform(i);
        }
        return count;
    }

    @Override
    public int getDeviceCount() {
        return deviceCount;
    }

    @Override
    public TornadoDevice getDevice(int index) {
        if (index < flatBackends.length) {
            return flatBackends[index].getDeviceContext().asMapping();
        } else {
            throw new TornadoRuntimeException("[ERROR]-[PTX-DRIVER] Device required not found: " + index + " - Max: " + backends.length);
        }
    }

    @Override
    public TornadoDeviceType getTypeDefaultDevice() {
        return getDefaultDevice().getDeviceType();
    }

    @Override
    public String getName() {
        return "SPIRV";
    }

    @Override
    public int getNumPlatforms() {
        return backends.length;
    }
}
