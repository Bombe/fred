package freenet.io.xfer;

import freenet.io.xfer.PartiallyReceivedBlock.PacketReceivedListener;
import freenet.support.Buffer;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;

public class PartiallyReceivedBlockTest {

	@Test
	public void partiallyReceivedBlockCanBeCreated() {
		new PartiallyReceivedBlock(5, 6);
	}

	@Test
	public void partiallyReceivedBlockReturnsNumberOfExpectedPackets() throws AbortedException {
		assertThat(new PartiallyReceivedBlock(5, 6).getNumPackets(), equalTo(5));
	}

	@Test
	public void partiallyReceivedBlockReturnsSizeOfPackets() throws AbortedException {
		assertThat(new PartiallyReceivedBlock(5, 6).getPacketSize(), equalTo(6));
	}

	@Test
	public void requestingNumberOfPacketsFromAbortedBlockThrowsException() {
		singlePacketBlock.abort(0, null, false);
		assertThrows(AbortedException.class, () -> singlePacketBlock.getNumPackets());
	}

	@Test
	public void requestingPacketSizeFromAbortedBlockThrowsException() {
		singlePacketBlock.abort(0, null, false);
		assertThrows(AbortedException.class, () -> singlePacketBlock.getPacketSize());
	}

	@Test
	public void partiallyReceivedBlockCanBeCreatedWithData() {
		new PartiallyReceivedBlock(3, 4, new byte[12]);
	}

	@Test
	public void creatingPartiallyReceivedBlockWithInvalidlySizedDataThrowsRuntimeException() {
		assertThrows(RuntimeException.class, () -> new PartiallyReceivedBlock(3, 4, new byte[11]));
	}

	@Test
	public void freshPartiallyReceivedBlockIsNotAllReceived() throws AbortedException {
		assertThat(new PartiallyReceivedBlock(5, 6).allReceived(), equalTo(false));
	}

	@Test
	public void freshPartiallyReceivedBlockThrowsExceptionWhenDataIsRequested() {
		assertThrows(RuntimeException.class, () -> singlePacketBlock.getBlock());
	}

	@Test
	public void abortedBlockWillThrowExceptionWhenAllReceivedIsChecked() {
		singlePacketBlock.abort(0, null, false);
		assertThrows(AbortedException.class, () -> singlePacketBlock.allReceived());
	}

	@Test
	public void packetsOfPartiallyReceivedBlockAreInitiallyMarkedAsNotReceived() throws AbortedException {
		assertThat(singlePacketBlock.isReceived(0), equalTo(false));
	}

	@Test
	public void packetsOfPartiallyReceivedBlockAreInitiallyMarkedAsReceivedIfBlockIsCreatedWithData() throws AbortedException {
		PartiallyReceivedBlock partiallyReceivedBlock = new PartiallyReceivedBlock(1, 1, new byte[] { 1 });
		assertThat(partiallyReceivedBlock.isReceived(0), equalTo(true));
	}

	@Test
	public void blockCreatedWithDataCanReturnData() throws AbortedException {
		PartiallyReceivedBlock partiallyReceivedBlock = new PartiallyReceivedBlock(1, 1, new byte[] { 1 });
		assertThat(partiallyReceivedBlock.getPacket(0), equalTo(new Buffer(new byte[] { 1 })));
	}

	@Test
	public void blockCreatedWithDataIsAllReceived() throws AbortedException {
		PartiallyReceivedBlock partiallyReceivedBlock = new PartiallyReceivedBlock(1, 1, new byte[] { 1 });
		assertThat(partiallyReceivedBlock.allReceived(), equalTo(true));
	}

	@Test
	public void blockCreatedWithDataIsAllReceivedAndNotAborted() throws AbortedException {
		PartiallyReceivedBlock partiallyReceivedBlock = new PartiallyReceivedBlock(1, 1, new byte[] { 1 });
		assertThat(partiallyReceivedBlock.allReceivedAndNotAborted(), equalTo(true));
	}

	@Test
	public void checkingForReceivedPacketsWillThrowExceptionOnAbortedBlock() {
		singlePacketBlock.abort(0, null, false);
		assertThrows(AbortedException.class, () -> singlePacketBlock.isReceived(0));
	}

	@Test
	public void addingPacketToPartiallyReceivedBlockMakesAllReceivedReturnTrue() throws AbortedException {
		singlePacketBlock.addPacket(0, new Buffer(new byte[] { 1 }));
		assertThat(singlePacketBlock.allReceived(), equalTo(true));
	}

