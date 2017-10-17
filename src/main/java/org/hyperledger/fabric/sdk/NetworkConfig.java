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
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.Channel.PeerOptions;
import org.hyperledger.fabric.sdk.Peer.PeerRole;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.exception.NetworkConfigurationException;
import org.yaml.snakeyaml.Yaml;

import static java.lang.String.format;

/**
 *
 * Holds details of network and channel configurations typically loaded from an external config file.
 * <br>
 * Also contains convenience methods for utilizing the config details,
 * including the main {@link HFClient#getChannel(String)} method
 *
 */

public class NetworkConfig {

    private final JsonObject jsonConfig;

    private JsonObject clientOrganization;

    private Map<String, Node> orderers;
    private Map<String, Node> peers;
    private Map<String, Node> eventHubs;

    private Map<String, JsonObject> certificateAuthorities;

    //private Map<String, JsonObject> clientCertificateAuthorities;

    private static final Log logger = LogFactory.getLog(NetworkConfig.class);


    private NetworkConfig(JsonObject jsonConfig) throws InvalidArgumentException, NetworkConfigurationException {

        this.jsonConfig = jsonConfig;

        // Extract the main details
        String configName = getJsonValueAsString(jsonConfig.get("name"));
        if (configName == null || configName.length() == 0) {
            throw new InvalidArgumentException("Network config must have a name");
        }

        String configVersion = getJsonValueAsString(jsonConfig.get("version"));
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

        // Preload and create all peers, orderers, etc
        createAllPeers();
        createAllOrderers();
        createAllCertificateAuthorities();

    }


    /**
     * Creates a new NetworkConfig instance configured with details supplied in a YAML file.
     *
     * @param configFile    The file containing the network configuration
     * @return A new NetworkConfig instance
     * @throws InvalidArgumentException
     * @throws IOException
     */
    public static NetworkConfig fromYamlFile(File configFile) throws InvalidArgumentException, IOException, NetworkConfigurationException {
        return fromFile(configFile, false);
    }

    /**
     * Creates a new NetworkConfig instance configured with details supplied in a JSON file.
     *
     * @param configFile    The file containing the network configuration
     * @return A new NetworkConfig instance
     * @throws InvalidArgumentException
     * @throws IOException
     */
    public static NetworkConfig fromJsonFile(File configFile) throws InvalidArgumentException, IOException, NetworkConfigurationException {
        return fromFile(configFile, true);
    }

    /**
     * Creates a new NetworkConfig instance configured with details supplied in YAML format
     *
     * @param configStream A stream opened on a YAML document containing network configuration details
     * @return A new NetworkConfig instance
     * @throws InvalidArgumentException
     */
    public static NetworkConfig fromYamlStream(InputStream configStream) throws InvalidArgumentException, IOException, NetworkConfigurationException {

        logger.trace("NetworkConfig.fromYamlStream...");

        // Sanity check
        if (configStream == null) {
            throw new InvalidArgumentException("configStream must be specified");
        }

        Yaml yaml = new Yaml();

        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) yaml.load(configStream);

        JsonObjectBuilder builder = Json.createObjectBuilder(map);

