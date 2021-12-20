package org.asciidoctor.diagram

import org.asciidoctor.Asciidoctor
import org.asciidoctor.Options
import org.asciidoctor.OptionsBuilder
import spock.lang.Specification

class WhenDitaaDiagramIsRendered extends Specification {

    static final String ASCIIDOCTOR_DIAGRAM = 'asciidoctor-diagram'

    def 'should render ditaa diagram to PDF'() throws Exception {

        given:
        Asciidoctor asciidoctor = Asciidoctor.Factory.create()
        String destinationFileName = 'build/test.pdf'
        String imageFileName = UUID.randomUUID()

        String document = """= Document Title

Hello World

[ditaa,${imageFileName}]
....

+---+
| A |
+---+
....

"""

        asciidoctor.requireLibrary(ASCIIDOCTOR_DIAGRAM)

        when:
        asciidoctor.convert(document, Options.builder()
                .toFile(new File(destinationFileName))
                .backend('pdf')
                .build())

        then:
        new File(destinationFileName).exists()
        File png = new File("build/${imageFileName}.png")
        File pngCache = new File("build/.asciidoctor/diagram/${imageFileName}.png.cache")
        png.exists()
        pngCache.exists()

        //cleanup:
        //png.delete()
        //pngCache.delete()

    }

}
