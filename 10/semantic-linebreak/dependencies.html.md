---
date_published: 2026-05-09
date_modified: 2026-05-09
canonical_url: https://github.com/IKE-Network/ike-docs/dependencies.html
---

# Project Dependencies

## [compile](#compile)

The following is a list of compile dependencies for this project. These dependencies are required to compile and run the application:

| GroupId | ArtifactId | Version | Type | Licenses |
| --- | --- | --- | --- | --- |
| org.asciidoctor | [asciidoctorj](https://github.com/asciidoctor/asciidoctorj)[1] | 3.0.1 | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| org.jruby | [jruby](https://github.com/jruby/jruby/jruby-artifacts/jruby)[3] | 10.0.3.0 | jar | [GPL-2.0](http://www.gnu.org/licenses/gpl-2.0-standalone.html)[4][LGPL-2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)[5][EPL-2.0](http://www.eclipse.org/legal/epl-v20.html)[6] |

## [test](#test)

The following is a list of test dependencies for this project. These dependencies are only required to compile and run unit tests for the application:

| GroupId | ArtifactId | Version | Type | Licenses |
| --- | --- | --- | --- | --- |
| org.assertj | [assertj-core](https://assertj.github.io/doc/#assertj-core)[7] | 3.27.3 | jar | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
| org.junit.jupiter | [junit-jupiter](https://junit.org/)[9] | 6.0.0 | jar | [Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |

## [provided](#provided)

The following is a list of provided dependencies for this project. These dependencies are required to compile the application, but should be provided by default when using the library:

| GroupId | ArtifactId | Version | Classifier | Type | Licenses |
| --- | --- | --- | --- | --- | --- |
| network.ike.tooling | [ike-build-standards](https://ike.network/ike-tooling/ike-build-standards/)[11] | 148 | built-with | zip | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
| network.ike.tooling | [ike-build-standards](https://ike.network/ike-tooling/ike-build-standards/)[11] | 148 | claude | zip | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
| network.ike.tooling | [ike-build-standards](https://ike.network/ike-tooling/ike-build-standards/)[11] | 148 | site-theme | zip | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
| org.apache.maven | [maven-plugin-api](https://maven.apache.org/ref/3.9.9/maven-plugin-api/)[12] | 3.9.9 | - | jar | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
| org.apache.maven.plugin-tools | [maven-plugin-annotations](https://maven.apache.org/plugin-tools/maven-plugin-annotations)[13] | 3.15.1 | - | jar | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |

# Project Transitive Dependencies

The following is a list of transitive dependencies for this project. Transitive dependencies are the dependencies of the project dependencies.

## [compile](#compile_2)

The following is a list of compile dependencies for this project. These dependencies are required to compile and run the application:

| GroupId | ArtifactId | Version | Classifier | Type | Licenses |
| --- | --- | --- | --- | --- | --- |
| com.github.jnr | [jffi](http://github.com/jnr/jffi)[14] | 1.3.14 | native | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2][GNU Lesser General Public License version 3](https://www.gnu.org/licenses/lgpl-3.0.txt)[15] |
| com.github.jnr | [jffi](http://github.com/jnr/jffi)[14] | 1.3.14 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2][GNU Lesser General Public License version 3](https://www.gnu.org/licenses/lgpl-3.0.txt)[15] |
| com.github.jnr | [jnr-a64asm](http://nexus.sonatype.org/oss-repository-hosting.html/jnr-a64asm)[16] | 1.0.0 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.github.jnr | [jnr-constants](http://github.com/jnr/jnr-constants)[17] | 0.10.4 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.github.jnr | [jnr-enxio](http://github.com/jnr/jnr-enxio)[18] | 0.32.19 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.github.jnr | [jnr-ffi](http://github.com/jnr/jnr-ffi)[19] | 2.2.18 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.github.jnr | [jnr-netdb](http://github.com/jnr/jnr-netdb)[20] | 1.2.0 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.github.jnr | [jnr-posix](http://github.com/jnr/jnr-posix)[21] | 3.1.21 | - | jar | [Eclipse Public License - v 2.0](https://www.eclipse.org/legal/epl-2.0/)[22][GNU General Public License Version 2](http://www.gnu.org/copyleft/gpl.html)[23][GNU Lesser General Public License Version 2.1](http://www.gnu.org/licenses/lgpl.html)[24] |
| com.github.jnr | [jnr-unixsocket](http://github.com/jnr/jnr-unixsocket)[25] | 0.38.24 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.github.jnr | [jnr-x86asm](http://github.com/jnr/jnr-x86asm)[26] | 1.0.2 | - | jar | [MIT License](http://www.opensource.org/licenses/mit-license.php)[27] |
| com.headius | [backport9](http://nexus.sonatype.org/oss-repository-hosting.html/backport9)[28] | 1.13 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.headius | [invokebinder](http://maven.apache.org)[29] | 1.14 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.headius | [options](https://github.com/headius/options)[30] | 1.6 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| joda-time | [joda-time](https://www.joda.org/joda-time/)[31] | 2.14.0 | - | jar | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
| me.qmx.jitescript | [jitescript](https://github.com/qmx/jitescript)[32] | 0.4.1 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| org.asciidoctor | [asciidoctorj-api](https://github.com/asciidoctor/asciidoctorj)[1] | 3.0.1 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| org.crac | [crac](https://github.com/crac/org.crac)[33] | 1.5.0 | - | jar | [BSD-2-Clause](https://opensource.org/licenses/BSD-2-Clause)[34] |
| org.jruby | [dirgra](https://github.com/jruby/dirgra)[35] | 0.5 | - | jar | [EPL](http://www.eclipse.org/legal/epl-v10.html)[36] |
| org.jruby | [jruby-base](https://github.com/jruby/jruby/jruby-base)[37] | 10.0.3.0 | - | jar | [GPL-2.0](http://www.gnu.org/licenses/gpl-2.0-standalone.html)[4][LGPL-2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)[5][EPL-2.0](http://www.eclipse.org/legal/epl-v20.html)[6] |
| org.jruby | [jruby-stdlib](https://github.com/jruby/jruby/jruby-stdlib)[38] | 10.0.3.0 | - | jar | [GPL-2.0](http://www.gnu.org/licenses/gpl-2.0-standalone.html)[4][LGPL-2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)[5][EPL-2.0](http://www.eclipse.org/legal/epl-v20.html)[6] |
| org.jruby | [jzlib](http://www.jcraft.com/jzlib/)[39] | 1.1.5 | - | jar | [BSD](http://www.jcraft.com/jzlib/LICENSE.txt)[40] |
| org.jruby.jcodings | [jcodings](http://nexus.sonatype.org/oss-repository-hosting.html/jcodings)[41] | 1.0.63 | - | jar | [MIT License](http://www.opensource.org/licenses/mit-license.php)[27] |
| org.jruby.joni | [joni](http://nexus.sonatype.org/oss-repository-hosting.html/joni)[42] | 2.2.6 | - | jar | [MIT License](http://www.opensource.org/licenses/mit-license.php)[27] |
| org.ow2.asm | [asm](http://asm.ow2.io/)[43] | 9.7.1 | - | jar | [BSD-3-Clause](https://asm.ow2.io/license.html)[44] |
| org.ow2.asm | [asm-analysis](http://asm.ow2.io/)[43] | 9.7.1 | - | jar | [BSD-3-Clause](https://asm.ow2.io/license.html)[44] |
| org.ow2.asm | [asm-commons](http://asm.ow2.io/)[43] | 9.7.1 | - | jar | [BSD-3-Clause](https://asm.ow2.io/license.html)[44] |
| org.ow2.asm | [asm-tree](http://asm.ow2.io/)[43] | 9.7.1 | - | jar | [BSD-3-Clause](https://asm.ow2.io/license.html)[44] |
| org.ow2.asm | [asm-util](http://asm.ow2.io/)[43] | 9.7.1 | - | jar | [BSD-3-Clause](https://asm.ow2.io/license.html)[44] |

## [test](#test_2)

The following is a list of test dependencies for this project. These dependencies are only required to compile and run unit tests for the application:

| GroupId | ArtifactId | Version | Type | Licenses |
| --- | --- | --- | --- | --- |
| net.bytebuddy | [byte-buddy](https://bytebuddy.net/byte-buddy)[45] | 1.15.11 | jar | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
| org.apiguardian | [apiguardian-api](https://github.com/apiguardian-team/apiguardian)[46] | 1.1.2 | jar | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| org.jspecify | [jspecify](http://jspecify.org/)[47] | 1.0.0 | jar | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| org.junit.jupiter | [junit-jupiter-api](https://junit.org/)[9] | 6.0.0 | jar | [Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |
| org.junit.jupiter | [junit-jupiter-engine](https://junit.org/)[9] | 6.0.0 | jar | [Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |
| org.junit.jupiter | [junit-jupiter-params](https://junit.org/)[9] | 6.0.0 | jar | [Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |
| org.junit.platform | [junit-platform-commons](https://junit.org/)[9] | 6.0.0 | jar | [Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |
| org.junit.platform | [junit-platform-engine](https://junit.org/)[9] | 6.0.0 | jar | [Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |
| org.opentest4j | [opentest4j](https://github.com/ota4j-team/opentest4j)[48] | 1.3.0 | jar | [The Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |

## [provided](#provided_2)

The following is a list of provided dependencies for this project. These dependencies are required to compile the application, but should be provided by default when using the library:

| GroupId | ArtifactId | Version | Type | Licenses |
| --- | --- | --- | --- | --- |
| org.apache.maven | [maven-artifact](https://maven.apache.org/ref/3.9.9/maven-artifact/)[49] | 3.9.9 | jar | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
| org.apache.maven | [maven-model](https://maven.apache.org/ref/3.9.9/maven-model/)[50] | 3.9.9 | jar | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
| org.codehaus.plexus | [plexus-classworlds](https://codehaus-plexus.github.io/plexus-classworlds/)[51] | 2.8.0 | jar | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
| org.codehaus.plexus | [plexus-component-annotations](http://codehaus-plexus.github.io/plexus-containers/plexus-component-annotations/)[52] | 2.1.0 | jar | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| org.codehaus.plexus | [plexus-utils](https://codehaus-plexus.github.io/plexus-utils/)[53] | 3.5.1 | jar | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| org.codehaus.plexus | [plexus-xml](https://codehaus-plexus.github.io/plexus-xml/)[54] | 3.0.1 | jar | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
| org.eclipse.sisu | [org.eclipse.sisu.inject](http://www.eclipse.org/sisu/org.eclipse.sisu.inject/)[55] | 0.9.0.M3 | jar | [Eclipse Public License, Version 2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |
| org.eclipse.sisu | [org.eclipse.sisu.plexus](http://www.eclipse.org/sisu/org.eclipse.sisu.plexus/)[56] | 0.9.0.M3 | jar | [Eclipse Public License, Version 2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |

# Project Dependency Graph

## [Dependency Tree](#dependency-tree)

- network.ike.docs:semantic-linebreak:maven-plugin:10 ** 
  
  | Semantic Linebreak |
  | --- |
  | **Description: **Maven plugin that reformats AsciiDoc prose to one-sentence-per-line using AsciidoctorJ AST parsing **URL: **[https://github.com/IKE-Network/ike-docs](https://github.com/IKE-Network/ike-docs)[57] **Project Licenses: **[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
  
    - org.apache.maven:maven-plugin-api:jar:3.9.9 (provided) ** 
      
      | Maven Plugin API |
      | --- |
      | **Description: **The API for plugins - Mojos - development. **URL: **[https://maven.apache.org/ref/3.9.9/maven-plugin-api/](https://maven.apache.org/ref/3.9.9/maven-plugin-api/)[12] **Project Licenses: **[Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
      
          - org.apache.maven:maven-model:jar:3.9.9 (provided) ** 
            
            | Maven Model |
            | --- |
            | **Description: **Model for Maven POM (Project Object Model) **URL: **[https://maven.apache.org/ref/3.9.9/maven-model/](https://maven.apache.org/ref/3.9.9/maven-model/)[50] **Project Licenses: **[Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
          - org.apache.maven:maven-artifact:jar:3.9.9 (provided) ** 
            
            | Maven Artifact |
            | --- |
            | **Description: **Maven is a software build management and comprehension tool. Based on the concept of a project object model: builds, dependency management, documentation creation, site publication, and distribution publication are all controlled from the declarative file. Maven can be extended by plugins to utilise a number of other development tools for reporting or the build process. **URL: **[https://maven.apache.org/ref/3.9.9/maven-artifact/](https://maven.apache.org/ref/3.9.9/maven-artifact/)[49] **Project Licenses: **[Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
          - org.eclipse.sisu:org.eclipse.sisu.plexus:jar:0.9.0.M3 (provided) ** 
            
            | org.eclipse.sisu.plexus |
            | --- |
            | **Description: **Plexus-JSR330 adapter; adds Plexus support to the Sisu-Inject container **URL: **[http://www.eclipse.org/sisu/org.eclipse.sisu.plexus/](http://www.eclipse.org/sisu/org.eclipse.sisu.plexus/)[56] **Project Licenses: **[Eclipse Public License, Version 2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |
            
                  - org.eclipse.sisu:org.eclipse.sisu.inject:jar:0.9.0.M3 (provided) ** 
                    
                    | org.eclipse.sisu.inject |
                    | --- |
                    | **Description: **JSR330-based container; supports classpath scanning, auto-binding, and dynamic auto-wiring **URL: **[http://www.eclipse.org/sisu/org.eclipse.sisu.inject/](http://www.eclipse.org/sisu/org.eclipse.sisu.inject/)[55] **Project Licenses: **[Eclipse Public License, Version 2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |
                  - org.codehaus.plexus:plexus-component-annotations:jar:2.1.0 (provided) ** 
                    
                    | Plexus :: Component Annotations |
                    | --- |
                    | **Description: **Plexus Component "Java 5" Annotations, to describe plexus components properties in java sources with standard annotations instead of javadoc annotations. **URL: **[http://codehaus-plexus.github.io/plexus-containers/plexus-component-annotations/](http://codehaus-plexus.github.io/plexus-containers/plexus-component-annotations/)[52] **Project Licenses: **[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - org.codehaus.plexus:plexus-xml:jar:3.0.1 (provided) ** 
                    
                    | Plexus XML Utilities |
                    | --- |
                    | **Description: **A collection of various utility classes to ease working with XML in Maven 3. **URL: **[https://codehaus-plexus.github.io/plexus-xml/](https://codehaus-plexus.github.io/plexus-xml/)[54] **Project Licenses: **[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
          - org.codehaus.plexus:plexus-utils:jar:3.5.1 (provided) ** 
            
            | Plexus Common Utilities |
            | --- |
            | **Description: **A collection of various utility classes to ease working with strings, files, command lines, XML and more. **URL: **[https://codehaus-plexus.github.io/plexus-utils/](https://codehaus-plexus.github.io/plexus-utils/)[53] **Project Licenses: **[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
          - org.codehaus.plexus:plexus-classworlds:jar:2.8.0 (provided) ** 
            
            | Plexus Classworlds |
            | --- |
            | **Description: **A class loader framework **URL: **[https://codehaus-plexus.github.io/plexus-classworlds/](https://codehaus-plexus.github.io/plexus-classworlds/)[51] **Project Licenses: **[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
    - org.apache.maven.plugin-tools:maven-plugin-annotations:jar:3.15.1 (provided) ** 
      
      | Maven Plugin Tools Java Annotations |
      | --- |
      | **Description: **Java annotations to use in Mojos **URL: **[https://maven.apache.org/plugin-tools/maven-plugin-annotations](https://maven.apache.org/plugin-tools/maven-plugin-annotations)[13] **Project Licenses: **[Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
    - org.asciidoctor:asciidoctorj:jar:3.0.1 (compile) ** 
      
      | asciidoctorj |
      | --- |
      | **Description: **AsciidoctorJ provides Java bindings for the Asciidoctor RubyGem (asciidoctor) using JRuby. **URL: **[https://github.com/asciidoctor/asciidoctorj](https://github.com/asciidoctor/asciidoctorj)[1] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
      
          - org.asciidoctor:asciidoctorj-api:jar:3.0.1 (compile) ** 
            
            | asciidoctorj-api |
            | --- |
            | **Description: **API for AsciidoctorJ **URL: **[https://github.com/asciidoctor/asciidoctorj](https://github.com/asciidoctor/asciidoctorj)[1] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
    - org.jruby:jruby:jar:10.0.3.0 (compile) ** 
      
      | JRuby Main Maven Artifact |
      | --- |
      | **Description: **JRuby is the effort to recreate the Ruby (https://www.ruby-lang.org) interpreter in Java. **URL: **[https://github.com/jruby/jruby/jruby-artifacts/jruby](https://github.com/jruby/jruby/jruby-artifacts/jruby)[3] **Project Licenses: **[GPL-2.0](http://www.gnu.org/licenses/gpl-2.0-standalone.html)[4], [LGPL-2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)[5], [EPL-2.0](http://www.eclipse.org/legal/epl-v20.html)[6] |
      
          - org.jruby:jruby-base:jar:10.0.3.0 (compile) ** 
            
            | JRuby Base |
            | --- |
            | **Description: **JRuby is the effort to recreate the Ruby (https://www.ruby-lang.org) interpreter in Java. **URL: **[https://github.com/jruby/jruby/jruby-base](https://github.com/jruby/jruby/jruby-base)[37] **Project Licenses: **[GPL-2.0](http://www.gnu.org/licenses/gpl-2.0-standalone.html)[4], [LGPL-2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)[5], [EPL-2.0](http://www.eclipse.org/legal/epl-v20.html)[6] |
            
                  - org.ow2.asm:asm:jar:9.7.1 (compile) ** 
                    
                    | asm |
                    | --- |
                    | **Description: **ASM, a very small and fast Java bytecode manipulation framework **URL: **[http://asm.ow2.io/](http://asm.ow2.io/)[43] **Project Licenses: **[BSD-3-Clause](https://asm.ow2.io/license.html)[44] |
                  - org.ow2.asm:asm-commons:jar:9.7.1 (compile) ** 
                    
                    | asm-commons |
                    | --- |
                    | **Description: **Usefull class adapters based on ASM, a very small and fast Java bytecode manipulation framework **URL: **[http://asm.ow2.io/](http://asm.ow2.io/)[43] **Project Licenses: **[BSD-3-Clause](https://asm.ow2.io/license.html)[44] |
                    
                            - org.ow2.asm:asm-tree:jar:9.7.1 (compile) ** 
                              
                              | asm-tree |
                              | --- |
                              | **Description: **Tree API of ASM, a very small and fast Java bytecode manipulation framework **URL: **[http://asm.ow2.io/](http://asm.ow2.io/)[43] **Project Licenses: **[BSD-3-Clause](https://asm.ow2.io/license.html)[44] |
                  - org.ow2.asm:asm-util:jar:9.7.1 (compile) ** 
                    
                    | asm-util |
                    | --- |
                    | **Description: **Utilities for ASM, a very small and fast Java bytecode manipulation framework **URL: **[http://asm.ow2.io/](http://asm.ow2.io/)[43] **Project Licenses: **[BSD-3-Clause](https://asm.ow2.io/license.html)[44] |
                    
                            - org.ow2.asm:asm-analysis:jar:9.7.1 (compile) ** 
                              
                              | asm-analysis |
                              | --- |
                              | **Description: **Static code analysis API of ASM, a very small and fast Java bytecode manipulation framework **URL: **[http://asm.ow2.io/](http://asm.ow2.io/)[43] **Project Licenses: **[BSD-3-Clause](https://asm.ow2.io/license.html)[44] |
                  - com.github.jnr:jnr-netdb:jar:1.2.0 (compile) ** 
                    
                    | jnr-netdb |
                    | --- |
                    | **Description: **Lookup TCP and UDP services from java **URL: **[http://github.com/jnr/jnr-netdb](http://github.com/jnr/jnr-netdb)[20] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - com.github.jnr:jnr-enxio:jar:0.32.19 (compile) ** 
                    
                    | jnr-enxio |
                    | --- |
                    | **Description: **Native I/O access for java **URL: **[http://github.com/jnr/jnr-enxio](http://github.com/jnr/jnr-enxio)[18] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - com.github.jnr:jnr-unixsocket:jar:0.38.24 (compile) ** 
                    
                    | jnr-unixsocket |
                    | --- |
                    | **Description: **UNIX socket channels for java **URL: **[http://github.com/jnr/jnr-unixsocket](http://github.com/jnr/jnr-unixsocket)[25] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - com.github.jnr:jnr-posix:jar:3.1.21 (compile) ** 
                    
                    | jnr-posix |
                    | --- |
                    | **Description: **Common cross-project/cross-platform POSIX APIs **URL: **[http://github.com/jnr/jnr-posix](http://github.com/jnr/jnr-posix)[21] **Project Licenses: **[Eclipse Public License - v 2.0](https://www.eclipse.org/legal/epl-2.0/)[22], [GNU General Public License Version 2](http://www.gnu.org/copyleft/gpl.html)[23], [GNU Lesser General Public License Version 2.1](http://www.gnu.org/licenses/lgpl.html)[24] |
                  - com.github.jnr:jnr-constants:jar:0.10.4 (compile) ** 
                    
                    | jnr-constants |
                    | --- |
                    | **Description: **A set of platform constants (e.g. errno values) **URL: **[http://github.com/jnr/jnr-constants](http://github.com/jnr/jnr-constants)[17] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - com.github.jnr:jnr-ffi:jar:2.2.18 (compile) ** 
                    
                    | jnr-ffi |
                    | --- |
                    | **Description: **A library for invoking native functions from java **URL: **[http://github.com/jnr/jnr-ffi](http://github.com/jnr/jnr-ffi)[19] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                    
                            - com.github.jnr:jnr-a64asm:jar:1.0.0 (compile) ** 
                              
                              | jnr-a64asm |
                              | --- |
                              | **Description: **A pure-java A64 assembler **URL: **[http://nexus.sonatype.org/oss-repository-hosting.html/jnr-a64asm](http://nexus.sonatype.org/oss-repository-hosting.html/jnr-a64asm)[16] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                            - com.github.jnr:jnr-x86asm:jar:1.0.2 (compile) ** 
                              
                              | jnr-x86asm |
                              | --- |
                              | **Description: **A pure-java X86 and X86_64 assembler **URL: **[http://github.com/jnr/jnr-x86asm](http://github.com/jnr/jnr-x86asm)[26] **Project Licenses: **[MIT License](http://www.opensource.org/licenses/mit-license.php)[27] |
                  - com.github.jnr:jffi:jar:1.3.14 (compile) ** 
                    
                    | jffi |
                    | --- |
                    | **Description: **Java Foreign Function Interface **URL: **[http://github.com/jnr/jffi](http://github.com/jnr/jffi)[14] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2], [GNU Lesser General Public License version 3](https://www.gnu.org/licenses/lgpl-3.0.txt)[15] |
                  - com.github.jnr:jffi:jar:native:1.3.14 (compile) ** 
                    
                    | jffi |
                    | --- |
                    | **Description: **Java Foreign Function Interface **URL: **[http://github.com/jnr/jffi](http://github.com/jnr/jffi)[14] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2], [GNU Lesser General Public License version 3](https://www.gnu.org/licenses/lgpl-3.0.txt)[15] |
                  - org.jruby.joni:joni:jar:2.2.6 (compile) ** 
                    
                    | Joni |
                    | --- |
                    | **Description: **Java port of Oniguruma: http://www.geocities.jp/kosako3/oniguruma that uses byte arrays directly instead of java Strings and chars **URL: **[http://nexus.sonatype.org/oss-repository-hosting.html/joni](http://nexus.sonatype.org/oss-repository-hosting.html/joni)[42] **Project Licenses: **[MIT License](http://www.opensource.org/licenses/mit-license.php)[27] |
                  - org.jruby.jcodings:jcodings:jar:1.0.63 (compile) ** 
                    
                    | JCodings |
                    | --- |
                    | **Description: **Byte based encoding support library for java **URL: **[http://nexus.sonatype.org/oss-repository-hosting.html/jcodings](http://nexus.sonatype.org/oss-repository-hosting.html/jcodings)[41] **Project Licenses: **[MIT License](http://www.opensource.org/licenses/mit-license.php)[27] |
                  - org.jruby:dirgra:jar:0.5 (compile) ** 
                    
                    | Dirgra |
                    | --- |
                    | **Description: **Simple Directed Graph **URL: **[https://github.com/jruby/dirgra](https://github.com/jruby/dirgra)[35] **Project Licenses: **[EPL](http://www.eclipse.org/legal/epl-v10.html)[36] |
                  - com.headius:invokebinder:jar:1.14 (compile) ** 
                    
                    | invokebinder |
                    | --- |
                    | **Description: **Sonatype helps open source projects to set up Maven repositories on https://oss.sonatype.org/ **URL: **[http://maven.apache.org](http://maven.apache.org)[29] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - com.headius:options:jar:1.6 (compile) ** 
                    
                    | options |
                    | --- |
                    | **Description: **Sonatype helps open source projects to set up Maven repositories on https://oss.sonatype.org/ **URL: **[https://github.com/headius/options](https://github.com/headius/options)[30] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - org.jruby:jzlib:jar:1.1.5 (compile) ** 
                    
                    | JZlib |
                    | --- |
                    | **Description: **JZlib is a re-implementation of zlib in pure Java **URL: **[http://www.jcraft.com/jzlib/](http://www.jcraft.com/jzlib/)[39] **Project Licenses: **[BSD](http://www.jcraft.com/jzlib/LICENSE.txt)[40] |
                  - joda-time:joda-time:jar:2.14.0 (compile) ** 
                    
                    | Joda-Time |
                    | --- |
                    | **Description: **Date and time library to replace JDK date handling **URL: **[https://www.joda.org/joda-time/](https://www.joda.org/joda-time/)[31] **Project Licenses: **[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
                  - me.qmx.jitescript:jitescript:jar:0.4.1 (compile) ** 
                    
                    | jitescript |
                    | --- |
                    | **Description: **Java API for Bytecode **URL: **[https://github.com/qmx/jitescript](https://github.com/qmx/jitescript)[32] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - com.headius:backport9:jar:1.13 (compile) ** 
                    
                    | backport9 |
                    | --- |
                    | **Description: **Sonatype helps open source projects to set up Maven repositories on https://oss.sonatype.org/ **URL: **[http://nexus.sonatype.org/oss-repository-hosting.html/backport9](http://nexus.sonatype.org/oss-repository-hosting.html/backport9)[28] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - org.crac:crac:jar:1.5.0 (compile) ** 
                    
                    | crac |
                    | --- |
                    | **Description: **A wrapper for OpenJDK CRaC API to build and run on any JDK **URL: **[https://github.com/crac/org.crac](https://github.com/crac/org.crac)[33] **Project Licenses: **[BSD-2-Clause](https://opensource.org/licenses/BSD-2-Clause)[34] |
          - org.jruby:jruby-stdlib:jar:10.0.3.0 (compile) ** 
            
            | JRuby Lib Setup |
            | --- |
            | **Description: **JRuby is the effort to recreate the Ruby (https://www.ruby-lang.org) interpreter in Java. **URL: **[https://github.com/jruby/jruby/jruby-stdlib](https://github.com/jruby/jruby/jruby-stdlib)[38] **Project Licenses: **[GPL-2.0](http://www.gnu.org/licenses/gpl-2.0-standalone.html)[4], [LGPL-2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)[5], [EPL-2.0](http://www.eclipse.org/legal/epl-v20.html)[6] |
    - network.ike.tooling:ike-build-standards:zip:claude:148 (provided) ** 
      
      | IKE Build Standards |
      | --- |
      | **Description: **Versioned Claude instruction files for IKE projects. Modular standards (Maven, Java, IKE-specific) distributed as a classified Maven artifact. **URL: **[https://ike.network/ike-tooling/ike-build-standards/](https://ike.network/ike-tooling/ike-build-standards/)[11] **Project Licenses: **[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
    - org.junit.jupiter:junit-jupiter:jar:6.0.0 (test) ** 
      
      | JUnit Jupiter (Aggregator) |
      | --- |
      | **Description: **Module "junit-jupiter" of JUnit **URL: **[https://junit.org/](https://junit.org/)[9] **Project Licenses: **[Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |
      
          - org.junit.jupiter:junit-jupiter-api:jar:6.0.0 (test) ** 
            
            | JUnit Jupiter API |
            | --- |
            | **Description: **Module "junit-jupiter-api" of JUnit **URL: **[https://junit.org/](https://junit.org/)[9] **Project Licenses: **[Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |
            
                  - org.opentest4j:opentest4j:jar:1.3.0 (test) ** 
                    
                    | org.opentest4j:opentest4j |
                    | --- |
                    | **Description: **Open Test Alliance for the JVM **URL: **[https://github.com/ota4j-team/opentest4j](https://github.com/ota4j-team/opentest4j)[48] **Project Licenses: **[The Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
                  - org.junit.platform:junit-platform-commons:jar:6.0.0 (test) ** 
                    
                    | JUnit Platform Commons |
                    | --- |
                    | **Description: **Module "junit-platform-commons" of JUnit **URL: **[https://junit.org/](https://junit.org/)[9] **Project Licenses: **[Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |
                  - org.apiguardian:apiguardian-api:jar:1.1.2 (test) ** 
                    
                    | org.apiguardian:apiguardian-api |
                    | --- |
                    | **Description: **@API Guardian **URL: **[https://github.com/apiguardian-team/apiguardian](https://github.com/apiguardian-team/apiguardian)[46] **Project Licenses: **[The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - org.jspecify:jspecify:jar:1.0.0 (test) ** 
                    
                    | JSpecify annotations |
                    | --- |
                    | **Description: **An artifact of well-named and well-specified annotations to power static analysis checks **URL: **[http://jspecify.org/](http://jspecify.org/)[47] **Project Licenses: **[The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
          - org.junit.jupiter:junit-jupiter-params:jar:6.0.0 (test) ** 
            
            | JUnit Jupiter Params |
            | --- |
            | **Description: **Module "junit-jupiter-params" of JUnit **URL: **[https://junit.org/](https://junit.org/)[9] **Project Licenses: **[Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |
          - org.junit.jupiter:junit-jupiter-engine:jar:6.0.0 (test) ** 
            
            | JUnit Jupiter Engine |
            | --- |
            | **Description: **Module "junit-jupiter-engine" of JUnit **URL: **[https://junit.org/](https://junit.org/)[9] **Project Licenses: **[Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |
            
                  - org.junit.platform:junit-platform-engine:jar:6.0.0 (test) ** 
                    
                    | JUnit Platform Engine API |
                    | --- |
                    | **Description: **Module "junit-platform-engine" of JUnit **URL: **[https://junit.org/](https://junit.org/)[9] **Project Licenses: **[Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[10] |
    - org.assertj:assertj-core:jar:3.27.3 (test) ** 
      
      | AssertJ Core |
      | --- |
      | **Description: **Rich and fluent assertions for testing in Java **URL: **[https://assertj.github.io/doc/#assertj-core](https://assertj.github.io/doc/#assertj-core)[7] **Project Licenses: **[Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
      
          - net.bytebuddy:byte-buddy:jar:1.15.11 (test) ** 
            
            | Byte Buddy (without dependencies) |
            | --- |
            | **Description: **Byte Buddy is a Java library for creating Java classes at run time. This artifact is a build of Byte Buddy with all ASM dependencies repackaged into its own name space. **URL: **[https://bytebuddy.net/byte-buddy](https://bytebuddy.net/byte-buddy)[45] **Project Licenses: **[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
    - network.ike.tooling:ike-build-standards:zip:site-theme:148 (provided) ** 
      
      | IKE Build Standards |
      | --- |
      | **Description: **Versioned Claude instruction files for IKE projects. Modular standards (Maven, Java, IKE-specific) distributed as a classified Maven artifact. **URL: **[https://ike.network/ike-tooling/ike-build-standards/](https://ike.network/ike-tooling/ike-build-standards/)[11] **Project Licenses: **[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |
    - network.ike.tooling:ike-build-standards:zip:built-with:148 (provided) ** 
      
      | IKE Build Standards |
      | --- |
      | **Description: **Versioned Claude instruction files for IKE projects. Modular standards (Maven, Java, IKE-specific) distributed as a classified Maven artifact. **URL: **[https://ike.network/ike-tooling/ike-build-standards/](https://ike.network/ike-tooling/ike-build-standards/)[11] **Project Licenses: **[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[8] |

# Licenses

**EPL: **Dirgra

**LGPL-2.1: **JRuby Base, JRuby Lib Setup, JRuby Main Maven Artifact

**The Apache License, Version 2.0: **JSpecify annotations, org.apiguardian:apiguardian-api, org.opentest4j:opentest4j

**BSD-3-Clause: **asm, asm-analysis, asm-commons, asm-tree, asm-util

**MIT License: **JCodings, Joni, jnr-x86asm

**BSD-2-Clause: **crac

**GPL-2.0: **JRuby Base, JRuby Lib Setup, JRuby Main Maven Artifact

**Eclipse Public License v2.0: **JUnit Jupiter (Aggregator), JUnit Jupiter API, JUnit Jupiter Engine, JUnit Jupiter Params, JUnit Platform Commons, JUnit Platform Engine API

**Eclipse Public License, Version 2.0: **org.eclipse.sisu.inject, org.eclipse.sisu.plexus

**BSD: **JZlib

**Apache License, Version 2.0: **Byte Buddy (without dependencies), IKE Build Standards, Joda-Time, Plexus :: Component Annotations, Plexus Classworlds, Plexus Common Utilities, Plexus XML Utilities, Semantic Linebreak

**Apache-2.0: **AssertJ Core, Maven Artifact, Maven Model, Maven Plugin API, Maven Plugin Tools Java Annotations

**GNU Lesser General Public License Version 2.1: **jnr-posix

**Eclipse Public License - v 2.0: **jnr-posix

**EPL-2.0: **JRuby Base, JRuby Lib Setup, JRuby Main Maven Artifact

**The Apache Software License, Version 2.0: **asciidoctorj, asciidoctorj-api, backport9, invokebinder, jffi, jitescript, jnr-a64asm, jnr-constants, jnr-enxio, jnr-ffi, jnr-netdb, jnr-unixsocket, options

**GNU General Public License Version 2: **jnr-posix

**GNU Lesser General Public License version 3: **jffi

# Dependency File Details

| Total | Size | Entries | Classes | Packages | Java Version | Debug Information |
| --- | --- | --- | --- | --- | --- | --- |
| jffi-1.3.14-native.jar | 1 MB | 49 | 0 | 0 | - | - |
| jffi-1.3.14.jar | 163.2 kB | 144 | 133 | 2 | 1.8 | Yes |
| jnr-a64asm-1.0.0.jar | 86.3 kB | 57 | 48 | 1 | 1.7 | Yes |
| jnr-constants-0.10.4.jar | 1.6 MB | 1063 | 1038 | 17 | 1.8 | Yes |
| jnr-enxio-0.32.19.jar | 34.6 kB | 37 | 27 | 1 | 1.8 | Yes |
| jnr-ffi-2.2.18.jar | 744.6 kB | 745 | 669 | 50 | 1.8 | Yes |
| jnr-netdb-1.2.0.jar | 63.1 kB | 55 | 46 | 1 | 1.8 | Yes |
| jnr-posix-3.1.21.jar | 289.7 kB | 256 | 245 | 3 | 1.8 | Yes |
| jnr-unixsocket-0.38.24.jar | 48.2 kB | 40 | 30 | 2 | 1.8 | Yes |
| jnr-x86asm-1.0.2.jar | 219.9 kB | 97 | 84 | 2 | 1.5 | Yes |
| backport9-1.13.jar | 14 kB | 29 | 13 | 7 | 1.8 | Yes |
| invokebinder-1.14.jar | 53.1 kB | 34 | 23 | 3 | 1.8 | Yes |
| options-1.6.jar | 14.9 kB | 21 | 10 | 3 | 1.8 | Yes |
| joda-time-2.14.0.jar | 639.8 kB | 770 | 248 | 7 | 1.5 | Yes |
| jitescript-0.4.1.jar | 23 kB | 20 | 9 | 2 | 1.6 | Yes |
| byte-buddy-1.15.11.jar | 8.5 MB | 5890 | - | - | - | - |
|    • Root | - | 2950 | 2897 | 38 | 1.5 | Yes |
|    • Versioned | - | 2940 | 2898 | 39 | 1.8 | Yes |
| ike-build-standards-148-built-with.zip | 3.5 kB | - | - | - | - | - |
| ike-build-standards-148-claude.zip | 81 kB | - | - | - | - | - |
| ike-build-standards-148-site-theme.zip | 3.4 kB | - | - | - | - | - |
| maven-artifact-3.9.9.jar | 58.8 kB | 59 | 34 | 11 | 1.8 | Yes |
| maven-model-3.9.9.jar | 217.8 kB | 97 | 80 | 3 | 1.8 | Yes |
| maven-plugin-api-3.9.9.jar | 47.1 kB | 48 | 27 | 6 | 1.8 | Yes |
| maven-plugin-annotations-3.15.1.jar | 13.8 kB | 22 | 7 | 1 | 1.8 | Yes |
| apiguardian-api-1.1.2.jar | 6.8 kB | 9 | 3 | 2 | 1.6 | Yes |
| asciidoctorj-3.0.1.jar | 1.9 MB | 1255 | 142 | 11 | 11 | Yes |
| asciidoctorj-api-3.0.1.jar | 60.3 kB | 91 | 82 | 6 | 11 | Yes |
| assertj-core-3.27.3.jar | 1.4 MB | 881 | - | - | - | - |
|    • Root | - | 877 | 838 | 27 | 1.8 | Yes |
|    • Versioned | - | 4 | 1 | 1 | 9 | No |
| plexus-classworlds-2.8.0.jar | 53.6 kB | 51 | 36 | 5 | 1.8 | Yes |
| plexus-component-annotations-2.1.0.jar | 4.2 kB | 15 | 3 | 1 | 1.6 | No |
| plexus-utils-3.5.1.jar | 269.3 kB | 152 | - | - | - | - |
|    • Root | - | 134 | 108 | 9 | 1.8 | Yes |
|    • Versioned | - | 6 | 1 | 1 | 9 | Yes |
|    • Versioned | - | 6 | 1 | 1 | 10 | Yes |
|    • Versioned | - | 6 | 1 | 1 | 11 | Yes |
| plexus-xml-3.0.1.jar | 94.3 kB | 44 | 25 | 2 | 1.8 | Yes |
| crac-1.5.0.jar | 13.4 kB | 24 | 14 | 3 | 1.8 | Yes |
| org.eclipse.sisu.inject-0.9.0.M3.jar | 433.6 kB | 318 | 297 | 9 | 1.8 | Yes |
| org.eclipse.sisu.plexus-0.9.0.M3.jar | 216.3 kB | 205 | 167 | 20 | 1.8 | Yes |
| dirgra-0.5.jar | 17 kB | 21 | 11 | 2 | 1.8 | Yes |
| jruby-10.0.3.0.jar | 26.2 kB | 12 | 0 | 0 | - | - |
| jruby-base-10.0.3.0.jar | 9.4 MB | 6530 | 6346 | 115 | 21 | Yes |
| jruby-stdlib-10.0.3.0.jar | 19 MB | 3052 | 0 | 0 | - | - |
| jzlib-1.1.5.jar | 74.9 kB | 36 | 26 | 1 | 1.7 | Yes |
| jcodings-1.0.63.jar | 1.8 MB | 862 | 166 | 11 | 1.8 | Yes |
| joni-2.2.6.jar | 232.4 kB | 121 | 107 | 7 | 1.8 | Yes |
| jspecify-1.0.0.jar | 3.8 kB | 14 | - | - | - | - |
|    • Root | - | 10 | 4 | 1 | 1.8 | No |
|    • Versioned | - | 4 | 1 | 1 | 9 | No |
| junit-jupiter-6.0.0.jar | 6.4 kB | 5 | 1 | 1 | 17 | No |
| junit-jupiter-api-6.0.0.jar | 249.9 kB | 224 | 208 | 9 | 17 | Yes |
| junit-jupiter-engine-6.0.0.jar | 353.7 kB | 188 | 171 | 9 | 17 | Yes |
| junit-jupiter-params-6.0.0.jar | 293.7 kB | 215 | 194 | 9 | 17 | Yes |
| junit-platform-commons-6.0.0.jar | 171.1 kB | 103 | 87 | 10 | 17 | Yes |
| junit-platform-engine-6.0.0.jar | 277.6 kB | 193 | 175 | 9 | 17 | Yes |
| opentest4j-1.3.0.jar | 14.3 kB | 15 | 9 | 2 | 1.6 | Yes |
| asm-9.7.1.jar | 126.1 kB | 45 | 39 | 3 | 1.5 | Yes |
| asm-analysis-9.7.1.jar | 35.1 kB | 22 | 15 | 2 | 1.5 | Yes |
| asm-commons-9.7.1.jar | 73.5 kB | 34 | 28 | 2 | 1.5 | Yes |
| asm-tree-9.7.1.jar | 51.9 kB | 45 | 39 | 2 | 1.5 | Yes |
| asm-util-9.7.1.jar | 94.5 kB | 33 | 27 | 2 | 1.5 | Yes |
| 54 | 50.6 MB | 24348 | 15036 | 452 | 21 | 45 |
| compile: 30 | compile: 37.8 MB | compile: 15600 | compile: 9665 | compile: 268 | 21 | compile: 27 |
| provided: 13 | provided: 1.5 MB | provided: 1011 | provided: 784 | provided: 67 | provided: 9 |
| test: 11 | test: 11.2 MB | test: 7737 | test: 4587 | test: 117 | 17 | test: 9 |
