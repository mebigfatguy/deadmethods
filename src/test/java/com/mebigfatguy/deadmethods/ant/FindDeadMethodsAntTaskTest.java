package com.mebigfatguy.deadmethods.ant;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.junit.Test;

import java.io.File;

public class FindDeadMethodsAntTaskTest {

    @Test
    public void testAnt() {

        FindDeadMethodsAntTask t = new FindDeadMethodsAntTask();
        Project p = new Project();
        p.addBuildListener(new LoggingListener());
        t.setProject(p);

        File base = new File(System.getProperty("user.dir"));
        Path cp = new Path(p);
        cp.setLocation(new File(base, "target/classes"));
        t.addConfiguredClasspath(cp);

        Path aux = new Path(p);
        t.addConfiguredAuxClasspath(aux);

        t.execute();
    }

    class LoggingListener implements BuildListener {
        @Override
        public void buildStarted(BuildEvent buildEvent) {
        }

        @Override
        public void buildFinished(BuildEvent buildEvent) {
        }

        @Override
        public void targetStarted(BuildEvent buildEvent) {
        }

        @Override
        public void targetFinished(BuildEvent buildEvent) {
        }

        @Override
        public void taskStarted(BuildEvent buildEvent) {
        }

        @Override
        public void taskFinished(BuildEvent buildEvent) {
        }

        @Override
        public void messageLogged(BuildEvent buildEvent) {
            System.out.println(buildEvent.getMessage());
        }
    }
}
