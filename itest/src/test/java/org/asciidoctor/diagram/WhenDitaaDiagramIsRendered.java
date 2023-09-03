package org.asciidoctor.diagram;

import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Options;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


class WhenDitaaDiagramIsRendered {

    static final String ASCIIDOCTOR_DIAGRAM = "asciidoctor-diagram";

    @Test
    void should_render_ditaa_diagram_to_PDF() {

        final Asciidoctor asciidoctor = Asciidoctor.Factory.create();

        final String destinationFileName = "build/test.pdf";
        final String imageFileName = UUID.randomUUID().toString();
        final String document = new StringBuilder()
                .append("= Document Title\n")
                .append("\n")
                .append("Hello World\n")
                .append("[ditaa," + imageFileName + "]\n")
                .append("....\n")
                .append("+---+\n")
                .append("| A |\n")
                .append("+---+\n")
                .append("....\n")
                .append("\n")
                .toString();

        asciidoctor.requireLibrary(ASCIIDOCTOR_DIAGRAM);
        asciidoctor.convert(document, Options.builder()
                .toFile(new File(destinationFileName))
                .backend("pdf")
                .build());

        assertThat(new File(destinationFileName)).exists();
        File png = new File("build", imageFileName + ".png");
        assertThat(png).exists();
        File pngCache = new File("build/.asciidoctor/diagram/", imageFileName + ".png.cache");
        assertThat(pngCache).exists();
    }
}
