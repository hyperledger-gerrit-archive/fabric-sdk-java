package org.hyperledger.fabric.sdkintegration;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.hamcrest.Matchers;
import org.hyperledger.fabric.protos.common.MspPrincipal.MSPPrincipal;
import org.hyperledger.fabric.protos.common.MspPrincipal.MSPRole;
import org.hyperledger.fabric.protos.common.MspPrincipal.MSPRole.MSPRoleType;
import org.hyperledger.fabric.protos.common.Policies.SignaturePolicy;
import org.hyperledger.fabric.protos.common.Policies.SignaturePolicy.NOutOf;
import org.hyperledger.fabric.protos.common.Policies.SignaturePolicyEnvelope;
import org.hyperledger.fabric.sdk.BlockEvent.TransactionEvent;
import org.hyperledger.fabric.sdk.Chain;
import org.hyperledger.fabric.sdk.ChainCodeID;
import org.hyperledger.fabric.sdk.ChainCodeResponse;
import org.hyperledger.fabric.sdk.ChainCodeResponse.Status;
import org.hyperledger.fabric.sdk.ChainConfiguration;
import org.hyperledger.fabric.sdk.ChaincodeEndorsementPolicy;
import org.hyperledger.fabric.sdk.Enrollment;
import org.hyperledger.fabric.sdk.HFClient;
import org.hyperledger.fabric.sdk.InstallProposalRequest;
import org.hyperledger.fabric.sdk.InstantiateProposalRequest;
import org.hyperledger.fabric.sdk.InvokeProposalRequest;
import org.hyperledger.fabric.sdk.MemberServices;
import org.hyperledger.fabric.sdk.Orderer;
import org.hyperledger.fabric.sdk.Peer;
import org.hyperledger.fabric.sdk.ProposalResponse;
import org.hyperledger.fabric.sdk.QueryProposalRequest;
import org.hyperledger.fabric.sdk.TransactionRequest.Type;
import org.hyperledger.fabric.sdk.User;
import org.hyperledger.fabric.sdk.events.EventHub;
import org.hyperledger.fabric.sdk.exception.BaseException;
import org.hyperledger.fabric.sdk.exception.CryptoException;
import org.hyperledger.fabric.sdk.exception.InvalidArgumentException;
import org.hyperledger.fabric.sdk.security.CryptoSuite;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class End2EndJavaIT {
	
	final static Logger logger = Logger.getLogger(End2EndJavaIT.class.getName());

	private static class TestEnvironmentContext {
		static class Peer {
			String containerId;
			String address;
			String eventServerAddress;
		}
		String networkName;
		String networkId;
		Map<String, TestEnvironmentContext.Peer> peers = new LinkedHashMap<>();
		String ordererContainerId;
		String ordererAddress;
		String certificateAuthorityContainerId;
		String certificateAuthorityAddress;
	}
	
	final static TestEnvironmentContext testEnvironment = new TestEnvironmentContext();
	
//	@BeforeClass
	public static void setUpBeforeClassStatic() throws Exception {
		
		// configure logging TODO move to logging.settings
		final ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);
		logger.setLevel(Level.ALL);

		// comment out the @BeforeClass from setUpBeforeClass() and 
		// uncomment out the @BeforeClass for this method to use an
		// existing environment (edit hosts/ports below to match)
		
		testEnvironment.certificateAuthorityAddress = "localhost:7054";
		testEnvironment.ordererAddress = "localhost:7050";
		testEnvironment.peers.put("vp0", new TestEnvironmentContext.Peer());
		testEnvironment.peers.get("vp0").address = "localhost:7051";
		testEnvironment.peers.get("vp0").eventServerAddress = "localhost:7053";
		testEnvironment.peers.put("vp1", new TestEnvironmentContext.Peer());
		testEnvironment.peers.get("vp1").address = "localhost:7056";
		
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		
		// configure logging TODO move to logging.settings
		final ConsoleHandler handler = new ConsoleHandler();
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);
		logger.setLevel(Level.ALL);
		
		// start network
		testEnvironment.networkName = "end2end_" + (new SecureRandom().nextInt(9999990) + 1000000);
		testEnvironment.networkId = callProcessAndGetOutput("/usr/local/bin/docker", "network", "create", testEnvironment.networkName).trim();
		
		// start fabric-ca
		testEnvironment.certificateAuthorityContainerId = callProcessAndGetOutput(
				"/usr/local/bin/docker", "run", "--detach", "--publish", "7054",
		        "--network", testEnvironment.networkName,
		        "--network-alias", "ca",
				"hyperledger/fabric-ca",
		        "fabric-ca-server", "start", "--boot", "admin:adminpw", "--debug"
		        ).trim();
		testEnvironment.certificateAuthorityAddress = callProcessAndGetOutput("/usr/local/bin/docker", "port", testEnvironment.certificateAuthorityContainerId, "7054").trim();
		
		// start fabric-orderer
		testEnvironment.ordererContainerId = callProcessAndGetOutput(
				"/usr/local/bin/docker", "run", "--detach", "--publish", "7050",
		        "--network", testEnvironment.networkName,
				"--network-alias", "orderer",
				"--env", "ORDERER_GENERAL_LISTENADDRESS=0.0.0.0",
				"--env", "CONFIGTX_ORDERER_BATCHSIZE_MAXMESSAGECOUNT=1",
				"--env", "CONFIGTX_ORDERER_BATCHTIMEOUT=1s",
				"hyperledger/fabric-orderer"
				).trim();
		testEnvironment.ordererAddress = callProcessAndGetOutput("/usr/local/bin/docker", "port", testEnvironment.ordererContainerId, "7050").trim();
		
		// start fabric-peer vp0, vp1
		for(final String peerName: new String[]{"vp0", "vp1"}) {
			testEnvironment.peers.put(peerName, new TestEnvironmentContext.Peer());
			testEnvironment.peers.get(peerName).containerId = callProcessAndGetOutput(
					"/usr/local/bin/docker", "run", "--detach", "--publish", "7051", "--publish", "7053", 
					"--network", testEnvironment.networkName,
					"--network-alias", peerName,
					"--env", "CORE_PEER_ADDRESSAUTODETECT=true",
					"--env", "CORE_PEER_ID=" + peerName,
					"--env", "CORE_CHAINCODE_STARTUPTIMEOUT=5000",
					"--env", "CORE_VM_DOCKER_ATTACHSTDOUT=true",
					"--volume", "/var/run/docker.sock:/var/run/docker.sock",
					"hyperledger/fabric-peer",
					"peer", "node", "start", "--logging-level", "debug", "--orderer", "orderer:7050"
					).trim();
			testEnvironment.peers.get(peerName).address = callProcessAndGetOutput("/usr/local/bin/docker", "port", testEnvironment.peers.get(peerName).containerId, "7051").trim();
			testEnvironment.peers.get(peerName).eventServerAddress = callProcessAndGetOutput("/usr/local/bin/docker", "port", testEnvironment.peers.get(peerName).containerId, "7053").trim();
			// just give the peer a second
			Thread.sleep(TimeUnit.SECONDS.toMillis(1));
		}
	}
	
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		dumpContainerLogs();
		Thread.sleep(TimeUnit.SECONDS.toMillis(5));
		decomposeTestEnvironment();
	}

	private static void dumpContainerLogs() throws IOException, InterruptedException, ExecutionException {
		
		// dump peer logs
		for(Entry<String, TestEnvironmentContext.Peer> entry: testEnvironment.peers.entrySet()) {
			if(entry.getValue().containerId != null) {
				Files.write(Paths.get(entry.getKey() + ".log"), callProcessAndGetOutput(new ProcessBuilder("/usr/local/bin/docker", "logs", entry.getValue().containerId)).getBytes(), StandardOpenOption.CREATE);
			}
		}
		
		// stop fabric-orderer
		if(testEnvironment.ordererContainerId != null) {
			Files.write(Paths.get("orderer.log"), callProcessAndGetOutput(new ProcessBuilder("/usr/local/bin/docker", "logs", testEnvironment.ordererContainerId)).getBytes(), StandardOpenOption.CREATE);
		}
		
		// stop fabric-ca
		if(testEnvironment.certificateAuthorityContainerId != null) {
			Files.write(Paths.get("ca.log"), callProcessAndGetOutput(new ProcessBuilder("/usr/local/bin/docker", "logs", testEnvironment.certificateAuthorityContainerId)).getBytes(), StandardOpenOption.CREATE);
		}
		
	}
	
	private static void decomposeTestEnvironment() throws IOException, InterruptedException, ExecutionException {
		// stop fabric-peer vp0, vp1
		for(TestEnvironmentContext.Peer peer: testEnvironment.peers.values()) {
			if(peer.containerId != null) {
				logger.fine(callProcessAndGetOutput(new ProcessBuilder("/usr/local/bin/docker", "rm", "--force", "--volumes", peer.containerId)));
			}
		}
		
		// stop fabric-orderer
		if(testEnvironment.ordererContainerId != null) {
			logger.fine(callProcessAndGetOutput(new ProcessBuilder("/usr/local/bin/docker", "rm", "--force", "--volumes", testEnvironment.ordererContainerId)));
		}
		
		// stop fabric-ca
		if(testEnvironment.certificateAuthorityContainerId != null) {
			logger.fine(callProcessAndGetOutput(new ProcessBuilder("/usr/local/bin/docker", "rm", "--force", "--volumes", testEnvironment.certificateAuthorityContainerId)));
		}
		
		// stop network
		if(testEnvironment.networkId != null) {
			logger.fine(callProcessAndGetOutput(new ProcessBuilder("/usr/local/bin/docker", "network", "rm", testEnvironment.networkId)));
		}
	}
	
	@Before
	public void setUp() throws Exception {
		// TODO do I need to do this (it's just one test)?
		// reset java sdk config
		// load java sdk config from environment
	}

	@After
	public void tearDown() throws Exception {
		// TODO do I need to do this ?
		// reset java sdk config
	}
		
	@Test
	public void simpleEndToEndScenario() throws Exception {
		
		// initialize crypto suite
		final CryptoSuite cryptoSuite = initializeCryptoSuite();
		
		// initialize member services
		final MemberServices memberServices = initializeMemberServices(cryptoSuite);
		
		// initialize the fabric admin user
		final User adminUser = initializeAdminUser(memberServices);
		
		// initialize a fabric user
		final User user = initializeFabricUser(memberServices, adminUser);
		
		// initialize fabric client for user
		final HFClient client = initializeFabricClient(cryptoSuite, memberServices, user);
		
		// initialize a channel
		final Chain channel = initializeChannel(client);
		
		// install chaincode
		installChaincode(client, channel);
		
		// instantiate chaincode
		instantiateChaincode(client, channel);
		
		// invoke chaincode
		invokeChaincode(client, channel);
		
		// query chaincode
		queryChaincode(client, channel);
		
	}

	private CryptoSuite initializeCryptoSuite() throws CryptoException, InvalidArgumentException {
		final CryptoSuite suite = CryptoSuite.Factory.getCryptoSuite();
		suite.init();
		return suite;
	}

	private HFCAClient initializeMemberServices(CryptoSuite cryptoSuite) throws MalformedURLException {
		final HFCAClient client = new HFCAClient("http://" + testEnvironment.certificateAuthorityAddress, null);
		client.setCryptoSuite(cryptoSuite);
		return client;
	}

    private void queryChaincode(final HFClient client, final Chain channel) throws Exception {
    	
    	// create query proposal request
    	final QueryProposalRequest queryProposalRequest = client.newQueryProposalRequest();
    	queryProposalRequest.setChaincodeID(ChainCodeID.newBuilder()
    			.setName("end2end_cc_java")
    			.setVersion("1.0.0.0")
    			.build());
    	queryProposalRequest.setFcn("invoke");
    	queryProposalRequest.setArgs(new String[] { "query", "Bob" });
   	
    	// send proposal to peers
    	final Collection<ProposalResponse> proposalResponses = channel.sendQueryProposal(queryProposalRequest, channel.getPeers());
    	
    	// group responses by status
    	final Map<Object, List<ProposalResponse>> responsesByStatus = proposalResponses.stream().collect(Collectors.groupingBy(x -> x.getStatus()));
    	
    	// make sure we got at least one successful response
    	assertThat(responsesByStatus.get(Status.SUCCESS), hasSize(greaterThan(1)));

    	// get the response payloads
    	final List<String> responses = responsesByStatus.get(Status.SUCCESS).stream().map(r -> r.getProposalResponse().getResponse().getPayload().toStringUtf8()).collect(Collectors.toList());
    	
    	// make sure each response was correct
    	assertThat(responses,everyItem(Matchers.is("271")));
        
    }

    private void invokeChaincode(HFClient client, Chain channel) throws Exception {
    	
    	// create invoke proposal request
    	final InvokeProposalRequest invokeProposalRequest = client.newInvokeProposalRequest();
    	invokeProposalRequest.setChaincodeID(ChainCodeID.newBuilder()
    			.setName("end2end_cc_java")
    			.setVersion("1.0.0.0")
    			.build());
    	invokeProposalRequest.setFcn("invoke");
    	invokeProposalRequest.setArgs(new String[] { "move", "Alice", "Bob", "71" });
   	
    	// send proposal to peers
    	final Collection<ProposalResponse> proposalResponses = channel.sendInvokeProposal(invokeProposalRequest, channel.getPeers());
    	
    	// group responses by status
    	final Map<Object, List<ProposalResponse>> responsesByStatus = proposalResponses.stream().collect(Collectors.groupingBy(x -> x.getStatus()));
    	
    	// make sure we got at least one successful response
    	assertThat(responsesByStatus.get(Status.SUCCESS), hasSize(greaterThan(1)));
    	
    	// create tx and broadcast to orderer
    	final CompletableFuture<TransactionEvent> future = channel.sendTransaction(responsesByStatus.get(Status.SUCCESS), channel.getOrderers());
    	
    	// wait for reply
    	final TransactionEvent transactionEvent = future.get();

    	// we should get a good (VALID) validation code 
    	assertThat(transactionEvent.isValid(), Matchers.is(true));
    	
    	// give all the peers a moment to catch up
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
        
    }

    private void instantiateChaincode(HFClient client, Chain channel) throws Exception {
    	
    	// create instantiate proposal request
    	final InstantiateProposalRequest instantiateProposalRequest = client.newInstantiationProposalRequest();
    	instantiateProposalRequest.setChaincodeID(ChainCodeID.newBuilder()
    			.setName("end2end_cc_java")
    			.setVersion("1.0.0.0")
    			.setPath("SimpleChaincode") // TODO should not be needed
    			.build());
    	instantiateProposalRequest.setChaincodeLanguage(Type.JAVA); // TODO should not be needed
    	instantiateProposalRequest.setFcn("init");
    	instantiateProposalRequest.setArgs(new String[] { "Alice", "100", "Bob", "200" });
    	instantiateProposalRequest.setChaincodeEndorsementPolicy(parseChaincodeEndorsementPolicy("AND(DEFAULT.admin)"));
//    	instantiateProposalRequest.setChaincodeEndorsementPolicy(new ChaincodeEndorsementPolicy(new File("src/test/resources/policyBitsAdmin")));
    	
    	// send proposal to peers
    	final Collection<ProposalResponse> proposalResponses = channel.sendInstantiationProposal(instantiateProposalRequest, channel.getPeers());
    	
    	// group responses by status
    	final Map<Object, List<ProposalResponse>> responsesByStatus = proposalResponses.stream().collect(Collectors.groupingBy(x -> x.getStatus()));
    	
    	// make sure we got at least one successful response
    	assertThat(responsesByStatus.get(Status.SUCCESS), hasSize(greaterThan(1)));
    	
    	// create tx and broadcast to orderer
    	final CompletableFuture<TransactionEvent> future = channel.sendTransaction(responsesByStatus.get(Status.SUCCESS), channel.getOrderers());
    	
    	// wait for reply
    	final TransactionEvent transactionEvent = future.get();

    	// we should get a good (VALID) validation code 
    	assertThat(transactionEvent.isValid(), Matchers.is(true));
    	
    	// give all the peers a moment to catch up
        Thread.sleep(TimeUnit.SECONDS.toMillis(2));
    	
    }

	private ChaincodeEndorsementPolicy parseChaincodeEndorsementPolicy(final String policyString) {
		// just cheating for now
		if(!"AND(DEFAULT.admin)".equals(policyString)) throw new AssertionError("Can only parse: AND(DEFAULT.admin)");
		return new ChaincodeEndorsementPolicy(
    			SignaturePolicyEnvelope.newBuilder()
    			.setPolicy(SignaturePolicy.newBuilder()
    					.setNOutOf(NOutOf.newBuilder()
    							.setN(1)
    							.addPolicies(SignaturePolicy.newBuilder()
    									.setSignedBy(0))
    							)
    					)
    			.addIdentities(MSPPrincipal.newBuilder()
    					.setPrincipal(MSPRole.newBuilder()
    							.setMspIdentifier("DEFAULT")
    							.setRole(MSPRoleType.ADMIN)
    							.build().toByteString()))
    			.build().toByteArray()
    			);
	}

    private void installChaincode(HFClient client, Chain channel) throws Exception {
    	
    	// create install proposal request
    	final InstallProposalRequest installProposalRequest = client.newInstallProposalRequest();
    	installProposalRequest.setChaincodeSourceLocation(Paths.get("src/test/fixture/chaincode/java").toFile());
    	installProposalRequest.setChaincodeID(ChainCodeID.newBuilder()
    			.setName("end2end_cc_java")
    			.setVersion("1.0.0.0")
    			.setPath("SimpleChaincode")
    			.build());
    	installProposalRequest.setChaincodeLanguage(Type.JAVA);
    	
    	// send proposal to peers
    	final Collection<ProposalResponse> proposalResponses = channel.sendInstallProposal(installProposalRequest, channel.getPeers());
    	
    	// group responses by status
    	final Map<Object, List<ProposalResponse>> responsesByStatus = proposalResponses.stream().collect(Collectors.groupingBy(x -> x.getStatus()));
    	
    	// make sure we got at least one successful response
    	assertThat(responsesByStatus.get(ChainCodeResponse.Status.SUCCESS), hasSize(greaterThan(1)));

    }

    private Chain initializeChannel(HFClient client) throws IOException, BaseException {
    	    	
        // TODO Auto-generated method stub
    	final String channelName = "foo";
    	
    	// initialize orderer client
    	final Orderer orderer = client.newOrderer("grpc://" + testEnvironment.ordererAddress);
    	
    	// load channel configuration tx
    	final ChainConfiguration configTx = new ChainConfiguration(new File("src/test/fixture/" + channelName + ".configtx")); 
    	
    	// register channel on client
    	final Chain channel = client.newChain(channelName, orderer, configTx);
    	
    	// specify peers to join channel
    	for(final String peerName:testEnvironment.peers.keySet()) {
    		final Peer peer = client.newPeer("grpc://" + testEnvironment.peers.get(peerName).address);
    		peer.setName(peerName);
    		channel.addPeer(peer);
    	}
    	
    	// specify bootstrap orderers for channel
    	channel.addOrderer(orderer);
    	
    	// specify event servers for channel events
    	for(final String peerName:testEnvironment.peers.keySet()) {
    		if(testEnvironment.peers.get(peerName).eventServerAddress != null) {
    			final EventHub eventServer = client.newEventHub("grpc://" + testEnvironment.peers.get(peerName).eventServerAddress);
    			channel.addEventHub(eventServer);
    		}
    	}

    	// make it so
    	channel.initialize();
    	
        return channel;
    }

    private HFClient initializeFabricClient(CryptoSuite cryptoSuite, MemberServices memberServices, User userContext) throws CryptoException, InvalidArgumentException {
    	final HFClient client = HFClient.createNewInstance();
    	client.setCryptoSuite(cryptoSuite);
    	client.setMemberServices(memberServices);
    	client.setUserContext(userContext);
        return client;
    }

    private User initializeFabricUser(MemberServices memberServices, User adminUser) throws Exception {
    	final String userName = "user1";
    	final String affiliation = "org1.department1";
		final String enrollmentSecret = memberServices.register(new RegistrationRequest(userName, affiliation), adminUser);
    	final Enrollment enrollment = memberServices.enroll(userName, enrollmentSecret);
        return new User() {
			@Override
			public ArrayList<String> getRoles() {
				return new ArrayList<>();
			}
			@Override
			public String getName() {
				return userName;
			}
			@Override
			public String getMSPID() {
				return "DEFAULT";
			}
			@Override
			public Enrollment getEnrollment() {
				return enrollment;
			}
			@Override
			public String getAffiliation() {
				return affiliation;
			}
			@Override
			public String getAccount() {
				return null;
			}
		};
    }

    private User initializeAdminUser(MemberServices memberServices) throws EnrollmentException, InvalidArgumentException {
    	final Enrollment enrollment = memberServices.enroll("admin", "adminpw");
        return new User() {
			@Override
			public ArrayList<String> getRoles() {
				return new ArrayList<>();
			}
			@Override
			public String getName() {
				return "admin";
			}
			@Override
			public String getMSPID() {
				return "DEFAULT";
			}
			@Override
			public Enrollment getEnrollment() {
				return enrollment;
			}
			@Override
			public String getAffiliation() {
				return null;
			}
			@Override
			public String getAccount() {
				return null;
			}
		};
    }

    static class CalledProcessException extends RuntimeException {
		private static final long serialVersionUID = -6276485776308640779L;
		final int exitValue;
    	final List<String> command;
    	final String output;
		CalledProcessException(List<String> command, int exitValue, String output) {
			super("Process exited with a return code of " + exitValue + "." + "Output: " + output);
			this.command = command;
			this.exitValue = exitValue;
			this.output = output;
		}
		public int getExitValue() {
			return exitValue;
		}
		public List<String> getCommand() {
			return command;
		}
		public String getOutput() {
			return output;
		}
    }

	private static String callProcessAndGetOutput(String... command ) throws IOException, InterruptedException, ExecutionException {
		return callProcessAndGetOutput(new ProcessBuilder(command));
	}
	
	private static String callProcessAndGetOutput(ProcessBuilder processBuilder) throws IOException, InterruptedException, ExecutionException {
		
		processBuilder.redirectErrorStream(true);
		
		// start process
		final Process process = processBuilder.start();
		
		// start thread to capture output
		final Future<String> future = Executors.newSingleThreadExecutor().submit(
			new Callable<String>() {
				@Override
				public String call() throws Exception {
					final ByteArrayOutputStream output = new ByteArrayOutputStream();
					final InputStream inputStream = process.getInputStream();
					for(int b = inputStream.read(); b != -1; b = inputStream.read()) {
						output.write(b);
					}
					return output.toString();
				}
			}
		);
		
		// wait for process to end
		final int exitValue = process.waitFor();
		
		// get output
		final String result = future.get();
		
		// throw exception if exitValue != 0
		if(exitValue != 0) {
			throw new CalledProcessException(processBuilder.command(), exitValue, result);
		}
		
		return result;
	}
    
}
