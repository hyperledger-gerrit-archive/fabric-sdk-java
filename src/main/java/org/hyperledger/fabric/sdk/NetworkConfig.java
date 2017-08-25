/*
 *  Copyright 2017 DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.hyperledger.fabric.sdk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.Peer.PeerRole;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.yaml.snakeyaml.Yaml;

import static java.lang.String.format;

/**
 *
 * Holds details of network and channel configurations typically loaded from an external config file.
 * <br>
 * Also contains convenience methods for utilizing the config details,
 * including the main {@link #getChannel(String)} method
 *
 */

public class NetworkConfig {

    private JsonObject jsonConfig;

    private String configName;
    private String configVersion;
    private JsonObject clientOrganization;

    private Map<String, Node> orderers;
    private Map<String, Node> peers;
    private Map<String, Node> eventHubs;

    private static final Log logger = LogFactory.getLog(NetworkConfig.class);


    private NetworkConfig(JsonObject jsonConfig) throws InvalidArgumentException {

        //out("\n\n\n\n\n*******************************************************************************************\n");
        //out("\n\n   ****                                NetworkConfig                                 **** \n\n");
        //out("\n*******************************************************************************************\n\n\n\n\n");

        this.jsonConfig = jsonConfig;

        // Extract the main details
        configName = getJsonValueAsString(jsonConfig.get("name"));
        if (configName == null || configName.length() == 0) {
            throw new InvalidArgumentException("Network config must have a name");
        }

        configVersion = getJsonValueAsString(jsonConfig.get("version"));
        if (configVersion == null || configVersion.length() == 0) {
            throw new InvalidArgumentException("Network config must have a version");
            // TODO: Validate the version
        }

        // Extract the organization for this client
        JsonObject jsonClient = getJsonObject(jsonConfig, "client");
        String orgName = jsonClient == null ? null : getJsonValueAsString(jsonClient.get("organization"));
        if (orgName == null || orgName.length() == 0) {
            throw new InvalidArgumentException("A client organization must be specified");
        }

        clientOrganization = getOrganization(orgName);
        if (clientOrganization == null) {
            throw new InvalidArgumentException("Client organization " + orgName + " is not defined");
        }

        // Preload and create all peers and orderers
        createAllPeers();
        createAllOrderers();

    }

    /**
     * Creates a new NetworkConfig instance configured with details supplied in a JSON or YAML file.
     * <p>
     * If the file's extension starts with a "y", the data is assumed to be in YAML format, otherwise
     * it should be in JSON format
     *
     * @param configFile    The file containing the network configuration
     * @return A new NetworkConfig instance
     * @throws InvalidArgumentException
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static NetworkConfig fromFile(File configFile) throws InvalidArgumentException, FileNotFoundException, IOException {

        // Sanity check
        if (configFile == null) {
            throw new InvalidArgumentException("configFile must be specified");
        }

        NetworkConfig config = null;

        // Determine if this looks like a json or a yaml file
        // If the file extension starts with a "y" then we assume yaml. Otherwise json
        String ext = FilenameUtils.getExtension(configFile.getName());
        if (ext.toLowerCase().startsWith("y")) {
            // Yaml file
            String yaml = new String(Files.readAllBytes(configFile.toPath()));
            JsonObject json = convertYamlToJson(yaml);
            config = fromJsonObject(json);

        } else {
            // Json file
            InputStream stream = null;

            try {
                stream = new FileInputStream(configFile);
                config = fromJsonStream(stream);

            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        // Not really much we can do here!
                        logger.error("Failed to close the stream!", e);
                        e.printStackTrace();
                    }
                }
            }
        }

        return config;
    }

    /**
     * Creates a new NetworkConfig instance configured with details supplied in JSON format
     *
     * @param configStream A stream opened on a JSON file containing network configuration details
     * @return A new NetworkConfig instance
     * @throws InvalidArgumentException
     */
    public static NetworkConfig fromJsonStream(InputStream configStream) throws InvalidArgumentException {

        // Sanity check
        if (configStream == null) {
            throw new InvalidArgumentException("configStream must be specified");
        }

        // Read the input stream and convert to JSON
        JsonReader reader = Json.createReader(configStream);
        JsonObject jsonConfig = (JsonObject) reader.read();

        return fromJsonObject(jsonConfig);
    }

