package freenet.node.fcp;

import freenet.node.Node;
import freenet.support.SimpleFieldSet;

public class IdentifierCollisionMessage extends FCPMessage {

	final String identifier;
	
	public IdentifierCollisionMessage(String id) {
		this.identifier = id;
	}

	public SimpleFieldSet getFieldSet() {
		SimpleFieldSet sfs = new SimpleFieldSet();
		sfs.put("Identifier", identifier);
		return sfs;
	}

	public String getName() {
		return "IdentifierCollision";
	}

	public void run(FCPConnectionHandler handler, Node node)
			throws MessageInvalidException {
		throw new MessageInvalidException(ProtocolErrorMessage.INVALID_MESSAGE, "IdentifierCollision goes from server to client not the other way around", identifier);
	}

}
