package freenet.clients.fcp;

import com.sun.net.httpserver.HttpServer;
import freenet.client.ClientMetadata;
import freenet.client.FetchResult;
import freenet.io.comm.PeerParseException;
import freenet.io.comm.ReferenceSignatureVerificationException;
import freenet.keys.FreenetURI;
import freenet.node.DarknetPeerNode;
import freenet.node.DarknetPeerNode.FRIEND_TRUST;
import freenet.node.DarknetPeerNode.FRIEND_VISIBILITY;
import freenet.node.FSParseException;
import freenet.node.Node;
import freenet.node.OpennetDisabledException;
import freenet.node.OpennetPeerNode;
import freenet.node.PeerTooOldException;
import freenet.support.SimpleFieldSet;
import freenet.support.io.ArrayBucket;
import java.io.File;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import static com.sun.net.httpserver.spi.HttpServerProvider.provider;
import static freenet.clients.fcp.ProtocolErrorMessage.ACCESS_DENIED;
import static freenet.clients.fcp.ProtocolErrorMessage.CANNOT_PEER_WITH_SELF;
import static freenet.clients.fcp.ProtocolErrorMessage.DUPLICATE_PEER_REF;
import static freenet.clients.fcp.ProtocolErrorMessage.NOT_A_FILE_ERROR;
import static freenet.clients.fcp.ProtocolErrorMessage.OPENNET_DISABLED;
import static freenet.clients.fcp.ProtocolErrorMessage.REF_PARSE_ERROR;
import static freenet.clients.fcp.ProtocolErrorMessage.REF_SIGNATURE_INVALID;
import static freenet.clients.fcp.ProtocolErrorMessage.URL_PARSE_ERROR;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AddPeerTest {

	@Test
	public void addPeerReturnsItsName() {
		assertThat(addPeer.getName(), equalTo("AddPeer"));
	}

	@Test
	public void fieldSetIsEmpty() {
		assertThat(addPeer.getFieldSet().isEmpty(), equalTo(true));
	}

	@Test
	public void addPeerRequiresTrust() {
		simpleFieldSet.putSingle("Visibility", "YES");
		assertThrows(MessageInvalidException.class, () -> new AddPeer(simpleFieldSet));
	}

	@Test
	public void addPeerRequiresValidTrust() {
		simpleFieldSet.putSingle("Trust", "invalid trust");
		simpleFieldSet.putSingle("Visibility", "YES");
		assertThrows(MessageInvalidException.class, () -> new AddPeer(simpleFieldSet));
	}

	@Test
	public void addPeerRequiresVisibility() {
		simpleFieldSet.putSingle("Trust", "HIGH");
		assertThrows(MessageInvalidException.class, () -> new AddPeer(simpleFieldSet));
	}

	@Test
	public void addPeerRequiresValidVisibility() {
		simpleFieldSet.putSingle("Trust", "HIGH");
		simpleFieldSet.putSingle("Visibility", "invalid");
		assertThrows(MessageInvalidException.class, () -> new AddPeer(simpleFieldSet));
	}

	@Test
	public void addingPeerWithoutFullAccessHandlerThrowsException() {
		when(fcpConnectionHandler.hasFullAccess()).thenReturn(false);
		MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
		assertThat(messageInvalidException.protocolCode, equalTo(ACCESS_DENIED));
	}

	@Test
	public void addingDarknetPeerWithFullAccessAndDifferentPubKeyHashSucceeds() throws Exception {
		when(node.createNewDarknetNode(simpleFieldSet, FRIEND_TRUST.HIGH, FRIEND_VISIBILITY.YES)).thenReturn(darknetPeerNode);
		when(node.addPeerConnection(darknetPeerNode)).thenReturn(true);
		addPeer.run(fcpConnectionHandler, node);
		verify(node).addPeerConnection(darknetPeerNode);
	}

	@Test
	public void addingDarknetPeerWithFullAccessAndPubKeyHashOfCurrentNodeFails() throws Exception {
		when(node.createNewDarknetNode(simpleFieldSet, FRIEND_TRUST.HIGH, FRIEND_VISIBILITY.YES)).thenReturn(darknetPeerNode);
		when(node.getDarknetPubKeyHash()).thenReturn(new byte[] { 1, 2, 3, 4 });
		when(node.addPeerConnection(darknetPeerNode)).thenReturn(true);
		MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
		assertThat(messageInvalidException.protocolCode, equalTo(CANNOT_PEER_WITH_SELF));
	}

	@Test
	public void addingDarknetPeerWithFullAccessAndExistingHashFails() throws Exception {
		when(node.createNewDarknetNode(simpleFieldSet, FRIEND_TRUST.HIGH, FRIEND_VISIBILITY.YES)).thenReturn(darknetPeerNode);
		when(node.addPeerConnection(darknetPeerNode)).thenReturn(false);
		MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
		assertThat(messageInvalidException.protocolCode, equalTo(DUPLICATE_PEER_REF));
	}

	@Test
	public void throwingFSParseExceptionWhenAddingDarknetPeerWillThrowException() throws Exception {
		throwExceptionOnAddingDarknetPeerAndVerifyThrownException(FSParseException.class, REF_PARSE_ERROR);
	}

	@Test
	public void throwingPeerParseExceptionWhenAddingDarknetPeerWillThrowException() throws Exception {
		throwExceptionOnAddingDarknetPeerAndVerifyThrownException(PeerParseException.class, REF_PARSE_ERROR);
	}

	@Test
	public void throwingReferenceSignatureVerificationExceptionWhenAddingDarknetPeerWillThrowException() throws Exception {
		throwExceptionOnAddingDarknetPeerAndVerifyThrownException(ReferenceSignatureVerificationException.class, REF_SIGNATURE_INVALID);
	}

	@Test
	public void throwingPeerTooOldExceptionWhenAddingDarknetPeerWillThrowException() throws Exception {
		throwExceptionOnAddingDarknetPeerAndVerifyThrownException(PeerTooOldException.class, REF_PARSE_ERROR);
	}

	private void throwExceptionOnAddingDarknetPeerAndVerifyThrownException(Class<? extends Exception> toThrow, int expectedProtocolCode) throws Exception {
		when(node.createNewDarknetNode(simpleFieldSet, FRIEND_TRUST.HIGH, FRIEND_VISIBILITY.YES)).thenThrow(toThrow);
		MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
		assertThat(messageInvalidException.protocolCode, equalTo(expectedProtocolCode));
	}

	@Test
	public void addingOpennetPeerWithFullAccessAndDifferentPubKeyHashSucceeds() throws Exception {
		simpleFieldSet.putSingle("opennet", "true");
		when(node.createNewOpennetNode(simpleFieldSet)).thenReturn(opennetPeerNode);
		when(node.addPeerConnection(opennetPeerNode)).thenReturn(true);
		addPeer.run(fcpConnectionHandler, node);
		verify(node).addPeerConnection(opennetPeerNode);
	}

	@Test
	public void addingOpennetPeerWithoutFullAccessAndDifferentPubKeyHashFails() {
		when(fcpConnectionHandler.hasFullAccess()).thenReturn(false);
		simpleFieldSet.putSingle("opennet", "true");
		assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
	}

	@Test
	public void addingOpennetPeerWithFullAccessButSamePubKeyHashAsNodeFails() throws Exception {
		simpleFieldSet.putSingle("opennet", "true");
		when(node.getOpennetPubKeyHash()).thenReturn(new byte[] { 2, 3, 4, 5 });
		when(node.createNewOpennetNode(simpleFieldSet)).thenReturn(opennetPeerNode);
		when(node.addPeerConnection(opennetPeerNode)).thenReturn(true);
		assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
	}

	@Test
	public void addingOpennetPeerWithFullAccessAndExistingHashFails() throws Exception {
		simpleFieldSet.putSingle("opennet", "true");
		when(node.createNewOpennetNode(simpleFieldSet)).thenReturn(opennetPeerNode);
		when(node.addPeerConnection(opennetPeerNode)).thenReturn(false);
		MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
		assertThat(messageInvalidException.protocolCode, equalTo(DUPLICATE_PEER_REF));
	}

	@Test
	public void throwingFSParseExceptionWhenAddingOpennetPeerWillThrowException() throws Exception {
		throwExceptionOnAddingOpennetPeerAndVerifyThrownException(FSParseException.class, REF_PARSE_ERROR);
	}

	@Test
	public void throwingOpennetDisabledExceptionWhenAddingOpennetPeerWillThrowException() throws Exception {
		throwExceptionOnAddingOpennetPeerAndVerifyThrownException(OpennetDisabledException.class, OPENNET_DISABLED);
	}

	@Test
	public void throwingPeerParseExceptionWhenAddingOpennetPeerWillThrowException() throws Exception {
		throwExceptionOnAddingOpennetPeerAndVerifyThrownException(PeerParseException.class, REF_PARSE_ERROR);
	}

	@Test
	public void throwingReferenceSignatureVerificationExceptionWhenAddingOpennetPeerWillThrowException() throws Exception {
		throwExceptionOnAddingOpennetPeerAndVerifyThrownException(ReferenceSignatureVerificationException.class, REF_SIGNATURE_INVALID);
	}

	@Test
	public void throwingPeerTooOldExceptionWhenAddingOpennetPeerWillThrowException() throws Exception {
		throwExceptionOnAddingOpennetPeerAndVerifyThrownException(PeerTooOldException.class, REF_PARSE_ERROR);
	}

	private void throwExceptionOnAddingOpennetPeerAndVerifyThrownException(Class<? extends Exception> toThrow, int expectedProtocolCode) throws Exception {
		simpleFieldSet.putSingle("opennet", "true");
		when(node.createNewOpennetNode(simpleFieldSet)).thenThrow(toThrow);
		MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
		assertThat(messageInvalidException.protocolCode, equalTo(expectedProtocolCode));
	}

	@Test
	public void addingDarknetPeerFromFileSucceeds() throws Exception {
		File peerRefFile = temporaryFolder.newFile();
		Files.write(peerRefFile.toPath(), asList("identity=123", "End"));
		simpleFieldSet.putSingle("File", peerRefFile.getAbsolutePath());
		ArgumentCaptor<SimpleFieldSet> simpleFieldSetCaptor = ArgumentCaptor.forClass(SimpleFieldSet.class);
		when(node.createNewDarknetNode(any(), any(), any())).thenReturn(darknetPeerNode);
		when(node.addPeerConnection(any())).thenReturn(true);
		addPeer.run(fcpConnectionHandler, node);
		verify(node).createNewDarknetNode(simpleFieldSetCaptor.capture(), any(), any());
		assertThat(simpleFieldSetCaptor.getValue().get("identity"), equalTo("123"));
	}

	@Test
	public void addingDarknetPeerFromFileThatIsDirectoryThrowsException() throws Exception {
		File peerRefFile = temporaryFolder.newFolder();
		storeFileInFieldSetRunAddPeerAndVerifyExceptionProtocolCode(peerRefFile, NOT_A_FILE_ERROR);
	}

	@Test
	public void addingDarknetPeerFromEmptyFileThrowsException() throws Exception {
		File peerRefFile = temporaryFolder.newFile();
		storeFileInFieldSetRunAddPeerAndVerifyExceptionProtocolCode(peerRefFile, REF_PARSE_ERROR);
	}

	@Test
	public void addingDarknetPeerFromFileWithInvalidBase64ThrowsException() throws Exception {
		File peerRefFile = temporaryFolder.newFile();
		Files.write(peerRefFile.toPath(), asList("identity==-'!/&/%$&", "End"));
		storeFileInFieldSetRunAddPeerAndVerifyExceptionProtocolCode(peerRefFile, REF_PARSE_ERROR);
	}

	private void storeFileInFieldSetRunAddPeerAndVerifyExceptionProtocolCode(File peerRefFile, int exceptionProtocolCode) {
		simpleFieldSet.putSingle("File", peerRefFile.getAbsolutePath());
		MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
		assertThat(messageInvalidException.protocolCode, equalTo(exceptionProtocolCode));
	}

	@Test
	public void addingOpennetPeerFromFileSucceeds() throws Exception {
		File peerRefFile = temporaryFolder.newFile();
		Files.write(peerRefFile.toPath(), asList("identity=123", "opennet=true", "End"));
		simpleFieldSet.putSingle("File", peerRefFile.getAbsolutePath());
		ArgumentCaptor<SimpleFieldSet> simpleFieldSetCaptor = ArgumentCaptor.forClass(SimpleFieldSet.class);
		when(node.createNewOpennetNode(any())).thenReturn(opennetPeerNode);
		when(node.addPeerConnection(any())).thenReturn(true);
		addPeer.run(fcpConnectionHandler, node);
		verify(node).createNewOpennetNode(simpleFieldSetCaptor.capture());
		assertThat(simpleFieldSetCaptor.getValue().get("identity"), equalTo("123"));
	}

	@Test
	public void addingOpennetPeerFromFileWithInvalidBase64ThrowsException() throws Exception {
		File peerRefFile = temporaryFolder.newFile();
		Files.write(peerRefFile.toPath(), asList("identity==-'!/&/%$&", "opennet=true", "End"));
		storeFileInFieldSetRunAddPeerAndVerifyExceptionProtocolCode(peerRefFile, REF_PARSE_ERROR);
	}

	@Test
	public void addingDarknetPeerFromFreenetUriSucceeds() throws Exception {
		simpleFieldSet.putSingle("URL", "KSK@Test");
		when(node.getClientCore().makeClient(anyShort(), anyBoolean(), anyBoolean()).fetch(eq(new FreenetURI("KSK@Test")), anyLong())).thenReturn(new FetchResult(new ClientMetadata("application/test"), new ArrayBucket("identity=123".getBytes(UTF_8))));
		ArgumentCaptor<SimpleFieldSet> simpleFieldSetCaptor = ArgumentCaptor.forClass(SimpleFieldSet.class);
		when(node.createNewDarknetNode(any(), any(), any())).thenReturn(darknetPeerNode);
		when(node.addPeerConnection(any())).thenReturn(true);
		addPeer.run(fcpConnectionHandler, node);
		verify(node).createNewDarknetNode(simpleFieldSetCaptor.capture(), any(), any());
		assertThat(simpleFieldSetCaptor.getValue().get("identity"), equalTo("123"));
	}

	@Test
	public void addingDarknetPeerFromFreenetUriForEmptyFileThrowsException() throws Exception {
		simpleFieldSet.putSingle("URL", "KSK@Test");
		when(node.getClientCore().makeClient(anyShort(), anyBoolean(), anyBoolean()).fetch(eq(new FreenetURI("KSK@Test")), anyLong())).thenReturn(new FetchResult(new ClientMetadata("application/test"), new ArrayBucket(new byte[0])));
		MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
		assertThat(messageInvalidException.protocolCode, equalTo(REF_PARSE_ERROR));
	}

	@Test
	public void addingDarknetPeerFromFreenetUriForInvalidFileThrowsException() throws Exception {
		simpleFieldSet.putSingle("URL", "KSK@Test");
		when(node.getClientCore().makeClient(anyShort(), anyBoolean(), anyBoolean()).fetch(eq(new FreenetURI("KSK@Test")), anyLong())).thenReturn(new FetchResult(new ClientMetadata("application/test"), new ArrayBucket("'identity==!/&/%$&".getBytes(UTF_8))));
		MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
		assertThat(messageInvalidException.protocolCode, equalTo(REF_PARSE_ERROR));
	}

	@Test
	public void addingDarknetPeerFromHorriblyMalformedUrlThrowsException() {
		simpleFieldSet.putSingle("URL", "broken://invalid.wtf");
		MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
		assertThat(messageInvalidException.protocolCode, equalTo(URL_PARSE_ERROR));
	}

	@Test
	public void addingOpennetPeerFromFreenetUriSucceeds() throws Exception {
		simpleFieldSet.putSingle("URL", "KSK@Test");
		when(node.getClientCore().makeClient(anyShort(), anyBoolean(), anyBoolean()).fetch(eq(new FreenetURI("KSK@Test")), anyLong())).thenReturn(new FetchResult(new ClientMetadata("application/test"), new ArrayBucket("identity=123\nopennet=true".getBytes(UTF_8))));
		ArgumentCaptor<SimpleFieldSet> simpleFieldSetCaptor = ArgumentCaptor.forClass(SimpleFieldSet.class);
		when(node.createNewOpennetNode(any())).thenReturn(opennetPeerNode);
		when(node.addPeerConnection(opennetPeerNode)).thenReturn(true);
		addPeer.run(fcpConnectionHandler, node);
		verify(node).createNewOpennetNode(simpleFieldSetCaptor.capture());
		assertThat(simpleFieldSetCaptor.getValue().get("identity"), equalTo("123"));
	}

	@Test
	public void addingOpennetPeerFromFreenetUriForEmptyFileThrowsException() throws Exception {
		simpleFieldSet.putSingle("URL", "KSK@Test");
		when(node.getClientCore().makeClient(anyShort(), anyBoolean(), anyBoolean()).fetch(eq(new FreenetURI("KSK@Test")), anyLong())).thenReturn(new FetchResult(new ClientMetadata("application/test"), new ArrayBucket(new byte[0])));
		MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
		assertThat(messageInvalidException.protocolCode, equalTo(REF_PARSE_ERROR));
	}

	@Test
	public void addingOpennetPeerFromFreenetUriForInvalidFileThrowsException() throws Exception {
		simpleFieldSet.putSingle("URL", "KSK@Test");
		when(node.getClientCore().makeClient(anyShort(), anyBoolean(), anyBoolean()).fetch(eq(new FreenetURI("KSK@Test")), anyLong())).thenReturn(new FetchResult(new ClientMetadata("application/test"), new ArrayBucket("'identity==!/&/%$&\nopennet=true".getBytes(UTF_8))));
		MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
		assertThat(messageInvalidException.protocolCode, equalTo(REF_PARSE_ERROR));
	}

	@Test
	public void addingOpennetPeerFromHorriblyMalformedUrlThrowsException() {
		simpleFieldSet.putSingle("URL", "broken://invalid.wtf");
		MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
		assertThat(messageInvalidException.protocolCode, equalTo(URL_PARSE_ERROR));
	}

	@Test
	public void addingDarknetPeerFromUrlSucceeds() throws Exception {
		startHttpServerForPeerRefAndRunTest("identity=123\nEnd", address -> {
			simpleFieldSet.putSingle("URL", address + "/peer.ref");
			ArgumentCaptor<SimpleFieldSet> simpleFieldSetCaptor = ArgumentCaptor.forClass(SimpleFieldSet.class);
			when(node.createNewDarknetNode(any(), any(), any())).thenReturn(darknetPeerNode);
			when(node.addPeerConnection(darknetPeerNode)).thenReturn(true);
			addPeer.run(fcpConnectionHandler, node);
			verify(node).createNewDarknetNode(simpleFieldSetCaptor.capture(), any(), any());
			assertThat(simpleFieldSetCaptor.getValue().get("identity"), equalTo("123"));
		});
	}

	@Test
	public void addingDarknetPeerFromIncorrectUrlThrowsException() throws Exception {
		startHttpServerForPeerRefAndRunTest("identity=123\nEnd", address -> {
			simpleFieldSet.putSingle("URL", address + "/invalid.ref");
			MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
			assertThat(messageInvalidException.protocolCode, equalTo(URL_PARSE_ERROR));
		});
	}

	@Test
	public void addingOpennetPeerFromUrlSucceeds() throws Exception {
		startHttpServerForPeerRefAndRunTest("identity=123\nopennet=true\nEnd", address -> {
			simpleFieldSet.putSingle("URL", address + "/peer.ref");
			ArgumentCaptor<SimpleFieldSet> simpleFieldSetCaptor = ArgumentCaptor.forClass(SimpleFieldSet.class);
			when(node.createNewOpennetNode(any())).thenReturn(opennetPeerNode);
			when(node.addPeerConnection(opennetPeerNode)).thenReturn(true);
			addPeer.run(fcpConnectionHandler, node);
			verify(node).createNewOpennetNode(simpleFieldSetCaptor.capture());
			assertThat(simpleFieldSetCaptor.getValue().get("identity"), equalTo("123"));
		});
	}

	@Test
	public void addingOpennetPeerFromIncorrectUrlThrowsException() throws Exception {
		startHttpServerForPeerRefAndRunTest("identity=123\nopennet=true\nEnd", address -> {
			simpleFieldSet.putSingle("URL", address + "/invalid.ref");
			MessageInvalidException messageInvalidException = assertThrows(MessageInvalidException.class, () -> addPeer.run(fcpConnectionHandler, node));
			assertThat(messageInvalidException.protocolCode, equalTo(URL_PARSE_ERROR));
		});
	}

	private void startHttpServerForPeerRefAndRunTest(String noderef, ThrowingConsumer<String> httpServerAddressConsumer) throws Exception {
		HttpServer httpServer = provider().createHttpServer(new InetSocketAddress(InetAddress.getLocalHost(), 0), 0);
		httpServer.createContext("/peer.ref", exchange -> {
			exchange.sendResponseHeaders(200, 0);
			try (OutputStream responseBody = exchange.getResponseBody()) {
				responseBody.write(noderef.getBytes(UTF_8));
			}
		});
		httpServer.start();
		try {
			httpServerAddressConsumer.accept("http://" + httpServer.getAddress().getHostName() + ":" + httpServer.getAddress().getPort());
		} finally {
			httpServer.stop(0);
		}
	}

	private interface ThrowingConsumer<T> {
		void accept(T t) throws Exception;
	}

	@Rule
	public final TemporaryFolder temporaryFolder = new TemporaryFolder();

	private final SimpleFieldSet simpleFieldSet = new SimpleFieldSet(true);
	private final AddPeer addPeer;
	private final FCPConnectionHandler fcpConnectionHandler = mock(FCPConnectionHandler.class);
	private final Node node = mock(Node.class, RETURNS_DEEP_STUBS);
	private final DarknetPeerNode darknetPeerNode = mock(DarknetPeerNode.class);
	private final OpennetPeerNode opennetPeerNode = mock(OpennetPeerNode.class);

	public AddPeerTest() throws MessageInvalidException {
		simpleFieldSet.putSingle("Trust", "HIGH");
		simpleFieldSet.putSingle("Visibility", "YES");
		addPeer = new AddPeer(simpleFieldSet);
		when(fcpConnectionHandler.hasFullAccess()).thenReturn(true);
		when(node.getDarknetPubKeyHash()).thenReturn(new byte[] { 0, 0, 0, 0 });
		when(node.getOpennetPubKeyHash()).thenReturn(new byte[] { 7, 7, 7, 7 });
		when(darknetPeerNode.getPubKeyHash()).thenReturn(new byte[] { 1, 2, 3, 4 });
		when(opennetPeerNode.getPubKeyHash()).thenReturn(new byte[] { 2, 3, 4, 5 });
	}

}
