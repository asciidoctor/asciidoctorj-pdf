package org.asciidoctor;

import org.asciidoctor.internal.JRubyRuntimeContext;
import org.junit.Test;

public class WhenRougeGemIsRequired {

    @Test
    public void itShouldNotFail() {

        Asciidoctor asciidoctor = Asciidoctor.Factory.create((String) null);
        JRubyRuntimeContext.get().evalScriptlet("require 'rouge'");

    }

}