    /**
     * Creates a new NetworkConfig instance configured with details supplied in a JSON object
     *
     * @param jsonConfig JSON object containing network configuration details
     * @return A new NetworkConfig instance
     * @throws InvalidArgumentException
     */
    public static NetworkConfig fromJsonObject(JsonObject jsonConfig) throws InvalidArgumentException {

        // Sanity check
        if (jsonConfig == null) {
            throw new InvalidArgumentException("jsonConfig must be specified");
        }

        return NetworkConfig.load(jsonConfig);
    }


    /**
     * Returns a new NetworkConfig instance and populates it from the specified JSON object
     *
     * @param jsonConfig The JSON object containing the config details
     * @return A populated NetworkConfig instance
     * @throws InvalidArgumentException
     */
    public static NetworkConfig load(JsonObject jsonConfig) throws InvalidArgumentException {

        // Sanity check
        if (jsonConfig == null) {
            throw new InvalidArgumentException("config must be specified");
        }

       return new NetworkConfig(jsonConfig);
    }

    /**
     * Returns a channel configured using the details in the Network Configuration file
     * @param client The associated client
     * @param channelName The name of the channel
     * @return A configured Channel instance
     */
    Channel loadChannel(HFClient client, String channelName) {

        //out("\n\n\n\n\n*******************************************************************************************\n");
        //out("\n\n   ****                                getChannel                                 **** \n\n");
        //out("\n*******************************************************************************************\n\n\n\n\n");


        Channel channel = null;

        JsonObject channels = getJsonObject(jsonConfig, "channels");

        if (channels != null) {
            JsonObject jsonChannel = getJsonObject(channels, channelName);
            if (jsonChannel != null) {
                channel = client.getChannel(channelName);
                if (channel != null) {
                    // The channel already exists in the client!
                    // TODO: How should this be handled?
                    throw new RuntimeException("Channel " + channelName + " is already configured in the client!");
                }
                channel = reconstructChannel(client, channelName, jsonChannel);
            }

        }

        return channel;
    }

    /**
     * Returns a peer from the specified organization and having the desired role.
     * <p>
     * Note that if more than one peer matches the supplied attributes, it is arbitrary which peer will be returned.
     *
     * @param orgName The name of the organization (or null to use the client organization)
     * @param role The desired role (or null for any role)
     * @return A matching peer (or null if a suitable peer was not found)
     */
/*
    Peer findPeerWithRole(String orgName, PeerRole role) {

        JsonObject org = orgName == null ? clientOrganization : getOrganization(orgName);
        if (org == null) {
            // The organization is not defined, so no suitable peer exists
            return null;
        }

        // Examine the peers associated with this organization
        JsonArray peerNames = getJsonValueAsArray(org.get("peers"));
        if (peerNames != null) {
            for (JsonValue val: peerNames) {
                String peerName = getJsonValueAsString(val);
                if (peerName != null) {
                    Node peer = peers.get(peerName);
                    if (peer != null) {
                        // TODO: Currently we are ignoring the role - because roles are channel-based and hence we need to know the channel before we can get the roles!
                        try {
                            return Peer.createNewInstance(peer.getName(), peer.getUrl(), peer.getProperties());
                        } catch (InvalidArgumentException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

        return null;
    }
*/

    // Creates Node instances representing all the orderers defined in the config file
    private void createAllOrderers() throws InvalidArgumentException {

        orderers = new HashMap<String, Node>();

        // orderers is a JSON object containing a nested object for each orderers
        JsonObject jsonOrderers = getJsonObject(jsonConfig, "orderers");

        if (jsonOrderers != null) {

            for (Entry<String, JsonValue> entry : jsonOrderers.entrySet()) {
                String ordererName = entry.getKey();

                JsonObject jsonOrderer = getJsonValueAsObject(entry.getValue());
                if (jsonOrderer == null) {
                    throw new RuntimeException("Error loading config. Invalid orderer entry: " + ordererName);
                }

                Node orderer = createNode(ordererName, jsonOrderer, "url");
                if (orderer == null) {
                    // TODO: Determine appropriate exception handling...
                    throw new RuntimeException("Error loading config. Orderer " + ordererName + " not defined in configuration");
                }
                orderers.put(ordererName, orderer);
            }
        }

    }

