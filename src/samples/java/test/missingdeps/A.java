package test.missingdeps;

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class A {
    public void parse(String name) throws IOException {
        
        InputStream is = A.class.getResourceAsStream(name);

        ClassReader r = new ClassReader(is);
        r.accept(new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS), ClassReader.SKIP_DEBUG);
    }
}
