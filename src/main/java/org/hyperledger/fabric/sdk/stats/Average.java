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

public class Average {
    private double avg;
    private double sampleWeight;
    private double avgWeight;

    public Average() {
        this.avg = 0;
        this.setSampleWeight(0.5);
    }

    // Get the average value
    public double getValue() {
        return this.avg;
    }

    /**
     * Add a sample.
     */
    public void addSample(double sample) {
        if (this.avg == 0) {
            this.avg = sample;
        } else {
            this.avg = (this.avg * this.avgWeight) + (sample * this.sampleWeight);
        }
    }

    /**
     * Get the weight.
     * The weight determines how heavily to weight the most recent sample in calculating the average.
     */
    public double getSampleWeight() {
        return this.sampleWeight;
    }

    /**
     * Set the weight.
     * @params weight A value between 0 and 1.
     */
    public void setSampleWeight(double weight) {
        if ((weight < 0) || (weight > 1)) {
            throw new RuntimeException("weight must be in range [0,1]; "+weight+" is an invalid value");
        }
        this.sampleWeight = weight;
        this.avgWeight = 1 - weight;
    }
}