    // Creates Node instances representing all the peers (and associated event hubs) defined in the config file
    private void createAllPeers() throws InvalidArgumentException {

        peers = new HashMap<String, Node>();
        eventHubs = new HashMap<String, Node>();

        // peers is a JSON object containing a nested object for each peer
        JsonObject jsonPeers = getJsonObject(jsonConfig, "peers");

        //out("Peers: " + (jsonPeers == null ? "null" : jsonPeers.toString()));
        if (jsonPeers != null) {

            for (Entry<String, JsonValue> entry : jsonPeers.entrySet()) {
                String peerName = entry.getKey();

                JsonObject jsonPeer = getJsonValueAsObject(entry.getValue());
                if (jsonPeer == null) {
                    throw new RuntimeException("Error loading config. Invalid peer entry: " + peerName);
                }

                Node peer = createNode(peerName, jsonPeer, "url");
                if (peer == null) {
                    throw new RuntimeException("Error loading config. Peer " + peerName + " not defined in configuration");
                }
                peers.put(peerName, peer);

                // Also create an event hub with the same name as the peer
                Node eventHub = createNode(peerName, jsonPeer, "eventUrl");
                if (eventHub == null) {
                    throw new RuntimeException("Error loading config. Peer " + peerName + " not defined in configuration");
                }
                eventHubs.put(peerName, eventHub);
            }
        }

    }


    // Reconstructs an existing channel
    private Channel reconstructChannel(HFClient client, String channelName, JsonObject jsonChannel) {

        Channel channel = null;

        try {
            channel = client.newChannel(channelName);

            // orderers is an array of orderer name strings
            JsonArray ordererNames = getJsonValueAsArray(jsonChannel.get("orderers"));
            boolean foundOrderer = false;

            //out("Orderer names: " + (ordererNames == null ? "null" : ordererNames.toString()));
            if (ordererNames != null) {
                for (JsonValue jsonVal: ordererNames) {

                    String ordererName = getJsonValueAsString(jsonVal);
                    Orderer orderer = getOrderer(client, ordererName);
                    if (orderer == null) {
                        throw new RuntimeException("Error constructing channel " + channelName + ". Orderer " + ordererName + " not defined in configuration");
                    }
                    channel.addOrderer(orderer);
                    foundOrderer = true;
                }
            }

            if (!foundOrderer) {
                // orderers is a required field
                throw new RuntimeException("Error constructing channel " + channelName + ". At least one orderer must be specified");
            }

            // peers is an object containing a nested object for each peer
            JsonObject peers = getJsonObject(jsonChannel, "peers");
            boolean foundPeer = false;

            //out("Peers: " + (peers == null ? "null" : peers.toString()));
            if (peers != null) {

                for (Entry<String, JsonValue> entry : peers.entrySet()) {
                    String peerName = entry.getKey();

                    out("NetworkConfig.reconstructChannel: Processing peer " + peerName);


                    JsonObject jsonPeer = getJsonValueAsObject(entry.getValue());
                    if (jsonPeer == null) {
                        throw new RuntimeException("Error constructing channel " + channelName + ". Invalid peer entry: " + peerName);
                    }

                    Peer peer = getPeer(client, peerName);
                    if (peer == null) {
                        throw new RuntimeException("Error constructing channel " + channelName + ". Peer " + peerName + " not defined in configuration");
                    }

                    // Set the various roles
                    setPeerRole(channelName, peer, jsonPeer, PeerRole.ENDORSING_PEER);
                    setPeerRole(channelName, peer, jsonPeer, PeerRole.CHAINCODE_QUERY);
                    setPeerRole(channelName, peer, jsonPeer, PeerRole.LEDGER_QUERY);
                    setPeerRole(channelName, peer, jsonPeer, PeerRole.EVENT_SOURCE);

                    channel.addPeer(peer);

                    foundPeer = true;

                    // Add the event hub associated with this peer
                    EventHub eventHub = getEventHub(client, peerName);
                    if (eventHub == null) {
                        // By rights this should never happen!
                        throw new RuntimeException("Error constructing channel " + channelName + ". EventHub for " + peerName + " not defined in configuration");
                    }
                    channel.addEventHub(eventHub);

                }

            }

            if (!foundPeer) {
                // peers is a required field
                throw new RuntimeException("Error constructing channel " + channelName + ". At least one peer must be specified");
            }

            // TODO: Certificate Authorities
            JsonArray cas = getJsonValueAsArray(clientOrganization.get("certificateAuthorities"));
            if (cas != null) {
                for (JsonValue ca: cas) {
                    String caName = getJsonValueAsString(ca);
                    if (caName != null) {
                        JsonObject jsonCa = getCertificateAuthority(caName);
                        if (jsonCa == null) {
                            throw new RuntimeException("Error constructing channel " + channelName + ". Client Org Certificate Authority " + caName + " is not defined");
                        }
                    }
                }
            }

        } catch (InvalidArgumentException e) {
            throw new IllegalArgumentException(e);
        }

        return channel;
    }

