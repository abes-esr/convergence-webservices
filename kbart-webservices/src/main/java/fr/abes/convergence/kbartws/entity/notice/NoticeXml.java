package fr.abes.convergence.kbartws.entity.notice;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import fr.abes.convergence.kbartws.utils.TYPE_SUPPORT;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Représente une notice au format d'export UnimarcXML
 */
@NoArgsConstructor
@Getter
@Setter
@JacksonXmlRootElement(localName = "record")
public class NoticeXml {

    @JacksonXmlProperty(localName = "leader")
    private String leader;

    @JacksonXmlProperty(localName = "controlfield")
    private List<Controlfield> controlfields = new ArrayList<>();

    @JacksonXmlProperty(localName = "datafield")
    private List<Datafield> datafields = new ArrayList<>();

    @Override
    public String toString() {
        return "Notice {leader=" + leader + ", ppn=" + getPpn() + "}";
    }


    /**
     * Indique si la notice est en état supprimée
     *
     * @return
     */
    public boolean isDeleted() {
        return leader.charAt(5) == 'd';
    }

    public boolean isNoticeElectronique() {
        return getTypeDocument().startsWith("O");
    }

    public boolean isNoticeImprimee() {
        return getTypeDocument().startsWith("A");
    }

    /**
     * Retourne le type de document de la notice en se basant sur la zone 008
     *
     * @return les x caractères du code correspondant au type de document
     */
    public String getTypeDocument() {
        Optional<Controlfield> typeDocument = controlfields.stream().filter(cf -> cf.getTag().equals("008")).findFirst();
        return typeDocument.map(Controlfield::getValue).orElse(null);
    }

    /**
     * Retourne le type de support de la notice en se basant sur le premier caractère de la 008
     *
     * @return le type de support sous forme d'enum
     */
    public TYPE_SUPPORT getTypeSupport() {
        return switch (getTypeDocument().substring(0, 1)) {
            case "A" -> TYPE_SUPPORT.IMPRIME;
            case "O" -> TYPE_SUPPORT.ELECTRONIQUE;
            default -> TYPE_SUPPORT.AUTRE;
        };
    }

    public String getPpn() {
        Optional<Controlfield> ppn = controlfields.stream().filter(cf -> cf.getTag().equals("001")).findFirst();
        return ppn.map(Controlfield::getValue).orElse(null);
    }

    public List<String> get4XXDollar0(String zone) {
        List<String> ppns = new ArrayList<>();
        List<Datafield> listeZone = this.datafields.stream().filter(datafield -> datafield.getTag().equals(zone)).collect(Collectors.toList());
        if (!listeZone.isEmpty()) {
            for (Datafield datafield : listeZone) {
                List<SubField> subFields = datafield.getSubFields().stream().filter(subField -> subField.getCode().equals("0")).collect(Collectors.toList());
                subFields.forEach(subField -> {
                    ppns.add(subField.getValue());
                });
            }
        }
        return ppns;
    }
}