        JsonObject jsonConfig = builder.build();
        return fromJsonObject(jsonConfig);
    }

    /**
     * Creates a new NetworkConfig instance configured with details supplied in JSON format
     *
     * @param configStream A stream opened on a JSON document containing network configuration details
     * @return A new NetworkConfig instance
     * @throws InvalidArgumentException
     */
    public static NetworkConfig fromJsonStream(InputStream configStream) throws InvalidArgumentException, NetworkConfigurationException {

        logger.trace("NetworkConfig.fromJsonStream...");

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
    public static NetworkConfig fromJsonObject(JsonObject jsonConfig) throws InvalidArgumentException, NetworkConfigurationException {

        // Sanity check
        if (jsonConfig == null) {
            throw new InvalidArgumentException("jsonConfig must be specified");
        }

        if (logger.isTraceEnabled()) {
            logger.trace(format("NetworkConfig.fromJsonObject: %s", jsonConfig.toString()));
        }

        return NetworkConfig.load(jsonConfig);
    }

    // Loads a NetworkConfig object from a Json or Yaml file
    private static NetworkConfig fromFile(File configFile, boolean isJson) throws InvalidArgumentException, IOException, NetworkConfigurationException {

        // Sanity check
        if (configFile == null) {
            throw new InvalidArgumentException("configFile must be specified");
        }

        if (logger.isTraceEnabled()) {
            logger.trace(format("NetworkConfig.fromFile: %s  isJson = %b", configFile.getAbsolutePath(), isJson));
        }

        NetworkConfig config = null;

        // Json file
        InputStream stream = null;

        try {
            stream = new FileInputStream(configFile);
            config = isJson ? fromJsonStream(stream) : fromYamlStream(stream);

        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // Not really much we can do here!
                    logger.error(format("Failed to close the stream for file: %s", configFile.getAbsolutePath()), e);
                }
            }
        }

        return config;
    }

    /**
     * Returns a new NetworkConfig instance and populates it from the specified JSON object
     *
     * @param jsonConfig The JSON object containing the config details
     * @return A populated NetworkConfig instance
     * @throws InvalidArgumentException
     */
    private static NetworkConfig load(JsonObject jsonConfig) throws InvalidArgumentException, NetworkConfigurationException {

        // Sanity check
        if (jsonConfig == null) {
            throw new InvalidArgumentException("config must be specified");
        }

       return new NetworkConfig(jsonConfig);
    }

    public String getClientOrganizationName() {
        return getJsonValueAsString(clientOrganization.get("name"));
    }

    public UserInfo getPeerAdmin() throws NetworkConfigurationException {
/*
        NetworkConfig.OrganizationConfig peerOrg1 = config.getOrganization(NETWORK_CONFIG_PEERORG);
        NetworkConfig.OrganizationConfig.CertificateAuthorityConfig certificateAuthority = peerOrg1.getCertificateAuthority(NETWORK_CONFIG_PEERORG_CA);

        NetworkConfig.NetworkConfigUser networkConfigRegistrar = certificateAuthority.getRegistrar(PEER_ADMIN_NAME);

        User admin = SAMPLE_STORE.getMember(PEER_ADMIN_NAME, NETWORK_CONFIG_PEERORG);
        admin.setEnrollmentSecret(networkConfigRegistrar.getEnrollSecret());
        admin.setEnrollmentSecret(PEER_ADMIN_SECRET);
        admin.setAffiliation(networkConfigRegistrar.getAffiliation());
        admin.setMspId(networkConfigRegistrar.getMspId());
*/

        // Get the details from the client organization
        // TODO: Note the camel-case inconsistency with "mspid" vs "mspId"!
        String mspId = getJsonValueAsString(clientOrganization.get("mspid"));

        JsonObject ca = null;

        JsonArray jsonCAs = getJsonValueAsArray(clientOrganization.get("certificateAuthorities"));
        if (jsonCAs != null) {
            // Use the first defined CA
            // TODO: What should we do if multiple CAs are defined?
            JsonValue val = jsonCAs.iterator().next();
            String caName = getJsonValueAsString(val);
            ca = certificateAuthorities.get(caName);
            if (ca == null) {
                throw new NetworkConfigurationException(format("Certificate Authority %s is not defined", caName));
            }
        }

        String enrollId = null;
        String enrollSecret = null;

        if (ca != null) {
            enrollId = getJsonValueAsString(ca.get("enrollId"));
            enrollSecret = getJsonValueAsString(ca.get("enrollSecret"));
        }


        //String adminPrivateKeyPem = null;
        String adminPrivateKeyPath = null;

        //String adminSignedCertPem = null;
        String adminSignedCertPath = null;

        JsonObject jsonKey = getJsonValueAsObject(clientOrganization.get("adminPrivateKey"));
        if (jsonKey != null) {
            //String pem = getJsonValueAsString(jsonKey.get("pem"));
            adminPrivateKeyPath = getJsonValueAsString(jsonKey.get("path"));
        }

        JsonObject jsonCert = getJsonValueAsObject(clientOrganization.get("signedCert"));
        if (jsonCert != null) {
            //String pem = getJsonValueAsString(jsonCert.get("pem"));
            adminSignedCertPath = getJsonValueAsString(jsonCert.get("path"));
        }

        UserInfo admin = new UserInfo(enrollId, enrollSecret, mspId);

        admin.setPrivateKeyFile(adminPrivateKeyPath);
        admin.setSignedCertFile(adminSignedCertPath);

        return admin;
    }

    public Set<CertificateAuthority> getPeerCertificateAuthorites(String peerName) {
        return null;
    }


    /**
     * Returns a channel configured using the details in the Network Configuration file
     *
     * @param client The associated client
     * @param channelName The name of the channel
     * @return A configured Channel instance
     */
    Channel loadChannel(HFClient client, String channelName) throws NetworkConfigurationException {

        if (logger.isTraceEnabled()) {
            logger.trace(format("NetworkConfig.loadChannel: %s", channelName));
        }

        Channel channel = null;

        JsonObject channels = getJsonObject(jsonConfig, "channels");

        if (channels != null) {
            JsonObject jsonChannel = getJsonObject(channels, channelName);
            if (jsonChannel != null) {
                channel = client.getChannel(channelName);
                if (channel != null) {
                    // The channel already exists in the client!
                    // Note that by rights this should never happen as HFClient.loadChannelFromConfig should have already checked for this!
                    throw new NetworkConfigurationException(format("Channel %s is already configured in the client!", channelName));
                }
                channel = reconstructChannel(client, channelName, jsonChannel);
            }

        }

        return channel;
    }

