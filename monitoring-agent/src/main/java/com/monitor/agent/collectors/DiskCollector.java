package com.monitor.agent.collectors;

import oshi.SystemInfo;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

import java.util.List;

/**
 * Collects Disk usage using OSHI library.
 * Returns the average usage across all file stores.
 */
public class DiskCollector implements ICollector {
    
    private final FileSystem fileSystem;
    
    public DiskCollector() {
        SystemInfo si = new SystemInfo();
        this.fileSystem = si.getOperatingSystem().getFileSystem();
    }
    
    @Override
    public double collect() {
        List<OSFileStore> fileStores = fileSystem.getFileStores();
        
        if (fileStores.isEmpty()) {
            return 0.0;
        }
        
        long totalSpace = 0;
        long usedSpace = 0;
        
        for (OSFileStore store : fileStores) {
            long total = store.getTotalSpace();
            long usable = store.getUsableSpace();
            
            if (total > 0) {
                totalSpace += total;
                usedSpace += (total - usable);
            }
        }
        
        if (totalSpace == 0) {
            return 0.0;
        }
        
        double usagePercent = (usedSpace * 100.0) / totalSpace;
        return Math.round(usagePercent * 100.0) / 100.0; // Round to 2 decimal places
    }
    
    @Override
    public String getName() {
        return "DISK";
    }
}
