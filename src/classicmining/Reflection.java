package classicmining;

import java.lang.reflect.*;

public final class Reflection {
    private Reflection() {}
    public static Method method(Class<?> type, int count, String... names) {
        for (String name : names) {
            for (Class<?> c = type; c != null; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (m.getName().equals(name) && m.getParameterTypes().length == count) {
                        m.setAccessible(true);
                        return m;
                    }
                }
            }
        }
        throw new IllegalStateException("Missing method " + type.getName() + " " + java.util.Arrays.toString(names));
    }
    public static Object call(Method m, Object target, Object... args) {
        try { return m.invoke(target, args); }
        catch (InvocationTargetException e) { throw new IllegalStateException(m.toString(), e.getCause()); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(m.toString(), e); }
    }
    public static Object field(Object target, String name) {
        try { return target.getClass().getField(name).get(target); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(name, e); }
    }
    public static Class<?> type(String name) {
        try { return Class.forName(name); }
        catch (ClassNotFoundException e) { throw new IllegalStateException(name, e); }
    }
}
