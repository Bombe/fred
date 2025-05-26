package freenet.clients.http;

import freenet.client.HighLevelSimpleClient;
import freenet.node.NodeClientCore;
import freenet.support.HTMLNode;
import freenet.support.MultiValueTable;
import freenet.support.api.HTTPRequest;
import java.net.URI;
import org.junit.Test;

import static freenet.test.TestHttpResponse.getHttpResponse;
import static freenet.test.TestHttpResponse.hasBody;
import static freenet.test.TestHttpResponse.hasBodyLines;
import static freenet.test.TestHttpResponse.hasMimeType;
import static freenet.test.TestHttpResponse.hasStatus;
import static freenet.test.TestHttpResponse.isRedirectTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FProxyToadletTest {

	@Test
	public void requestForRootRedirectsToWelcomePage() {
		RedirectException redirectException = assertThrows(RedirectException.class, () -> fproxyToadlet.handleMethodGET(new URI("/"), mock(HTTPRequest.class), toadletContext));
		assertThat(redirectException.getTarget().toString(), equalTo("/welcome/"));
	}

	@Test
	public void requestForFaviconRedirectsToStaticToadlet() {
		RedirectException redirectException = assertThrows(RedirectException.class, () -> fproxyToadlet.handleMethodGET(new URI("/favicon.ico"), mock(HTTPRequest.class), toadletContext));
		assertThat(redirectException.getTarget().toString(), equalTo("/static/favicon.ico"));
	}

	@Test
	public void requestForFeedWithoutSlashReturnsTheFeed() throws Exception {
		verifyAtomFeedBeingDeliveredForUri("/feed");
	}

	@Test
	public void requestForFeedWithSlashAlsoReturnsTheFeed() throws Exception {
		verifyAtomFeedBeingDeliveredForUri("/feed/");
	}

	private void verifyAtomFeedBeingDeliveredForUri(String uri) throws Exception {
		when(nodeClientCore.getNode().getConfig().get("fproxy").getOption("port").getValueString()).thenReturn("1234");
		when(nodeClientCore.getNode().getConfig().get("fproxy").getOption("bindTo").getValueString()).thenReturn("1.2.3.4");
		when(toadletContext.getAlertManager().getAtom("http://1.2.3.4:1234")).thenReturn("<atom/>");
		fproxyToadlet.handleMethodGET(new URI(uri), mock(HTTPRequest.class), toadletContext);
		assertThat(getHttpResponse(toadletContext), allOf(
				hasStatus(equalTo(200)),
				hasMimeType(equalTo("application/atom+xml")),
				hasBody(equalTo("<atom/>"))
		));
	}

	@Test
	public void requestForRobotsTxtReturnsSpecDisallowingEverything() throws Exception {
		when(toadletContext.doRobots()).thenReturn(true);
		fproxyToadlet.handleMethodGET(new URI("/robots.txt"), mock(HTTPRequest.class), toadletContext);
		assertThat(getHttpResponse(toadletContext), allOf(
				hasStatus(equalTo(200)),
				hasMimeType(equalTo("text/plain; charset=utf-8")),
				hasBodyLines(
						containsInAnyOrder(equalToIgnoringCase("User-Agent: *"), equalToIgnoringCase("Disallow: /"))
				)
		));
	}

	@Test
	public void requestForRobotsTxtWithRobotsDisabledReturnsError400() throws Exception {
		fproxyToadlet.handleMethodGET(new URI("/robots.txt"), mock(HTTPRequest.class), toadletContext);
		assertThat(getHttpResponse(toadletContext), allOf(
				hasStatus(equalTo(400)),
				hasMimeType(equalTo("text/html; charset=utf-8"))
		));
	}

	@Test
	public void requestForDarknetWithoutSlashSendsRedirectToFriendsPage() throws Exception {
		fproxyToadlet.handleMethodGET(new URI("/darknet"), mock(), toadletContext);
		assertThat(getHttpResponse(toadletContext), isRedirectTo("/friends/"));
	}

	@Test
	public void requestForDarknetWithSlashSendsRedirectToFriendsPage() throws Exception {
		fproxyToadlet.handleMethodGET(new URI("/darknet/"), mock(), toadletContext);
		assertThat(getHttpResponse(toadletContext), isRedirectTo("/friends/"));
	}

	@Test
	public void requestForOpennetWithoutSlashSendsRedirectToStrangersPage() throws Exception {
		fproxyToadlet.handleMethodGET(new URI("/opennet"), mock(), toadletContext);
		assertThat(getHttpResponse(toadletContext), isRedirectTo("/strangers/"));
	}

	@Test
	public void requestForOpennetWithSlashSendsRedirectToStrangersPage() throws Exception {
		fproxyToadlet.handleMethodGET(new URI("/opennet/"), mock(), toadletContext);
		assertThat(getHttpResponse(toadletContext), isRedirectTo("/strangers/"));
	}

	private final HighLevelSimpleClient mock = mock(HighLevelSimpleClient.class, RETURNS_DEEP_STUBS);
	private final NodeClientCore nodeClientCore = mock(NodeClientCore.class, RETURNS_DEEP_STUBS);
	private final MultiValueTable<String, String> headers = new MultiValueTable<>();
	private final ToadletContext toadletContext = mock(ToadletContext.class, RETURNS_DEEP_STUBS);
	private final ToadletContainer toadletContainer = mock(ToadletContainer.class, RETURNS_DEEP_STUBS);
	private final Toadlet fproxyToadlet = new FProxyToadlet(mock, nodeClientCore, null);

	{
		HTMLNode.HTMLDoctype htmlNode = new HTMLNode.HTMLDoctype("html", "");
		HTMLNode headNode = htmlNode.addChild("head");
		HTMLNode contentNode = htmlNode.addChild("body");
		PageNode pageNode = new PageNode(htmlNode, headNode, contentNode);
		when(toadletContext.getHeaders()).thenReturn(headers);
		when(toadletContext.getPageMaker().getPageNode(any(), any())).thenReturn(pageNode);
		fproxyToadlet.container = toadletContainer;
	}

}