    private void setPeerRole(String channelName, Peer peer, JsonObject jsonPeer, PeerRole role) {
        String propName = role.getPropertyName();
        JsonValue val = jsonPeer.get(propName);
        if (val != null) {
            Boolean isSet = getJsonValueAsBoolean(val);
            if (isSet == null) {
                // This is an invalid boolean value
                throw new RuntimeException("Error constructing channel " + channelName + ". Role " + propName + " has invalid boolean value: " + val.toString());
            }
            peer.setRole(role, isSet);
        }
    }

    // Returns a new Orderer instance for the specified orderer name
    private Orderer getOrderer(HFClient client, String ordererName) throws InvalidArgumentException {
        Orderer orderer = null;
        Node o = orderers.get(ordererName);
        if (o != null) {
            orderer = client.newOrderer(o.getName(), o.getUrl(), o.getProperties());
        }
        return orderer;
    }

    // Creates a new Node instance from a JSON object
    private Node createNode(String nodeName, JsonObject jsonOrderer, String urlPropName) {

        String url = jsonOrderer.getString(urlPropName);

        Properties props = new Properties();

        // Extract the pem details
        JsonObject jsonTlsCaCerts = getJsonObject(jsonOrderer, "tlsCACerts");
        if (jsonTlsCaCerts != null) {
            String pemFilename = getJsonValueAsString(jsonTlsCaCerts.get("path"));
            String pemBytes = getJsonValueAsString(jsonTlsCaCerts.get("pem"));

            if (pemFilename != null && pemBytes != null) {
                throw new RuntimeException("Endpoint " + nodeName + " should not specify both tlsCACerts path and pem");
            }

            if (pemFilename != null) {
                // Determine full pathname and ensure the file exists
                File pemFile = new File(pemFilename);
                String fullPathname = pemFile.getAbsolutePath();
                if (!pemFile.exists()) {
                    throw new RuntimeException("Endpoint " + nodeName + ": Pem file " + fullPathname + " does not exist");
                }
                props.put("pemFile", fullPathname);
            }

            if (pemBytes != null) {
                props.put("pemBytes", pemBytes.getBytes());
            }

        }

        // Extract any other grpc options
        JsonObject jsonGrpcOptions = getJsonObject(jsonOrderer, "grpcOptions");
        if (jsonGrpcOptions != null) {

            for (Entry<String, JsonValue> entry : jsonGrpcOptions.entrySet()) {
                String key = entry.getKey();
                JsonValue value = entry.getValue();
                props.setProperty(key, getJsonValue(value));
            }
        }

        Node node = new Node(nodeName, url, props);

        /*
        let opts = {};
        opts.pem = getTLSCACert(orderer_config);
        Object.assign(opts, orderer_config[GRPC_CONNECTION_OPTIONS]);
        orderer = new Orderer(orderer_config[URL], opts);

         */

         return node;
    }

    // Returns a new Peer instance for the specified peer name
    private Peer getPeer(HFClient client, String peerName) throws InvalidArgumentException {
        Peer peer = null;
        Node p = peers.get(peerName);
        if (p != null) {
            peer = client.newPeer(p.getName(), p.getUrl(), p.getProperties());
        }
        return peer;
    }


    // Returns a new EventHub instance for the specified name
    private EventHub getEventHub(HFClient client, String name) throws InvalidArgumentException {
        EventHub ehub = null;
        Node e = eventHubs.get(name);
        if (e != null) {
            ehub = client.newEventHub(e.getName(), e.getUrl(), e.getProperties());
        }
        return ehub;
    }

    // Returns the JSON organization object with the specified name - or null if not found
    private JsonObject getOrganization(String orgName) {
        return getNamedObject(jsonConfig, "organizations", orgName);
    }

    // Returns the JSON certificateAuthority object with the specified name - or null if not found
    private JsonObject getCertificateAuthority(String caName) {
        return getNamedObject(jsonConfig, "certificateAuthorities", caName);
    }

