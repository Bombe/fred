package freenet.clients.http;

import freenet.node.DarknetPeerNode;
import freenet.node.Node;
import freenet.support.Base64;
import freenet.test.TestHTTPRequest;
import freenet.test.UseTestTranslation;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Rule;
import org.junit.Test;

import static freenet.test.Matchers.isHtml;
import static freenet.test.Matchers.withElement;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.lessThan;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class N2NTMToadletTest {

	@Test
	public void getRequestWithoutFullAccessContextDoesNotRenderAnything() throws Exception {
		toadlet.handleMethodGET(null, request, toadletTestSupport.denyFullAccess().getToadletContext());
		assertThat(toadletTestSupport.getHttpStatus(), allOf(greaterThanOrEqualTo(400), lessThan(600)));
	}

	@Test
	public void getRequestWithFullAccessWithoutRequestParametersResultsInRedirectToFriendsPage() throws Exception {
		toadlet.handleMethodGET(null, request, toadletTestSupport.getToadletContext());
		assertThat(toadletTestSupport.getHttpStatus(), equalTo(302));
		assertThat(toadletTestSupport.getResponseHeaders(), hasEntry(equalToIgnoringCase("location"), contains("/friends/")));
	}

	@Test
	public void getRequestWithFullAccessWithInvalidPeernodeHashcodeParameterResultsInErrorMessage() throws Exception {
		request.setRequestParameter("peernode_hashcode", "invalid");
		toadlet.handleMethodGET(null, request, toadletTestSupport.getToadletContext());
		assertThat(toadletTestSupport.getPageNode().generate(), isHtml(withElement(".infobox.infobox-error .infobox-header:contains(peerNotFoundTitle)")));
	}

	@Test
	public void getRequestWithFullAccessWithUnknownPeernodeHashcodeParameterResultsInErrorMessage() throws Exception {
		request.setRequestParameter("peernode_hashcode", String.valueOf(darknetPeerNode.hashCode() + 1));
		toadlet.handleMethodGET(null, request, toadletTestSupport.getToadletContext());
		assertThat(toadletTestSupport.getPageNode().generate(), isHtml(withElement(".infobox.infobox-error .infobox-header:contains(peerNotFoundTitle)")));
	}

	@Test
	public void getRequestWithFullAccessWithPeernodeHashcodeParameterResultsInFormToWriteN2NTM() throws Exception {
		request.setRequestParameter("peernode_hashcode", String.valueOf(darknetPeerNode.hashCode()));
		toadlet.handleMethodGET(null, request, toadletTestSupport.getToadletContext());
		assertThat(toadletTestSupport.getHttpStatus(), equalTo(200));
		assertThat(toadletTestSupport.getPageNode().generate(), isHtml(withElement("form#sendN2NTMForm[action='/send_n2ntm/'][method='post']")));
	}

	@Test
	public void replyingQuotesOriginalMessage() throws Exception {
		request.setRequestPart("replyTo", Base64.encodeUTF8("hi test user\n\n> this is a test.\ngreetings!\n"));
		request.setRequestPart("peernode_hashcode", String.valueOf(darknetPeerNode.hashCode()));
		toadlet.handleMethodPOST(null, request, toadletTestSupport.getToadletContext());
		Element formElement = Jsoup.parse(toadletTestSupport.getPageNode().generate()).selectFirst("form#sendN2NTMForm[action='/send_n2ntm/'][method='post']");
		assertThat(formElement.selectFirst("textarea").wholeText(), equalTo("> hi test user\n> \n>> this is a test.\n> greetings!\n\n"));
	}

	@Test
	public void replyingToInvalidlyEncodedMessageRemovesMessage() throws Exception {
		request.setRequestPart("replyTo", "Täst!".getBytes(ISO_8859_1));
		request.setRequestPart("peernode_hashcode", String.valueOf(darknetPeerNode.hashCode()));
		toadlet.handleMethodPOST(null, request, toadletTestSupport.getToadletContext());
		Element formElement = Jsoup.parse(toadletTestSupport.getPageNode().generate()).selectFirst("form#sendN2NTMForm[action='/send_n2ntm/'][method='post']");
		assertThat(formElement.selectFirst("textarea").wholeText(), equalTo(""));
	}

	@Test
	public void replyingToInvalidPeerNodeHashcodeResultsInErrorMessage() throws Exception {
		request.setRequestPart("replyTo", Base64.encodeUTF8("Message"));
		request.setRequestPart("peernode_hashcode", "invalid");
		toadlet.handleMethodPOST(null, request, toadletTestSupport.getToadletContext());
		assertThat(toadletTestSupport.getPageNode().generate(), isHtml(withElement(".infobox.infobox-error .infobox-header:contains(peerNotFoundTitle)")));
	}

	@Test
	public void replyingToNonExistingPeerNodeHashcodeResultsInErrorMessage() throws Exception {
		request.setRequestPart("replyTo", Base64.encodeUTF8("Message"));
		request.setRequestPart("peernode_hashcode", String.valueOf(darknetPeerNode.hashCode() + 1));
		toadlet.handleMethodPOST(null, request, toadletTestSupport.getToadletContext());
		assertThat(toadletTestSupport.getPageNode().generate(), isHtml(withElement(".infobox.infobox-error .infobox-header:contains(peerNotFoundTitle)")));
	}

	private final Node node = mock(Node.class, RETURNS_DEEP_STUBS);
	private final N2NTMToadlet toadlet = new N2NTMToadlet(node, null, null);
	private final DarknetPeerNode darknetPeerNode = mock(DarknetPeerNode.class, RETURNS_DEEP_STUBS);
	private final TestHTTPRequest request = new TestHTTPRequest();
	private final ToadletTestSupport toadletTestSupport = new ToadletTestSupport().allowFullAccess();

	{
		when(darknetPeerNode.getName()).thenReturn("Test Node");
		when(node.getDarknetConnections()).thenReturn(new DarknetPeerNode[] { darknetPeerNode });
	}

	@Rule
	public final UseTestTranslation useTestTranslation = new UseTestTranslation();

}
