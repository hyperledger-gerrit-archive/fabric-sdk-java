/*
 *
 *  Copyright 2016,2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.hyperledger.fabric_ca.sdk;

import java.util.LinkedList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class ConfigureResponse {

    List<CommandResponse> commandResponses = new LinkedList<>();

    ConfigureResponse(JsonObject json) {

        final JsonArray responses = json.getJsonArray("responses");

        for (JsonValue responseVal : responses
                ) {
            if (responseVal instanceof JsonObject) {
                new CommandResponse(((JsonObject) responseVal).getString("request"),
                        ((JsonObject) responseVal).getString("result"));
            }
        }
    }

    public List<CommandResponse> getCommandResponses() {
        return commandResponses;
    }

    public class CommandResponse {
        public String getRequest() {
            return request;
        }

        public String getResult() {
            return result;
        }

        final String request;
        final String result;

        CommandResponse(String request, String result) {
            this.request = request;
            this.result = result;
            ConfigureResponse.this.commandResponses.add(this);
        }
    }

}
