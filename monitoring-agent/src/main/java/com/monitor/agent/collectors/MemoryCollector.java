package com.monitor.agent.collectors;

import oshi.SystemInfo;
import oshi.hardware.GlobalMemory;

/**
 * Collects RAM usage using OSHI library.
 */
public class MemoryCollector implements ICollector {
    
    private final GlobalMemory memory;
    
    public MemoryCollector() {
        SystemInfo si = new SystemInfo();
        this.memory = si.getHardware().getMemory();
    }
    
    @Override
    public double collect() {
        long totalMemory = memory.getTotal();
        long availableMemory = memory.getAvailable();
        long usedMemory = totalMemory - availableMemory;
        
        double usagePercent = (usedMemory * 100.0) / totalMemory;
        return Math.round(usagePercent * 100.0) / 100.0; // Round to 2 decimal places
    }
    
    @Override
    public String getName() {
        return "RAM";
    }
}
