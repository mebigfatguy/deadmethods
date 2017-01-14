package test.dynamic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Dynamic {

    public void dynamics() throws IOException {
        Stream<Path> stream = Files.list(JavaFileFilter.toPath())
        .filter(JavaFileFilter::isJavaFile);
    }
    
    static class JavaFileFilter {
        
        public static Path toPath() {
            return Paths.get("fee", "fi", "fo", "fum");
        }
        
        public static boolean isJavaFile(Path path) {
            return path.toString().endsWith(".java");
        }
    }
}
