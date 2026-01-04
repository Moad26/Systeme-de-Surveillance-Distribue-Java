package com.monitor.agent.collectors;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;

/**
 * Collects CPU usage using OSHI library.
 */
public class CpuCollector implements ICollector {
    
    private final CentralProcessor processor;
    private long[] previousTicks;
    
    public CpuCollector() {
        SystemInfo si = new SystemInfo();
        this.processor = si.getHardware().getProcessor();
        this.previousTicks = processor.getSystemCpuLoadTicks();
    }
    
    @Override
    public double collect() {
        // Calculate CPU load between ticks
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(previousTicks) * 100;
        previousTicks = processor.getSystemCpuLoadTicks();
        return Math.round(cpuLoad * 100.0) / 100.0; // Round to 2 decimal places
    }
    
    @Override
    public String getName() {
        return "CPU";
    }
}
