# frozen_string_literal: true

#
# SVG Fix Postprocessor for AsciidoctorJ
# ────────────────────────────────────────────────────────────────
# Fixes known Mermaid SVG rendering bugs in ALL backends.
#
# Applies two complementary fixes:
#
#   1. HTML output string: gsub on the final document output
#      (handles inline SVGs in HTML5 and DocBook backends)
#
#   2. SVG files on disk: patches all .svg files in the output
#      directory (handles DocBook/XEP pipeline and standalone
#      SVG viewing in browsers)
#
# Currently fixes:
#   - ER diagram edge paths with style="undefined;undefined stroke:..."
#     Mermaid's ER renderer emits undefined CSS property prefixes that
#     cause compliant CSS parsers to skip the stroke declaration,
#     making relationship lines invisible.
#
# Copyright 2025 IKE Community — Apache License 2.0
#

require 'asciidoctor/extensions'

class SvgFixPostprocessor < Asciidoctor::Extensions::Postprocessor
  UNDEFINED_PATTERN = /undefined;undefined\s*/
  UNDEFINED_LEADING = /^undefined[;\s]*/

  def process(document, output)
    # ── Fix 1: Inline SVGs in the document output string ──────────
    output = fix_undefined_styles(output)

    # ── Fix 2: SVG files on disk ──────────────────────────────────
    # The output directory may contain standalone .svg files generated
    # by asciidoctor-diagram (e.g., DocBook backend writes SVGs as
    # separate files referenced by <mediaobject>).
    fix_svg_files_in(document)

    output
  end

  private

  def fix_undefined_styles(text)
    text.gsub(UNDEFINED_PATTERN, '')
  end

  def fix_svg_files_in(document)
    # Try multiple ways to find the output directory —
    # the attribute name varies by backend and configuration.
    outdir = document.attr('outdir') \
          || document.attr('to_dir') \
          || (document.options[:to_dir] if document.options) \
          || nil

    return unless outdir && Dir.exist?(outdir)

    Dir.glob(File.join(outdir, '**', '*.svg')).each do |svg_path|
      content = File.read(svg_path, encoding: 'UTF-8')
      next unless content.include?('undefined;undefined')

      fixed = content
        .gsub(UNDEFINED_PATTERN, '')  # "undefined;undefined stroke:" → "stroke:"
        .gsub(/style="#{UNDEFINED_LEADING.source}/, 'style="')  # clean any residual at start

      File.write(svg_path, fixed, encoding: 'UTF-8')
    end
  rescue => e
    # Don't fail the build if SVG patching fails —
    # the CSS !important fallback in docinfo.html still works for HTML.
    warn "svg-fix-postprocessor: could not patch SVG files: #{e.message}"
  end
end

Asciidoctor::Extensions.register do
  postprocessor SvgFixPostprocessor
end
