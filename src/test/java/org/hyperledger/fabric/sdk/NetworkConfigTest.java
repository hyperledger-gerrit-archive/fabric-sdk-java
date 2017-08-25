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
import java.io.InputStream;
import java.io.Serializable;
import java.security.PrivateKey;
import java.util.Collection;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.commons.io.IOUtils;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric.sdkintegration.SampleStore;
import org.hyperledger.fabric.sdkintegration.SampleUser;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static java.lang.String.format;

public class NetworkConfigTest {

    static final String CHANNEL_NAME = "myChannel";

    @Rule
    public ExpectedException thrown = ExpectedException.none();


    @Test
    public void testLoadFromConfigNullStream() throws Exception {

        // Should not be able to instantiate a new instance of "Client" without a valid path to the configuration');
        thrown.expect(InvalidArgumentException.class);
        thrown.expectMessage("configStream must be specified");

        HFClient.loadFromConfig((InputStream) null);
    }

    @Test
    public void testLoadFromConfigNullFile() throws Exception {
        // Should not be able to instantiate a new instance of "Client" without a valid path to the configuration');
        thrown.expect(InvalidArgumentException.class);
        thrown.expectMessage("configFile must be specified");

        HFClient.loadFromConfig((File) null);
    }

    @Test
    public void testLoadFromConfigFileNotExists() throws Exception {

        // Should not be able to instantiate a new instance of "Client" without an actual configuration file
        thrown.expect(FileNotFoundException.class);
        thrown.expectMessage("FileDoesNotExist.json");

        File f = new File("FileDoesNotExist.json");
        HFClient.loadFromConfig(f);
    }

    // TODO: Needs a user context...
    @Ignore
    @Test
    public void testLoadFromConfigFileYaml() throws Exception {

        // Should be able to instantiate a new instance of "Client" with a valid path to the YAML configuration
        File f = new File("src/test/fixture/sdkintegration/e2e-2Orgs/channel/network-config.yaml");
        HFClient client = HFClient.loadFromConfig(f);
        Assert.assertNotNull(client);

        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());

        // TODO...
        //client.setUserContext(sampleOrg.getPeerAdmin());

        Channel channel = client.getChannel("mychannel");
        Assert.assertNotNull(channel);
    }

    // TODO: Needs a user context...
    @Ignore
    @Test
    public void testLoadFromConfigFileJson() throws Exception {

        // Should be able to instantiate a new instance of "Client" with a valid path to the JSON configuration
        File f = new File("src/test/fixture/sdkintegration/e2e-2Orgs/channel/network-config.json");
        HFClient client = HFClient.loadFromConfig(f);
        Assert.assertNotNull(client);

        client.setCryptoSuite(CryptoSuite.Factory.getCryptoSuite());
        client.setUserContext(new MockUser());

        Channel channel = client.getChannel("mychannel");
        Assert.assertNotNull(channel);
    }

    @Test
    public void testLoadFromConfigNoOrganization() throws Exception {

        // Should not be able to instantiate a new instance of "Channel" without specifying a valid client organization
        thrown.expect(InvalidArgumentException.class);
        thrown.expectMessage("client organization must be specified");

        JsonObject jsonConfig = getJsonConfig1(0, 1, 0);

        HFClient.loadFromConfig(jsonConfig);
    }

    @Test
    public void testNewChannel() throws Exception {

        // Should be able to instantiate a new instance of "Channel" with the definition in the network configuration'
        JsonObject jsonConfig = getJsonConfig1(1, 0, 0);

        HFClient client = HFClient.loadFromConfig(jsonConfig);
        TestHFClient.setupClient(client);

        // TODO: Do we need to modify client.newChannel() to use the network config???

        Channel channel = client.newChannel(CHANNEL_NAME);
        Assert.assertNotNull(channel);
        Assert.assertEquals(CHANNEL_NAME, channel.getName());
    }

    @Test
    public void testGetChannelNotExists() throws Exception {

        //thrown.expect(InvalidArgumentException.class);
        //thrown.expectMessage("Channel is not configured");

        // Should not be able to instantiate a new instance of "Channel" with an invalid channel name
        JsonObject jsonConfig = getJsonConfig1(1, 1, 1);

        HFClient client = HFClient.loadFromConfig(jsonConfig);
        Channel channel = client.getChannel("ThisChannelDoesNotExist");
        Assert.assertNull("Expected null to be returned for channels that are not configured", channel);

    }

    @Test
    public void testGetChannelNoOrderersOrPeers() throws Exception {

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Error constructing");

        // Should not be able to instantiate a new instance of "Channel" with no orderers or peers configured
        JsonObject jsonConfig = getJsonConfig1(1, 0, 0);

        HFClient client = HFClient.loadFromConfig(jsonConfig);
        TestHFClient.setupClient(client);

        client.getChannel(CHANNEL_NAME);
    }

    @Test
    public void testGetChannelNoOrderers() throws Exception {

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Error constructing");

        // Should not be able to instantiate a new instance of "Channel" with no orderers configured
        JsonObject jsonConfig = getJsonConfig1(1, 0, 1);

        HFClient client = HFClient.loadFromConfig(jsonConfig);
        TestHFClient.setupClient(client);


        client.getChannel(CHANNEL_NAME);

    }

    @Test
    public void testGetChannelNoPeers() throws Exception {

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Error constructing");

        // Should not be able to instantiate a new instance of "Channel" with no peers configured
        JsonObject jsonConfig = getJsonConfig1(1, 1, 0);

        HFClient client = HFClient.loadFromConfig(jsonConfig);
        TestHFClient.setupClient(client);


        client.getChannel(CHANNEL_NAME);

    }



    @Test
    public void testGetChannel() throws Exception {

        // Should be able to instantiate a new instance of "Channel" with orderer, org and peer defined in the network configuration
        JsonObject jsonConfig = getJsonConfig1(4, 1, 1);

        HFClient client = HFClient.loadFromConfig(jsonConfig);
        TestHFClient.setupClient(client);

        Channel channel = client.getChannel(CHANNEL_NAME);
        Assert.assertNotNull(channel);
        Assert.assertEquals(CHANNEL_NAME, channel.getName());

        Collection<Orderer> orderers = channel.getOrderers();
        Assert.assertNotNull(orderers);
        Assert.assertEquals(1, orderers.size());

        Orderer orderer = orderers.iterator().next();
        Assert.assertEquals("orderer1.example.com", orderer.getName());

        Collection<Peer> peers = channel.getPeers();
        Assert.assertNotNull(peers);
        Assert.assertEquals(1, peers.size());

        Peer peer = peers.iterator().next();
        Assert.assertEquals("peer0.org1.example.com", peer.getName());

    }



