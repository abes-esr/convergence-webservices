package fr.abes.convergence.kbartws.service;

import fr.abes.convergence.kbartws.component.BaseXmlFunctionsCaller;
import fr.abes.convergence.kbartws.utils.TYPE_ID;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IsbnService implements IIdentifiantService {
    private final BaseXmlFunctionsCaller caller;

    public IsbnService(BaseXmlFunctionsCaller caller) {
        this.caller = caller;
    }

    @Override
    public boolean checkFormat(String isbn) {
        return false;
    }

    @Override
    public List<String> getPpnFromIdentifiant(String isbn) {
        return caller.isbnToPpn(isbn);
    }
}