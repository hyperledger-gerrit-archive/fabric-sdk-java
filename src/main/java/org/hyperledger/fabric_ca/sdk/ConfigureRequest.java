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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;

/**
 * An configure request is information required to provide fabric-ca server configuration.
 */
public class ConfigureRequest {

    private String caName;
    List<Command> commands = new LinkedList<>();

    // Constructor
    public ConfigureRequest() {

    }

    void setCAName(String caName) {
        this.caName = caName;
    }

    /**
     * Add a new command to the request.
     *
     * @param commandName The command name.
     * @return the new Command.
     */
    public Command addCommand(String commandName) {
        final Command command = new Command(commandName);
        return command;
    }

    // Convert the configure request to a JSON string
    String toJson() {
        StringWriter stringWriter = new StringWriter();
        JsonWriter jsonWriter = Json.createWriter(new PrintWriter(stringWriter));
        jsonWriter.writeObject(toJsonObject());
        jsonWriter.close();
        return stringWriter.toString();
    }

    // Convert the configure request to a JSON object
    JsonObject toJsonObject() {
        JsonObjectBuilder factory = Json.createObjectBuilder();

        if (caName != null) {
            factory.add(HFCAClient.FABRIC_CA_REQPROP, caName);
        }

        if (!commands.isEmpty()) {
            JsonArrayBuilder ab = Json.createArrayBuilder();
            for (Command command : commands) {
                ab.add(command.toJson());
            }
            factory.add("commands", ab.build());
        }

        return factory.build();
    }

    class Command {

        List<Arg> args = new LinkedList<>();

        JsonObject toJson() {
            JsonObjectBuilder factory = Json.createObjectBuilder();

            if (!args.isEmpty()) {
                JsonArrayBuilder ab = Json.createArrayBuilder();
                for (Arg arg : args) {
                    ab.add(arg.arg);
                }
                factory.add("args", ab.build());
            }
            return factory.build();
        }

        class Arg {
            final String arg;

            Arg(String arg) {
                this.arg = arg;
                args.add(this);
            }

        }

        /**
         * Add an argument to a command.
         *
         * @param arg argument to the command.
         * @return Command
         */

        public Command addArg(String arg) {
            new Arg(arg);

            return Command.this;

        }

        Command(String name) {
            addArg(name); //Command name is treated like an arg aaugh
            commands.add(this);

        }

    }
}
