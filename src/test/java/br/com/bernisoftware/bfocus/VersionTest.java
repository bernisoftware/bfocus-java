package br.com.bernisoftware.bfocus;

import static br.com.bernisoftware.bfocus.TestSupport.arr;
import static br.com.bernisoftware.bfocus.TestSupport.ok;
import static br.com.bernisoftware.bfocus.TestSupport.projectDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * A versão existe em dois lugares ({@code pom.xml} e {@code Version.java}) e eles não podem divergir.
 *
 * <p>{@link Version#VERSION} vai no header {@code X-Bfocus-Client} de TODA requisição — é por ele que a API sabe
 * quem avisar quando uma correção exige atualizar a SDK. Se ele congelar (no bZapper congelou por releases
 * seguidas), o aviso vai para o alvo errado.
 */
class VersionTest extends ServerTestBase {
    // Os mesmos padrões do scripts/release-sdks.sh — se não casarem, o bump não acontece.
    private static final Pattern POM_RE = Pattern.compile("(<artifactId>bfocus</artifactId>\\s*<version>)([^<]+)(</version>)");
    private static final Pattern VERSION_JAVA_RE = Pattern.compile("(public static final String VERSION = \")([^\"]+)(\")");

    private static List<String> versions(Pattern pattern, Path file) throws IOException {
        Matcher m = pattern.matcher(Files.readString(file, StandardCharsets.UTF_8));
        List<String> out = new ArrayList<>();
        while (m.find()) {
            out.add(m.group(2));
        }
        return out;
    }

    @Test
    void constanteBateComOPom() throws IOException {
        List<String> found = versions(POM_RE, projectDir().resolve("pom.xml"));
        assertEquals(1, found.size(), "um único <artifactId>bfocus</artifactId><version> no pom.xml");
        assertEquals(found.get(0), Version.VERSION, "Version.VERSION divergiu do pom.xml — o bump precisa alterar os dois");
    }

    @Test
    void padraoDoReleaseCasaNoVersionJava() throws IOException {
        Path file = projectDir().resolve("src/main/java/br/com/bernisoftware/bfocus/Version.java");
        List<String> found = versions(VERSION_JAVA_RE, file);
        assertEquals(1, found.size());
        assertEquals(Version.VERSION, found.get(0));
    }

    @Test
    void identificacaoDoCliente() {
        assertEquals("bfocus-java/" + Version.VERSION, Version.CLIENT_ID);
        assertTrue(Version.CLIENT_ID.matches("^bfocus-java/\\d+\\.\\d+\\.\\d+$"), Version.CLIENT_ID);
        assertEquals(Version.VERSION, BfocusClient.VERSION);
    }

    @Test
    void vaiNoXBfocusClientENoUserAgent() {
        client(ok(arr())).products().list();
        FakeServer.Recorded r = requests().get(0);
        Pattern expected = Pattern.compile("^bfocus-java/" + Pattern.quote(Version.VERSION) + "$");
        assertTrue(expected.matcher(r.header("x-bfocus-client")).matches(), r.header("x-bfocus-client"));
        assertEquals(r.header("x-bfocus-client"), r.header("user-agent"));
    }

    @Test
    void semDependenciaDeRuntime() throws IOException {
        String pom = Files.readString(projectDir().resolve("pom.xml"), StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("<dependency>(.*?)</dependency>", Pattern.DOTALL).matcher(pom);
        while (m.find()) {
            assertTrue(m.group(1).contains("<scope>test</scope>"), "dependência fora do escopo de teste: " + m.group(1).trim());
        }
    }

    /** No perfil {@code artifact} (failsafe), a suíte roda contra o jar empacotado — confere que é mesmo o jar. */
    @Test
    void rodandoContraOJarEmpacotado() throws URISyntaxException, IOException {
        assumeTrue(Boolean.getBoolean("bfocus.artifactTest"), "só no perfil artifact (mvn -P artifact verify)");
        Path origin = Path.of(BfocusClient.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertTrue(origin.getFileName().toString().endsWith(".jar"), "a SDK veio de " + origin + ", não do jar");
        // Lido do jar (o Package do classpath pode ter sido definido pelas classes de teste, sem manifesto).
        try (JarFile jar = new JarFile(origin.toFile())) {
            Attributes main = jar.getManifest().getMainAttributes();
            assertEquals(Version.VERSION, main.getValue("Implementation-Version"), "Implementation-Version do manifesto");
            assertEquals("br.com.bernisoftware.bfocus", main.getValue("Automatic-Module-Name"));
            assertTrue(jar.getEntry("br/com/bernisoftware/bfocus/BfocusClient.class") != null);
            assertTrue(jar.getEntry("br/com/bernisoftware/bfocus/FakeServer.class") == null, "classe de teste no jar");
        }
        Path jsonOrigin = Path.of(br.com.bernisoftware.bfocus.internal.Json.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        assertEquals(origin, jsonOrigin, "o codec JSON interno está dentro do mesmo jar");
        assertFalse(origin.toString().contains("target" + java.io.File.separator + "classes"));
    }
}
