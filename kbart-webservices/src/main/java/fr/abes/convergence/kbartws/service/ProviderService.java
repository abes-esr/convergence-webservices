package fr.abes.convergence.kbartws.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import fr.abes.convergence.kbartws.component.BaseXmlFunctionsCaller;
import fr.abes.convergence.kbartws.dto.provider.ElementDto;
import fr.abes.convergence.kbartws.dto.provider.ResultDto;
import fr.abes.convergence.kbartws.dto.provider.ResultProviderDto;
import fr.abes.convergence.kbartws.dto.provider035.BaconDto;
import fr.abes.convergence.kbartws.dto.provider035.ResultProvider035Dto;
import fr.abes.convergence.kbartws.exception.IllegalPpnException;
import fr.abes.convergence.kbartws.utils.ExecutionTime;
import fr.abes.convergence.kbartws.utils.Utilitaire;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLRecoverableException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProviderService {
    private final WsService wsService;

    private final BaseXmlFunctionsCaller caller;

    private final ObjectMapper objectMapper;

    @ExecutionTime
    public Optional<ElementDto> getProviderDisplayName(String shortName) throws IOException {
        ResultProviderDto result = wsService.callProviderList();
        return Arrays.stream(result.getBacon().getQuery().getResults()).toList().stream().filter(el -> el.getElements().getProvider().equalsIgnoreCase(shortName)).map(ResultDto::getElements).findFirst();
    }

    @ExecutionTime
    public List<String> getProviderFor035(Integer provider) throws IOException {
        List<String> listValeurs = new ArrayList<>();
        try {
            ResultProvider035Dto result = objectMapper.readValue(caller.baconProvider035(provider), ResultProvider035Dto.class);
            if (result.getBacon().getQuery().getResult() != null) {
                String valeur035 = result.getBacon().getQuery().getResult().getElements().getValeur035();
                if (valeur035 != null) {
                    listValeurs.addAll(Arrays.stream(valeur035.split("\\|")).toList());
                }
            }
        } catch (JsonProcessingException ex) {
            throw new IOException("Impossible de récupérer les ppns correspondant à cet identifiant");
        } catch (SQLRecoverableException ex) {
            throw new IOException("Incident technique lors de l'accès à la base de données");
        }
        return listValeurs;
    }
}
