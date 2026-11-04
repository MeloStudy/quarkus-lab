package lab.shop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Task 2: rules about the build itself, read from the POM files (plain JUnit, no Quarkus). The same rules, enforced on every
 * build, are what the Maven Enforcer plugin is for.
 */
class BuildStructureTest {

    // Maven runs the tests of a module with the module folder as the working directory
    private static final String PARENT = "../pom.xml";
    private static final String LIBRARY = "../pricing-lib/pom.xml";
    private static final String APP = "pom.xml";

    private static Element project(String path) throws Exception {
        File file = new File(path);
        assertTrue(file.isFile(), path + " not found from " + new File(".").getCanonicalPath());
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
        return document.getDocumentElement();
    }

    private static List<Element> children(Element parent, String name) {
        List<Element> found = new ArrayList<>();
        if (parent == null) {
            return found;
        }
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element element && element.getTagName().equals(name)) {
                found.add(element);
            }
        }
        return found;
    }

    private static Element child(Element parent, String name) {
        List<Element> found = children(parent, name);
        return found.isEmpty() ? null : found.get(0);
    }

    private static String text(Element parent, String name) {
        Element element = child(parent, name);
        return element == null ? null : element.getTextContent().trim();
    }

    private static List<String> propertyNames(Element project) {
        return allChildNames(child(project, "properties"));
    }

    private static List<String> allChildNames(Element element) {
        List<String> names = new ArrayList<>();
        if (element == null) {
            return names;
        }
        NodeList nodes = element.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element e) {
                names.add(e.getTagName());
            }
        }
        return names;
    }

    private static List<Element> dependencies(Element project) {
        return children(child(project, "dependencies"), "dependency");
    }

    @Test
    void theParentListsBothModules() throws Exception {
        Element parent = project(PARENT);

        assertEquals("pom", text(parent, "packaging"));
        List<String> modules = children(child(parent, "modules"), "module").stream().map(Element::getTextContent).map(String::trim).toList();
        assertTrue(modules.containsAll(List.of("pricing-lib", "app")), "modules: " + modules);
    }

    @Test
    void bothModulesInheritFromTheSameParent() throws Exception {
        Element parent = project(PARENT);

        for (String path : List.of(LIBRARY, APP)) {
            Element declared = child(project(path), "parent");
            assertNotNull(declared, path + " has no <parent>");
            assertEquals(text(parent, "artifactId"), text(declared, "artifactId"), path);
            assertEquals(text(parent, "version"), text(declared, "version"), path);
        }
    }

    @Test
    void theQuarkusVersionIsDecidedInOnePlace() throws Exception {
        assertTrue(propertyNames(project(PARENT)).contains("quarkus.platform.version"), "the parent must define quarkus.platform.version");

        assertFalse(propertyNames(project(LIBRARY)).contains("quarkus.platform.version"), "the library repeats the Quarkus version");
        assertFalse(propertyNames(project(APP)).contains("quarkus.platform.version"), "the application repeats the Quarkus version");
    }

    @Test
    void noModuleSetsTheVersionOfADependencyTheParentManages() throws Exception {
        for (String path : List.of(LIBRARY, APP)) {
            for (Element dependency : dependencies(project(path))) {
                assertNull(text(dependency, "version"), path + ": " + text(dependency, "artifactId") + " has its own <version>");
            }
        }
    }

    @Test
    void thePluginsOfTheModulesTakeTheirVersionFromTheParent() throws Exception {
        for (String path : List.of(LIBRARY, APP)) {
            Element build = child(project(path), "build");
            for (Element plugin : children(child(build, "plugins"), "plugin")) {
                assertNull(text(plugin, "version"), path + ": plugin " + text(plugin, "artifactId") + " has its own <version>");
            }
        }
    }

    @Test
    void theLibraryNeedsNoQuarkusExtension() throws Exception {
        for (Element dependency : dependencies(project(LIBRARY))) {
            assertFalse(text(dependency, "groupId").startsWith("io.quarkus"), "the library depends on " + text(dependency, "artifactId"));
        }
    }
}
