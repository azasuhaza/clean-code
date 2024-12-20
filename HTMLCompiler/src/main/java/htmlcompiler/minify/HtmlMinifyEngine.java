package htmlcompiler.minify;

import htmlcompiler.utils.HTML;

public enum HtmlMinifyEngine {

    hazendaz;

    public Minifier toMinifier() {
        return switch (this) {
            case hazendaz -> HTML.HTML_COMPRESSOR::compress;
        };
    }

}
