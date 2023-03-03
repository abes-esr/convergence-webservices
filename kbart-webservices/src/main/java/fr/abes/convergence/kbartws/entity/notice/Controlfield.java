package fr.abes.convergence.kbartws.entity.notice;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Controlfield {

    @JacksonXmlProperty(isAttribute = true)
    private String tag;

    @JacksonXmlText(value = true)
    private String value;
}
