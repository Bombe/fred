package freenet.test;

import freenet.support.api.Bucket;
import freenet.support.api.HTTPRequest;
import freenet.support.api.HTTPUploadedFile;
import freenet.support.api.RandomAccessBucket;
import freenet.support.io.ArrayBucket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import javax.naming.SizeLimitExceededException;

import static java.lang.Math.min;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;

/**
 * Simple implementation of {@link HTTPRequest}, hopefully  suitable for most
 * tests.
 * <p style="font-weight: bold;">
 * At the moment many methods are throwing
 * {@link UnsupportedOperationException}s, because they have not been needed
 * for any tests yet.
 * </p>
 */
public class TestHTTPRequest implements HTTPRequest {

	/**
	 * Sets a parameter on the request.
	 *
	 * @param name The name of the parameter
	 * @param value The value of the parameter
	 */
	public void setRequestParameter(String name, String value) {
		parameters.compute(name, (key, values) -> (values == null) ? new ArrayList<>() : values).add(value);
	}

	/**
	 * Sets the request part with the given name to the byte array generated
	 * by encoding the given value to UTF-8.
	 *
	 * @param name The name of the part
	 * @param value The value of the part
	 */
	public void setRequestPart(String name, String value) {
		setRequestPart(name, value.getBytes(UTF_8));
	}

	/**
	 * Sets the request part with the given name to the given byte array.
	 *
	 * @param name The name of the part
	 * @param value The value of the part
	 */
	public void setRequestPart(String name, byte[] value) {
		requestParts.put(name, value);
	}

	@Override
	public String getPath() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean hasParameters() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isParameterSet(String name) {
		return parameters.containsKey(name);
	}

	@Override
	public String getParam(String name) {
		return parameters.getOrDefault(name, emptyList()).stream().findFirst().orElse(null);
	}

	@Override
	public String getParam(String name, String defaultValue) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getIntParam(String name) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getIntParam(String name, int defaultValue) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getIntPart(String name, int defaultValue) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String[] getMultipleParam(String name) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int[] getMultipleIntParam(String name) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public HTTPUploadedFile getUploadedFile(String name) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public RandomAccessBucket getPart(String name) {
		return Optional.ofNullable(requestParts.get(name)).map(ArrayBucket::new).orElse(null);
	}

	@Override
	public boolean isPartSet(String name) {
		return requestParts.containsKey(name);
	}

	@Override
	public String getPartAsString(String name, int maxLength) {
		return new String(getPartAsBytes(name, maxLength), UTF_8);
	}

	@Override
	public String getPartAsStringThrowing(String name, int maxLength) throws NoSuchElementException, SizeLimitExceededException {
		return new String(getPartAsBytesThrowing(name, maxLength), UTF_8);
	}

	@Override
	public String getPartAsStringFailsafe(String name, int maxLength) {
		return new String(getPartAsBytesFailsafe(name, maxLength), UTF_8);
	}

	@Override
	public byte[] getPartAsBytes(String name, int maxLength) {
		if (!requestParts.containsKey(name)) {
			return new byte[0];
		}
		byte[] data = requestParts.get(name);
		if (data.length > maxLength) {
			return new byte[0];
		}
		return data;
	}

	@Override
	public byte[] getPartAsBytesThrowing(String name, int maxlength) throws NoSuchElementException, SizeLimitExceededException {
		if (!requestParts.containsKey(name)) {
			throw new NoSuchElementException(name);
		}
		byte[] data = requestParts.get(name);
		if (data.length > maxlength) {
			throw new SizeLimitExceededException();
		}
		return data;
	}

	@Override
	public byte[] getPartAsBytesFailsafe(String name, int maxLength) {
		if (!requestParts.containsKey(name)) {
			return new byte[0];
		}
		byte[] data = requestParts.get(name);
		return Arrays.copyOf(data, min(data.length, maxLength));
	}

	@Override
	public void freeParts() {
	}

	@Override
	public long getLongParam(String name, long defaultValue) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getMethod() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Bucket getRawData() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String getHeader(String name) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public int getContentLength() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String[] getParts() {
		return requestParts.keySet().toArray(new String[0]);
	}

	@Override
	public Collection<String> getParameterNames() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isIncognito() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public boolean isChrome() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	private final Map<String, List<String>> parameters = new HashMap<>();
	private final Map<String, byte[]> requestParts = new HashMap<>();

}
