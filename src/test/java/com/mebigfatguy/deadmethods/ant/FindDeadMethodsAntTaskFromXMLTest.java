package com.mebigfatguy.deadmethods.ant;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.types.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.mebigfatguy.deadmethods.IgnoredClass;
import com.mebigfatguy.deadmethods.IgnoredMethod;
import com.mebigfatguy.deadmethods.IgnoredPackage;
import com.mebigfatguy.deadmethods.ReflectiveAnnotation;

@RunWith(Parameterized.class)
public class FindDeadMethodsAntTaskFromXMLTest {

	private File xmlInput;
	private String envVar = "";

	@Parameters
	public static Collection<File> data() {
		return Collections.singletonList(new File(System.getProperty("dm.input")));
	}

	public FindDeadMethodsAntTaskFromXMLTest(File test) {
		xmlInput = test;
	}

	@Test
	public void testAnt() {
		FindDeadMethodsAntTask t = parseXML(xmlInput);

		t.getProject().addBuildListener(new DMBuildListener());
		t.execute();
	}

	private FindDeadMethodsAntTask parseXML(File f) throws BuildException {
		try (BufferedReader br = Files.newBufferedReader(f.toPath())) {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();

			Document d = db.parse(new InputSource(br));

			FindDeadMethodsAntTask t = new FindDeadMethodsAntTask();
			Project p = new Project();
			t.setProject(p);

			Map<String, String> properties = parseProperties(d);

			Path cp = new Path(p);

			XPathFactory xpf = XPathFactory.newInstance();
			XPath xp = xpf.newXPath();
			XPathExpression xpe = xp.compile("/project/target/deadmethods/classpath/pathelement");
			NodeList cpElements = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
			for (int i = 0; i < cpElements.getLength(); i++) {
				Element cpElement = (Element) cpElements.item(i);
				String path = cpElement.getAttribute("location");

				Path jarPath = new Path(p);
				jarPath.setLocation(new File(replaceMacro(path, properties)));
				cp.add(jarPath);
			}
			t.addConfiguredClasspath(cp);

			cp = new Path(p);

			xpe = xp.compile("/project/target/deadmethods/auxClasspath/pathelement");
			cpElements = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
			for (int i = 0; i < cpElements.getLength(); i++) {
				Element cpElement = (Element) cpElements.item(i);
				String path = cpElement.getAttribute("location");

				Path jarPath = new Path(p);
				jarPath.setLocation(new File(replaceMacro(path, properties)));
				cp.add(jarPath);
			}
			t.addConfiguredAuxClasspath(cp);

			xpe = xp.compile("/project/target/deadmethods/reflectiveAnnotation");
			NodeList raElements = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
			for (int i = 0; i < raElements.getLength(); i++) {
				Element raElement = (Element) raElements.item(i);
				String name = raElement.getAttribute("name");

				ReflectiveAnnotation ra = t.createReflectiveAnnotation();
				ra.setName(replaceMacro(name, properties));
			}

			xpe = xp.compile("/project/target/deadmethods/ignoredPackage");
			NodeList ipElements = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
			for (int i = 0; i < ipElements.getLength(); i++) {
				Element ipElement = (Element) ipElements.item(i);
				String pattern = ipElement.getAttribute("pattern");

				IgnoredPackage ip = t.createIgnoredPackage();
				ip.setPattern(replaceMacro(pattern, properties));
			}

			xpe = xp.compile("/project/target/deadmethods/ignoredClass");
			NodeList icElements = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
			for (int i = 0; i < icElements.getLength(); i++) {
				Element icElement = (Element) icElements.item(i);
				String pattern = icElement.getAttribute("pattern");

				IgnoredClass ic = t.createIgnoredClass();
				ic.setPattern(replaceMacro(pattern, properties));
			}

			xpe = xp.compile("/project/target/deadmethods/ignoredMethod");
			NodeList imElements = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
			for (int i = 0; i < imElements.getLength(); i++) {
				Element imElement = (Element) imElements.item(i);
				String pattern = imElement.getAttribute("pattern");

				IgnoredMethod im = t.createIgnoredMethod();
				im.setPattern(replaceMacro(pattern, properties));
			}

			return t;
		} catch (IOException | ParserConfigurationException | SAXException | XPathExpressionException e) {
			throw new BuildException("Failed parsing ant xml file: " + f, e);
		}
	}

	private Map<String, String> parseProperties(Document d) throws XPathExpressionException {
		Map<String, String> properties = new HashMap<>();

		XPathFactory xpf = XPathFactory.newInstance();
		XPath xp = xpf.newXPath();
		XPathExpression xpe = xp.compile("/project/@basedir");
		String base = (String) xpe.evaluate(d, XPathConstants.STRING);
		if (base.equals(".")) {
			base = xmlInput.getParent();
		}
		properties.put("basedir", base);

		xpe = xp.compile("/project/property");
		NodeList propNodes = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
		for (int i = 0; i < propNodes.getLength(); i++) {
			Element propNode = (Element) propNodes.item(i);
			String name = propNode.getAttribute("name");
			String value = propNode.getAttribute("value");

			if (!name.isEmpty()) {
				properties.put(name, replaceMacro(value, properties));
			} else if (propNode.hasAttribute("environment")) {
				envVar = propNode.getAttribute("environment");
			}
		}

		return properties;
	}

	private String replaceMacro(String value, Map<String, String> properties) {
		Pattern macro = Pattern.compile("\\$\\{([^\\}]+)\\}");

		StringBuilder rawValue = new StringBuilder(value.length() + 50);
		Matcher m = macro.matcher(value);
		int lastPos = 0;
		while (m.find(lastPos)) {
			int start = m.start();
			int end = m.end();

			if (start > lastPos) {
				rawValue.append(value.substring(lastPos, start));
			}

			String macroName = m.group(1);
			String foundValue = properties.get(macroName);
			if (foundValue == null) {
				if (macroName.startsWith(envVar + ".")) {
					String envName = macroName.substring(envVar.length() + 1);
					foundValue = System.getenv(envName);
					if (foundValue == null) {
						switch (envName) {
						case "JAVA_HOME":
							foundValue = System.getProperty("java.home");
							if (foundValue.endsWith("jre")) {
								foundValue = foundValue.substring(0, foundValue.length() - "/jre".length());
							}
							break;
						default:
							foundValue = value;
							break;
						}
					}
				} else {
					foundValue = value;
				}
			}

			rawValue.append(foundValue);
			lastPos = end;
		}
		if (lastPos < value.length()) {
			rawValue.append(value.substring(lastPos));
		}

		return rawValue.toString();
	}

	class DMBuildListener implements BuildListener {

		@Override
		public void buildFinished(BuildEvent event) {
		}

		@Override
		public void buildStarted(BuildEvent event) {
		}

		@Override
		public void messageLogged(BuildEvent event) {
			System.out.println(event.getMessage());
		}

		@Override
		public void targetFinished(BuildEvent event) {
		}

		@Override
		public void targetStarted(BuildEvent event) {
		}

		@Override
		public void taskFinished(BuildEvent event) {
		}

		@Override
		public void taskStarted(BuildEvent event) {
		}

	}
}