	@Test
	public void addingPacketToPartiallyReceivedBlockMakesAllReceivedAndNotAbortedReturnTrue() throws AbortedException {
		singlePacketBlock.addPacket(0, new Buffer(new byte[] { 1 }));
		assertThat(singlePacketBlock.allReceivedAndNotAborted(), equalTo(true));
	}

	@Test
	public void freshBlockIsNotAllReceivedAndNotAborted() {
		assertThat(singlePacketBlock.allReceivedAndNotAborted(), equalTo(false));
	}

	@Test
	public void abortingBlockAfterCompletingItMakesItAllReceivedAndNotAborted() throws AbortedException {
		singlePacketBlock.addPacket(0, new Buffer(new byte[] { 1 }));
		singlePacketBlock.abort(0, null, false);
		assertThat(singlePacketBlock.allReceivedAndNotAborted(), equalTo(true));
	}

	@Test
	public void partiallyReceivedBlockReturnsBlockWhenAllPacketsHaveBeenAdded() throws AbortedException {
		singlePacketBlock.addPacket(0, new Buffer(new byte[] { 1 }));
		assertThat(singlePacketBlock.getBlock(), equalTo(new byte[] { 1 }));
	}

	@Test
	public void addingPacketToPartiallyReceivedBlockMarksThatPacketAsReceived() throws AbortedException {
		PartiallyReceivedBlock partiallyReceivedBlock = new PartiallyReceivedBlock(5, 1);
		partiallyReceivedBlock.addPacket(2, new Buffer(new byte[] { 2 }));
		assertThat(partiallyReceivedBlock.isReceived(2), equalTo(true));
	}

	@Test
	public void addingPacketWithInvalidSizeToPartiallyReceivedBlockThrowsException() {
		assertThrows(RuntimeException.class, () -> singlePacketBlock.addPacket(0, new Buffer(new byte[] { 1, 2 })));
	}

	@Test
	public void abortingPartiallyReceivedBlockSetsBlockToAborted() {
		singlePacketBlock.abort(0, null, false);
		assertThat(singlePacketBlock.isAborted(), equalTo(true));
	}

	@Test
	public void abortingPartiallyReceivedBlockReturnsNullIfBlockWasNotComplete() {
		assertThat(singlePacketBlock.abort(0, null, false), nullValue());
	}

	@Test
	public void abortingPartiallyReceivedBlockReturnsDataIfBlockWasComplete() throws AbortedException {
		singlePacketBlock.addPacket(0, new Buffer(new byte[] { 2 }));
		assertThat(singlePacketBlock.abort(0, null, false), equalTo(new byte[] { 2 }));
	}

	@Test
	public void abortingCompletePartiallyReceivedBlockDoesNotSetAborted() throws AbortedException {
		singlePacketBlock.addPacket(0, new Buffer(new byte[] { 2 }));
		singlePacketBlock.abort(0, null, false);
		assertThat(singlePacketBlock.isAborted(), equalTo(false));
	}

	@Test
	public void abortingIncompletePartiallyReceivedBlockTwiceWithDifferentReasonDoesNotChangeReason() {
		singlePacketBlock.abort(1, "A", true);
		singlePacketBlock.abort(2, "B", false);
		assertThat(singlePacketBlock.getAbortReason(), equalTo(1));
		assertThat(singlePacketBlock.getAbortDescription(), equalTo("A"));
		assertThat(singlePacketBlock.abortedLocally(), equalTo(true));
	}

	@Test
	public void addingPacketToAbortedBlockThrowsException() {
		singlePacketBlock.abort(0, null, false);
		assertThrows(AbortedException.class, () -> singlePacketBlock.addPacket(0, new Buffer(new byte[] { 1 })));
	}

	@Test
	public void addingSamePacketTwiceDoesNotCompleteBlock() throws AbortedException {
		PartiallyReceivedBlock partiallyReceivedBlock = new PartiallyReceivedBlock(2, 1);
		partiallyReceivedBlock.addPacket(0, new Buffer(new byte[] { 1 }));
		partiallyReceivedBlock.addPacket(0, new Buffer(new byte[] { 1 }));
		assertThat(partiallyReceivedBlock.allReceived(), equalTo(false));
	}

