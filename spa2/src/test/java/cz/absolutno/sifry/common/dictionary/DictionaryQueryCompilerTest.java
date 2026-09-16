package cz.absolutno.sifry.common.dictionary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.regex.Pattern;

import org.junit.Test;

public class DictionaryQueryCompilerTest {

    private static boolean matchesKey(String pattern, String key) {
        if (pattern == null || pattern.isEmpty())
            return true;
        return Pattern.compile(pattern).matcher(key + ":v").find();
    }

    private static boolean matchesLine(String pattern, String key) {
        return matchesKey(pattern, key);
    }

    @Test
    public void exactKeyMatchesOnlyWholeKey() {
        String p = DictionaryQueryCompiler.exactKeyPattern("AbCd");
        assertEquals(FilterRulePattern.probeEquals("abcd"), p);
        assertTrue(matchesKey(p, "abcd"));
        assertFalse(matchesKey(p, "abcde"));
        assertFalse(matchesKey(p, "abc"));
        assertFalse(matchesKey(p, "abcy"));
        assertFalse(matchesKey(p, "xabcd"));
    }

    @Test
    public void exactKeyBlankYieldsEmpty() {
        assertEquals("", DictionaryQueryCompiler.exactKeyPattern(""));
        assertEquals("", DictionaryQueryCompiler.exactKeyPattern(null));
        assertEquals("", DictionaryQueryCompiler.exactKeyPattern("   "));
    }

    @Test
    public void emptyQueryYieldsEmpty() {
        assertEquals("", DictionaryQueryCompiler.compile(WordPatternQuery.builder().build()));
        assertEquals("", DictionaryQueryCompiler.compile(null));
    }

    @Test
    public void lengthOnly() {
        String p = DictionaryQueryCompiler.compile(WordPatternQuery.builder().length(4).build());
        assertEquals("^(?=[a-z]{4}:)[a-z]{4}:", p);
        assertTrue(matchesKey(p, "word"));
        assertTrue(matchesKey(p, "walk"));
        assertFalse(matchesKey(p, "wordy"));
        assertFalse(matchesKey(p, "wrd"));
    }

