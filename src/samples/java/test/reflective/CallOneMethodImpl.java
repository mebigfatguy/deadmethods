package test.reflective;

public class CallOneMethodImpl {

    public void useReflection(String clsName) throws Exception {
        Class<?> c = Class.forName(clsName);
        OneMethod o = (OneMethod) c.getConstructor(String.class).newInstance("Foo");

        o.oneMethod();
    }
}