	@Test
	public void addingSamePacketWithDifferentDataTwiceDoesNotOverwriteExistingData() throws AbortedException {
		singlePacketBlock.addPacket(0, new Buffer(new byte[] { 1 }));
		singlePacketBlock.addPacket(0, new Buffer(new byte[] { 2 }));
		assertThat(singlePacketBlock.getBlock(), equalTo(new byte[] { 1 }));
	}

	@Test
	public void gettingPacketOnAbortedBlockThrowsException() {
		singlePacketBlock.abort(0, null, false);
		assertThrows(AbortedException.class, () -> singlePacketBlock.getPacket(0));
	}

	@Test
	public void gettingAddedPacketFromBlockReturnsBlock() throws AbortedException {
		singlePacketBlock.addPacket(0, new Buffer(new byte[] { 1 }));
		assertThat(singlePacketBlock.getPacket(0), equalTo(new Buffer(new byte[] { 1 })));
	}

	@Test
	public void gettingNotAddedPacketFromBlockThrowsException() {
		assertThrows(IllegalStateException.class, () -> singlePacketBlock.getPacket(0));
	}

	@Test
	public void addingListenerOnAbortedBlockThrowsException() {
		singlePacketBlock.abort(0, null, false);
		assertThrows(AbortedException.class, () -> singlePacketBlock.addListener(packetReceivedListener));
	}

	@Test
	public void addingListenerReturnsListOfReceivedPackets() throws AbortedException {
		PartiallyReceivedBlock partiallyReceivedBlock = new PartiallyReceivedBlock(5, 1);
		partiallyReceivedBlock.addPacket(1, new Buffer(new byte[] { 1 }));
		partiallyReceivedBlock.addPacket(3, new Buffer(new byte[] { 3 }));
		partiallyReceivedBlock.addPacket(4, new Buffer(new byte[] { 4 }));
		Deque<Integer> receivedPackets = partiallyReceivedBlock.addListener(packetReceivedListener);
		assertThat(receivedPackets, contains(1, 3, 4));
	}

	@Test
	public void addingPacketNotifiesListener() throws AbortedException {
		singlePacketBlock.addListener(packetReceivedListener);
		singlePacketBlock.addPacket(0, new Buffer(new byte[] { 1 }));
		assertThat(packetReceivedListener.receivedPacketNumbers, contains(0));
	}

	@Test
	public void addingPacketTwiceNotifiesListenerOnce() throws AbortedException {
		singlePacketBlock.addListener(packetReceivedListener);
		singlePacketBlock.addPacket(0, new Buffer(new byte[] { 1 }));
		singlePacketBlock.addPacket(0, new Buffer(new byte[] { 1 }));
		assertThat(packetReceivedListener.receivedPacketNumbers, contains(0));
	}

	@Test
	public void listenerCanBeRemoved() throws AbortedException {
		singlePacketBlock.addListener(packetReceivedListener);
		singlePacketBlock.removeListener(packetReceivedListener);
		singlePacketBlock.addPacket(0, new Buffer(new byte[] { 1 }));
		assertThat(packetReceivedListener.receivedPacketNumbers, empty());
	}

	@Test
	public void abortingABlockNotifiesListener() throws AbortedException {
		singlePacketBlock.addListener(packetReceivedListener);
		singlePacketBlock.abort(1, "A", true);
		assertThat(packetReceivedListener.abortReasons, contains(1));
		assertThat(packetReceivedListener.abortDescriptions, contains("A"));
	}

	@Test
	public void abortingABlockTwiceNotifiesListenerOnce() throws AbortedException {
		singlePacketBlock.addListener(packetReceivedListener);
		singlePacketBlock.abort(1, "A", true);
		singlePacketBlock.abort(2, "B", false);
		assertThat(packetReceivedListener.abortReasons, contains(1));
		assertThat(packetReceivedListener.abortDescriptions, contains("A"));
	}

	private final PartiallyReceivedBlock singlePacketBlock = new PartiallyReceivedBlock(1, 1);
	private final TestPacketReceivedListener packetReceivedListener = new TestPacketReceivedListener();

	private static class TestPacketReceivedListener implements PacketReceivedListener {

		private final List<Integer> receivedPacketNumbers = new ArrayList<>();
		private final List<Integer> abortReasons = new ArrayList<>();
		private final List<String> abortDescriptions = new ArrayList<>();

		@Override
		public void packetReceived(int packetNo) {
			receivedPacketNumbers.add(packetNo);
		}

		@Override
		public void receiveAborted(int reason, String description) {
			abortReasons.add(reason);
			abortDescriptions.add(description);
		}

	}

}