    @Test
    public void fixedLetters() {
        String p = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .length(4).fixedLetter(1, 'p').fixedLetter(4, 'k').build());
        assertEquals("^(?=[a-z]{4}:)p[a-z][a-z]k:", p);
        assertTrue(matchesKey(p, "peak"));
        assertTrue(matchesKey(p, "puck"));
        assertFalse(matchesKey(p, "packy"));
        assertFalse(matchesKey(p, "wack"));
        assertFalse(matchesKey(p, "paki"));
    }

    @Test
    public void equalityGroup() {
        String p = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .length(5).equalityGroup(2, 5).build());
        assertTrue(matchesKey(p, "aqcdq"));
        assertTrue(matchesKey(p, "zzzzz"));
        assertFalse(matchesKey(p, "abcda"));
        assertFalse(matchesKey(p, "abbba"));
    }

    @Test
    public void inequalityGroup() {
        String p = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .length(5).inequalityGroup(2, 5).build());
        assertTrue(matchesKey(p, "abcda"));
        assertTrue(matchesKey(p, "abxba"));
        assertFalse(matchesKey(p, "aqcdq"));
    }

    @Test
    public void allLettersDifferent() {
        String p = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .length(4).allLettersDifferent(true).build());
        assertTrue(matchesKey(p, "abcd"));
        assertTrue(matchesKey(p, "wxyz"));
        assertFalse(matchesKey(p, "abca"));
        assertFalse(matchesKey(p, "abac"));
        assertFalse(matchesKey(p, "aabb"));
    }

    @Test
    public void allUnmatchedLettersDifferent() {
        String p = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .length(5).fixedLetter(3, 'u').allUnmatchedLettersDifferent(true).build());
        assertTrue(matchesKey(p, "axuyv"));
        assertFalse(matchesKey(p, "axuyx"));
        assertFalse(matchesKey(p, "aauyv"));
    }

    @Test
    public void prefixSuffixContainsUnbounded() {
        String p = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .prefix("str").build());
        assertTrue(matchesKey(p, "straw"));
        assertTrue(matchesKey(p, "str"));
        assertFalse(matchesKey(p, "saraw"));
        assertFalse(matchesKey(p, "xstraw"));

        p = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .suffix("ing").build());
        assertTrue(matchesKey(p, "running"));
        assertTrue(matchesKey(p, "ing"));
        assertFalse(matchesKey(p, "rungnn"));
        assertFalse(matchesKey(p, "runningx"));

        p = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .prefix("ex").suffix("ly").build());
        assertTrue(matchesKey(p, "exactly"));
        assertFalse(matchesKey(p, "axactally"));

        p = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .contains("q").build());
        assertTrue(matchesKey(p, "quaint"));
        assertFalse(matchesKey(p, "paint"));
    }

    @Test
    public void prefixSuffixContainsFoldCaseAtBoundary() {
        String upper = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .prefix("STR").suffix("ING").contains("N F O R M A T").build());
        String lower = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .prefix("str").suffix("ing").contains("nformat").build());
        assertEquals(lower, upper);
        assertTrue(matchesKey(upper, "strnformatzing"));
        assertFalse(matchesKey(upper, "straw"));
        assertFalse(matchesKey(upper, "nformat"));
        assertFalse(matchesKey(lower, "nformat"));
    }

    @Test
    public void containsFramesNformatFixtureFromAnyCaseSpacing() {
        String p = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .contains("N F O R M A T").build());
        assertTrue(matchesKey(p, "information"));
        assertTrue(matchesKey(p, "disinformation"));
        assertEquals(p, DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .contains("nFoRmAt").build()));
    }

    @Test
    public void boundedContainsFoldsCase() {
        String upper = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .length(11).contains("NFORMAT").build());
        String lower = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .length(11).contains("nformat").build());
        assertEquals(lower, upper);
        assertTrue(matchesKey(upper, "information"));
    }

    @Test
    public void containsCombinedWithLength() {
        String p = DictionaryQueryCompiler.compile(WordPatternQuery.builder()
                .length(5).contains("qu").build());
        assertTrue(matchesKey(p, "quote"));
        assertTrue(matchesKey(p, "quark"));
        assertFalse(matchesKey(p, "qtrze"));
        assertFalse(matchesKey(p, "quartz"));
    }

    @Test
    public void positionalWithoutLengthRejected() {
        try {
            WordPatternQuery.builder().fixedLetter(1, 'a').build();
            fail("expected IllegalStateException");
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void positionBeyondLengthRejected() {
        try {
            DictionaryQueryCompiler.compile(WordPatternQuery.builder().length(3).fixedLetter(5, 'a').build());
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
        try {
            DictionaryQueryCompiler.compile(WordPatternQuery.builder().length(3).equalityGroup(2, 9).build());
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void parityWithFilterRuleEquals() {
        String ours = DictionaryQueryCompiler.exactKeyPattern("walk");
        String filter = FilterRulePattern.probeEquals("walk");
        assertEquals(filter, ours);
        assertTrue(matchesKey(ours, "walk"));
        assertFalse(matchesKey(ours, "walker"));
    }

    @Test
    public void parityWithFilterRuleStartsEndsContains() {
        assertTrue(matchesKey(DictionaryQueryCompiler.compile(
                WordPatternQuery.builder().prefix("str").build()), "straw"));
        assertTrue(matchesLine(FilterRulePattern.probeStarts("str"), "straw"));
        assertTrue(matchesKey(DictionaryQueryCompiler.compile(
                WordPatternQuery.builder().suffix("ing").build()), "running"));
        assertTrue(matchesLine(FilterRulePattern.probeEnds("ing"), "running"));
        assertTrue(matchesKey(DictionaryQueryCompiler.compile(
                WordPatternQuery.builder().contains("qu").build()), "quaint"));
        assertTrue(matchesLine(FilterRulePattern.probeContains("qu"), "quaint"));
        assertFalse(matchesLine(FilterRulePattern.probeContains("qu"), "paint"));
        assertFalse(matchesLine(FilterRulePattern.probeStarts("str"), "saraw"));
        assertFalse(matchesLine(FilterRulePattern.probeEnds("ing"), "rungnn"));
    }

    private static final class FilterRulePattern {
        private static String probeEquals(String w) {
            return cz.absolutno.sifry.regexp.FilterRule.pattern(
                    cz.absolutno.sifry.regexp.FilterRule.Kind.EQUALS,
                    cz.absolutno.sifry.regexp.FilterRule.Op.EQ, w, null);
        }

        private static String probeStarts(String w) {
            return cz.absolutno.sifry.regexp.FilterRule.pattern(
                    cz.absolutno.sifry.regexp.FilterRule.Kind.STARTS,
                    cz.absolutno.sifry.regexp.FilterRule.Op.EQ, w, null);
        }

        private static String probeEnds(String w) {
            return cz.absolutno.sifry.regexp.FilterRule.pattern(
                    cz.absolutno.sifry.regexp.FilterRule.Kind.ENDS,
                    cz.absolutno.sifry.regexp.FilterRule.Op.EQ, w, null);
        }

        private static String probeContains(String w) {
            return cz.absolutno.sifry.regexp.FilterRule.pattern(
                    cz.absolutno.sifry.regexp.FilterRule.Kind.CONTAINS,
                    cz.absolutno.sifry.regexp.FilterRule.Op.EQ, w, null);
        }
    }

}