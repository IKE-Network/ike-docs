import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import network.ike.docs.typst.asciidoc.TypstConverter;
import network.ike.docs.koncept.KonceptInlineMacro;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Headless stand-in for idoc:asciidoc with ike.skip.typst=false: converts one
 * topic file with the typst backend, the Koncept inline macro registered, and
 * the attributes the pipeline sets (typst-root, stem).
 */
public class Render {
    public static void main(String[] args) throws Exception {
        Path source = Path.of(args[0]).toAbsolutePath();
        Path target = Path.of(args[1]).toAbsolutePath();
        long t0 = System.nanoTime();
        try (Asciidoctor a = Asciidoctor.Factory.create()) {
            a.javaConverterRegistry().register(TypstConverter.class);
            a.javaExtensionRegistry().inlineMacro(KonceptInlineMacro.class);
            Options o = Options.builder().backend("typst").standalone(true).safe(SafeMode.UNSAFE)
                    .baseDir(source.getParent().toFile())
                    .toFile(target.toFile())
                    .attributes(Attributes.builder()
                            .attribute("stem", "latexmath")
                            .attribute("typst-root", "/Users/kec/ike-dev")
                            .attribute("idprefix", "")
                            .attribute("idseparator", "-")
                            .build())
                    .build();
            a.convertFile(source.toFile(), o);
        }
        long ms = (System.nanoTime() - t0) / 1_000_000;
        Files.writeString(target.resolveSibling("converter-timing.txt"),
                "AsciidoctorJ start + convert: " + ms + " ms\n");
        System.err.println("converted " + source.getFileName() + " -> " + target + " in " + ms + " ms");
    }
}
