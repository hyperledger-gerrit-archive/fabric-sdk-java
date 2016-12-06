/*
 *  Copyright 2016 Wanda Group - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 	  http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdk.stats;

import java.util.Date;

public class Rate {
    private long prevTime;
    private Average avg = new Average();

    public Rate() {
        prevTime = 0;
        this.avg.setSampleWeight(0.25);
    }

    public void tick() {
        long curTime = new Date().getTime();
        if (this.prevTime > 0) {
            long elapsed = curTime - this.prevTime;
            this.avg.addSample(elapsed);
        }
        this.prevTime = curTime;
    }

    // Get the rate in ticks/ms
    public double getValue() {
        return this.avg.getValue();
    }
}
