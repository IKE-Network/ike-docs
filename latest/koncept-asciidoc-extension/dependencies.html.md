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
| org.yaml | [snakeyaml](https://bitbucket.org/snakeyaml/snakeyaml)[1] | 2.2 | jar | [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |

## [test](#test)

The following is a list of test dependencies for this project. These dependencies are only required to compile and run unit tests for the application:

| GroupId | ArtifactId | Version | Type | Licenses |
| --- | --- | --- | --- | --- |
| org.apache.logging.log4j | [log4j-core](https://logging.apache.org/log4j/3.x/log4j/log4j-core/)[3] | 3.0.0-beta2 | jar | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |
| org.apache.logging.log4j | [log4j-slf4j2-impl](https://logging.apache.org/log4j/3.x/log4j/log4j-slf4j2-impl/)[5] | 3.0.0-beta2 | jar | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |
| org.junit.jupiter | [junit-jupiter](https://junit.org/junit5/)[6] | 5.10.2 | jar | [Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[7] |

## [provided](#provided)

The following is a list of provided dependencies for this project. These dependencies are required to compile the application, but should be provided by default when using the library:

| GroupId | ArtifactId | Version | Classifier | Type | Licenses |
| --- | --- | --- | --- | --- | --- |
| network.ike.tooling | [ike-build-standards](https://ike.network/ike-tooling/ike-build-standards/)[8] | 146 | site-theme | zip | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |
| org.asciidoctor | [asciidoctorj](https://github.com/asciidoctor/asciidoctorj)[9] | 3.0.1 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| org.jruby | [jruby](https://github.com/jruby/jruby/jruby-artifacts/jruby)[10] | 10.0.3.0 | - | jar | [GPL-2.0](http://www.gnu.org/licenses/gpl-2.0-standalone.html)[11][LGPL-2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)[12][EPL-2.0](http://www.eclipse.org/legal/epl-v20.html)[13] |
| org.slf4j | [slf4j-api](http://www.slf4j.org)[14] | 2.0.12 | - | jar | [MIT License](http://www.opensource.org/licenses/mit-license.php)[15] |

# Project Transitive Dependencies

The following is a list of transitive dependencies for this project. Transitive dependencies are the dependencies of the project dependencies.

## [test](#test_2)

The following is a list of test dependencies for this project. These dependencies are only required to compile and run unit tests for the application:

| GroupId | ArtifactId | Version | Type | Licenses |
| --- | --- | --- | --- | --- |
| org.apache.logging.log4j | [log4j-api](https://logging.apache.org/log4j/3.x/log4j/log4j-api/)[16] | 3.0.0-beta2 | jar | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |
| org.apache.logging.log4j | [log4j-plugins](https://logging.apache.org/log4j/3.x/log4j/log4j-plugins/)[17] | 3.0.0-beta2 | jar | [Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |
| org.apiguardian | [apiguardian-api](https://github.com/apiguardian-team/apiguardian)[18] | 1.1.2 | jar | [The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| org.junit.jupiter | [junit-jupiter-api](https://junit.org/junit5/)[6] | 5.10.2 | jar | [Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[7] |
| org.junit.jupiter | [junit-jupiter-engine](https://junit.org/junit5/)[6] | 5.10.2 | jar | [Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[7] |
| org.junit.jupiter | [junit-jupiter-params](https://junit.org/junit5/)[6] | 5.10.2 | jar | [Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[7] |
| org.junit.platform | [junit-platform-commons](https://junit.org/junit5/)[6] | 1.10.2 | jar | [Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[7] |
| org.junit.platform | [junit-platform-engine](https://junit.org/junit5/)[6] | 1.10.2 | jar | [Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[7] |
| org.opentest4j | [opentest4j](https://github.com/ota4j-team/opentest4j)[19] | 1.3.0 | jar | [The Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |

## [provided](#provided_2)

The following is a list of provided dependencies for this project. These dependencies are required to compile the application, but should be provided by default when using the library:

| GroupId | ArtifactId | Version | Classifier | Type | Licenses |
| --- | --- | --- | --- | --- | --- |
| com.github.jnr | [jffi](http://github.com/jnr/jffi)[20] | 1.3.14 | native | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2][GNU Lesser General Public License version 3](https://www.gnu.org/licenses/lgpl-3.0.txt)[21] |
| com.github.jnr | [jffi](http://github.com/jnr/jffi)[20] | 1.3.14 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2][GNU Lesser General Public License version 3](https://www.gnu.org/licenses/lgpl-3.0.txt)[21] |
| com.github.jnr | [jnr-a64asm](http://nexus.sonatype.org/oss-repository-hosting.html/jnr-a64asm)[22] | 1.0.0 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.github.jnr | [jnr-constants](http://github.com/jnr/jnr-constants)[23] | 0.10.4 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.github.jnr | [jnr-enxio](http://github.com/jnr/jnr-enxio)[24] | 0.32.19 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.github.jnr | [jnr-ffi](http://github.com/jnr/jnr-ffi)[25] | 2.2.18 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.github.jnr | [jnr-netdb](http://github.com/jnr/jnr-netdb)[26] | 1.2.0 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.github.jnr | [jnr-posix](http://github.com/jnr/jnr-posix)[27] | 3.1.21 | - | jar | [Eclipse Public License - v 2.0](https://www.eclipse.org/legal/epl-2.0/)[28][GNU General Public License Version 2](http://www.gnu.org/copyleft/gpl.html)[29][GNU Lesser General Public License Version 2.1](http://www.gnu.org/licenses/lgpl.html)[30] |
| com.github.jnr | [jnr-unixsocket](http://github.com/jnr/jnr-unixsocket)[31] | 0.38.24 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.github.jnr | [jnr-x86asm](http://github.com/jnr/jnr-x86asm)[32] | 1.0.2 | - | jar | [MIT License](http://www.opensource.org/licenses/mit-license.php)[15] |
| com.headius | [backport9](http://nexus.sonatype.org/oss-repository-hosting.html/backport9)[33] | 1.13 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.headius | [invokebinder](http://maven.apache.org)[34] | 1.14 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| com.headius | [options](https://github.com/headius/options)[35] | 1.6 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| joda-time | [joda-time](https://www.joda.org/joda-time/)[36] | 2.14.0 | - | jar | [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |
| me.qmx.jitescript | [jitescript](https://github.com/qmx/jitescript)[37] | 0.4.1 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| org.asciidoctor | [asciidoctorj-api](https://github.com/asciidoctor/asciidoctorj)[9] | 3.0.1 | - | jar | [The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
| org.crac | [crac](https://github.com/crac/org.crac)[38] | 1.5.0 | - | jar | [BSD-2-Clause](https://opensource.org/licenses/BSD-2-Clause)[39] |
| org.jruby | [dirgra](https://github.com/jruby/dirgra)[40] | 0.5 | - | jar | [EPL](http://www.eclipse.org/legal/epl-v10.html)[41] |
| org.jruby | [jruby-base](https://github.com/jruby/jruby/jruby-base)[42] | 10.0.3.0 | - | jar | [GPL-2.0](http://www.gnu.org/licenses/gpl-2.0-standalone.html)[11][LGPL-2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)[12][EPL-2.0](http://www.eclipse.org/legal/epl-v20.html)[13] |
| org.jruby | [jruby-stdlib](https://github.com/jruby/jruby/jruby-stdlib)[43] | 10.0.3.0 | - | jar | [GPL-2.0](http://www.gnu.org/licenses/gpl-2.0-standalone.html)[11][LGPL-2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)[12][EPL-2.0](http://www.eclipse.org/legal/epl-v20.html)[13] |
| org.jruby | [jzlib](http://www.jcraft.com/jzlib/)[44] | 1.1.5 | - | jar | [BSD](http://www.jcraft.com/jzlib/LICENSE.txt)[45] |
| org.jruby.jcodings | [jcodings](http://nexus.sonatype.org/oss-repository-hosting.html/jcodings)[46] | 1.0.63 | - | jar | [MIT License](http://www.opensource.org/licenses/mit-license.php)[15] |
| org.jruby.joni | [joni](http://nexus.sonatype.org/oss-repository-hosting.html/joni)[47] | 2.2.6 | - | jar | [MIT License](http://www.opensource.org/licenses/mit-license.php)[15] |
| org.ow2.asm | [asm](http://asm.ow2.io/)[48] | 9.7.1 | - | jar | [BSD-3-Clause](https://asm.ow2.io/license.html)[49] |
| org.ow2.asm | [asm-analysis](http://asm.ow2.io/)[48] | 9.7.1 | - | jar | [BSD-3-Clause](https://asm.ow2.io/license.html)[49] |
| org.ow2.asm | [asm-commons](http://asm.ow2.io/)[48] | 9.7.1 | - | jar | [BSD-3-Clause](https://asm.ow2.io/license.html)[49] |
| org.ow2.asm | [asm-tree](http://asm.ow2.io/)[48] | 9.7.1 | - | jar | [BSD-3-Clause](https://asm.ow2.io/license.html)[49] |
| org.ow2.asm | [asm-util](http://asm.ow2.io/)[48] | 9.7.1 | - | jar | [BSD-3-Clause](https://asm.ow2.io/license.html)[49] |

# Project Dependency Graph

## [Dependency Tree](#dependency-tree)

- network.ike.docs:koncept-asciidoc-extension:jar:7 ** 
  
  | Koncept AsciiDoc Extension |
  | --- |
  | **Description: **AsciidoctorJ extension providing inline Koncept markup (k:ConceptName[]) with SVG badge rendering and auto-generated Referenced Koncepts glossary with description logic axiom display. **URL: **[https://github.com/IKE-Network/ike-docs](https://github.com/IKE-Network/ike-docs)[50] **Project Licenses: **[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |
  
    - org.asciidoctor:asciidoctorj:jar:3.0.1 (provided) ** 
      
      | asciidoctorj |
      | --- |
      | **Description: **AsciidoctorJ provides Java bindings for the Asciidoctor RubyGem (asciidoctor) using JRuby. **URL: **[https://github.com/asciidoctor/asciidoctorj](https://github.com/asciidoctor/asciidoctorj)[9] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
      
          - org.asciidoctor:asciidoctorj-api:jar:3.0.1 (provided) ** 
            
            | asciidoctorj-api |
            | --- |
            | **Description: **API for AsciidoctorJ **URL: **[https://github.com/asciidoctor/asciidoctorj](https://github.com/asciidoctor/asciidoctorj)[9] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
    - org.jruby:jruby:jar:10.0.3.0 (provided) ** 
      
      | JRuby Main Maven Artifact |
      | --- |
      | **Description: **JRuby is the effort to recreate the Ruby (https://www.ruby-lang.org) interpreter in Java. **URL: **[https://github.com/jruby/jruby/jruby-artifacts/jruby](https://github.com/jruby/jruby/jruby-artifacts/jruby)[10] **Project Licenses: **[GPL-2.0](http://www.gnu.org/licenses/gpl-2.0-standalone.html)[11], [LGPL-2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)[12], [EPL-2.0](http://www.eclipse.org/legal/epl-v20.html)[13] |
      
          - org.jruby:jruby-base:jar:10.0.3.0 (provided) ** 
            
            | JRuby Base |
            | --- |
            | **Description: **JRuby is the effort to recreate the Ruby (https://www.ruby-lang.org) interpreter in Java. **URL: **[https://github.com/jruby/jruby/jruby-base](https://github.com/jruby/jruby/jruby-base)[42] **Project Licenses: **[GPL-2.0](http://www.gnu.org/licenses/gpl-2.0-standalone.html)[11], [LGPL-2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)[12], [EPL-2.0](http://www.eclipse.org/legal/epl-v20.html)[13] |
            
                  - org.ow2.asm:asm:jar:9.7.1 (provided) ** 
                    
                    | asm |
                    | --- |
                    | **Description: **ASM, a very small and fast Java bytecode manipulation framework **URL: **[http://asm.ow2.io/](http://asm.ow2.io/)[48] **Project Licenses: **[BSD-3-Clause](https://asm.ow2.io/license.html)[49] |
                  - org.ow2.asm:asm-commons:jar:9.7.1 (provided) ** 
                    
                    | asm-commons |
                    | --- |
                    | **Description: **Usefull class adapters based on ASM, a very small and fast Java bytecode manipulation framework **URL: **[http://asm.ow2.io/](http://asm.ow2.io/)[48] **Project Licenses: **[BSD-3-Clause](https://asm.ow2.io/license.html)[49] |
                    
                            - org.ow2.asm:asm-tree:jar:9.7.1 (provided) ** 
                              
                              | asm-tree |
                              | --- |
                              | **Description: **Tree API of ASM, a very small and fast Java bytecode manipulation framework **URL: **[http://asm.ow2.io/](http://asm.ow2.io/)[48] **Project Licenses: **[BSD-3-Clause](https://asm.ow2.io/license.html)[49] |
                  - org.ow2.asm:asm-util:jar:9.7.1 (provided) ** 
                    
                    | asm-util |
                    | --- |
                    | **Description: **Utilities for ASM, a very small and fast Java bytecode manipulation framework **URL: **[http://asm.ow2.io/](http://asm.ow2.io/)[48] **Project Licenses: **[BSD-3-Clause](https://asm.ow2.io/license.html)[49] |
                    
                            - org.ow2.asm:asm-analysis:jar:9.7.1 (provided) ** 
                              
                              | asm-analysis |
                              | --- |
                              | **Description: **Static code analysis API of ASM, a very small and fast Java bytecode manipulation framework **URL: **[http://asm.ow2.io/](http://asm.ow2.io/)[48] **Project Licenses: **[BSD-3-Clause](https://asm.ow2.io/license.html)[49] |
                  - com.github.jnr:jnr-netdb:jar:1.2.0 (provided) ** 
                    
                    | jnr-netdb |
                    | --- |
                    | **Description: **Lookup TCP and UDP services from java **URL: **[http://github.com/jnr/jnr-netdb](http://github.com/jnr/jnr-netdb)[26] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - com.github.jnr:jnr-enxio:jar:0.32.19 (provided) ** 
                    
                    | jnr-enxio |
                    | --- |
                    | **Description: **Native I/O access for java **URL: **[http://github.com/jnr/jnr-enxio](http://github.com/jnr/jnr-enxio)[24] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - com.github.jnr:jnr-unixsocket:jar:0.38.24 (provided) ** 
                    
                    | jnr-unixsocket |
                    | --- |
                    | **Description: **UNIX socket channels for java **URL: **[http://github.com/jnr/jnr-unixsocket](http://github.com/jnr/jnr-unixsocket)[31] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - com.github.jnr:jnr-posix:jar:3.1.21 (provided) ** 
                    
                    | jnr-posix |
                    | --- |
                    | **Description: **Common cross-project/cross-platform POSIX APIs **URL: **[http://github.com/jnr/jnr-posix](http://github.com/jnr/jnr-posix)[27] **Project Licenses: **[Eclipse Public License - v 2.0](https://www.eclipse.org/legal/epl-2.0/)[28], [GNU General Public License Version 2](http://www.gnu.org/copyleft/gpl.html)[29], [GNU Lesser General Public License Version 2.1](http://www.gnu.org/licenses/lgpl.html)[30] |
                  - com.github.jnr:jnr-constants:jar:0.10.4 (provided) ** 
                    
                    | jnr-constants |
                    | --- |
                    | **Description: **A set of platform constants (e.g. errno values) **URL: **[http://github.com/jnr/jnr-constants](http://github.com/jnr/jnr-constants)[23] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - com.github.jnr:jnr-ffi:jar:2.2.18 (provided) ** 
                    
                    | jnr-ffi |
                    | --- |
                    | **Description: **A library for invoking native functions from java **URL: **[http://github.com/jnr/jnr-ffi](http://github.com/jnr/jnr-ffi)[25] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                    
                            - com.github.jnr:jnr-a64asm:jar:1.0.0 (provided) ** 
                              
                              | jnr-a64asm |
                              | --- |
                              | **Description: **A pure-java A64 assembler **URL: **[http://nexus.sonatype.org/oss-repository-hosting.html/jnr-a64asm](http://nexus.sonatype.org/oss-repository-hosting.html/jnr-a64asm)[22] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                            - com.github.jnr:jnr-x86asm:jar:1.0.2 (provided) ** 
                              
                              | jnr-x86asm |
                              | --- |
                              | **Description: **A pure-java X86 and X86_64 assembler **URL: **[http://github.com/jnr/jnr-x86asm](http://github.com/jnr/jnr-x86asm)[32] **Project Licenses: **[MIT License](http://www.opensource.org/licenses/mit-license.php)[15] |
                  - com.github.jnr:jffi:jar:1.3.14 (provided) ** 
                    
                    | jffi |
                    | --- |
                    | **Description: **Java Foreign Function Interface **URL: **[http://github.com/jnr/jffi](http://github.com/jnr/jffi)[20] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2], [GNU Lesser General Public License version 3](https://www.gnu.org/licenses/lgpl-3.0.txt)[21] |
                  - com.github.jnr:jffi:jar:native:1.3.14 (provided) ** 
                    
                    | jffi |
                    | --- |
                    | **Description: **Java Foreign Function Interface **URL: **[http://github.com/jnr/jffi](http://github.com/jnr/jffi)[20] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2], [GNU Lesser General Public License version 3](https://www.gnu.org/licenses/lgpl-3.0.txt)[21] |
                  - org.jruby.joni:joni:jar:2.2.6 (provided) ** 
                    
                    | Joni |
                    | --- |
                    | **Description: **Java port of Oniguruma: http://www.geocities.jp/kosako3/oniguruma that uses byte arrays directly instead of java Strings and chars **URL: **[http://nexus.sonatype.org/oss-repository-hosting.html/joni](http://nexus.sonatype.org/oss-repository-hosting.html/joni)[47] **Project Licenses: **[MIT License](http://www.opensource.org/licenses/mit-license.php)[15] |
                  - org.jruby.jcodings:jcodings:jar:1.0.63 (provided) ** 
                    
                    | JCodings |
                    | --- |
                    | **Description: **Byte based encoding support library for java **URL: **[http://nexus.sonatype.org/oss-repository-hosting.html/jcodings](http://nexus.sonatype.org/oss-repository-hosting.html/jcodings)[46] **Project Licenses: **[MIT License](http://www.opensource.org/licenses/mit-license.php)[15] |
                  - org.jruby:dirgra:jar:0.5 (provided) ** 
                    
                    | Dirgra |
                    | --- |
                    | **Description: **Simple Directed Graph **URL: **[https://github.com/jruby/dirgra](https://github.com/jruby/dirgra)[40] **Project Licenses: **[EPL](http://www.eclipse.org/legal/epl-v10.html)[41] |
                  - com.headius:invokebinder:jar:1.14 (provided) ** 
                    
                    | invokebinder |
                    | --- |
                    | **Description: **Sonatype helps open source projects to set up Maven repositories on https://oss.sonatype.org/ **URL: **[http://maven.apache.org](http://maven.apache.org)[34] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - com.headius:options:jar:1.6 (provided) ** 
                    
                    | options |
                    | --- |
                    | **Description: **Sonatype helps open source projects to set up Maven repositories on https://oss.sonatype.org/ **URL: **[https://github.com/headius/options](https://github.com/headius/options)[35] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - org.jruby:jzlib:jar:1.1.5 (provided) ** 
                    
                    | JZlib |
                    | --- |
                    | **Description: **JZlib is a re-implementation of zlib in pure Java **URL: **[http://www.jcraft.com/jzlib/](http://www.jcraft.com/jzlib/)[44] **Project Licenses: **[BSD](http://www.jcraft.com/jzlib/LICENSE.txt)[45] |
                  - joda-time:joda-time:jar:2.14.0 (provided) ** 
                    
                    | Joda-Time |
                    | --- |
                    | **Description: **Date and time library to replace JDK date handling **URL: **[https://www.joda.org/joda-time/](https://www.joda.org/joda-time/)[36] **Project Licenses: **[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |
                  - me.qmx.jitescript:jitescript:jar:0.4.1 (provided) ** 
                    
                    | jitescript |
                    | --- |
                    | **Description: **Java API for Bytecode **URL: **[https://github.com/qmx/jitescript](https://github.com/qmx/jitescript)[37] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - com.headius:backport9:jar:1.13 (provided) ** 
                    
                    | backport9 |
                    | --- |
                    | **Description: **Sonatype helps open source projects to set up Maven repositories on https://oss.sonatype.org/ **URL: **[http://nexus.sonatype.org/oss-repository-hosting.html/backport9](http://nexus.sonatype.org/oss-repository-hosting.html/backport9)[33] **Project Licenses: **[The Apache Software License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
                  - org.crac:crac:jar:1.5.0 (provided) ** 
                    
                    | crac |
                    | --- |
                    | **Description: **A wrapper for OpenJDK CRaC API to build and run on any JDK **URL: **[https://github.com/crac/org.crac](https://github.com/crac/org.crac)[38] **Project Licenses: **[BSD-2-Clause](https://opensource.org/licenses/BSD-2-Clause)[39] |
          - org.jruby:jruby-stdlib:jar:10.0.3.0 (provided) ** 
            
            | JRuby Lib Setup |
            | --- |
            | **Description: **JRuby is the effort to recreate the Ruby (https://www.ruby-lang.org) interpreter in Java. **URL: **[https://github.com/jruby/jruby/jruby-stdlib](https://github.com/jruby/jruby/jruby-stdlib)[43] **Project Licenses: **[GPL-2.0](http://www.gnu.org/licenses/gpl-2.0-standalone.html)[11], [LGPL-2.1](http://www.gnu.org/licenses/lgpl-2.1-standalone.html)[12], [EPL-2.0](http://www.eclipse.org/legal/epl-v20.html)[13] |
    - org.yaml:snakeyaml:jar:2.2 (compile) ** 
      
      | SnakeYAML |
      | --- |
      | **Description: **YAML 1.1 parser and emitter for Java **URL: **[https://bitbucket.org/snakeyaml/snakeyaml](https://bitbucket.org/snakeyaml/snakeyaml)[1] **Project Licenses: **[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
    - org.slf4j:slf4j-api:jar:2.0.12 (provided) ** 
      
      | SLF4J API Module |
      | --- |
      | **Description: **The slf4j API **URL: **[http://www.slf4j.org](http://www.slf4j.org)[14] **Project Licenses: **[MIT License](http://www.opensource.org/licenses/mit-license.php)[15] |
    - org.junit.jupiter:junit-jupiter:jar:5.10.2 (test) ** 
      
      | JUnit Jupiter (Aggregator) |
      | --- |
      | **Description: **Module "junit-jupiter" of JUnit 5. **URL: **[https://junit.org/junit5/](https://junit.org/junit5/)[6] **Project Licenses: **[Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[7] |
      
          - org.junit.jupiter:junit-jupiter-api:jar:5.10.2 (test) ** 
            
            | JUnit Jupiter API |
            | --- |
            | **Description: **Module "junit-jupiter-api" of JUnit 5. **URL: **[https://junit.org/junit5/](https://junit.org/junit5/)[6] **Project Licenses: **[Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[7] |
            
                  - org.opentest4j:opentest4j:jar:1.3.0 (test) ** 
                    
                    | org.opentest4j:opentest4j |
                    | --- |
                    | **Description: **Open Test Alliance for the JVM **URL: **[https://github.com/ota4j-team/opentest4j](https://github.com/ota4j-team/opentest4j)[19] **Project Licenses: **[The Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |
                  - org.junit.platform:junit-platform-commons:jar:1.10.2 (test) ** 
                    
                    | JUnit Platform Commons |
                    | --- |
                    | **Description: **Module "junit-platform-commons" of JUnit 5. **URL: **[https://junit.org/junit5/](https://junit.org/junit5/)[6] **Project Licenses: **[Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[7] |
                  - org.apiguardian:apiguardian-api:jar:1.1.2 (test) ** 
                    
                    | org.apiguardian:apiguardian-api |
                    | --- |
                    | **Description: **@API Guardian **URL: **[https://github.com/apiguardian-team/apiguardian](https://github.com/apiguardian-team/apiguardian)[18] **Project Licenses: **[The Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)[2] |
          - org.junit.jupiter:junit-jupiter-params:jar:5.10.2 (test) ** 
            
            | JUnit Jupiter Params |
            | --- |
            | **Description: **Module "junit-jupiter-params" of JUnit 5. **URL: **[https://junit.org/junit5/](https://junit.org/junit5/)[6] **Project Licenses: **[Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[7] |
          - org.junit.jupiter:junit-jupiter-engine:jar:5.10.2 (test) ** 
            
            | JUnit Jupiter Engine |
            | --- |
            | **Description: **Module "junit-jupiter-engine" of JUnit 5. **URL: **[https://junit.org/junit5/](https://junit.org/junit5/)[6] **Project Licenses: **[Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[7] |
            
                  - org.junit.platform:junit-platform-engine:jar:1.10.2 (test) ** 
                    
                    | JUnit Platform Engine API |
                    | --- |
                    | **Description: **Module "junit-platform-engine" of JUnit 5. **URL: **[https://junit.org/junit5/](https://junit.org/junit5/)[6] **Project Licenses: **[Eclipse Public License v2.0](https://www.eclipse.org/legal/epl-v20.html)[7] |
    - org.apache.logging.log4j:log4j-slf4j2-impl:jar:3.0.0-beta2 (test) ** 
      
      | Apache Log4j SLF4J 2.0 Binding |
      | --- |
      | **Description: **The Apache Log4j SLF4J 2.0 API binding to Log4j 2 Core **URL: **[https://logging.apache.org/log4j/3.x/log4j/log4j-slf4j2-impl/](https://logging.apache.org/log4j/3.x/log4j/log4j-slf4j2-impl/)[5] **Project Licenses: **[Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |
      
          - org.apache.logging.log4j:log4j-api:jar:3.0.0-beta2 (test) ** 
            
            | Apache Log4j API |
            | --- |
            | **Description: **The Apache Log4j API **URL: **[https://logging.apache.org/log4j/3.x/log4j/log4j-api/](https://logging.apache.org/log4j/3.x/log4j/log4j-api/)[16] **Project Licenses: **[Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |
    - org.apache.logging.log4j:log4j-core:jar:3.0.0-beta2 (test) ** 
      
      | Apache Log4j Core |
      | --- |
      | **Description: **The Apache Log4j Implementation **URL: **[https://logging.apache.org/log4j/3.x/log4j/log4j-core/](https://logging.apache.org/log4j/3.x/log4j/log4j-core/)[3] **Project Licenses: **[Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |
      
          - org.apache.logging.log4j:log4j-plugins:jar:3.0.0-beta2 (test) ** 
            
            | Apache Log4j Plugins |
            | --- |
            | **Description: **Log4j Plugin Support **URL: **[https://logging.apache.org/log4j/3.x/log4j/log4j-plugins/](https://logging.apache.org/log4j/3.x/log4j/log4j-plugins/)[17] **Project Licenses: **[Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |
    - network.ike.tooling:ike-build-standards:zip:site-theme:146 (provided) ** 
      
      | IKE Build Standards |
      | --- |
      | **Description: **Versioned Claude instruction files for IKE projects. Modular standards (Maven, Java, IKE-specific) distributed as a classified Maven artifact. **URL: **[https://ike.network/ike-tooling/ike-build-standards/](https://ike.network/ike-tooling/ike-build-standards/)[8] **Project Licenses: **[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0.txt)[4] |

# Licenses

**EPL: **Dirgra

**LGPL-2.1: **JRuby Base, JRuby Lib Setup, JRuby Main Maven Artifact

**The Apache License, Version 2.0: **org.apiguardian:apiguardian-api, org.opentest4j:opentest4j

**BSD-3-Clause: **asm, asm-analysis, asm-commons, asm-tree, asm-util

**MIT License: **JCodings, Joni, SLF4J API Module, jnr-x86asm

**BSD-2-Clause: **crac

**GPL-2.0: **JRuby Base, JRuby Lib Setup, JRuby Main Maven Artifact

**Eclipse Public License v2.0: **JUnit Jupiter (Aggregator), JUnit Jupiter API, JUnit Jupiter Engine, JUnit Jupiter Params, JUnit Platform Commons, JUnit Platform Engine API

**BSD: **JZlib

**Apache License, Version 2.0: **IKE Build Standards, Joda-Time, Koncept AsciiDoc Extension, SnakeYAML

**GNU Lesser General Public License Version 2.1: **jnr-posix

**Apache-2.0: **Apache Log4j API, Apache Log4j Core, Apache Log4j Plugins, Apache Log4j SLF4J 2.0 Binding

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
| ike-build-standards-146-site-theme.zip | 3.4 kB | - | - | - | - | - |
| log4j-api-3.0.0-beta2.jar | 354.8 kB | 241 | 213 | 11 | 17 | Yes |
| log4j-core-3.0.0-beta2.jar | 1.5 MB | 976 | 907 | 44 | 17 | Yes |
| log4j-plugins-3.0.0-beta2.jar | 205.9 kB | 186 | 156 | 14 | 17 | Yes |
| log4j-slf4j2-impl-3.0.0-beta2.jar | 26.5 kB | 27 | 11 | 2 | 17 | Yes |
| apiguardian-api-1.1.2.jar | 6.8 kB | 9 | 3 | 2 | 1.6 | Yes |
| asciidoctorj-3.0.1.jar | 1.9 MB | 1255 | 142 | 11 | 11 | Yes |
| asciidoctorj-api-3.0.1.jar | 60.3 kB | 91 | 82 | 6 | 11 | Yes |
| crac-1.5.0.jar | 13.4 kB | 24 | 14 | 3 | 1.8 | Yes |
| dirgra-0.5.jar | 17 kB | 21 | 11 | 2 | 1.8 | Yes |
| jruby-10.0.3.0.jar | 26.2 kB | 12 | 0 | 0 | - | - |
| jruby-base-10.0.3.0.jar | 9.4 MB | 6530 | 6346 | 115 | 21 | Yes |
| jruby-stdlib-10.0.3.0.jar | 19 MB | 3052 | 0 | 0 | - | - |
| jzlib-1.1.5.jar | 74.9 kB | 36 | 26 | 1 | 1.7 | Yes |
| jcodings-1.0.63.jar | 1.8 MB | 862 | 166 | 11 | 1.8 | Yes |
| joni-2.2.6.jar | 232.4 kB | 121 | 107 | 7 | 1.8 | Yes |
| junit-jupiter-5.10.2.jar | 6.4 kB | 5 | 1 | 1 | 9 | No |
| junit-jupiter-api-5.10.2.jar | 211 kB | 197 | 182 | 8 | 1.8 | Yes |
| junit-jupiter-engine-5.10.2.jar | 244.7 kB | 147 | 130 | 9 | 1.8 | Yes |
| junit-jupiter-params-5.10.2.jar | 586 kB | 381 | 347 | 22 | 1.8 | Yes |
| junit-platform-commons-1.10.2.jar | 106.2 kB | 64 | - | - | - | - |
|    • Root | - | 56 | 43 | 7 | 1.8 | Yes |
|    • Versioned | - | 8 | 2 | 1 | 9 | Yes |
| junit-platform-engine-1.10.2.jar | 204.8 kB | 153 | 136 | 10 | 1.8 | Yes |
| opentest4j-1.3.0.jar | 14.3 kB | 15 | 9 | 2 | 1.6 | Yes |
| asm-9.7.1.jar | 126.1 kB | 45 | 39 | 3 | 1.5 | Yes |
| asm-analysis-9.7.1.jar | 35.1 kB | 22 | 15 | 2 | 1.5 | Yes |
| asm-commons-9.7.1.jar | 73.5 kB | 34 | 28 | 2 | 1.5 | Yes |
| asm-tree-9.7.1.jar | 51.9 kB | 45 | 39 | 2 | 1.5 | Yes |
| asm-util-9.7.1.jar | 94.5 kB | 33 | 27 | 2 | 1.5 | Yes |
| slf4j-api-2.0.12.jar | 68.1 kB | 70 | - | - | - | - |
|    • Root | - | 68 | 54 | 4 | 1.8 | Yes |
|    • Versioned | - | 2 | 1 | 1 | 9 | No |
| snakeyaml-2.2.jar | 334.4 kB | 278 | - | - | - | - |
|    • Root | - | 270 | 229 | 23 | 1.7 | Yes |
|    • Versioned | - | 8 | 3 | 2 | 9 | Yes |
| 45 | 41.7 MB | 18349 | 12086 | 427 | 21 | 40 |
| compile: 1 | compile: 334.4 kB | compile: 278 | compile: 229 | compile: 23 | 21 | compile: 1 |
| provided: 32 | provided: 37.9 MB | provided: 15670 | provided: 9719 | provided: 272 | provided: 28 |
| test: 12 | test: 3.4 MB | test: 2401 | test: 2138 | test: 132 | 17 | test: 11 |
