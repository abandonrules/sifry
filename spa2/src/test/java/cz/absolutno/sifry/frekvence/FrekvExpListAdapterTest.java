package cz.absolutno.sifry.frekvence;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import cz.absolutno.sifry.R;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "en-rUS")
public class FrekvExpListAdapterTest {

    // input "ABAB CD": letters A2 B2 C1 D1, one space, two words
    private static final int GROUP_COUNT = 8;
    private static final int[] GROUP_IDS = {
            R.id.idFDGPismenaC,
            R.id.idFDGVseC,
            R.id.idFDGPismenaP,
            R.id.idFDGDelkyC,
            R.id.idFDGDelkyP,
            R.id.idFDGPrvni,
            R.id.idFDGPosledni,
            R.id.idFDGText,
    };
    private static final int[] CHILDREN = {3, 2, 26, 1, 4, 1, 1, 5};

    private static String childS(FrekvExpListAdapter a, int g, int c) {
        try {
            Object ret = FrekvExpListAdapter.class
                    .getMethod("getChild", int.class, int.class).invoke(a, g, c);
            Field f = ret.getClass().getDeclaredField("s");
            f.setAccessible(true);
            return (String) f.get(ret);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void groupStructure() {
        FrekvExpListAdapter a = new FrekvExpListAdapter();
        assertEquals(0, a.getGroupCount());

        a.go("ABAB CD");
        assertEquals(GROUP_COUNT, a.getGroupCount());
        for (int i = 0; i < GROUP_COUNT; i++)
            assertEquals(GROUP_IDS[i], a.getGroupId(i));
        for (int i = 0; i < GROUP_COUNT; i++)
            assertEquals("children of group " + i, CHILDREN[i], a.getChildrenCount(i));
    }

    @Test
    public void mergedLetterRows() {
        FrekvExpListAdapter a = new FrekvExpListAdapter();
        a.go("ABAB CD");
        assertEquals("A, B", childS(a, 0, 0)); // count 2
        assertEquals("C, D", childS(a, 0, 1)); // count 1
    }

    @Test
    public void mergedAllCharsRows() {
        FrekvExpListAdapter a = new FrekvExpListAdapter();
        a.go("ABAB CD");
        assertEquals("A, B", childS(a, 1, 0));
        assertEquals(2, a.getChildrenCount(1));
    }

    @Test
    public void wordLengthRows() {
        FrekvExpListAdapter a = new FrekvExpListAdapter();
        a.go("ABAB CD"); // words of length 2 and 4
        assertEquals("2, 4", childS(a, 3, 0)); // both lengths, merged (equal count)
        assertEquals(1, a.getChildrenCount(3));
    }

    @Test
    public void repeatedGoResets() {
        FrekvExpListAdapter a = new FrekvExpListAdapter();
        a.go("ABAB CD");
        a.go("XY"); // single word: no delky/prvni/posledni/... groups
        assertEquals(3, a.getGroupCount());
        assertEquals(R.id.idFDGPismenaC, a.getGroupId(0));
        assertEquals(R.id.idFDGPismenaP, a.getGroupId(1));
        assertEquals(R.id.idFDGText, a.getGroupId(2));
        assertEquals(2, a.getChildrenCount(0)); // "X, Y" merged (count 1) + zero-count row
        assertEquals(26, a.getChildrenCount(1));
        assertEquals(4, a.getChildrenCount(2)); // letters, digits, spaces, all
    }
}