package test.anonymous;

public class AnonOuter {

    public void runAnonProcess() {
        execute(new Processor() {

            @Override
            public void process() {
                System.out.println("processed");
            }

        });
    }

    public void execute(Processor p) {
        p.process();
    }
}

abstract class Processor {

    public void process() {

    }
}