/*
    **
     * Returns a peer from the specified organization and having the desired role.
     * <p>
     * Note that if more than one peer matches the supplied attributes, it is arbitrary which peer will be returned.
     *
     * @param orgName The name of the organization (or null to use the client organization)
     * @param role The desired role (or null for any role)
     * @return A matching peer (or null if a suitable peer was not found)
     *
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
    private void createAllOrderers() throws NetworkConfigurationException {

        //orderers = new HashMap<String, Node>();
        orderers = new HashMap<>();

        // orderers is a JSON object containing a nested object for each orderers
        JsonObject jsonOrderers = getJsonObject(jsonConfig, "orderers");

        if (jsonOrderers != null) {

            for (Entry<String, JsonValue> entry : jsonOrderers.entrySet()) {
                String ordererName = entry.getKey();

                JsonObject jsonOrderer = getJsonValueAsObject(entry.getValue());
                if (jsonOrderer == null) {
                    // TODO: Determine appropriate exception handling...
                    throw new NetworkConfigurationException(format("Error loading config. Invalid orderer entry: %s", ordererName));
                }

                Node orderer = createNode(ordererName, jsonOrderer, "url");
                orderers.put(ordererName, orderer);
            }
        }

    }

    // Creates Node instances representing all the peers (and associated event hubs) defined in the config file
    private void createAllPeers() throws NetworkConfigurationException {

        peers = new HashMap<>();
        eventHubs = new HashMap<>();

        // peers is a JSON object containing a nested object for each peer
        JsonObject jsonPeers = getJsonObject(jsonConfig, "peers");

        //out("Peers: " + (jsonPeers == null ? "null" : jsonPeers.toString()));
        if (jsonPeers != null) {

            for (Entry<String, JsonValue> entry : jsonPeers.entrySet()) {
                String peerName = entry.getKey();

                JsonObject jsonPeer = getJsonValueAsObject(entry.getValue());
                if (jsonPeer == null) {
                    throw new NetworkConfigurationException(format("Error loading config. Invalid peer entry: %s", peerName));
                }

                Node peer = createNode(peerName, jsonPeer, "url");
                peers.put(peerName, peer);

                // Also create an event hub with the same name as the peer
                Node eventHub = createNode(peerName, jsonPeer, "eventUrl");
                eventHubs.put(peerName, eventHub);
            }
        }

    }

    // Creates JsonObjects representing all the CertificateAuthorities defined in the config file
    private void createAllCertificateAuthorities() throws NetworkConfigurationException {

        certificateAuthorities = new HashMap<>();

        // certificateAuthorities is a JSON object containing a nested object for each CA
        JsonObject jsonCertificateAuthorities = getJsonObject(jsonConfig, "certificateAuthorities");

        //out("Peers: " + (jsonPeers == null ? "null" : jsonPeers.toString()));
        if (jsonCertificateAuthorities != null) {

            for (Entry<String, JsonValue> entry : jsonCertificateAuthorities.entrySet()) {
                String caName = entry.getKey();

                JsonObject jsonCA = getJsonValueAsObject(entry.getValue());
                if (jsonCA == null) {
                    throw new NetworkConfigurationException(format("Error loading config. Invalid CA entry: %s", caName));
                }

                certificateAuthorities.put(caName, jsonCA);
            }
        }


/*
        // Certificate Authorities
        clientCertificateAuthorities = new HashMap<String, JsonObject>();

        JsonArray cas = getJsonValueAsArray(clientOrganization.get("certificateAuthorities"));
        if (cas != null) {
            for (JsonValue ca: cas) {
                String caName = getJsonValueAsString(ca);
                if (caName != null) {
                    JsonObject jsonCa = getCertificateAuthority(caName);
                    if (jsonCa == null) {
                        throw new RuntimeException("Error constructing channel " + channelName + ". Client Org Certificate Authority " + caName + " is not defined");
                    }
                    clientCertificateAuthorities.put(caName, jsonCa);
                }
            }
        }
*/
    }


    // Reconstructs an existing channel
    private Channel reconstructChannel(HFClient client, String channelName, JsonObject jsonChannel) throws NetworkConfigurationException {

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
                        throw new NetworkConfigurationException(format("Error constructing channel %s. Orderer %s not defined in configuration", channelName, ordererName));
                    }
                    channel.addOrderer(orderer);
                    foundOrderer = true;
                }
            }

            if (!foundOrderer) {
                // orderers is a required field
                throw new NetworkConfigurationException(format("Error constructing channel %s. At least one orderer must be specified", channelName));
            }

            // peers is an object containing a nested object for each peer
            JsonObject peers = getJsonObject(jsonChannel, "peers");
            boolean foundPeer = false;

            //out("Peers: " + (peers == null ? "null" : peers.toString()));
            if (peers != null) {

                for (Entry<String, JsonValue> entry : peers.entrySet()) {
                    String peerName = entry.getKey();

                    if (logger.isTraceEnabled()) {
                        logger.trace(format("NetworkConfig.reconstructChannel: Processing peer %s", peerName));
                    }

                    JsonObject jsonPeer = getJsonValueAsObject(entry.getValue());
                    if (jsonPeer == null) {
                        throw new NetworkConfigurationException(format("Error constructing channel %s. Invalid peer entry: %s", channelName, peerName));
                    }

                    Peer peer = getPeer(client, peerName);
                    if (peer == null) {
                        throw new NetworkConfigurationException(format("Error constructing channel %s. Peer %s not defined in configuration", channelName, peerName));
                    }

                    // Set the various roles
                    PeerOptions peerOptions = PeerOptions.create();
                    setPeerRole(channelName, peerOptions, peer, jsonPeer, PeerRole.ENDORSING_PEER);
                    setPeerRole(channelName, peerOptions, peer, jsonPeer, PeerRole.CHAINCODE_QUERY);
                    setPeerRole(channelName, peerOptions, peer, jsonPeer, PeerRole.LEDGER_QUERY);
                    setPeerRole(channelName, peerOptions, peer, jsonPeer, PeerRole.EVENT_SOURCE);

                    channel.addPeer(peer, peerOptions);

                    foundPeer = true;

                    // Add the event hub associated with this peer
                    EventHub eventHub = getEventHub(client, peerName);
                    if (eventHub == null) {
                        // By rights this should never happen!
                        throw new NetworkConfigurationException(format("Error constructing channel %s. EventHub for %s not defined in configuration", channelName, peerName));
                    }
                    channel.addEventHub(eventHub);

                }

            }

            if (!foundPeer) {
                // peers is a required field
                throw new NetworkConfigurationException(format("Error constructing channel %s. At least one peer must be specified", channelName));
            }


        } catch (InvalidArgumentException e) {
            throw new IllegalArgumentException(e);
        }

        return channel;
    }

    private void setPeerRole(String channelName, PeerOptions peerOptions, Peer peer, JsonObject jsonPeer, PeerRole role) throws NetworkConfigurationException {
        String propName = role.getPropertyName();
        JsonValue val = jsonPeer.get(propName);
        if (val != null) {
            Boolean isSet = getJsonValueAsBoolean(val);
            if (isSet == null) {
                // This is an invalid boolean value
                throw new NetworkConfigurationException(format("Error constructing channel %s. Role %s has invalid boolean value: %s", channelName, propName, val.toString()));
            }
            if (isSet) {
                peerOptions.addPeerRole(role);
            }
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
    private Node createNode(String nodeName, JsonObject jsonOrderer, String urlPropName) throws NetworkConfigurationException {

        String url = jsonOrderer.getString(urlPropName);

        Properties props = new Properties();

        // Extract the pem details
        JsonObject jsonTlsCaCerts = getJsonObject(jsonOrderer, "tlsCACerts");
        if (jsonTlsCaCerts != null) {
            String pemFilename = getJsonValueAsString(jsonTlsCaCerts.get("path"));
            String pemBytes = getJsonValueAsString(jsonTlsCaCerts.get("pem"));

            if (pemFilename != null && pemBytes != null) {
                throw new NetworkConfigurationException(format("Endpoint %s should not specify both tlsCACerts path and pem", nodeName));
            }

            if (pemFilename != null) {
                // Determine full pathname and ensure the file exists
                File pemFile = new File(pemFilename);
                String fullPathname = pemFile.getAbsolutePath();
                if (!pemFile.exists()) {
                    throw new NetworkConfigurationException(format("Endpoint %s: Pem file %s does not exist", nodeName, fullPathname));
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
    //private JsonObject getCertificateAuthority(String caName) {
    //    return getNamedObject(jsonConfig, "certificateAuthorities", caName);
    //}

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

        private String getName() {
            return name;
        }

        public String getUrl() {
            return url;
        }

        public Properties getProperties() {
            return properties;
        }


    }


    public static class UserInfo {

        private String enrollId;
        private String enrollSecret;
        private String mspId;
        private String privateKeyFile;
        private String signedCertFile;
        // TODO: Also add adminPrivateKeyBytes and signedCertBytes

        UserInfo(String enrollId, String enrollSecret, String mspId) {
            this.enrollId = enrollId;
            this.enrollSecret = enrollSecret;
            this.mspId = mspId;
        }

        public String getEnrollId() {
            return enrollId;
        }

        public String getEnrollSecret() {
            return enrollSecret;
        }

        public String getMspId() {
            return mspId;
        }

        public String getPrivateKeyFile() {
            return privateKeyFile;
        }

        public String getSignedCertFile() {
            return signedCertFile;
        }


        void setPrivateKeyFile(String keyFile) {
            privateKeyFile = keyFile;
        }

        void setSignedCertFile(String certFile) {
            signedCertFile = certFile;
        }
    }


    /**
    *
    * Holds the details of a Certificate Authority
    *
    */
    public static class CertificateAuthority {
        //private String caName;
        //private String url;
        //private UserInfo registrar;

    }

}
