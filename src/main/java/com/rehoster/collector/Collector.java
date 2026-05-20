package com.rehoster.collector;

import com.rehoster.model.run.LaunchResult;
import com.rehoster.model.run.RunConfig;
import com.rehoster.model.snapshot.RuntimeSnapshot;

public interface Collector {
    
    String getName();
    
    void collect(RunConfig config, LaunchResult launchResult, RuntimeSnapshot snapshot);
}
