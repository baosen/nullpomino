package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;

/**
 * Covers the NetUtil class declaration / default constructor (line 21).
 */
class NetUtilConstructorTest {

    @Test
    void netUtilClassIsPublic() {
        assertEquals("NetUtil", NetUtil.class.getSimpleName());
    }

    @Test
    void netUtilCanBeInstantiated() throws Exception {
        // The default constructor is implicitly public.
        Constructor<NetUtil> ctor = NetUtil.class.getDeclaredConstructor();
        assertNotNull(ctor);

        NetUtil instance = ctor.newInstance();
        assertNotNull(instance);
    }
}