    /**
     * Returns a named/nested JsonObject from a JsonObject
     *
     * @param sourceObject The top-level object
     * @param propName The name of the property of sourceObject that contains the nested objects
     * @param propValue The name of the nested object to return
     * @return The child JsonObject having the specified name
     */
    private JsonObject getNamedObject(JsonObject sourceObject, String propName, String propValue) {
        JsonObject o = getJsonObject(sourceObject, propName);
        if (o != null) {
            for (Entry<String, JsonValue> entry : o.entrySet()) {
                if (propValue.equals(entry.getKey())) {
                    // This is the one!
                    return getJsonValueAsObject(entry.getValue());
                }
            }
        }
        return null;
    }

    // Returns the set of peer names associated with this organization
/*
    private HashSet<String> getClientOrgPeerNames() {

        HashSet<String> peerNames = new HashSet<String>();

        JsonArray peers = getJsonValueAsArray(clientOrganization.get("peers"));
        if (peers != null) {
            for (JsonValue peer: peers) {
                String peerName = getJsonValueAsString(peer);
                if (peerName != null) {
                    peerNames.add(peerName);
                }
            }
        }

        return peerNames;
    }
*/

    // Returns the specified JsonValue in a suitable format
    // If it's a JsonString - it returns the string
    // If it's a number = it returns the string represenation of that number
    // If it's TRUE or FALSE - it returns "true" and "false" respectively
    // If it's anything else it returns null
    private String getJsonValue(JsonValue value) {
        String s = null;
        if (value != null) {
            s = getJsonValueAsString(value);
            if (s == null) {
                s = getJsonValueAsNumberString(value);
            }
            if (s == null) {
                Boolean b = getJsonValueAsBoolean(value);
                if (b != null) {
                    s = b ? "true" : "false";
                }
            }
        }
        return s;
    }

    // Returns the specified JsonValue as a JsonObject, or null if it's not an object
    private JsonObject getJsonValueAsObject(JsonValue value) {
        return (value != null && value.getValueType() == ValueType.OBJECT) ? value.asJsonObject() : null;
    }

    // Returns the specified JsonValue as a JsonArray, or null if it's not an array
    private JsonArray getJsonValueAsArray(JsonValue value) {
        return (value != null && value.getValueType() == ValueType.ARRAY) ? value.asJsonArray() : null;
    }

    // Returns the specified JsonValue as a String, or null if it's not a string
    private String getJsonValueAsString(JsonValue value) {
        return (value != null && value.getValueType() == ValueType.STRING) ? ((JsonString) value).getString() : null;
    }

    // Returns the specified JsonValue as a String, or null if it's not a string
    private String getJsonValueAsNumberString(JsonValue value) {
        return (value != null && value.getValueType() == ValueType.NUMBER) ? ((JsonNumber) value).toString() : null;
    }

    // Returns the specified JsonValue as a Boolean, or null if it's not a boolean
    private Boolean getJsonValueAsBoolean(JsonValue value) {
        if (value != null) {
            if (value.getValueType() == ValueType.TRUE) {
                return true;
            } else if (value.getValueType() == ValueType.FALSE) {
                return false;
            }
        }
        return null;
    }

    // Returns the specified property as a JsonObject
    private JsonObject getJsonObject(JsonObject object, String propName) {
        JsonObject obj = null;
        JsonValue val = object.get(propName);
        if (val != null && val.getValueType() == ValueType.OBJECT) {
            obj = val.asJsonObject();
        }
        return obj;
    }

    // Converts a string containing YAML to a JsonObject
    private static JsonObject convertYamlToJson(String yamlString) {
        Yaml yaml = new Yaml();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) yaml.load(yamlString);

        JsonObjectBuilder builder = Json.createObjectBuilder(map);

        return builder.build();
    }


    static void out(String format, Object... args) {

        System.err.flush();
        System.out.flush();

        System.out.println(format(format, args));
        System.err.flush();
        System.out.flush();

    }

    // Holds a network "node" (eg. Peer, Orderer, EventHub)
    private class Node {

        private final String name;
        private final String url;
        private final Properties properties;

        Node(String name, String url, Properties properties) {
            this.url = url;
            this.name = name;
            this.properties = properties;
        }

        public String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public Properties getProperties() {
            return properties;
        }


    }

}
