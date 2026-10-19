/*
 * Copyright © 2026 Knowledge Graphlet / IKE Network
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package network.ike.docs.typst.asciidoc;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.jruby.converter.spi.ConverterRegistry;

/**
 * SPI registration of the {@code typst} backend, so any AsciidoctorJ instance
 * with this jar on its classpath (the doc plugin, asciidoctor-maven-plugin,
 * tests) can select {@code backend: typst} without programmatic registration.
 */
public class TypstConverterRegistry implements ConverterRegistry {

    /** Creates the registry (ServiceLoader entry point). */
    public TypstConverterRegistry() {}

    @Override
    public void register(Asciidoctor asciidoctor) {
        asciidoctor.javaConverterRegistry().register(TypstConverter.class);
    }
}
