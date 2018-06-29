/*
 * deadmethods - A unused methods detector
 * Copyright 2011-2018 MeBigFatGuy.com
 * Copyright 2011-2018 Dave Brosius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mebigfatguy.deadmethods.ant;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mebigfatguy.deadmethods.CalledMethodRemovingClassVisitor;
import com.mebigfatguy.deadmethods.ClassInfo;
import com.mebigfatguy.deadmethods.ClassRepository;
import com.mebigfatguy.deadmethods.IgnoredClass;
import com.mebigfatguy.deadmethods.IgnoredMethod;
import com.mebigfatguy.deadmethods.IgnoredPackage;
import com.mebigfatguy.deadmethods.MethodInfo;
import com.mebigfatguy.deadmethods.ReflectiveAnnotation;
import com.mebigfatguy.deadmethods.TaskFactory;

public class FindDeadMethods extends Task {

    private static final String DEFAULT_REFLECTIVE_ANNOTATION_PATH = "/com/mebigfatguy/deadmethods/defaultReflectiveAnnotations.properties";

    Path path;
    Path auxPath;
    Set<IgnoredPackage> ignoredPackages = new HashSet<>();
    Set<IgnoredClass> ignoredClasses = new HashSet<>();
    Set<IgnoredMethod> ignoredMethods = new HashSet<>();
    Set<ReflectiveAnnotation> reflectiveAnnotations = new HashSet<>();

    public void addConfiguredClasspath(final Path classpath) {
        path = classpath;
    }

    public void addConfiguredAuxClasspath(final Path auxClassPath) {
        auxPath = auxClassPath;
    }

    public IgnoredPackage createIgnoredPackage() {
        IgnoredPackage ip = new IgnoredPackage();
        ignoredPackages.add(ip);
        return ip;
    }

    public IgnoredClass createIgnoredClass() {
        IgnoredClass ic = new IgnoredClass();
        ignoredClasses.add(ic);
        return ic;
    }

    public IgnoredMethod createIgnoredMethod() {
        IgnoredMethod im = new IgnoredMethod();
        ignoredMethods.add(im);
        return im;
    }

    public ReflectiveAnnotation createReflectiveAnnotation() {
        ReflectiveAnnotation ra = new ReflectiveAnnotation();
        reflectiveAnnotations.add(ra);
        return ra;
    }

    @Override
    public void execute() throws BuildException {
        if (path == null) {
            throw new BuildException("classpath attribute not set");
        }

        if (auxPath == null) {
            auxPath = new Path(getProject());
        }

        loadDefaultReflectiveAnnotations();

        TaskFactory.setTask(this);

        ClassRepository repo = new ClassRepository(path, auxPath);
        Set<String> allMethods = new TreeSet<>();
        try {
            classloop: for (String className : repo) {
                if (!className.startsWith("[")) {
                    ClassInfo classInfo = repo.getClassInfo(className);
                    String packageName = classInfo.getPackageName();
                    for (IgnoredPackage ip : ignoredPackages) {
                        Matcher m = ip.getPattern().matcher(packageName);
                        if (m.matches()) {
                            continue classloop;
                        }
                    }
                    String clsName = classInfo.getClassName();
                    for (IgnoredClass ic : ignoredClasses) {
                        Matcher m = ic.getPattern().matcher(clsName);
                        if (m.matches()) {
                            continue classloop;
                        }
                    }

                    Set<MethodInfo> methods = classInfo.getMethodInfo();

                    add: for (MethodInfo methodInfo : methods) {
                        for (IgnoredMethod im : ignoredMethods) {
                            Matcher m = im.getPattern().matcher(methodInfo.getMethodName());
                            if (m.matches()) {
                                continue add;
                            }
                        }

                        allMethods.add(className + ":" + methodInfo.getMethodName() + methodInfo.getMethodSignature());
                    }
                }
            }

            getProject().log("Method repository build", Project.MSG_VERBOSE);

            removeObjectMethods(repo, allMethods);
            removeMainMethods(repo, allMethods);
            removeNoArgCtors(repo, allMethods);
            removeJUnitMethods(repo, allMethods);
            removeReflectiveAnnotatedMethods(repo, allMethods);
            removeInterfaceImplementationMethods(repo, allMethods);
            removeAnonymousInnerImplementationMethods(repo, allMethods);
            removeSyntheticMethods(repo, allMethods);
            removeStandardEnumMethods(repo, allMethods);
            removeSpecialSerializableMethods(repo, allMethods);
            removeAnnotations(repo, allMethods);
            removeSpringMethods(repo, allMethods);
            removeSPIClasses(repo, allMethods);
            removeWebMethods(repo, allMethods);

            for (String className : repo) {
                try (InputStream is = repo.getClassStream(className)) {
                    ClassReader r = new ClassReader(is);
                    r.accept(new CalledMethodRemovingClassVisitor(repo, allMethods), ClassReader.SKIP_DEBUG);
                }
            }

            for (String m : allMethods) {
                System.out.println(m);
            }
        } catch (Exception ioe) {
            throw new BuildException("Failed collecting methods: " + ioe.getMessage(), ioe);
        }
    }

    private void loadDefaultReflectiveAnnotations() {
        try (BufferedInputStream bis = new BufferedInputStream(FindDeadMethods.class.getResourceAsStream(DEFAULT_REFLECTIVE_ANNOTATION_PATH))) {
            Properties p = new Properties();
            p.load(bis);
            for (Object k : p.keySet()) {
                ReflectiveAnnotation ra = new ReflectiveAnnotation();
                ra.setName(k.toString().trim());
                reflectiveAnnotations.add(ra);
            }
        } catch (IOException e) {
            // just go on assuming no annotations
        }
    }

    private void removeObjectMethods(ClassRepository repo, Set<String> methods) throws IOException {
        ClassInfo info = repo.getClassInfo("java/lang/Object");
        for (MethodInfo methodInfo : info.getMethodInfo()) {
            clearDerivedMethods(methods, info, methodInfo.toString());
        }
        getProject().log("Object methods removed", Project.MSG_VERBOSE);
    }

    private void removeMainMethods(ClassRepository repo, Set<String> methods) {
        MethodInfo mainInfo = new MethodInfo("main", "([Ljava/lang/String;)V", Opcodes.ACC_STATIC);
        for (ClassInfo classInfo : repo.getAllClassInfos()) {
            Set<MethodInfo> methodInfo = classInfo.getMethodInfo();
            if (methodInfo.contains(mainInfo)) {
                methods.remove(classInfo.getClassName() + ":main([Ljava/lang/String;)V");
            }
        }
        getProject().log("Main methods removed", Project.MSG_VERBOSE);
    }

    private void removeNoArgCtors(ClassRepository repo, Set<String> methods) {
        MethodInfo ctorInfo = new MethodInfo("<init>", "()V", Opcodes.ACC_STATIC);
        for (ClassInfo classInfo : repo.getAllClassInfos()) {
            Set<String> infs = new HashSet<>(Arrays.asList(classInfo.getInterfaceNames()));
            if (infs.contains("java/lang/Serializable")) {
                Set<MethodInfo> methodInfo = classInfo.getMethodInfo();
                if (methodInfo.contains(ctorInfo)) {
                    methods.remove(classInfo.getClassName() + ":" + methodInfo);
                }
            }
        }
        getProject().log("No arg constructors removed", Project.MSG_VERBOSE);
    }

    private void removeJUnitMethods(ClassRepository repo, Set<String> methods) {
        for (ClassInfo classInfo : repo.getAllClassInfos()) {
            for (MethodInfo methodInfo : classInfo.getMethodInfo()) {
                if (methodInfo.isTest()) {
                    methods.remove(classInfo.getClassName() + ":" + methodInfo);
                }
            }
        }
        getProject().log("JUnit method removed", Project.MSG_VERBOSE);

    }

    private void removeReflectiveAnnotatedMethods(ClassRepository repo, Set<String> methods) {
        for (ClassInfo classInfo : repo.getAllClassInfos()) {
            if (classInfo.hasAnnotations()) {
                for (ReflectiveAnnotation ra : reflectiveAnnotations) {
                    if (classInfo.hasAnnotation(ra.toString())) {
                        for (MethodInfo methodInfo : classInfo.getMethodInfo()) {
                            if ((methodInfo.getMethodAccess() & Opcodes.ACC_PUBLIC) != 0) {
                                methods.remove(classInfo.getClassName() + ":" + methodInfo);
                            }
                        }
                        break;
                    }
                }
            }

            for (MethodInfo methodInfo : classInfo.getMethodInfo()) {
                if (methodInfo.hasAnnotations()) {
                    for (ReflectiveAnnotation ra : reflectiveAnnotations) {
                        if (methodInfo.hasAnnotation(ra.toString())) {
                            methods.remove(classInfo.getClassName() + ":" + methodInfo);
                            break;
                        }
                    }
                }
            }
        }

        getProject().log("Methods with reflective annotations removed", Project.MSG_VERBOSE);
    }

    private void removeInterfaceImplementationMethods(ClassRepository repo, Set<String> methods) throws IOException {
        for (ClassInfo classInfo : repo.getAllClassInfos()) {
            if (classInfo.isInterface()) {
                for (MethodInfo methodInfo : classInfo.getMethodInfo()) {
                    clearDerivedMethods(methods, classInfo, methodInfo.toString());
                }
            }
        }

        getProject().log("Interface implementing methods removed", Project.MSG_VERBOSE);
    }

    private void removeAnonymousInnerImplementationMethods(ClassRepository repo, Set<String> methods) throws IOException {
        for (ClassInfo classInfo : repo.getAllClassInfos()) {
            if (classInfo.isAnonymous()) {
                for (MethodInfo methodInfo : classInfo.getMethodInfo()) {
                    clearDerivedMethods(methods, classInfo, methodInfo.toString());
                }
            }
        }
        getProject().log("Anonymous inner class implementing methods removed", Project.MSG_VERBOSE);
    }

    private void removeSyntheticMethods(ClassRepository repo, Set<String> methods) {
        for (ClassInfo classInfo : repo.getAllClassInfos()) {
            for (MethodInfo methodInfo : classInfo.getMethodInfo()) {
                if (methodInfo.isSynthetic()) {
                    methods.remove(classInfo.getClassName() + ":" + methodInfo);
                }
            }
        }
        getProject().log("Synthetic methods removed", Project.MSG_VERBOSE);
    }

    private void removeStandardEnumMethods(ClassRepository repo, Set<String> methods) throws IOException {
        ClassInfo info = repo.getClassInfo("java/lang/Enum");
        {
            MethodInfo methodInfo = new MethodInfo("valueOf", "(Ljava/lang/String;)?", Opcodes.ACC_PUBLIC);
            clearDerivedMethods(methods, info, methodInfo.toString());
        }
        {
            MethodInfo methodInfo = new MethodInfo("values", "()[?", Opcodes.ACC_PUBLIC);
            clearDerivedMethods(methods, info, methodInfo.toString());
        }
        getProject().log("Standard enum methods removed", Project.MSG_VERBOSE);
    }

    private void removeSpecialSerializableMethods(ClassRepository repo, Set<String> methods) {
        for (ClassInfo classInfo : repo.getAllClassInfos()) {
            for (MethodInfo methodInfo : classInfo.getMethodInfo()) {
                if ("writeObject".equals(methodInfo.getMethodName()) && "(Ljava/io/ObjectOutputStream;)V".equals(methodInfo.getMethodSignature())) {
                    methods.remove(classInfo.getClassName() + ":" + methodInfo);
                } else if ("readObject".equals(methodInfo.getMethodName()) && "(Ljava/io/ObjectInputStream;)V".equals(methodInfo.getMethodSignature())) {
                    methods.remove(classInfo.getClassName() + ":" + methodInfo);
                } else if ("writeExternal".equals(methodInfo.getMethodName()) && "(Ljava/io/ObjectOutput;)V".equals(methodInfo.getMethodSignature())) {
                    methods.remove(classInfo.getClassName() + ":" + methodInfo);
                } else if ("readExternal".equals(methodInfo.getMethodName()) && "(Ljava/io/ObjectInput;)V".equals(methodInfo.getMethodSignature())) {
                    methods.remove(classInfo.getClassName() + ":" + methodInfo);
                }
            }
        }
        getProject().log("Special Serializable methods removed", Project.MSG_VERBOSE);
    }

    private void removeAnnotations(ClassRepository repo, Set<String> methods) {
        for (ClassInfo classInfo : repo.getAllClassInfos()) {
            if (classInfo.isAnnotation()) {
                for (MethodInfo methodInfo : classInfo.getMethodInfo()) {
                    methods.remove(classInfo.getClassName() + ":" + methodInfo);
                }
            }
        }
        getProject().log("Runtime Annotated methods removed", Project.MSG_VERBOSE);

    }

    private void removeSpringMethods(ClassRepository repo, Set<String> methods) {
        try {
            removeSpringMethodsFromXML(repo, methods);
            removeSpringMethodsFromAnnotations(repo, methods);
        } catch (Exception e) {
            throw new BuildException("Failed removing spring methods", e);
        }
    }

    private void removeSpringMethodsFromXML(ClassRepository repo, Set<String> methods) throws ParserConfigurationException, XPathExpressionException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setExpandEntityReferences(false);
        dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
        dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        DocumentBuilder db = dbf.newDocumentBuilder();
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xp = xpf.newXPath();
        XPathExpression beanExpression = xp.compile("/beans/bean");
        XPathExpression beanClassExpression = xp.compile("@class");
        XPathExpression initMethodExpression = xp.compile("@init-method");
        XPathExpression destroyMethodExpression = xp.compile("@destroy-method");
        XPathExpression propertyExpression = xp.compile("property");
        XPathExpression propertyNameExpression = xp.compile("@name");
        XPathExpression propertyRefExpression = xp.compile("@ref");
        XPathExpression refBeanExpression = xp.compile("ref/@bean");

        Iterator<String> xmlIterator = repo.xmlIterator();
        while (xmlIterator.hasNext()) {
            String xmlName = xmlIterator.next() + ".xml";
            try (BufferedInputStream bis = new BufferedInputStream(repo.getStream(xmlName))) {
                Document doc = db.parse(bis);

                NodeList beans = (NodeList) beanExpression.evaluate(doc, XPathConstants.NODESET);
                for (int i = 0; i < beans.getLength(); i++) {
                    Element bean = (Element) beans.item(i);
                    Attr beanClass = (Attr) beanClassExpression.evaluate(bean, XPathConstants.NODE);
                    if (beanClass != null) {
                        Attr initMethod = (Attr) initMethodExpression.evaluate(bean, XPathConstants.NODE);
                        Attr destroyMethod = (Attr) destroyMethodExpression.evaluate(bean, XPathConstants.NODE);
                        NodeList properties = (NodeList) propertyExpression.evaluate(bean, XPathConstants.NODESET);

                        ClassInfo classInfo = repo.getClassInfo(beanClass.getValue().replaceAll("\\.", "/"));
                        if (classInfo != null) {
                            if (initMethod != null) {
                                String initMethodName = initMethod.getValue();
                                methods.remove(classInfo.getClassName() + ":" + initMethodName + "()V");
                            }
                            if (destroyMethod != null) {
                                String destroyMethodName = destroyMethod.getValue();
                                methods.remove(classInfo.getClassName() + ":" + destroyMethodName + "()V");
                            }
                            for (int j = 0; j < properties.getLength(); j++) {
                                Element property = (Element) properties.item(j);
                                Attr propertyAttr = (Attr) propertyNameExpression.evaluate(property, XPathConstants.NODE);
                                String propNameValue = propertyAttr.getValue();
                                Attr refAttr = (Attr) propertyRefExpression.evaluate(property, XPathConstants.NODE);
                                if (refAttr == null) {
                                    refAttr = (Attr) refBeanExpression.evaluate(property, XPathConstants.NODE);
                                }

                                // Don't handle sub xml files thru value attributes yet
                                if (refAttr != null) {
                                    XPathExpression refClassExpression = xp.compile("/beans/bean[@id='" + refAttr.getValue() + "']/@class");
                                    Attr refClassAttr = (Attr) refClassExpression.evaluate(doc, XPathConstants.NODE);

                                    if (refClassAttr != null) {
                                        String methodName = "set" + Character.toUpperCase(propNameValue.charAt(0)) + propNameValue.substring(1);
                                        String methodSig = "(L" + refClassAttr.getValue().replaceAll("\\.", "/") + ";)V";
                                        methods.remove(classInfo.getClassName() + ":" + methodName + methodSig);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception ioe) {
                log("Failed parsing possible spring bean xml file: " + xmlName);
                try (StringWriter sw = new StringWriter()) {
                    try (PrintWriter pw = new PrintWriter(sw)) {
                        ioe.printStackTrace(pw);
                    }
                    log(sw.toString(), Project.MSG_VERBOSE);
                } catch (IOException e) {
                }
            }
        }
        log("XML based Spring methods removed", Project.MSG_VERBOSE);
    }

    private void removeSpringMethodsFromAnnotations(ClassRepository repo, Set<String> methods) {
        for (ClassInfo classInfo : repo.getAllClassInfos()) {
            for (MethodInfo methodInfo : classInfo.getMethodInfo()) {
                if ("<init>".equals(methodInfo.getMethodName()) && methodInfo.hasAnnotation("org.springframework.beans.factory.annotation.Autowired")) {
                    methods.remove(classInfo.getClassName() + ":" + methodInfo);
                }
            }
        }
        getProject().log("Annotated Spring methods removed", Project.MSG_VERBOSE);
    }

    private void removeSPIClasses(ClassRepository repo, Set<String> methods) throws IOException {
        Iterator<String> spiIterator = repo.serviceIterator();
        while (spiIterator.hasNext()) {
            String fileName = spiIterator.next();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(repo.getStream(fileName), StandardCharsets.UTF_8))) {
                String clsName = br.readLine();
                if (clsName != null) {
                    clsName = clsName.replaceAll("\\.", "/");
                    for (MethodInfo m : repo.getMethodInfo(clsName)) {
                        if ((m.getMethodAccess() & Opcodes.ACC_PUBLIC) != 0) {
                            String methodInfo = clsName + ":" + m.getMethodName() + m.getMethodSignature();
                            methods.remove(methodInfo);
                        }
                    }
                }
            }
        }
        getProject().log("SPI methods removed", Project.MSG_VERBOSE);
    }

    private void removeWebMethods(ClassRepository repo, Set<String> methods) throws IOException {
        ClassInfo info = repo.getClassInfo("javax/servlet/http/HttpServlet");

        for (MethodInfo methodInfo : info.getMethodInfo()) {
            clearDerivedMethods(methods, info, methodInfo.toString());
        }
        getProject().log("Standard Web methods removed", Project.MSG_VERBOSE);
    }

    private void clearDerivedMethods(Set<String> methods, ClassInfo info, String methodInfo) throws IOException {
        Set<ClassInfo> derivedInfos = info.getDerivedClasses();

        for (ClassInfo derivedInfo : derivedInfos) {
            // regex chokes because of the $ in output classname, so do it the old way
            int qMarkPos = methodInfo.indexOf('?');
            String appliedMethodInfo;
            if (qMarkPos >= 0) {
                appliedMethodInfo = methodInfo.substring(0, qMarkPos);
                appliedMethodInfo += "L" + derivedInfo.getClassName() + ";";
                appliedMethodInfo += methodInfo.substring(qMarkPos + 1);
            } else {
                appliedMethodInfo = methodInfo;
            }
            methods.remove(derivedInfo.getClassName() + ":" + appliedMethodInfo);
            clearDerivedMethods(methods, derivedInfo, methodInfo);
        }
    }

    /** for testing only */
    public static void main(String[] args) {

        if (args.length < 1) {
            throw new IllegalArgumentException("args (" + Arrays.toString(args) + ") must contain classpath root");
        }

        FindDeadMethods fdm = new FindDeadMethods();
        Project project = new Project();
        fdm.setProject(project);

        Path path = new Path(project);
        path.setLocation(new File(args[0]));
        fdm.addConfiguredClasspath(path);
        ReflectiveAnnotation ra = fdm.createReflectiveAnnotation();
        ra.setName("test.reflective.ReflectiveUse");
        IgnoredPackage ip = fdm.createIgnoredPackage();
        ip.setPattern("test\\.ignored");

        fdm.execute();
    }
}
