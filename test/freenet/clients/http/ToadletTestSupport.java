package freenet.clients.http;

import freenet.support.HTMLNode;
import freenet.support.MultiValueTable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.mockito.stubbing.Answer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Class that can help writing tests for {@link Toadlet} implementations.
 * It takes care of the many weird interactions between a {@link Toadlet},
 * a {@link ToadletContext}, and e.g. the {@link PageMaker}.
 * <p style="font-weight: bold;">
 * At the moment this class only supports what has been needed in a test.
 * If you need it to do more, you need to expand it.
 * </p>
 */
public class ToadletTestSupport {

	/**
	 * Returns the {@link ToadletContext} managed by this class.
	 *
	 * @return The {@link ToadletContext}
	 */
	public ToadletContext getToadletContext() {
		return toadletContext;
	}

	/**
	 * Configures the {@link ToadletContext} managed by this class to
	 * claim that the current request has full access.
	 *
	 * @return this {@link ToadletTestSupport} instance
	 */
	public ToadletTestSupport allowFullAccess() {
		when(toadletContext.isAllowedFullAccess()).thenReturn(true);
		return this;
	}

	/**
	 * Configures the {@link ToadletContext} managed by this class to
	 * deny that the current request has full access.
	 *
	 * @return this {@link ToadletTestSupport} instance
	 */
	public ToadletTestSupport denyFullAccess() {
		when(toadletContext.isAllowedFullAccess()).thenReturn(false);
		return this;
	}

	/**
	 * Returns the HTTP status sent by the toadlet.
	 *
	 * @return The HTTP status sent by the toadlet,
	 * 		or {@code 0} if nothing was sent
	 */
	public int getHttpStatus() {
		return httpStatus;
	}

	/**
	 * Returns the response headers sent by the toadlet.
	 *
	 * @return The response headers sent by the toadlet
	 */
	public Map<String, List<String>> getResponseHeaders() {
		return responseHeaders;
	}

	/**
	 * Returns the {@link PageNode} created by the {@link PageMaker} returned
	 * by the {@link ToadletContext}. To be more specific, this returns the last
	 * {@link PageNode} created.
	 *
	 * @return The last created {@link PageNode} instance
	 */
	public PageNode getPageNode() {
		return pageNode;
	}

	private final ToadletContext toadletContext = mock(ToadletContextImpl.class, RETURNS_DEEP_STUBS);
	private PageNode pageNode;
	private int httpStatus = 0;
	private Map<String, List<String>> responseHeaders = new HashMap<>();

	{
		Answer<Object> createPageAndStore = invocation -> {
			HTMLNode page = new HTMLNode.HTMLDoctype("html", "");
			HTMLNode htmlNode = page.addChild("html");
			HTMLNode headNode = htmlNode.addChild("head");
			HTMLNode bodyNode = htmlNode.addChild("body");
			pageNode = new PageNode(page, headNode, bodyNode);
			return pageNode;
		};
		when(toadletContext.getPageMaker().getPageNode(any(), any())).then(createPageAndStore);
		when(toadletContext.getPageMaker().getPageNode(any(), any(), any())).then(createPageAndStore);
		when(toadletContext.getPageMaker().getPageNode(any(), anyBoolean(), any())).then(createPageAndStore);
		when(toadletContext.getPageMaker().getPageNode(any(), anyBoolean(), anyBoolean(), any())).then(createPageAndStore);

		when(toadletContext.addFormChild(any(), any(), any())).then(invocation -> {
			HTMLNode formNode = invocation.getArgument(0, HTMLNode.class)
					.addChild("div")
					.addChild("form",
							new String[] { "action", "method", "enctype", "id", "accept-charset" },
							new String[] { invocation.getArgument(1, String.class), "post", "multipart/form-data", invocation.getArgument(2, String.class), "utf-8" }
					);
			formNode.addChild("input",
					new String[] { "type", "name", "value" },
					new String[] { "hidden", "formPassword", "form-password" }
			);
			return formNode;
		});

		try {
			Answer<Void> storeReplyContent = invocation -> {
				httpStatus = invocation.getArgument(0, Integer.class);
				MultiValueTable<String, String> headers = invocation.getArgument(2, MultiValueTable.class);
				if (headers != null) {
					headers.entrySet().forEach(entry -> responseHeaders.put(entry.getKey(), entry.getValue()));
				}
				return null;
			};
			doAnswer(storeReplyContent).when(toadletContext).sendReplyHeaders(anyInt(), anyString(), any(), any(), anyLong());
			doAnswer(storeReplyContent).when(toadletContext).sendReplyHeaders(anyInt(), anyString(), any(), any(), anyLong(), anyBoolean());
			doAnswer(storeReplyContent).when(toadletContext).sendReplyHeaders(anyInt(), anyString(), any(), any(), anyLong(), any());
			doAnswer(storeReplyContent).when(toadletContext).sendReplyHeadersStatic(anyInt(), anyString(), any(), any(), anyLong(), any());
			doAnswer(storeReplyContent).when(toadletContext).sendReplyHeadersFProxy(anyInt(), anyString(), any(), any(), anyLong());

			when(toadletContext.checkFullAccess(any())).thenCallRealMethod();
		} catch (ToadletContextClosedException | IOException e) {
			throw new RuntimeException(e);
		}
	}

}
