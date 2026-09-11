package cz.absolutno.sifry.regexp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.junit.Test;

public class FilterRuleTest {

    @Test
    public void digitRangeMatchesExactlyTheIntegerRange() {
        int[][] cases = {{1, 9}, {1, 10}, {5, 15}, {10, 10}, {1, 99}, {23, 26}, {86, 118},
                {100, 200}, {950, 1025}, {1, 118}, {1, 1025}, {99, 999}, {1000, 1025}, {1, 2000}};
        for (int[] c : cases) {
            Pattern p = Pattern.compile("(?:" + FilterRule.digitRange(c[0], c[1]) + ")");
            for (int n = 1; n <= 2000; n++) {
                boolean expected = n >= c[0] && n <= c[1];
                assertEquals("digitRange(" + c[0] + "," + c[1] + ") vs " + n,
                        expected, p.matcher(String.valueOf(n)).matches());
            }
        }
    }

    @Test
    public void digitRangeRejectsInvalidBounds() {
        assertNull(FilterRule.digitRange(10, 5));
        assertNull(FilterRule.digitRange(0, 3));
    }

    @Test
    public void lexRangeMatchesExactlyTheWordInterval() {
        List<String> words = new ArrayList<String>();
        String alpha = "abc";
        for (char c : alpha.toCharArray())
            words.add(String.valueOf(c));
        for (char c : alpha.toCharArray())
            for (char d : alpha.toCharArray())
                words.add("" + c + d);
        for (char c : alpha.toCharArray())
            for (char d : alpha.toCharArray())
                for (char e : alpha.toCharArray())
                    words.add("" + c + d + e);

        String[][] cases = {{"a", "c"}, {"a", "b"}, {"aa", "cc"}, {"ab", "bc"},
                {"a", "aaa"}, {"ba", "bb"}, {"a", "cba"}};
        for (String[] c : cases) {
            Pattern p = Pattern.compile(FilterRule.lexRange(c[0], c[1]));
            for (String w : words) {
                boolean expected = w.compareTo(c[0]) >= 0 && w.compareTo(c[1]) <= 0;
                assertEquals("lexRange(" + c[0] + "," + c[1] + ") vs " + w,
                        expected, p.matcher(w + ":").find());
            }
        }
    }

    @Test
    public void lexRangeCoversRealWordSlices() {
        Pattern p = Pattern.compile(FilterRule.lexRange("crane", "soare"));
        assertTrue(p.matcher("crane:").find());
        assertTrue(p.matcher("soare:").find());
        assertTrue(p.matcher("scare:").find());
        assertTrue(p.matcher("crazz:").find());
        assertFalse(p.matcher("cranc:").find());
        assertFalse(p.matcher("sopic:").find());
    }

    @Test
    public void lexRangeRejectsReversedBoundsButAcceptsEqual() {
        assertNull(FilterRule.lexRange("soare", "crane"));
        Pattern p = Pattern.compile(FilterRule.lexRange("abc", "abc"));
        assertTrue(p.matcher("abc:").find());
        assertFalse(p.matcher("abd:").find());
    }

    @Test
    public void containsKindPassesRawFreeRegex() {
        assertEquals("ab", FilterRule.pattern(FilterRule.Kind.CONTAINS, null, "ab", null));
        assertEquals(".at", FilterRule.pattern(FilterRule.Kind.CONTAINS, null, ".at", null));
        assertNull(FilterRule.pattern(FilterRule.Kind.CONTAINS, null, "  ", null));
    }

    @Test
    public void anchorKindsEscapeLiteralInput() {
        assertEquals("^\\Qcrane\\E", FilterRule.pattern(FilterRule.Kind.STARTS, null, "crane", null));
        assertEquals("^\\Qcrane\\E:", FilterRule.pattern(FilterRule.Kind.EQUALS, null, "crane", null));
        assertEquals("\\Qcrane\\E:", FilterRule.pattern(FilterRule.Kind.ENDS, null, "crane", null));
    }

