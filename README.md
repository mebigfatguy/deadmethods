# DeadMethods

deadmethods is an ant task that will find uncalled methods regardless of access qualifier.
Licensed by Apache 2.0 license

This project uses ant, to build do

    ant

You can run deadmethods on itself, by running

    ant test

To run it on your project do the following:

Copy the deadmethods.jar and the asm.jar to your ~/.ant/lib directory

Then create a task such as:

    <target name="dm" xmlns:dm="antlib:com.mebigfatguy.deadmethods"> 
        <dm:deadmethods>
            <classpath refid="your.classes.classpath"/>
            <auxClasspath refid="your.aux.classpath"/>
            <reflectiveAnnotation name="YourAnnotation"/>
            <reflectiveAnnotation name="YourOtherAnnotation"/>
            <ignoredPackage pattern="com.you.ignore.me.*"/>
            <ignoredPackage pattern="you.ignore.me2.*"/>
            <ignoredClass pattern=".*MBean"/>
            <ignoredMethod pattern="_get_.*"/>
        </dm:deadmethods>
    </target>

  
Obviously, methods will be reported as 'dead' even though they are important to you. This will
happen because
* Methods are used via reflection
* Methods are part of an api, that clients are expected to use
* Methods are called from unit tests, or other code not included in your deadmethods ant task

Care should be taken to not indescriminately remove methods reported by this tool. This tool offers you
a starting point. Use due diligence to validate what the results are.
    
    
#### Deadmethods is available on maven with the following coordinates:

    groupId: com.mebigfatguy.deadmethods
    artifactId: deadmethods
    version: 0.6.0