/*
        t.doesNotThrow(
            () => {
                var client = Client.loadFromConfig('test/fixtures/network.yaml');
                client.loadFromConfig({ version:'1.0.0', client : {organization : 'Org1'}});
                t.equals('Org1', client._network_config._network_config.client.organization, ' org should be Org1');
                client.loadFromConfig({ version:'1.0.0', client : {organization : 'Org2'}});
                t.equals('Org2', client._network_config._network_config.client.organization, ' org should be Org2');
                var channel = client.getChannel('mychannel');
            },
            null,
            '2 Should be able to instantiate a new instance of "Channel" with the definition in the network configuration'
        );

        t.doesNotThrow(
            () => {
                var config_loc = path.resolve('test/fixtures/network.yaml');
                var file_data = fs.readFileSync(config_loc);
                var network_data = yaml.safeLoad(file_data);
                var client = Client.loadFromConfig(network_data);
                var channel = client.newChannel('mychannel');
                client.loadFromConfig(network_data);
            },
            null,
            '3 Should be able to instantiate a new instance of "Channel" with the definition in the network configuration'
        );

        var network_config = {};

        t.doesNotThrow(
            () => {
                var client = new Client();
                client._network_config = new NetworkConfig(network_config, client);
                var channel = client.newChannel('mychannel');
            },
            null,
            'Should be able to instantiate a new instance of "Channel" with blank definition in the network configuration'
        );

*/







/*



    network_config.channels = {
        'mychannel' : {
            orderers : ['orderer0']
        }
    };

    network_config.orderers = {
        'orderer0' : {
            url : 'grpcs://localhost:7050',
            'tlsCACerts' : {
                path : 'test/fixtures/channel/crypto-config/ordererOrganizations/example.com/orderers/orderer.example.com/tlscacerts/example.com-cert.pem'
            }
        }
    };


    t.doesNotThrow(
        () => {
            var client = new Client();
            client._network_config = new NetworkConfig(network_config, client);
            var channel = client.getChannel('mychannel');
            t.equals('mychannel',channel.getName(),'Channel should be named');
            var orderer = channel.getOrderers()[0];
            if(orderer instanceof Orderer) t.pass('Successfully got an orderer');
            else t.fail('Failed to get an orderer');
        },
        null,
        'Should be able to instantiate a new instance of "Channel" with only orderer definition in the network configuration'
    );
*/