    @Test
    public void lengthKindEmitsQuantifierFamilies() {
        assertEquals("^[a-z]{5,5}:", FilterRule.pattern(FilterRule.Kind.LENGTH, FilterRule.Op.EQ, "5", null));
        assertEquals("^[a-z]{1,4}:", FilterRule.pattern(FilterRule.Kind.LENGTH, FilterRule.Op.LT, "5", null));
        assertEquals("^[a-z]{6,40}:", FilterRule.pattern(FilterRule.Kind.LENGTH, FilterRule.Op.GT, "5", null));
        assertEquals("^[a-z]{1,5}:", FilterRule.pattern(FilterRule.Kind.LENGTH, FilterRule.Op.LE, "5", null));
        assertEquals("^[a-z]{5,40}:", FilterRule.pattern(FilterRule.Kind.LENGTH, FilterRule.Op.GE, "5", null));
        assertEquals("^[a-z]{10,20}:", FilterRule.pattern(FilterRule.Kind.LENGTH, FilterRule.Op.BETWEEN, "10", "20"));
        assertNull(FilterRule.pattern(FilterRule.Kind.LENGTH, null, "abc", null));
    }

    @Test
    public void lengthKindClampsToCap() {
        assertEquals("^[a-z]{40,40}:", FilterRule.pattern(FilterRule.Kind.LENGTH, FilterRule.Op.GE, "40", null));
        assertNull(FilterRule.pattern(FilterRule.Kind.LENGTH, FilterRule.Op.GT, "40", null));
        assertNull(FilterRule.pattern(FilterRule.Kind.LENGTH, FilterRule.Op.BETWEEN, "5", "4"));
    }

    @Test
    public void atomicNumberKindMatchesDisplayLine() {
        Pattern lt2 = Pattern.compile(FilterRule.pattern(FilterRule.Kind.ATOMIC_NUMBER, FilterRule.Op.LT, "2", null));
        assertTrue(lt2.matcher("Hydrogen (H, 1)").find());
        assertFalse(lt2.matcher("Helium (He, 2)").find());

        Pattern is86 = Pattern.compile(FilterRule.pattern(FilterRule.Kind.ATOMIC_NUMBER, FilterRule.Op.EQ, "86", null));
        assertTrue(is86.matcher("Radon (Rn, 86)").find());
        assertFalse(is86.matcher("Radium (Ra, 88)").find());

        assertNull(FilterRule.pattern(FilterRule.Kind.ATOMIC_NUMBER, FilterRule.Op.GT, "118", null));

        Pattern big = Pattern.compile(FilterRule.pattern(FilterRule.Kind.ATOMIC_NUMBER, FilterRule.Op.GE, "86", null));
        assertTrue(big.matcher("Radon (Rn, 86)").find());
        Pattern le85 = Pattern.compile(FilterRule.pattern(FilterRule.Kind.ATOMIC_NUMBER, FilterRule.Op.LE, "85", null));
        assertFalse(le85.matcher("Radium (Ra, 88)").find());
    }

    @Test
    public void dexNumberKindMatchesDisplayLine() {
        Pattern eq25 = Pattern.compile(FilterRule.pattern(FilterRule.Kind.DEX_NUMBER, FilterRule.Op.EQ, "25", null));
        assertTrue(eq25.matcher("Pikachu (Electric) #25").find());
        assertFalse(eq25.matcher("Raichu (Electric) #26").find());

        Pattern bw = Pattern.compile(FilterRule.pattern(FilterRule.Kind.DEX_NUMBER, FilterRule.Op.BETWEEN, "23", "26"));
        assertTrue(bw.matcher("Arbok (Poison) #24").find());
        assertTrue(bw.matcher("Pikachu (Electric) #25").find());
        assertFalse(bw.matcher("Ekans (Poison) #22".replace("22", "22")).find());
    }

    @Test
    public void symbolKindIsCaseInsensitive() {
        Pattern p = Pattern.compile(FilterRule.pattern(FilterRule.Kind.SYMBOL, null, "fe", null));
        assertTrue(p.matcher("Iron (Fe, 26)").find());
        assertFalse(p.matcher("Iron (Fi, 26)").find());
    }

    @Test
    public void typeKindMatchesAnyWordInTypeList() {
        Pattern p = Pattern.compile(FilterRule.pattern(FilterRule.Kind.TYPE, null, "grass", null));
        assertTrue(p.matcher("Bulbasaur (Grass, Poison) #1").find());
        assertTrue(p.matcher("Bulbasaur (Poison, Grass) #1").find());
        assertFalse(p.matcher("Charmander (Fire) #4").find());
    }

