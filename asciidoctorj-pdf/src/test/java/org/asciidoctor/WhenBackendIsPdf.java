package org.asciidoctor;

import org.asciidoctor.util.RougeColors;
import org.asciidoctor.util.pdf.ColorsProcessor;
import org.asciidoctor.util.pdf.Image;
import org.asciidoctor.util.pdf.ImageProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.asciidoctor.util.RougeColors.DARK_CHARCOAL;
import static org.assertj.core.api.Assertions.assertThat;


class WhenBackendIsPdf {

    private Asciidoctor asciidoctor;


    @BeforeEach
    void initAsciidoctor() {
        this.asciidoctor = Asciidoctor.Factory.create();
    }

    @Test
    void pdf_should_include_images() throws IOException {
        String filename = "image-sample";
        File inputFile = new File("build/resources/test/" + filename + ".adoc");
        File outputFile1 = new File(inputFile.getParentFile(), filename + ".pdf");
        removeFileIfItExists(outputFile1);

        asciidoctor.convertFile(inputFile, Options.builder().backend("pdf").safe(SafeMode.UNSAFE).build());

        assertThat(outputFile1).exists();
        ImageProcessor imageProcessor = new ImageProcessor();
        imageProcessor.parse(outputFile1.getAbsolutePath());
        List<Image> images = imageProcessor.getImages();
        assertThat(images).hasSize(2);
    }

    @Test
    void pdf_source_code_should_be_highlighted() throws IOException {
        String filename = "code-sample";
        File inputFile = new File("build/resources/test/" + filename + ".adoc");
        File outputFile1 = new File(inputFile.getParentFile(), filename + ".pdf");
        removeFileIfItExists(outputFile1);

        asciidoctor.convertFile(inputFile, Options.builder().backend("pdf").safe(SafeMode.UNSAFE).build());

        assertThat(outputFile1).exists();

        ColorsProcessor colorsProcessor = new ColorsProcessor("program", "System.out.println", "printHello", "HelloWorld", "<body>", "else", "Math.sqrt");
        colorsProcessor.parse(outputFile1.getAbsolutePath());
        Map<String, List<Color>> colors = colorsProcessor.getColors();

        assertThat(colors.get("program").get(0)).isEqualTo(RougeColors.GREY);
        assertThat(colors.get("System.out.println").get(0)).isEqualTo(RougeColors.LIGHT_BLUE);
        assertThat(colors.get("printHello").get(0)).isEqualTo(RougeColors.DARK_BLUE);
        assertThat(colors.get("HelloWorld").get(0)).isEqualTo(RougeColors.PINK);
        assertThat(colors.get("<body>").get(0)).isEqualTo(RougeColors.PINK);
        assertThat(colors.get("else").get(0)).isEqualTo(RougeColors.GREEN);
        assertThat(colors.get("Math.sqrt").get(0)).isEqualTo(RougeColors.LIGHT_BLUE);
    }

    @Test
    void pdf_text_should_be_hyphenated_german() throws IOException {
        String filename = "hyphenation-de-sample";
        File inputFile = new File("build/resources/test/" + filename + ".adoc");
        File outputFile1 = new File(inputFile.getParentFile(), filename + ".pdf");
        removeFileIfItExists(outputFile1);

        asciidoctor.convertFile(inputFile, Options.builder().backend("pdf").safe(SafeMode.UNSAFE).build());

        assertThat(outputFile1).exists();

        ColorsProcessor colorsProcessor = new ColorsProcessor("Feh\u00adler");
        colorsProcessor.parse(outputFile1.getAbsolutePath());
        assertThat(colorsProcessor.getColors()).containsEntry("Feh\u00adler", Arrays.asList(DARK_CHARCOAL));
    }

    @Test
    void pdf_text_should_be_hyphenated_english() throws IOException {
        String filename = "hyphenation-en-sample";
        File inputFile = new File("build/resources/test/" + filename + ".adoc");
        File outputFile1 = new File(inputFile.getParentFile(), filename + ".pdf");
        removeFileIfItExists(outputFile1);

        asciidoctor.convertFile(inputFile, Options.builder().backend("pdf").safe(SafeMode.UNSAFE).build());

        assertThat(outputFile1).exists();

        ColorsProcessor colorsProcessor = new ColorsProcessor("van\u00adquish");
        colorsProcessor.parse(outputFile1.getAbsolutePath());
        assertThat(colorsProcessor.getColors()).containsKey("van\u00adquish");
    }

    private void removeFileIfItExists(File file) throws IOException {
        if (file.exists() && !file.delete()) {
            throw new IOException("Can't delete file: " + file.getAbsolutePath());
        }
    }
}