/*
    network_config.channels = {
        'mychannel' : {
            peers : {
                peer1 : {},
                peer2 : {},
                peer3 : {},
                peer4 : {}
            },
            orderers : ['orderer0']
        }
    };
    network_config.orgainizations = { 'org1' : {} };

    t.doesNotThrow(
        () => {
            var client = new Client();
            client._network_config = new NetworkConfig(network_config, client);
            var channel = client.getChannel('mychannel');
            t.equals('mychannel',channel.getName(),'Channel should be named');
            t.equals(channel.getPeers().length, 0, 'Peers should be empty');
            var orderer = channel.getOrderers()[0];
            if(orderer instanceof Orderer) t.pass('Successfully got an orderer');
            else t.fail('Failed to get an orderer');
        },
        null,
        TODO: Why should this not be an error???
        'Should be able to instantiate a new instance of "Channel" with org that does not exist in the network configuration'
    );

    network_config.organizations = {
        'org1' : {
            mspid : 'mspid1',
            peers : ['peer1','peer2'],
            certificateAuthorities : ['ca1']
        },
        'org2' : {
            mspid : 'mspid2',
            peers : ['peer3','peer4'],
            certificateAuthorities : ['ca2']
        }
    };

    t.doesNotThrow(
        () => {
            var client = new Client();
            client._network_config = new NetworkConfig(network_config, client);
            var channel = client.getChannel('mychannel');
            t.equals('mychannel',channel.getName(),'Channel should be named');
            t.equals(channel.getPeers().length, 0, 'Peers should be empty');
            var orderer = channel.getOrderers()[0];
            if(orderer instanceof Orderer) t.pass('Successfully got an orderer');
            else t.fail('Failed to get an orderer');
        },
        null,
        TODO: Why should this not be an error???
        'Should be able to instantiate a new instance of "Channel" with a peer in the org that does not exist in the network configuration'
    );

    network_config.peers = {
        'peer1' : {
            url : 'grpcs://localhost:7051',
            'tlsCACerts' : {
                pem : '-----BEGIN CERTIFICATE-----MIIB8TCC5l-----END CERTIFICATE-----'
            }
        },
        'peer2' : {
            url : 'grpcs://localhost:7052',
            'tlsCACerts' : {
                path : 'test/fixtures/channel/crypto-config/ordererOrganizations/example.com/orderers/orderer.example.com/tlscacerts/example.com-cert.pem'
            }
        },
        'peer3' : {
            url : 'grpcs://localhost:7053',
            'tlsCACerts' : {
                path : 'test/fixtures/channel/crypto-config/ordererOrganizations/example.com/orderers/orderer.example.com/tlscacerts/example.com-cert.pem'
            }
        },
        'peer4' : {
            url : 'grpcs://localhost:7054',
            'tlsCACerts' : {
                path : 'test/fixtures/channel/crypto-config/ordererOrganizations/example.com/orderers/orderer.example.com/tlscacerts/example.com-cert.pem'
            }
        },
    };

    network_config.certificateAuthorities = {
        'ca1' : {
            url : 'https://localhost:7051',
            'tlsCACerts' : {
                pem : '-----BEGIN CERTIFICATE-----MIIB8TCC5l-----END CERTIFICATE-----'
            },
            grpcOptions : { verify : true},
            registrar : { enrollId: 'admin1', enrollSecret: 'adminpw1'}
        },
        'ca2' : {
            url : 'https://localhost:7052',
            'tlsCACerts' : {
                path : 'test/fixtures/channel/crypto-config/ordererOrganizations/example.com/orderers/orderer.example.com/tlscacerts/example.com-cert.pem'
            },
            grpcOptions : { verify : true},
            registrar : { enrollId: 'admin2', enrollSecret: 'adminpw2' }
        }
    };
    t.doesNotThrow(
        () => {
            var client = new Client();
            client._network_config = new NetworkConfig(network_config, client);
            var channel = client.getChannel('mychannel');
            t.equals('mychannel',channel.getName(),'Channel should be named');
            t.equals(channel.getPeers().length, 4, 'Peers should be four');
            var peer =  channel.getPeers()[0];
            if(peer instanceof Peer) t.pass('Successfully got a peer');
            else t.fail('Failed to get a peer');
        },
        null,
        'Should be able to instantiate a new instance of "Channel" with orderer, org and peer defined in the network configuration'
    );

    var peer1 = new Peer('grpcs://localhost:9999', {pem : '-----BEGIN CERTIFICATE-----MIIB8TCC5l-----END CERTIFICATE-----'});

    t.doesNotThrow(
        () => {
            var client = new Client();
            client._network_config = new NetworkConfig(network_config, client);

            var targets = client_utils.getTargets('peer1', client);
            if(Array.isArray(targets)) t.pass('targets is an array');
            else t.fail('targets is not an array');
            if(targets[0] instanceof Peer) t.pass('targets has a peer ');
            else t.fail('targets does not have a peer');

            var targets = client_utils.getTargets(['peer1'], client);
            if(Array.isArray(targets)) t.pass('targets is an array');
            else t.fail('targets is not an array');
            if(targets[0] instanceof Peer) t.pass('targets has a peer ');
            else t.fail('targets does not have a peer');

            var targets = client_utils.getTargets(peer1, client);
            if(Array.isArray(targets)) t.pass('targets is an array');
            else t.fail('targets is not an array');
            if(targets[0] instanceof Peer) t.pass('targets has a peer ');
            else t.fail('targets does not have a peer');

            var targets = client_utils.getTargets([peer1], client);
            if(Array.isArray(targets)) t.pass('targets is an array');
            else t.fail('targets is not an array');
            if(targets[0] instanceof Peer) t.pass('targets has a peer ');
            else t.fail('targets does not have a peer');

        },
        null,
        'Should be able to get targets for peer'
    );

    t.doesNotThrow(
        () => {
            var client = new Client();
            client._network_config = new NetworkConfig({}, client);
            var targets = client_utils.getTargets(null, client);
            t.equals(null, targets, 'targets should be null when request targets is null');
        },
        null,
        'Should return null targets when checking a null request target'
    );

    t.throws(
        () => {
            var client = new Client();
            client._network_config = new NetworkConfig({}, client);
            var targets = client_utils.getTargets({}, client);
        },
        /Target peer is not a valid peer object instance/,
        'Should not be able to get targets when targets is not a peer object'
    );

    t.throws(
        () => {
            var client = new Client();
            client._network_config = new NetworkConfig({}, client);
            var targets = client_utils.getTargets('somepeer', client);
        },
        /Target peer name was not found/,
        'Should not be able to get targets when targets is not a peer object'
    );

    t.doesNotThrow(
        () => {
            var client = new Client();
            client._network_config = new NetworkConfig({}, client);
            var targets = client_utils.getTargets([], client);
            t.equals(null, targets, 'targets should be null when list is empty');
        },
        null,
        'Should get a null when the target list is empty'
    );

    t.doesNotThrow(
            () => {
                var client = new Client();
                var network_config = new NetworkConfig(network_config, client);
                var client_config = network_config.getClientConfig();
                if(typeof client_config.credentialStore === 'undefined') {
                    t.pass('client config should be empty');
                } else {
                    t.fail('client config is not correct');
                }
                network_config.client = {};
                network_config.client.credentialStore = {path : '/tmp/something', cryptoStore : { path : 'relative/something'}};
                var network_config_impl = new NetworkConfig(network_config, client);
                client_config = network_config_impl.getClientConfig();
                t.equals(client_config.credentialStore.path, '/tmp/something','client config store path should be something');
                if(client_config.credentialStore.cryptoStore.path.indexOf('relative/something') > 1) {
                    t.pass('client config cryptoStore store path should be something relative');
                } else {
                    t.fail('client config cryptoStore store path should be something relative');
                }
                network_config.client.credentialStore = {dbsetting : '/tmp/something', cryptoStore : { dbsetting : 'relative/something'}};
                network_config_impl = new NetworkConfig(network_config, client);
                client_config = network_config_impl.getClientConfig();
                t.equals(client_config.credentialStore.dbsetting, '/tmp/something','client config store path should be something');
                t.equals(client_config.credentialStore.cryptoStore.dbsetting, 'relative/something','client config cryptoStore store path should be something');

            },
            null,
            'Should not get an error'
        );

    t.doesNotThrow(
        () => {
            var client = new Client();
            client._network_config = new NetworkConfig(network_config, client);
            var organizations = client._network_config.getOrganizations();
            if(Array.isArray(organizations)) t.pass('organizations is an array');
            else t.fail('organizations is not an array');
            if(organizations[0] instanceof Organization) t.pass('organizations has a organization ');
            else t.fail('organizations does not have a organization');
            var organization = client._network_config.getOrganization(organizations[0].getName());
            var ca = organization.getCertificateAuthorities()[0];
            t.equals('ca1',ca.getName(),'check the ca name');
            organization = client._network_config.getOrganization(organizations[1].getName());
            ca = organization.getCertificateAuthorities()[0];
            t.equals('ca2',ca.getName(),'check the ca name');
        },
        null,
        'Should be able to get organizations'
    );

    t.doesNotThrow(
        () => {
            var client = Client.loadFromConfig(network_config);
            var channel = client.getChannel('mychannel');
            var targets = channel._getTargetsFromConfig(); //all roles
            if(Array.isArray(targets)) t.pass('targets is an array');
            else t.fail('targets is not an array');
            if(targets[0] instanceof Peer) t.pass('targets has a peer ');
            else t.fail('targets does not have a peer');
            t.equals(2,targets.length,'Should have two targets in the list');

        },
        null,
        'Should be able to get targets for channel'
    );

    t.throws(
        () => {
            var client = new Client();
            var channel = client.newChannel('test');
            var targets = channel._getTargetsFromConfig('bad');
        },
        /Target role is unknown/,
        'Should get an error when the role is bad'
    );

    network_config.channels = {
        'mychannel' : {
            peers : {
                peer1: {endorsingPeer:false, chaincodeQuery:false, ledgerQuery:false},
                peer2 : {endorsingPeer:false, chaincodeQuery:false, ledgerQuery:false},
                peer3 : {ledgerQuery:true},
                peer4 : {ledgerQuery:false}
            },
            orderers : ['orderer0']
        }
    };

    t.doesNotThrow(
        () => {
            var client = Client.loadFromConfig(network_config);
            var channel = client.getChannel('mychannel');
            var targets = channel._getTargetsFromConfig('chaincodeQuery');
            t.equals(1,targets.length,'Should have one target in the list');

            checkTarget(channel._getTargetForQuery(), '7053', 'finding a default ledger query', t);
            checkTarget(channel._getTargets(null, 'ledgerQuery'), '7053', 'finding a default ledger query', t);
            checkTarget(channel._getTargetForQuery('peer1'), '7051', 'finding a string target for ledger query', t);
            checkTarget(channel._getTargets('peer1'), '7051', 'finding a string target', t);
            checkTarget(channel._getTargetForQuery(['peer1']), 'array', 'should get an error back when passing an array', t);
            checkTarget(channel._getTargetForQuery(['peer1']), 'array', 'should get an error back when passing an array', t);
            checkTarget(channel._getTargets('bad'), 'found', 'should get an error back when passing a bad name', t);
            checkTarget(channel._getTargetForQuery(peer1), '9999', 'should get back the same target if a good peer', t);
            checkTarget(channel._getTargets(peer1), '9999', 'should get back the same target if a good peer', t);
            client._network_config = null;
            checkTarget(channel._getTargetForQuery(), '7051', 'finding a default ledger query without networkconfig', t);
            checkTarget(channel._getTargets(), '7051', 'finding a default targets without networkconfig', t);
        },
        null,
        'Should be able to run channel target methods'
    );


    t.throws(
        () => {
            var client = new Client();
            client._network_config = new NetworkConfig({}, client);
            var targets = client_utils.getOrderer('someorderer', 'somechannel', client);
        },
        /Orderer name was not found in the network configuration/,
        'Should get an error when the request orderer name is not found'
    );

    t.throws(
        () => {
            var client = new Client();
            client._network_config = new NetworkConfig({}, client);
            var targets = client_utils.getOrderer({}, 'somechannel', client);
        },
        /request parameter is not valid/,
        'Should get an error when the request orderer is not a valid object'
    );

    t.throws(
        () => {
            var client = new Client();
            client._network_config = new NetworkConfig({ channels : {somechannel : {}}}, client);
            var targets = client_utils.getOrderer(null, 'somechannel', client);
        },
        /"orderer" request parameter is missing and there is no orderer defined on this channel in the network configuration/,
        'Should get an error when the request orderer is not defined and the channel does not have any orderers'
    );

    t.doesNotThrow(
        () => {
            var client = new Client();
            client._network_config = new NetworkConfig(network_config, client);

            var orderer = client_utils.getOrderer('orderer0', null, client);
            if(orderer instanceof Orderer) t.pass('orderer has a orderer ');
            else t.fail('orderer does not have a orderer');

            var orderer1 = new Orderer('grpcs://localhost:9999', {pem : '-----BEGIN CERTIFICATE-----MIIB8TCC5l-----END CERTIFICATE-----'});

            orderer = client_utils.getOrderer(orderer1, null, client);
            if(orderer instanceof Orderer) t.pass('orderer has a orderer ');
            else t.fail('orderer does not have a orderer');

            orderer = client_utils.getOrderer(null, 'mychannel', client);
            if(orderer instanceof Orderer) t.pass('orderer has a orderer ');
            else t.fail('orderer does not have a orderer');
        },
        null,
        'Should be able to get orderer'
    );

    t.doesNotThrow(
        () => {
            var client = Client.loadFromConfig(network_config);
            let channel = client.getChannel('mychannel');
            client.loadFromConfig({ version: '1.0.0',
                channels : {
                    'otherchannel' : {
                        orderers : ['orderer0']
                    }
                }
            });
            let otherchannel = client.getChannel('otherchannel');
        },
        null,
        'Should be able to load additional configurations'
    );

    t.doesNotThrow(
        () => {
            var organization = new Organization('name', 'mspid');
            t.equals('name',organization.getName(), 'check name');
            t.equals('mspid',organization.getMspid(), 'check mspid');
            organization.addPeer('peer');
            t.equals('peer',organization.getPeers()[0], 'check getPeers');
            organization.addCertificateAuthority('ca');
            t.equals('ca',organization.getCertificateAuthorities()[0], 'check getPeers');
            t.comment(organization.toString());
        },
        null,
        'Should be able to run all methods of Organization'
    );

    t.doesNotThrow(
        () => {
            var certificateAuthority = new CertificateAuthority('name', 'url', 'connection', 'tlscert', 'registrar');
            t.equals('name',certificateAuthority.getName(), 'check name');
            t.equals('url',certificateAuthority.getUrl(), 'check url');
            t.equals('connection',certificateAuthority.getConnectionOptions(), 'check connection options');
            t.equals('tlscert',certificateAuthority.getTlsCACerts(), 'check tlscert');
            t.equals('registrar',certificateAuthority.getRegistrar(), 'check registrar');
            t.comment(certificateAuthority.toString());
        },
        null,
        'Should be able to run all methods of CertificateAuthority'
    );

    var client = new Client();
    var p1 = client.setStoresFromConfig().then(function () {
        t.fail('p1 Should not have been able to resolve the promise');
    }).catch(function (err) {
        if (err.message.indexOf('No network configuration settings found') >= 0) {
            t.pass('p1 Successfully caught error');
        } else {
            t.fail('p1 Failed to catch error. Error: ' + err.stack ? err.stack : err);
        }
    });
    Promise.all([p1])
    .then(
        function (data) {
            t.end();
        }
    ).catch(
        function (err) {
            t.fail('Channel query calls, Promise.all: ');
            console.log(err.stack ? err.stack : err);
            t.end();
        }
    );

    t.throws(
        () => {
            var client = new Client();
            client.setAdminFromConfig();
        },
        /No network configuration has been loaded/,
        'Should get an error No network configuration has been loaded'
    );

    t.throws(
        () => {
            var client = new Client();
            client.setAdminSigningIdentity();
        },
        /Invalid parameter. Must have a valid private key./,
        'Should get an error Invalid parameter. Must have a valid private key.'
    );

    t.throws(
        () => {
            var client = new Client();
            client.setAdminSigningIdentity('privateKey');
        },
        /Invalid parameter. Must have a valid certificate./,
        'Should get an error Invalid parameter. Must have a valid certificate.'
    );

    t.throws(
        () => {
            var client = new Client();
            client.setAdminSigningIdentity('privateKey','cert');
        },
        /Invalid parameter. Must have a valid mspid./,
        'Should get an error Invalid parameter. Must have a valid mspid.'
    );

    t.throws(
        () => {
            var client = new Client();
            client.setAdminSigningIdentity('privateKey','cert', 'msipid');
        },
        /A crypto suite must be assigned to this client/,
        'Should get an error A crypto suite must be assigned to this client'
    );

    t.throws(
        () => {
            var client = new Client();
            client.getAdminSigningIdentity();
        },
        /No identity has been assigned to this client/,
        'Should get an error No identity has been assigned to this client'
    );


    var client = Client.loadFromConfig('test/fixtures/network.yaml');
    t.pass('Successfully loaded a network configuration');

    var p1 = client.setStoresFromConfig().then(()=> {
        t.pass('Should be able to load the stores from the config');
        client.setAdminFromConfig();
        t.pass('Should be able to load an admin from the config');
        client.getAdminSigningIdentity();
        t.pass('Should be able to get the loaded admin identity');
    }).catch(function (err) {
        t.fail('Should not get an error');
    });

    var p2 = client.setStoresFromConfig().then(()=> {
        t.pass('Should be able to load the stores from the config');
        client._network_config._network_config.client = {};
        client.setAdminFromConfig();
        t.fail('Should not be able to load an admin from the config');
    }).catch(function (err) {
        if (err.message.indexOf('No admin defined for the current organization') >= 0) {
            t.pass('Successfully caught No admin defined for the current organization');
        } else {
            t.fail('Failed to catch No admin defined for the current organization');
            console.log(err.stack ? err.stack : err);
        }
    });

    var p3 = client.setStoresFromConfig().then(()=> {
        t.pass('Should be able to load the stores from the config');
        client._network_config._network_config.client = {};
        return client.setUserFromConfig();
    }).then((user)=>{
        t.fail('Should not be able to load an user based on the config');
    }).catch(function (err) {
        if (err.message.indexOf('Missing parameter. Must have a username.') >= 0) {
            t.pass('Successfully caught Missing parameter. Must have a username.');
        } else {
            t.fail('Failed to catch Missing parameter. Must have a username.');
            console.log(err.stack ? err.stack : err);
        }
    });

    var p4 = client.setStoresFromConfig().then(()=> {
        t.pass('Should be able to load the stores from the config');
        client._network_config._network_config.client = {};
        return client.setUserFromConfig('username');
    }).then((user)=>{
        t.fail('Should not be able to load an user based on the config');
    }).catch(function (err) {
        if (err.message.indexOf('Missing parameter. Must have a password.') >= 0) {
            t.pass('Successfully caught Missing parameter. Must have a password.');
        } else {
            t.fail('Failed to catch Missing parameter. Must have a password.');
            console.log(err.stack ? err.stack : err);
        }
    });

    var other_client = new Client();
    var p5 = other_client.setUserFromConfig('username', 'password').then(()=> {
        t.fail('Should not be able to load an user based on the config');
    }).catch(function (err) {
        if (err.message.indexOf('Client requires a network configuration loaded, stores attached, and crypto suite.') >= 0) {
            t.pass('Successfully caught Client requires a network configuration loaded, stores attached, and crypto suite.');
        } else {
            t.fail('Failed to catch Client requires a network configuration loaded, stores attached, and crypto suite.');
            console.log(err.stack ? err.stack : err);
        }
    });

    Promise.all([p1,p2, p3, p4, p5])
    .then(
        function (data) {
            t.end();
        }
    ).catch(
        function (err) {
            t.fail('Client network config calls failed during the Promise.all');
            console.log(err.stack ? err.stack : err);
            t.end();
        }
    );

    t.end();
});


 */





















