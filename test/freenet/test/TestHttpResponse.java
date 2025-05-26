package freenet.test;

import freenet.clients.http.Toadlet;
import freenet.clients.http.ToadletContext;
import freenet.support.MultiValueTable;
import freenet.support.api.Bucket;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOfRange;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.verify;

/**
 * Container for an HTTP response sent by a {@link Toadlet}. The HTTP
 * response is extracted from the {@link ToadletContext} used in a test;
 * the {@link ToadletContext} <em>has to be a
 * {@link Mockito#mock(Object[]) mock}!</em>
 * <h2>Usage</h2>
 * <pre>
 * fproxyToadlet.handleMethodGET(new URI("/robots.txt"), mock(HTTPRequest.class), toadletContext);
 * assertThat(getHttpResponse(toadletContext), allOf(
 *     hasStatus(equalTo(200)),
 *     hasMimeType(equalTo("text/plain; charset=utf-8")),
 *     hasBodyLines(
 *         containsInAnyOrder(equalToIgnoringCase("User-Agent: *"), equalToIgnoringCase("Disallow: /"))
 *     )
 * ));
 * </pre>
 */
public class TestHttpResponse {

	public static TestHttpResponse getHttpResponse(ToadletContext toadletContext) throws Exception {
		ArgumentCaptor<Integer> httpCodeCaptor = ArgumentCaptor.forClass(Integer.class);
		ArgumentCaptor<String> httpReasonCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<MultiValueTable<String, String>> headersCaptor = ArgumentCaptor.forClass(MultiValueTable.class);
		ArgumentCaptor<String> mimeTypeCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Long> lengthCaptor = ArgumentCaptor.forClass(Long.class);
		verify(toadletContext, atMostOnce()).sendReplyHeaders(httpCodeCaptor.capture(), httpReasonCaptor.capture(), headersCaptor.capture(), mimeTypeCaptor.capture(), lengthCaptor.capture());
		if (httpCodeCaptor.getAllValues().isEmpty()) {
			verify(toadletContext, atMostOnce()).sendReplyHeaders(httpCodeCaptor.capture(), httpReasonCaptor.capture(), headersCaptor.capture(), mimeTypeCaptor.capture(), lengthCaptor.capture(), anyBoolean());
		}
		if (httpCodeCaptor.getAllValues().isEmpty()) {
			verify(toadletContext, atMostOnce()).sendReplyHeadersFProxy(httpCodeCaptor.capture(), httpReasonCaptor.capture(), headersCaptor.capture(), mimeTypeCaptor.capture(), lengthCaptor.capture());
		}
		if (httpCodeCaptor.getAllValues().isEmpty()) {
			verify(toadletContext, atMostOnce()).sendReplyHeadersStatic(httpCodeCaptor.capture(), httpReasonCaptor.capture(), headersCaptor.capture(), mimeTypeCaptor.capture(), lengthCaptor.capture(), any(Date.class));
		}
		if (httpCodeCaptor.getAllValues().isEmpty()) {
			/* okay, one of those _has_ to be called. */
			verify(toadletContext).sendReplyHeaders(httpCodeCaptor.capture(), httpReasonCaptor.capture(), headersCaptor.capture(), mimeTypeCaptor.capture(), lengthCaptor.capture(), any(Date.class));
		}
		ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
		byte[] body = new byte[0];
		int bodyOffset = 0;
		int bodyLength = 0;
		verify(toadletContext, atMostOnce()).writeData(bodyCaptor.capture());
		if (bodyCaptor.getAllValues().isEmpty()) {
			ArgumentCaptor<Integer> bodyOffsetCaptor = ArgumentCaptor.forClass(Integer.class);
			ArgumentCaptor<Integer> bodyLengthCaptor = ArgumentCaptor.forClass(Integer.class);
			verify(toadletContext, atMostOnce()).writeData(bodyCaptor.capture(), bodyOffsetCaptor.capture(), bodyLengthCaptor.capture());
			if (!bodyCaptor.getAllValues().isEmpty()) {
				body = bodyCaptor.getAllValues().get(0);
				bodyOffset = bodyOffsetCaptor.getValue();
				bodyLength = bodyLengthCaptor.getValue();
			}
		} else {
			body = bodyCaptor.getValue();
			bodyLength = bodyCaptor.getValue().length;
		}
		if (bodyCaptor.getAllValues().isEmpty()) {
			ArgumentCaptor<Bucket> bucketCaptor = ArgumentCaptor.forClass(Bucket.class);
			verify(toadletContext).writeData(bucketCaptor.capture());
			try (InputStream inputStream = bucketCaptor.getValue().getInputStream(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				byte[] buffer = new byte[4096];
				for (int read = inputStream.read(buffer); read != -1; read = inputStream.read(buffer)) {
					baos.write(buffer, 0, read);
				}
				body = baos.toByteArray();
			}
			bodyLength = body.length;
		}
		return new TestHttpResponse(httpCodeCaptor.getValue(), httpReasonCaptor.getValue(), headersCaptor.getValue(), mimeTypeCaptor.getValue(), copyOfRange(body, bodyOffset, bodyOffset + bodyLength));
	}

	public static Matcher<TestHttpResponse> hasStatus(Matcher<Integer> statusMatcher) {
		return isResponse(TestHttpResponse::getStatusCode, "status code", statusMatcher);
	}

	public static Matcher<TestHttpResponse> hasMimeType(Matcher<? super String> mimeTypeMatcher) {
		return isResponse(TestHttpResponse::getMimeType, "MIME type", mimeTypeMatcher);
	}

	public static Matcher<TestHttpResponse> isRedirectTo(String location) {
		return allOf(hasStatus(equalTo(301)), hasHeaderField("Location", hasItem(equalTo(location))));
	}

	public static Matcher<TestHttpResponse> hasHeaderField(String header, Matcher<? super List<String>> headerValueMatcher) {
		return isResponse(httpResponse -> httpResponse.getHeaders().getAllAsList(header), "headers", headerValueMatcher);
	}

	public static Matcher<TestHttpResponse> hasBody(Matcher<? super String> bodyMatcher) {
		return isResponse(httpResponse -> new String(httpResponse.getBody(), UTF_8), "body", bodyMatcher);
	}

	public static Matcher<TestHttpResponse> hasBodyLines(Matcher<? super Collection<String>> bodyLinesMatcher) {
		return isResponse(httpResponse -> asList(new String(httpResponse.getBody(), UTF_8).split("\n")), "body lines", bodyLinesMatcher);
	}

	private static <T> Matcher<TestHttpResponse> isResponse(Function<TestHttpResponse, T> getter, String attribute, Matcher<T> matcher) {
		return new TypeSafeDiagnosingMatcher<TestHttpResponse>() {
			@Override
			protected boolean matchesSafely(TestHttpResponse item, Description mismatchDescription) {
				T value = getter.apply(item);
				if (!matcher.matches(value)) {
					mismatchDescription.appendValue(attribute).appendText(" was ").appendValue(value);
					return false;
				}
				return true;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("is response with ").appendValue(attribute).appendText(" of ").appendDescriptionOf(matcher);
			}
		};
	}

	public TestHttpResponse(int statusCode, String statusReason, MultiValueTable<String, String> headers, String mimeType, byte[] body) {
		this.statusCode = statusCode;
		this.statusReason = statusReason;
		this.headers = headers;
		this.mimeType = mimeType;
		this.body = body;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public String getStatusReason() {
		return statusReason;
	}

	public MultiValueTable<String, String> getHeaders() {
		return headers;
	}

	public String getMimeType() {
		return mimeType;
	}

	public byte[] getBody() {
		return body;
	}

	private final int statusCode;
	private final String statusReason;
	private final MultiValueTable<String, String> headers;
	private final String mimeType;
	private final byte[] body;

}