    @Test
    public void rangeKindComposesFullWordRegex() {
        Pattern p = Pattern.compile(FilterRule.pattern(FilterRule.Kind.RANGE, null, "crane", "soare"));
        assertTrue(p.matcher("crane:some answer").find());
        assertTrue(p.matcher("soare:some answer").find());
        assertNull(FilterRule.pattern(FilterRule.Kind.RANGE, null, "soare", "crane"));
        assertNull(FilterRule.pattern(FilterRule.Kind.RANGE, null, "", "crane"));
    }

    @Test
    public void kindsForMapToTheirSource() {
        List<FilterRule.Kind> word = FilterRule.kindsFor("wordle.canon");
        assertTrue(word.contains(FilterRule.Kind.RANGE));
        assertTrue(word.contains(FilterRule.Kind.LENGTH));
        assertFalse(word.contains(FilterRule.Kind.SYMBOL));
        List<FilterRule.Kind> per = FilterRule.kindsFor("periodic.canon");
        assertTrue(per.contains(FilterRule.Kind.SYMBOL));
        assertTrue(per.contains(FilterRule.Kind.ATOMIC_NUMBER));
        assertFalse(per.contains(FilterRule.Kind.TYPE));
        List<FilterRule.Kind> pok = FilterRule.kindsFor("pokemon.canon");
        assertTrue(pok.contains(FilterRule.Kind.TYPE));
        assertTrue(pok.contains(FilterRule.Kind.DEX_NUMBER));
        assertFalse(pok.contains(FilterRule.Kind.RANGE));
    }

    @Test
    public void kindsForAllUnionsTheSearchedSources() {
        List<FilterRule.Kind> both = FilterRule.kindsForAll(
                Arrays.asList("periodic.canon", "pokemon.canon"));
        assertTrue(both.contains(FilterRule.Kind.SYMBOL));
        assertTrue(both.contains(FilterRule.Kind.ATOMIC_NUMBER));
        assertTrue(both.contains(FilterRule.Kind.TYPE));
        assertTrue(both.contains(FilterRule.Kind.DEX_NUMBER));
        assertFalse(both.contains(FilterRule.Kind.RANGE));
        assertFalse(both.contains(FilterRule.Kind.LENGTH));

        List<FilterRule.Kind> all = FilterRule.kindsForAll(
                Arrays.asList("en.canon", "periodic.canon", "pokemon.canon"));
        assertTrue(all.contains(FilterRule.Kind.RANGE));
        assertTrue(all.contains(FilterRule.Kind.LENGTH));
        assertTrue(all.contains(FilterRule.Kind.SYMBOL));
        assertTrue(all.contains(FilterRule.Kind.TYPE));

        List<FilterRule.Kind> none = FilterRule.kindsForAll(new ArrayList<String>());
        assertFalse(none.contains(FilterRule.Kind.SYMBOL));
        assertFalse(none.contains(FilterRule.Kind.RANGE));

        assertTrue(FilterRule.kindsForAll(Arrays.asList("en.canon"))
                .equals(FilterRule.kindsFor("en.canon")));
    }

    @Test
    public void numericKindRecognized() {
        assertTrue(FilterRule.isNumeric(FilterRule.Kind.LENGTH));
        assertTrue(FilterRule.isNumeric(FilterRule.Kind.ATOMIC_NUMBER));
        assertTrue(FilterRule.isNumeric(FilterRule.Kind.DEX_NUMBER));
        assertFalse(FilterRule.isNumeric(FilterRule.Kind.SYMBOL));
        assertFalse(FilterRule.isNumeric(FilterRule.Kind.CONTAINS));
    }

    @Test
    public void impossibleNumericKindsAreSkipped() {
        assertNull(FilterRule.pattern(FilterRule.Kind.LENGTH, FilterRule.Op.LT, "1", null));
        assertNull(FilterRule.pattern(FilterRule.Kind.LENGTH, FilterRule.Op.BETWEEN, "40", "1"));
        assertNull(FilterRule.pattern(FilterRule.Kind.ATOMIC_NUMBER, FilterRule.Op.GT, "118", null));
        assertNull(FilterRule.pattern(FilterRule.Kind.DEX_NUMBER, FilterRule.Op.GT, "1025", null));
    }
}