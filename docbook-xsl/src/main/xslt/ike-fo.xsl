<?xml version="1.0" encoding="UTF-8"?>
<!--
  IKE Community — DocBook XSL-FO Customization Layer
  ══════════════════════════════════════════════════════════════════

  Imports the stock DocBook XSL-NS FO stylesheet and overrides parameters
  to match the IKE visual identity (Noto fonts, IKE color palette, Letter
  paper, professional headers/footers).

  Packaged in the ike-docbook-xsl Maven artifact alongside the stock
  DocBook XSL 1.79.2 stylesheets.  When the JAR is unpacked, this file
  lives in custom/ike-fo.xsl and the stock FO stylesheet is at
  fo/docbook.xsl — the relative import resolves correctly.

  Reference:  https://tdg.docbook.org/tdg/5.2/
  Parameters: http://docbook.sourceforge.net/release/xsl/current/doc/fo/

  Copyright 2025 IKE Community — Apache License 2.0
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:d="http://docbook.org/ns/docbook"
                version="1.0">

  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- Import stock DocBook XSL-NS FO stylesheet.                     -->
  <!--                                                                 -->
  <!-- When the ike-docbook-xsl JAR is unpacked, this file lives at:  -->
  <!--   {unpack-dir}/custom/ike-fo.xsl                               -->
  <!--                                                                 -->
  <!-- The stock FO stylesheet lives at:                               -->
  <!--   {unpack-dir}/fo/docbook.xsl                                  -->
  <!--                                                                 -->
  <!-- So the relative import ../fo/docbook.xsl resolves correctly.    -->
  <!-- No XML catalog needed.                                          -->
  <!-- ═══════════════════════════════════════════════════════════════ -->
  <xsl:import href="../fo/docbook.xsl"/>

  <!-- Suppress stock "Making portrait pages on USletter paper" message.
       The stock template writes an xsl:message to stderr, which Maven
       flags as [WARNING] [stderr].  Override with empty template. -->
  <xsl:template name="root.messages"/>


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- IKE PDF RENDERER PARAMETER                                      -->
  <!-- Passed from Maven xml-maven-plugin as an XSLT parameter.        -->
  <!-- Used for title page annotation and colophon additive content.    -->
  <!-- ═══════════════════════════════════════════════════════════════ -->
  <xsl:param name="ike.pdf.renderer" select="''"/>


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- PAGE GEOMETRY                                                   -->
  <!-- Matches ike-default-theme.yml:                                  -->
  <!--   page size: Letter portrait                                    -->
  <!--   margins:   [0.75in, 1in, 0.75in, 1in]                        -->
  <!-- ═══════════════════════════════════════════════════════════════ -->
  <xsl:param name="paper.type">USletter</xsl:param>
  <xsl:param name="page.width">8.5in</xsl:param>
  <xsl:param name="page.height">11in</xsl:param>
  <xsl:param name="page.margin.inner">1in</xsl:param>
  <xsl:param name="page.margin.outer">1in</xsl:param>
  <xsl:param name="page.margin.top">0.75in</xsl:param>
  <xsl:param name="page.margin.bottom">0.75in</xsl:param>
  <xsl:param name="double.sided">1</xsl:param>

  <!-- Body region margins (space for header/footer content) -->
  <xsl:param name="region.before.extent">0.4in</xsl:param>
  <xsl:param name="region.after.extent">0.4in</xsl:param>
  <xsl:param name="body.margin.top">0.5in</xsl:param>
  <xsl:param name="body.margin.bottom">0.5in</xsl:param>


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- TYPOGRAPHY                                                      -->
  <!-- NotoSerif for body, NotoSans for titles, NotoSansMono for code  -->
  <!-- Font-family names must match xep.xml font registration.         -->
  <!-- ═══════════════════════════════════════════════════════════════ -->
  <xsl:param name="body.font.family">NotoSerif</xsl:param>
  <xsl:param name="title.font.family">NotoSans</xsl:param>
  <xsl:param name="monospace.font.family">NotoSansMono</xsl:param>
  <xsl:param name="sans.font.family">NotoSans</xsl:param>
  <xsl:param name="symbol.font.family">NotoSansSymbols,NotoSansSymbols2,NotoSansMath</xsl:param>

  <!-- Base font size: 10.5pt body (matches YML) -->
  <xsl:param name="body.font.master">10.5</xsl:param>
  <xsl:param name="body.font.size">
    <xsl:value-of select="$body.font.master"/><xsl:text>pt</xsl:text>
  </xsl:param>

  <!-- Footnote size -->
  <xsl:param name="footnote.font.size">
    <xsl:value-of select="$body.font.master * 0.8"/><xsl:text>pt</xsl:text>
  </xsl:param>

  <!-- Line height (YML: line-height-length 14 / font-size 10.5 ≈ 1.33) -->
  <xsl:param name="line-height">1.33</xsl:param>


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- HEADING SIZES                                                   -->
  <!-- YML: h1=24, h2=18, h3=14, h4=12, h5=11, h6=10                 -->
  <!-- DocBook XSL uses scaling from body.font.master.                 -->
  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- The title size parameters use a hierarchical scaling approach.   -->
  <!-- We override the section.title attribute-sets below for precise   -->
  <!-- control matching the YML pixel-perfect sizes.                    -->


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- SECTION NUMBERING & TOC                                         -->
  <!-- ═══════════════════════════════════════════════════════════════ -->
  <xsl:param name="section.autolabel">1</xsl:param>
  <xsl:param name="section.autolabel.max.depth">3</xsl:param>
  <xsl:param name="section.label.includes.component.label">1</xsl:param>

  <xsl:param name="generate.toc">
    book      toc,title
    article   toc,title
  </xsl:param>
  <xsl:param name="toc.indent.width">18</xsl:param>
  <xsl:param name="toc.section.depth">3</xsl:param>


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- CODE BLOCKS                                                     -->
  <!-- YML: font-size 9, background #f5f5f5, border #e3e3e3           -->
  <!-- ═══════════════════════════════════════════════════════════════ -->
  <xsl:param name="shade.verbatim">1</xsl:param>

  <xsl:attribute-set name="shade.verbatim.style">
    <xsl:attribute name="background-color">#f5f5f5</xsl:attribute>
    <xsl:attribute name="border-style">solid</xsl:attribute>
    <xsl:attribute name="border-width">0.5pt</xsl:attribute>
    <xsl:attribute name="border-color">#e3e3e3</xsl:attribute>
    <xsl:attribute name="padding">4pt</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="monospace.verbatim.properties">
    <xsl:attribute name="font-size">9pt</xsl:attribute>
    <xsl:attribute name="line-height">1.3</xsl:attribute>
    <xsl:attribute name="wrap-option">wrap</xsl:attribute>
  </xsl:attribute-set>


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- COLORS                                                          -->
  <!-- Body text: #333333   Headings: #2c3e50   Links: #428bca         -->
  <!-- ═══════════════════════════════════════════════════════════════ -->

  <!-- Body text color -->
  <xsl:attribute-set name="root.properties">
    <xsl:attribute name="color">#333333</xsl:attribute>
  </xsl:attribute-set>

  <!-- Heading color — applied to all section title levels -->
  <xsl:attribute-set name="section.title.properties">
    <xsl:attribute name="color">#2c3e50</xsl:attribute>
    <xsl:attribute name="font-family">
      <xsl:value-of select="$title.font.family"/>
    </xsl:attribute>
    <xsl:attribute name="keep-with-next.within-column">always</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="component.title.properties">
    <xsl:attribute name="color">#2c3e50</xsl:attribute>
  </xsl:attribute-set>

  <!-- Per-level heading sizes (matching YML exactly) -->
  <xsl:attribute-set name="section.title.level1.properties">
    <xsl:attribute name="font-size">24pt</xsl:attribute>
    <xsl:attribute name="space-before.minimum">14pt</xsl:attribute>
    <xsl:attribute name="space-before.optimum">18pt</xsl:attribute>
    <xsl:attribute name="space-before.maximum">24pt</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="section.title.level2.properties">
    <xsl:attribute name="font-size">18pt</xsl:attribute>
    <xsl:attribute name="space-before.minimum">12pt</xsl:attribute>
    <xsl:attribute name="space-before.optimum">14pt</xsl:attribute>
    <xsl:attribute name="space-before.maximum">18pt</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="section.title.level3.properties">
    <xsl:attribute name="font-size">14pt</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="section.title.level4.properties">
    <xsl:attribute name="font-size">12pt</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="section.title.level5.properties">
    <xsl:attribute name="font-size">11pt</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="section.title.level6.properties">
    <xsl:attribute name="font-size">10pt</xsl:attribute>
  </xsl:attribute-set>

  <!-- Link color -->
  <xsl:attribute-set name="xref.properties">
    <xsl:attribute name="color">#428bca</xsl:attribute>
    <xsl:attribute name="text-decoration">none</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="simple.xlink.properties">
    <xsl:attribute name="color">#428bca</xsl:attribute>
    <xsl:attribute name="text-decoration">none</xsl:attribute>
  </xsl:attribute-set>


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- TABLE STYLING                                                   -->
  <!-- YML: border #dddddd, head background #f5f5f5, stripe #f9f9f9   -->
  <!-- ═══════════════════════════════════════════════════════════════ -->
  <xsl:param name="table.frame.border.color">#dddddd</xsl:param>
  <xsl:param name="table.frame.border.thickness">0.5pt</xsl:param>
  <xsl:param name="table.cell.border.color">#dddddd</xsl:param>
  <xsl:param name="table.cell.border.thickness">0.5pt</xsl:param>

  <xsl:attribute-set name="table.properties">
    <xsl:attribute name="border-collapse">collapse</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="table.cell.padding">
    <xsl:attribute name="padding-start">3pt</xsl:attribute>
    <xsl:attribute name="padding-end">3pt</xsl:attribute>
    <xsl:attribute name="padding-top">2pt</xsl:attribute>
    <xsl:attribute name="padding-bottom">2pt</xsl:attribute>
  </xsl:attribute-set>


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- HEADERS & FOOTERS                                               -->
  <!-- YML: chapter-title in header, page-number and doc-title in      -->
  <!-- footer, mirrored for recto/verso.                                -->
  <!-- ═══════════════════════════════════════════════════════════════ -->
  <xsl:param name="header.rule">0</xsl:param>
  <xsl:param name="footer.rule">1</xsl:param>

  <!-- Column widths: left center right -->
  <xsl:param name="header.column.widths">1 3 1</xsl:param>
  <xsl:param name="footer.column.widths">1 3 1</xsl:param>

  <xsl:attribute-set name="header.content.properties">
    <xsl:attribute name="font-family">
      <xsl:value-of select="$sans.font.family"/>
    </xsl:attribute>
    <xsl:attribute name="font-size">9pt</xsl:attribute>
    <xsl:attribute name="color">#7f8c8d</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="footer.content.properties">
    <xsl:attribute name="font-family">
      <xsl:value-of select="$sans.font.family"/>
    </xsl:attribute>
    <xsl:attribute name="font-size">9pt</xsl:attribute>
    <xsl:attribute name="color">#333333</xsl:attribute>
  </xsl:attribute-set>


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- TITLE PAGE                                                      -->
  <!-- YML: centered, title at 40% from top, 36pt                      -->
  <!-- ═══════════════════════════════════════════════════════════════ -->
  <xsl:attribute-set name="book.titlepage.recto.style">
    <xsl:attribute name="text-align">center</xsl:attribute>
  </xsl:attribute-set>

  <!-- Override the book title template for precise positioning -->
  <xsl:template name="book.titlepage.recto">
    <fo:block space-before="3.5in" text-align="center">
      <fo:block font-size="36pt" font-family="{$title.font.family}"
                font-weight="bold" color="#2c3e50" line-height="1.2">
        <xsl:value-of select="/d:book/d:info/d:title
                              | /d:book/d:title
                              | /d:article/d:info/d:title
                              | /d:article/d:title"/>
      </fo:block>
      <xsl:if test="/d:book/d:info/d:subtitle | /d:article/d:info/d:subtitle">
        <fo:block font-size="18pt" font-family="{$title.font.family}"
                  color="#7f8c8d" space-before="12pt" line-height="1.4">
          <xsl:value-of select="/d:book/d:info/d:subtitle
                                | /d:article/d:info/d:subtitle"/>
        </fo:block>
      </xsl:if>
      <xsl:if test="/d:book/d:info/d:author | /d:article/d:info/d:author">
        <fo:block font-size="13pt" color="#333333" space-before="48pt">
          <xsl:for-each select="/d:book/d:info/d:author | /d:article/d:info/d:author">
            <xsl:if test="position() &gt; 1">, </xsl:if>
            <xsl:value-of select="d:personname/d:firstname"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="d:personname/d:surname"/>
          </xsl:for-each>
        </fo:block>
      </xsl:if>
      <xsl:if test="/d:book/d:info/d:revhistory/d:revision
                    | /d:article/d:info/d:revhistory/d:revision">
        <fo:block font-size="11pt" color="#7f8c8d" space-before="18pt">
          <xsl:text>Version </xsl:text>
          <xsl:value-of select="(/d:book/d:info/d:revhistory/d:revision[1]/d:revnumber
                                 | /d:article/d:info/d:revhistory/d:revision[1]/d:revnumber)[1]"/>
          <xsl:if test="(/d:book/d:info/d:revhistory/d:revision[1]/d:date
                         | /d:article/d:info/d:revhistory/d:revision[1]/d:date)[1]">
            <xsl:text> — </xsl:text>
            <xsl:value-of select="(/d:book/d:info/d:revhistory/d:revision[1]/d:date
                                   | /d:article/d:info/d:revhistory/d:revision[1]/d:date)[1]"/>
          </xsl:if>
        </fo:block>
      </xsl:if>
      <xsl:if test="$ike.pdf.renderer != ''">
        <fo:block font-size="9pt" color="#999999" space-before="24pt"
                  font-family="{$sans.font.family}">
          <xsl:text>Rendered with: </xsl:text>
          <xsl:choose>
            <xsl:when test="$ike.pdf.renderer = 'fop'">Apache FOP</xsl:when>
            <xsl:when test="$ike.pdf.renderer = 'xep'">RenderX XEP</xsl:when>
            <xsl:otherwise><xsl:value-of select="$ike.pdf.renderer"/></xsl:otherwise>
          </xsl:choose>
        </fo:block>
      </xsl:if>
    </fo:block>
  </xsl:template>

  <!-- Suppress verso (back of title page) -->
  <xsl:template name="book.titlepage.verso"/>


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- ADMONITIONS (NOTE, WARNING, TIP, CAUTION, IMPORTANT)           -->
  <!-- ═══════════════════════════════════════════════════════════════ -->
  <xsl:param name="admon.textlabel">1</xsl:param>
  <xsl:param name="admon.graphics">0</xsl:param>

  <xsl:attribute-set name="admonition.title.properties">
    <xsl:attribute name="font-family">
      <xsl:value-of select="$sans.font.family"/>
    </xsl:attribute>
    <xsl:attribute name="font-weight">bold</xsl:attribute>
    <xsl:attribute name="text-transform">uppercase</xsl:attribute>
    <xsl:attribute name="font-size">10pt</xsl:attribute>
  </xsl:attribute-set>

  <xsl:attribute-set name="admonition.properties">
    <xsl:attribute name="border-start-style">solid</xsl:attribute>
    <xsl:attribute name="border-start-width">3pt</xsl:attribute>
    <xsl:attribute name="border-start-color">#eeeeee</xsl:attribute>
    <xsl:attribute name="padding-start">12pt</xsl:attribute>
    <xsl:attribute name="keep-together.within-column">always</xsl:attribute>
  </xsl:attribute-set>


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- BLOCKQUOTES                                                     -->
  <!-- YML: color #555555, 10pt, border-left 5pt #eeeeee              -->
  <!-- ═══════════════════════════════════════════════════════════════ -->
  <xsl:attribute-set name="blockquote.properties">
    <xsl:attribute name="color">#555555</xsl:attribute>
    <xsl:attribute name="font-size">10pt</xsl:attribute>
    <xsl:attribute name="border-start-style">solid</xsl:attribute>
    <xsl:attribute name="border-start-width">5pt</xsl:attribute>
    <xsl:attribute name="border-start-color">#eeeeee</xsl:attribute>
    <xsl:attribute name="padding-start">12pt</xsl:attribute>
    <xsl:attribute name="margin-start">0pt</xsl:attribute>
    <xsl:attribute name="margin-end">0pt</xsl:attribute>
  </xsl:attribute-set>


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- IMAGES & FIGURES                                                -->
  <!-- Allow SVG diagrams from Kroki to scale to page width.           -->
  <!-- Use absolute body width (page.width minus margins) instead of   -->
  <!-- 100% because FOP does not support percentage units on           -->
  <!-- fo:external-graphic width.                                      -->
  <!-- ═══════════════════════════════════════════════════════════════ -->
  <xsl:param name="default.image.width">6.5in</xsl:param>
  <xsl:param name="ignore.image.scaling">0</xsl:param>


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- MISCELLANEOUS                                                   -->
  <!-- ═══════════════════════════════════════════════════════════════ -->

  <!-- Widow/orphan control -->
  <xsl:param name="widows">3</xsl:param>
  <xsl:param name="orphans">3</xsl:param>

  <!-- Hyphenation -->
  <xsl:param name="hyphenate">true</xsl:param>

  <!-- Draft mode off -->
  <xsl:param name="draft.mode">no</xsl:param>

  <!-- Inline code styling (programlisting, literal) -->
  <xsl:attribute-set name="monospace.properties">
    <xsl:attribute name="font-size">9pt</xsl:attribute>
    <xsl:attribute name="color">#b12146</xsl:attribute>
  </xsl:attribute-set>

  <!-- TOC styling -->
  <xsl:attribute-set name="toc.line.properties">
    <xsl:attribute name="font-family">
      <xsl:value-of select="$sans.font.family"/>
    </xsl:attribute>
    <xsl:attribute name="font-size">10pt</xsl:attribute>
    <xsl:attribute name="line-height">1.4</xsl:attribute>
  </xsl:attribute-set>

  <!-- Chapter/section break: always start on a new page -->
  <xsl:attribute-set name="component.titlepage.properties">
    <xsl:attribute name="break-before">page</xsl:attribute>
  </xsl:attribute-set>

  <!-- Koncept phrase styling (from koncept-asciidoc-extension DocBook backend) -->
  <xsl:template match="d:phrase[@role='koncept']">
    <fo:inline font-weight="bold" color="#2a5a8a">
      <xsl:apply-templates/>
    </fo:inline>
  </xsl:template>


  <!-- ═══════════════════════════════════════════════════════════════ -->
  <!-- COLOPHON: Additive FO processor identity                        -->
  <!-- The KonceptGlossaryProcessor Postprocessor emits a placeholder  -->
  <!-- @@IKE_FO_PROCESSOR@@ in the colophon table.  This template      -->
  <!-- replaces it with the actual FO processor name at XSLT time.     -->
  <!-- ═══════════════════════════════════════════════════════════════ -->
  <xsl:template match="d:entry[normalize-space(text())='@@IKE_FO_PROCESSOR@@']">
    <fo:table-cell xsl:use-attribute-sets="table.cell.padding">
      <fo:block>
        <xsl:choose>
          <xsl:when test="$ike.pdf.renderer = 'fop'">Apache FOP 2.11</xsl:when>
          <xsl:when test="$ike.pdf.renderer = 'xep'">RenderX XEP</xsl:when>
          <xsl:otherwise><xsl:value-of select="$ike.pdf.renderer"/></xsl:otherwise>
        </xsl:choose>
      </fo:block>
    </fo:table-cell>
  </xsl:template>

</xsl:stylesheet>
