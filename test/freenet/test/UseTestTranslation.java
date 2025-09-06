package freenet.test;

import freenet.l10n.BaseL10n;
import freenet.l10n.BaseL10nTest;
import freenet.l10n.NodeL10n;
import org.junit.rules.ExternalResource;

/**
 * JUnit rule that switches the current translation to the test translations
 * before running a test, and back to the previous value after the test.
 * <h2>Usage</h2>
 * <p>
 * Include the rule in your test by adding a {@code public final} field to
 * your test class:
 * </p>
 * <pre>
 * public class SomeTest {
 *     &#64;Test
 *     public void testSomeTranslation() {
 *         String title = getTranslatedTitle();
 *         assertThat(title, equalTo("SomeTest.Title"));
 *     }
 *     &#64;Rule
 *     public final UseTestTranslation useTestTranslation = new UseTestTranslation();
 * }
 * </pre>
 * <p>
 * The rule will make sure that during your test the real translations are
 * not used, so your test doesn’t have to know which language the system is
 * configured to use. Instead, for each translated value the key for the
 * translated value is returned, making it trivial to check that the correct
 * translation key was used.
 * </p>
 * <p>
 * After the test, the previously used translation will be restored.
 * </p>
 *
 * @see BaseL10nTest#useTestTranslation()
 */
public class UseTestTranslation extends ExternalResource {

	@Override
	protected void before() throws Throwable {
		baseL10n = NodeL10n.getBase();
		BaseL10nTest.useTestTranslation();
	}

	@Override
	protected void after() {
		BaseL10nTest.useTranslation(baseL10n);
	}

	private BaseL10n baseL10n;

}
