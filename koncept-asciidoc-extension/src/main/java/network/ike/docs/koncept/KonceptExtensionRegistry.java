package network.ike.docs.koncept;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.jruby.extension.spi.ExtensionRegistry;

/**
 * SPI entry point for automatic registration of Koncept extensions
 * with AsciidoctorJ.
 * <p>
 * Registered via {@code META-INF/services/org.asciidoctor.jruby.extension.spi.ExtensionRegistry}.
 * When this JAR is on the classpath of the {@code asciidoctor-maven-plugin},
 * the extensions are activated automatically — no explicit configuration required.
 */
public class KonceptExtensionRegistry implements ExtensionRegistry {

    /** Creates a new extension registry instance. */
    public KonceptExtensionRegistry() {
    }

    @Override
    public void register(Asciidoctor asciidoctor) {
        // Always register the inline macro — works with all backends
        asciidoctor.javaExtensionRegistry()
                .inlineMacro(KonceptInlineMacro.class);

        // The glossary Postprocessor is NOT registered here because it is
        // incompatible with the asciidoctorj-pdf (Prawn) backend. The PDF
        // converter returns a Ruby object instead of a String, causing a
        // TypeError in PostprocessorProxy before our Java code is reached.
        //
        // Instead, the Postprocessor is registered explicitly per-execution
        // in the asciidoctor-maven-plugin configuration via <extensions>:
        //
        //   <extensions>
        //     <extension>
        //       <className>network.ike.docs.koncept.KonceptGlossaryProcessor</className>
        //     </extension>
        //   </extensions>
        //
        // This allows HTML, DocBook, and CSS-based PDF backends to generate
        // the glossary while avoiding the crash with the Prawn PDF backend.
    }
}
