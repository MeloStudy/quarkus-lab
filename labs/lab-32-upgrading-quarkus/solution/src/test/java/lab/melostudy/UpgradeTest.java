package lab.melostudy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Task 1: the project files themselves, read as text (plain JUnit). */
class UpgradeTest {

    private static final String TARGET = "3.40.1.1";

    /** Configuration properties that were deprecated before Quarkus 3.33 and are removed in Quarkus 4 (from its migration guide). */
    private static final List<String> REMOVED_IN_QUARKUS_4 = List.of(
            "quarkus.log.console.enable", "quarkus.log.console.async", "quarkus.log.console.json",
            "quarkus.log.file.enable", "quarkus.log.file.async", "quarkus.log.syslog.enable", "quarkus.log.syslog.async",
            "quarkus.log.socket.enable");

    private static Element pomProject() throws Exception {
        File pom = new File("pom.xml");
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
        return document.getDocumentElement();
    }

    private static String property(String name) throws Exception {
        NodeList properties = pomProject().getElementsByTagName(name);
        return properties.getLength() == 0 ? null : properties.item(0).getTextContent().trim();
    }

    @Test
    void theProjectIsOnTheTargetPlatformVersion() throws Exception {
        assertEquals(TARGET, property("quarkus.platform.version"));
    }

    @Test
    void theQuarkusPluginUsesTheSamePlatformVersion() throws Exception {
        String pom = Files.readString(new File("pom.xml").toPath());
        int plugin = pom.indexOf("<artifactId>quarkus-maven-plugin</artifactId>");
        assertTrue(plugin > 0);
        String afterPlugin = pom.substring(plugin, pom.indexOf("</plugin>", plugin));

        assertTrue(afterPlugin.contains("${quarkus.platform.version}"), "the plugin must follow the platform property");
    }

    @Test
    void noPropertyThatQuarkus4RemovesIsStillInUse() throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(Files.readString(new File("src/main/resources/application.properties").toPath())));

        for (String removed : REMOVED_IN_QUARKUS_4) {
            assertFalse(properties.containsKey(removed), removed + " is deprecated and disappears in Quarkus 4");
        }
    }

    @Test
    void theLoggingConfigurationIsStillThere() throws Exception {
        Properties properties = new Properties();
        properties.load(new StringReader(Files.readString(new File("src/main/resources/application.properties").toPath())));

        assertEquals("true", properties.getProperty("quarkus.log.console.enabled"), "the renamed property must keep the value of the old one");
        assertTrue(properties.containsKey("quarkus.log.console.format"));
    }
}
