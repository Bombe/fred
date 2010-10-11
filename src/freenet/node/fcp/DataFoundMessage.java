/* This code is part of Freenet. It is distributed under the GNU General
 * Public License, version 2 (or at your option any later version). See
 * http://www.gnu.org/ for further details of the GPL. */
package freenet.node.fcp;

import com.db4o.ObjectContainer;

import freenet.client.FetchResult;
import freenet.crypt.DSAPublicKey;
import freenet.node.Node;
import freenet.support.SimpleFieldSet;

public class DataFoundMessage extends FCPMessage {

	final String identifier;
	final boolean global;
	final String mimeType;
	final long dataLength;
	final DSAPublicKey publicKey;

	public DataFoundMessage(FetchResult fr, String identifier, boolean global) {
		this.identifier = identifier;
		this.global = global;
		this.mimeType = fr.getMimeType();
		this.dataLength = fr.size();
		this.publicKey = fr.getPublicKey();
	}

	public DataFoundMessage(long foundDataLength, String foundDataMimeType, String identifier, boolean global) {
		this.mimeType = foundDataMimeType;
		this.identifier = identifier;
		this.global = global;
		this.dataLength = foundDataLength;
		this.publicKey = null;
	}

	public DataFoundMessage(long foundDataLength, String foundDataMimeType, String identifier, boolean global, DSAPublicKey publicKey) {
		this.mimeType = foundDataMimeType;
		this.identifier = identifier;
		this.global = global;
		this.dataLength = foundDataLength;
		this.publicKey = publicKey;
	}

	@Override
	public SimpleFieldSet getFieldSet() {
		SimpleFieldSet fs = new SimpleFieldSet(true);
		fs.putSingle("Identifier", identifier);
		if(global) fs.putSingle("Global", "true");
		fs.putSingle("Metadata.ContentType", mimeType);
		fs.putSingle("DataLength", Long.toString(dataLength));
		if (publicKey != null) {
			fs.putSingle("dsaPubKey.y", publicKey.getY().toString(16));
			fs.putSingle("dsaGroup.p", publicKey.getP().toString(16));
			fs.putSingle("dsaGroup.g", publicKey.getG().toString(16));
			fs.putSingle("dsaGroup.q", publicKey.getQ().toString(16));
		}
		return fs;
	}

	@Override
	public String getName() {
		return "DataFound";
	}

	@Override
	public void run(FCPConnectionHandler handler, Node node) throws MessageInvalidException {
		throw new MessageInvalidException(ProtocolErrorMessage.INVALID_MESSAGE, "DataFound goes from server to client not the other way around", identifier, global);
	}

	@Override
	public void removeFrom(ObjectContainer container) {
		container.delete(this);
	}

}