/*

    @Test
    public void testLoadFromConfig99Old() throws Exception {

        JsonObjectBuilder mainConfig = Json.createObjectBuilder();
        mainConfig.add("name", "myNetwork");
        mainConfig.add("description", "My Test Network");
        mainConfig.add("x-type", "hlf@^1.0.0");
        mainConfig.add("version", "1.0.0");


        JsonObject channel1 = createJsonChannel(
                createJsonArray("orderer.example.com"),
                createJsonArray(
                        createJsonChannelPeer("Org1", true, true, true, true),
                        createJsonChannelPeer("Org2", true, false, true, false)),
                createJsonArray("example02:v1", "marbles:1.0")
        );

        JsonObject channels = Json.createObjectBuilder()
                .add("mychannel", channel1)
                .build();

        mainConfig.add("channels", channels);


        JsonObject org1 = createJsonOrg(
                "Org1MSP",
                createJsonArray("peer0.org1.example.com"),
                createJsonArray("ca-org1"),
                createJsonArray(createJsonUser("admin", "adminpw")),
                "-----BEGIN PRIVATE KEY----- <etc>",
                "-----BEGIN CERTIFICATE----- <etc>"
        );

        JsonObject org2 = createJsonOrg(
                "Org2MSP",
                createJsonArray("peer0.org2.example.com"),
                createJsonArray("ca-org2"),
                createJsonArray(createJsonUser("admin2", "adminpw2")),
                "-----BEGIN PRIVATE KEY----- <etc>",
                "-----BEGIN CERTIFICATE----- <etc>"
        );

        mainConfig.add("organizations", createJsonArray(org1, org2));

        JsonObject orderer1 = createJsonOrderer(
                "grpcs://localhost:7050",
                Json.createObjectBuilder()
                    .add("ssl-target-name-override", "orderer.example.com")
                    .build(),
                Json.createObjectBuilder()
                    .add("pem", "-----BEGIN CERTIFICATE----- <etc>")
                    .build()
        );

        mainConfig.add("orderers", createJsonArray(orderer1));


        JsonObject peer1 = createJsonPeer(
                "grpcs://localhost:7051",
                "grpcs://localhost:7053",
                Json.createObjectBuilder()
                    .add("ssl-target-name-override", "peer0.org1.example.com")
                    .build(),
                Json.createObjectBuilder()
                    .add("path", "test/fixtures/channel/crypto-config/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tlscacerts/org1.example.com-cert.pem")
                    .build(),
                    createJsonArray("mychannel")
        );

        JsonObject peer2 = createJsonPeer(
                "grpcs://localhost:8051",
                "grpcs://localhost:8053",
                Json.createObjectBuilder()
                    .add("ssl-target-name-override", "peer0.org2.example.com")
                    .build(),
                Json.createObjectBuilder()
                    .add("path", "test/fixtures/channel/crypto-config/peerOrganizations/org2.example.com/peers/peer0.org2.example.com/tlscacerts/org2.example.com-cert.pem")
                    .build(),
                    createJsonArray()
        );


        mainConfig.add("peers", createJsonArray(peer1, peer2));
        //mainConfig.add("certificateAuthorities", x);


        JsonObject rec = mainConfig.build();

        String config = rec.toString();

        InputStream stream = new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8));

        HFClient client = HFClient.loadFromConfig(stream);
        Assert.assertTrue(client != null);
    }
*/

    private JsonObject getJsonConfig1(int nOrganizations, int nOrderers, int nPeers) {

        // Sanity check
        if (nPeers > nOrganizations) {
            // To keep things simple we require a maximum of 1 peer per organization
            throw new RuntimeException("Number of peers cannot exceed number of organizations!");
        }

        JsonObjectBuilder mainConfig = Json.createObjectBuilder();
        mainConfig.add("name", "myNetwork");
        mainConfig.add("description", "My Test Network");
        mainConfig.add("x-type", "hlf@^1.0.0");
        mainConfig.add("version", "1.0.0");

        JsonObjectBuilder client = Json.createObjectBuilder();
        if (nOrganizations > 0) {
            client.add("organization", "Org1");
        }
        mainConfig.add("client", client);

        JsonArray orderers = nOrderers > 0 ? createJsonArray("orderer1.example.com") : null;
        JsonArray chaincodes = (nOrderers > 0 && nPeers > 0) ? createJsonArray("example02:v1", "marbles:1.0") : null;

        JsonObject peers = null;
        if (nPeers > 0) {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            builder.add("peer0.org1.example.com", createJsonChannelPeer("Org1", true, true, true, true));
            if (nPeers > 1) {
                builder.add("peer0.org2.example.com", createJsonChannelPeer("Org2", true, false, true, false));
            }
            peers = builder.build();
        }

        JsonObject channel1 = createJsonChannel(
                orderers,
                peers,
                chaincodes
        );

        String channelName = CHANNEL_NAME;

        JsonObject channels = Json.createObjectBuilder()
                .add(channelName, channel1)
                .build();

        mainConfig.add("channels", channels);

        if (nOrganizations > 0) {

            // Add some organizations to the config
            JsonObjectBuilder builder = Json.createObjectBuilder();

            for (int i = 1; i <= nOrganizations; i++) {
                String orgName = "Org" + i;
                JsonObject org = createJsonOrg(
                        orgName + "MSP",
                        createJsonArray("peer0.org" + i + ".example.com"),
                        createJsonArray("ca-org" + i),
                        createJsonArray(createJsonUser("admin" + i, "adminpw" + i)),
                        "-----BEGIN PRIVATE KEY----- <etc>",
                        "-----BEGIN CERTIFICATE----- <etc>"
                );
                builder.add(orgName, org);
            }

            mainConfig.add("organizations", builder.build());

/*
            JsonObject org1 = createJsonOrg(
                    "Org1MSP",
                    createJsonArray("peer0.org1.example.com"),
                    createJsonArray("ca-org1"),
                    createJsonArray(createJsonUser("admin", "adminpw")),
                    "-----BEGIN PRIVATE KEY----- <etc>",
                    "-----BEGIN CERTIFICATE----- <etc>"
            );

            JsonObject org2 = createJsonOrg(
                    "Org2MSP",
                    createJsonArray("peer0.org2.example.com"),
                    createJsonArray("ca-org2"),
                    createJsonArray(createJsonUser("admin2", "adminpw2")),
                    "-----BEGIN PRIVATE KEY----- <etc>",
                    "-----BEGIN CERTIFICATE----- <etc>"
            );

            mainConfig.add("organizations", createJsonArray(org1, org2));
*/
        }

        if (nOrderers > 0) {
            // Add some orderers to the config
            JsonObjectBuilder builder = Json.createObjectBuilder();

            for (int i = 1; i <= nOrderers; i++) {
                String ordererName = "orderer" + i + ".example.com";
                int port = (6 + i) * 1000 + 50;         // 7050, 8050, etc
                JsonObject orderer = createJsonOrderer(
                        "grpcs://localhost:" + port,
                        Json.createObjectBuilder()
                            .add("ssl-target-name-override", "orderer" + i + ".example.com")
                            .build(),
                        Json.createObjectBuilder()
                            .add("pem", "-----BEGIN CERTIFICATE----- <etc>")
                            .build()
                );
                builder.add(ordererName, orderer);
            }
            mainConfig.add("orderers", builder.build());
        }


        if (nPeers > 0) {
            // Add some peers to the config
            JsonObjectBuilder builder = Json.createObjectBuilder();

            for (int i = 1; i <= nPeers; i++) {
                String peerName = "peer0.org" + i + ".example.com";

                int port1 = (6 + i) * 1000 + 51;         // 7051, 8051, etc
                int port2 = (6 + i) * 1000 + 53;         // 7053, 8053, etc

                int orgNo = i;
                int peerNo = 0;

                JsonObject peer = createJsonPeer(
                        "grpcs://localhost:" + port1,
                        "grpcs://localhost:" + port2,
                        Json.createObjectBuilder()
                            .add("ssl-target-name-override", "peer" + peerNo + ".org" + orgNo + ".example.com")
                            .build(),
                        Json.createObjectBuilder()
                            .add("path", "test/fixtures/channel/crypto-config/peerOrganizations/org" + orgNo + ".example.com/peers/peer" + peerNo + ".org" + orgNo + ".example.com/tlscacerts/org" + orgNo + ".example.com-cert.pem")
                            .build(),
                            createJsonArray(channelName)
                );
                builder.add(peerName, peer);
            }
            mainConfig.add("peers", builder.build());
        }

        JsonObject config = mainConfig.build();

        out("JsonConfig = " + config.toString());

        return config;
    }







    private JsonObject createJsonChannelPeer(String name, Boolean endorsingPeer, Boolean chaincodeQuery, Boolean ledgerQuery, Boolean eventSource) {

        return Json.createObjectBuilder()
            .add("name", name)
            .add("endorsingPeer", endorsingPeer)
            .add("chaincodeQuery", chaincodeQuery)
            .add("ledgerQuery", ledgerQuery)
            .add("eventSource", eventSource)
            .build();
    }

    private JsonObject createJsonChannel(JsonArray orderers, JsonObject peers, JsonArray chaincodes) {

        JsonObjectBuilder builder = Json.createObjectBuilder();

        if (orderers != null) {
            builder.add("orderers", orderers);
        }

        if (peers != null) {
            builder.add("peers", peers);
        }

        if (chaincodes != null) {
            builder.add("chaincodes", chaincodes);
        }

        return builder.build();
    }

    private JsonObject createJsonOrg(String mspid, JsonArray peers, JsonArray certificateAuthorities, JsonArray users, String adminPrivateKeyPem, String signedCertPem) {

        return Json.createObjectBuilder()
                .add("mspid", mspid)
                .add("peers", peers)
                // TODO: Add certificateAuthorities
                //.add("certificateAuthorities", certificateAuthorities)
                .add("users", users)
                .add("adminPrivateKeyPEM", adminPrivateKeyPem)
                .add("signedCertPEM", signedCertPem)
                .build();
    }

    private JsonObject createJsonUser(String enrollId, String enrollSecret) {

        return Json.createObjectBuilder()
                .add("enrollId", enrollId)
                .add("enrollSecret", enrollSecret)
                .build();
    }


    private JsonObject createJsonOrderer(String url, JsonObject grpcOptions, JsonObject tlsCaCerts) {

        return Json.createObjectBuilder()
                .add("url", url)
                .add("grpcOptions", grpcOptions)
                .add("tlsCaCerts", tlsCaCerts)
                .build();
    }

    private JsonObject createJsonPeer(String url, String eventUrl, JsonObject grpcOptions, JsonObject tlsCaCerts, JsonArray channels) {

        return Json.createObjectBuilder()
            .add("url", url)
            .add("eventUrl", eventUrl)
            .add("grpcOptions", grpcOptions)
            .add("tlsCaCerts", tlsCaCerts)
            .add("channels", channels)
            .build();
    }

    private JsonArray createJsonArray() {
        return Json.createArrayBuilder().build();
    }

    private JsonArray createJsonArray(String... elements) {

        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (String ele: elements) {
            builder.add(ele);
        }

        return builder.build();
    }

    private JsonArray createJsonArray(JsonValue... elements) {

        JsonArrayBuilder builder = Json.createArrayBuilder();

        for (JsonValue ele: elements) {
            builder.add(ele);
        }

        return builder.build();
    }


    static void out(String format, Object... args) {

        System.err.flush();
        System.out.flush();

        System.out.println(format(format, args));
        System.err.flush();
        System.out.flush();

    }

    private class MockUser implements User {
        //private static final long serialVersionUID = 8077132186383604355L;

        private Enrollment enrollment = new MockEnrollment();

        private String keyValStoreName;

        MockUser() {
        }

        @Override
        public String getName() {
            return "me";
        }

        @Override
        public Set<String> getRoles() {
            return null;
        }

        @Override
        public String getAccount() {
            return "account";
        }

        @Override
        public String getAffiliation() {
            return "affiliation";
        }

        @Override
        public Enrollment getEnrollment() {
            return enrollment;
        }




        @Override
        public String getMspId() {
            return "mspid";
        }

    }

    private class MockEnrollment implements Enrollment {

        @Override
        public PrivateKey getKey() {
            return null;
        }

        @Override
        public String getCert() {
            return "cert";
        }

    }


}
