package freenet.test;

import freenet.l10n.BaseL10n;
import freenet.l10n.BaseL10nTest;
import freenet.l10n.NodeL10n;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static freenet.l10n.BaseL10n.LANGUAGE.ENGLISH;
import static freenet.l10n.BaseL10n.LANGUAGE.GERMAN;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;

public class UseTestTranslationTest {

	@Test
	public void beforeMethodActivatesTestTranslations() throws Throwable {
		BaseL10nTest.useTranslation(BaseL10nTest.createL10n(GERMAN));
		useTestTranslation.apply(new Statement() {
			@Override
			public void evaluate() {
				assertThat(NodeL10n.getBase().getString("EmptyTranslation.Message"), equalTo("Test message of EmptyTranslation Rule"));
			}
		}, Description.EMPTY).evaluate();
	}

	@Test
	public void afterMethodRestoresPreviouslyUsedTranslations() throws Throwable {
		BaseL10n baseL10n = BaseL10nTest.createL10n(ENGLISH);
		BaseL10nTest.useTranslation(baseL10n);
		useTestTranslation.apply(new Statement() {
			@Override
			public void evaluate() {
			}
		}, Description.EMPTY).evaluate();
		assertThat(NodeL10n.getBase(), sameInstance(baseL10n));
	}

	private final UseTestTranslation useTestTranslation = new UseTestTranslation();

}
