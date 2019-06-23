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

import org.apache.tools.ant.BuildException;
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

@RunWith(Parameterized.class)
public class FindDeadMethodsAntTaskFromXMLTest {

	private File xmlInput;

	@Parameters
	public static Collection<File> data() {
		return Collections.singletonList(new File("/home/dave/dev/pgbu_platform-2.0/dm_build.xml"));
	}

	public FindDeadMethodsAntTaskFromXMLTest(File test) {
		xmlInput = test;
	}

	@Test
	public void testAnt() {
		FindDeadMethodsAntTask t = parseXML(xmlInput);
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

			XPathFactory xpf = XPathFactory.newInstance();
			XPath xp = xpf.newXPath();
			XPathExpression xpe = xp.compile("/project/target/deadmethods/classpath/pathelement");
			NodeList cpElements = (NodeList) xpe.evaluate(d, XPathConstants.NODESET);
			for (int i = 0; i < cpElements.getLength(); i++) {
				Element cpElement = (Element) cpElements.item(i);

				Path cp = new Path(p);
				// cp.setLocation(??);
				t.addConfiguredClasspath(cp);
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
				foundValue = value;
			}

			rawValue.append(foundValue);
			lastPos = end;
		}
		if (lastPos < value.length()) {
			rawValue.append(value.substring(lastPos));
		}

		return rawValue.toString();
	}

}
