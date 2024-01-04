package fr.abes.convergence.kbartws.controller;

import fr.abes.convergence.kbartws.dto.provider.ElementDto;
import fr.abes.convergence.kbartws.entity.notice.Controlfield;
import fr.abes.convergence.kbartws.entity.notice.Datafield;
import fr.abes.convergence.kbartws.entity.notice.NoticeXml;
import fr.abes.convergence.kbartws.entity.notice.SubField;
import fr.abes.convergence.kbartws.exception.ExceptionControllerHandler;
import fr.abes.convergence.kbartws.exception.IllegalPpnException;
import fr.abes.convergence.kbartws.service.*;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {KbartController.class})
@ContextConfiguration(classes = {IdentifiantFactory.class})
class KbartControllerTest {
    @Autowired
    WebApplicationContext context;

    @InjectMocks
    KbartController controller;

    @Autowired
    IdentifiantFactory factory;

    @MockBean
    ProviderService providerService;

    @MockBean
    NoticeService noticeService;

    @MockBean
    IsbnService isbnService;

    @MockBean
    IssnService issnService;

    @MockBean
    DoiService doiService;

    MockMvc mockMvc;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(context.getBean(KbartController.class))
                .setControllerAdvice(new ExceptionControllerHandler())
                .build();
    }

    @Test
    @DisplayName("test WS online_identifier_2_ppn : serial + ISSN ok + 1 PPN non supprimé de doc élec")
    void onlineIdentifier2PpnCas1() throws Exception, IllegalPpnException {
        String type = "serial";
        String onlineIdentifier = "1234-1234";

        Controlfield ctrlPpn = new Controlfield();
        ctrlPpn.setTag("001");
        ctrlPpn.setValue("123456789");

        Controlfield ctrlType = new Controlfield();
        ctrlType.setTag("008");
        ctrlType.setValue("Oax3");

        NoticeXml notice = new NoticeXml();
        notice.setLeader("     gam0 22        450 ");
        notice.setControlfields(Lists.newArrayList(ctrlPpn, ctrlType));
        Mockito.when(issnService.checkFormat("1234-1234")).thenReturn(true);
        Mockito.when(issnService.getPpnFromIdentifiant("1234-1234")).thenReturn(Lists.newArrayList("123456789"));
        Mockito.when(noticeService.getNoticeByPpn(Mockito.any())).thenReturn(notice);

        this.mockMvc.perform(get("/v1/online_identifier_2_ppn/" + type + "/" + onlineIdentifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ppns[0].ppn").value("123456789"))
                .andExpect(jsonPath("$.ppns[0].providerPresent").value(false));
    }

    @Test
    @DisplayName("test WS online_identifier_2_ppn : serial + ISSN ok + 2 PPN non supprimés dont un ppn qui n'est pas une notice electronique")
    void onlineIdentifier2PpnCas2() throws Exception, IllegalPpnException {
        String type = "serial";
        String onlineIdentifier = "1234-1234";

        Controlfield ctrlPpn = new Controlfield();
        ctrlPpn.setTag("001");
        ctrlPpn.setValue("123456789");

        Controlfield ctrlType = new Controlfield();
        ctrlType.setTag("008");
        ctrlType.setValue("Oax3");

        Controlfield ctrlPpn2 = new Controlfield();
        ctrlPpn2.setTag("001");
        ctrlPpn2.setValue("123456000");

        Controlfield ctrlType2 = new Controlfield();
        ctrlType2.setTag("008");
        ctrlType2.setValue("Aax3");

        NoticeXml notice = new NoticeXml();
        notice.setLeader("     gam0 22        450 ");
        notice.setControlfields(Lists.newArrayList(ctrlPpn, ctrlType));

        NoticeXml notice2 = new NoticeXml();
        notice2.setLeader("     gam0 22        450 ");
        notice2.setControlfields(Lists.newArrayList(ctrlPpn2, ctrlType2));

        Mockito.when(issnService.checkFormat("1234-1234")).thenReturn(true);
        Mockito.when(issnService.getPpnFromIdentifiant("1234-1234")).thenReturn(Lists.newArrayList("123456789", "123456000"));
        Mockito.when(noticeService.getNoticeByPpn("123456789")).thenReturn(notice);
        Mockito.when(noticeService.getNoticeByPpn("123456000")).thenReturn(notice2);

        this.mockMvc.perform(get("/v1/online_identifier_2_ppn/" + type + "/" + onlineIdentifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ppns[0].ppn").value("123456789"))
                .andExpect(jsonPath("$.ppns[0].providerPresent").value(false))
                .andExpect(jsonPath("$.erreurs[0]").value("Le PPN " + ctrlPpn2.getValue() + " n'est pas une ressource électronique"));
    }

    @Test
    @DisplayName("test WS online_identifier_2_ppn : serial + ISSN ok + exception erreur SQL")
    void onlineIdentifier2PpnCas3() throws Exception, IllegalPpnException {
        String type = "serial";
        String onlineIdentifier = "1234-1234";

        Controlfield ctrlPpn = new Controlfield();
        ctrlPpn.setTag("001");
        ctrlPpn.setValue("123456789");

        Controlfield ctrlType = new Controlfield();
        ctrlType.setTag("008");
        ctrlType.setValue("Oax3");

        NoticeXml notice = new NoticeXml();
        notice.setLeader("     gam0 22        450 ");
        notice.setControlfields(Lists.newArrayList(ctrlPpn, ctrlType));
        Mockito.when(issnService.checkFormat("1234-1234")).thenReturn(true);
        Mockito.when(issnService.getPpnFromIdentifiant("1234-1234")).thenReturn(Lists.newArrayList("123456789"));
        Mockito.doThrow(IOException.class).when(noticeService).getNoticeByPpn(Mockito.any());

        this.mockMvc.perform(get("/v1/online_identifier_2_ppn/" + type + "/" + onlineIdentifier))
                .andExpect(status().isNoContent())
                .andExpect(result -> Assertions.assertTrue((result.getResolvedException() instanceof IOException)));
    }

    @Test
    @DisplayName("test WS online_identifier_2_ppn : Serial + ISSN au mauvais format")
    void onlineIdentifier2PpnCas4() throws Exception {
        String type = "serial";
        String onlineIdentifier = "1234ZE234";

        Mockito.when(issnService.checkFormat("1234ZE234")).thenReturn(false);

        this.mockMvc.perform(get("/v1/online_identifier_2_ppn/" + type + "/" + onlineIdentifier))
                .andExpect(status().isBadRequest())
                .andExpect(result -> Assertions.assertTrue((result.getResolvedException() instanceof IllegalArgumentException)));
    }


    @Test
    @DisplayName("test WS online_identifier_2_ppn : check provider ok en 035$a")
    void onlineIdentifierCheckProviderOk035a() throws Exception, IllegalPpnException {
        String type = "serial";
        String onlineIdentifier = "1234-1234";
        String provider = "CAIRN";

        Controlfield ctrlPpn = new Controlfield();
        ctrlPpn.setTag("001");
        ctrlPpn.setValue("123456789");

        Controlfield ctrlType = new Controlfield();
        ctrlType.setTag("008");
        ctrlType.setValue("Oax3");

        Datafield datafield = new Datafield();
        datafield.setTag("035");
        SubField subField = new SubField();
        subField.setCode("a");
        subField.setValue("FRCAIRN");
        datafield.setSubFields(Lists.newArrayList(subField));

        NoticeXml notice = new NoticeXml();
        notice.setLeader("     gam0 22        450 ");
        notice.setControlfields(Lists.newArrayList(ctrlPpn, ctrlType));
        notice.setDatafields(Lists.newArrayList(datafield));

        ElementDto providerDto = new ElementDto("CAIRN", "CAIRN", 81);

        Mockito.when(providerService.getProviderDisplayName(Mockito.any())).thenReturn(Optional.of(providerDto));
        Mockito.when(providerService.getProviderFor035(81)).thenReturn(Lists.newArrayList("FRCAIRN"));
        Mockito.when(issnService.checkFormat("1234-1234")).thenReturn(true);
        Mockito.when(issnService.getPpnFromIdentifiant("1234-1234")).thenReturn(Lists.newArrayList("123456789"));
        Mockito.when(noticeService.getNoticeByPpn(Mockito.any())).thenReturn(notice);

        this.mockMvc.perform(get("/v1/online_identifier_2_ppn/" + type + "/" + onlineIdentifier + "/" + provider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ppns[0].ppn").value("123456789"))
                .andExpect(jsonPath("$.ppns[0].providerPresent").value(true));
    }

    @Test
    @DisplayName("test WS online_identifier_2_ppn : check provider ok en 210$c")
    void onlineIdentifierCheckProviderOk210c() throws Exception, IllegalPpnException {
        String type = "serial";
        String onlineIdentifier = "1234-1234";
        String provider = "CAIRN";

        Controlfield ctrlPpn = new Controlfield();
        ctrlPpn.setTag("001");
        ctrlPpn.setValue("123456789");

        Controlfield ctrlType = new Controlfield();
        ctrlType.setTag("008");
        ctrlType.setValue("Oax3");

        Datafield datafield = new Datafield();
        datafield.setTag("210");
        SubField subField = new SubField();
        subField.setCode("c");
        subField.setValue("CAIRN");
        datafield.setSubFields(Lists.newArrayList(subField));

        NoticeXml notice = new NoticeXml();
        notice.setLeader("     gam0 22        450 ");
        notice.setControlfields(Lists.newArrayList(ctrlPpn, ctrlType));
        notice.setDatafields(Lists.newArrayList(datafield));

        ElementDto providerDto = new ElementDto("CAIRN", "CAIRN", 81);

        Mockito.when(providerService.getProviderDisplayName(Mockito.any())).thenReturn(Optional.of(providerDto));
        Mockito.when(issnService.checkFormat("1234-1234")).thenReturn(true);
        Mockito.when(issnService.getPpnFromIdentifiant("1234-1234")).thenReturn(Lists.newArrayList("123456789"));
        Mockito.when(noticeService.getNoticeByPpn(Mockito.any())).thenReturn(notice);

        this.mockMvc.perform(get("/v1/online_identifier_2_ppn/" + type + "/" + onlineIdentifier + "/" + provider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ppns[0].ppn").value("123456789"))
                .andExpect(jsonPath("$.ppns[0].providerPresent").value(true));
    }

    @Test
    @DisplayName("test WS online_identifier_2_ppn : check provider non ok")
    void onlineIdentifierCheckProviderNonOk() throws Exception, IllegalPpnException {
        String type = "serial";
        String onlineIdentifier = "1234-1234";
        String provider = "CAIRN";

        Controlfield ctrlPpn = new Controlfield();
        ctrlPpn.setTag("001");
        ctrlPpn.setValue("123456789");

        Controlfield ctrlType = new Controlfield();
        ctrlType.setTag("008");
        ctrlType.setValue("Oax3");

        NoticeXml notice = new NoticeXml();
        notice.setLeader("     gam0 22        450 ");
        notice.setControlfields(Lists.newArrayList(ctrlPpn, ctrlType));

        ElementDto providerDto = new ElementDto("CAIRN", "CAIRN", 81);

        Mockito.when(providerService.getProviderDisplayName(Mockito.any())).thenReturn(Optional.of(providerDto));
        Mockito.when(issnService.checkFormat("1234-1234")).thenReturn(true);
        Mockito.when(issnService.getPpnFromIdentifiant("1234-1234")).thenReturn(Lists.newArrayList("123456789"));
        Mockito.when(noticeService.getNoticeByPpn(Mockito.any())).thenReturn(notice);

        this.mockMvc.perform(get("/v1/online_identifier_2_ppn/" + type + "/" + onlineIdentifier + "/" + provider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ppns[0].providerPresent").value(false));
    }

    @Test
    @DisplayName("test WS online_identifier_2_ppn : check provider diacritics")
    void onlineIdentifierCheckProviderDiacritics() throws Exception, IllegalPpnException {
        String type = "serial";
        String onlineIdentifier = "1234-1234";
        String provider = "CAèRN";

        Controlfield ctrlPpn = new Controlfield();
        ctrlPpn.setTag("001");
        ctrlPpn.setValue("123456789");

        Controlfield ctrlType = new Controlfield();
        ctrlType.setTag("008");
        ctrlType.setValue("Oax3");

        Datafield datafield = new Datafield();
        datafield.setTag("210");
        SubField subField = new SubField();
        subField.setCode("c");
        subField.setValue("CAERN");
        datafield.setSubFields(Lists.newArrayList(subField));

        NoticeXml notice = new NoticeXml();
        notice.setLeader("     gam0 22        450 ");
        notice.setControlfields(Lists.newArrayList(ctrlPpn, ctrlType));
        notice.setDatafields(Lists.newArrayList(datafield));

        ElementDto providerDto = new ElementDto("CAIRN", "CAèRN", 81);

        Mockito.when(providerService.getProviderDisplayName(Mockito.any())).thenReturn(Optional.of(providerDto));
        Mockito.when(issnService.checkFormat("1234-1234")).thenReturn(true);
        Mockito.when(issnService.getPpnFromIdentifiant("1234-1234")).thenReturn(Lists.newArrayList("123456789"));
        Mockito.when(noticeService.getNoticeByPpn(Mockito.any())).thenReturn(notice);

        this.mockMvc.perform(get("/v1/online_identifier_2_ppn/" + type + "/" + onlineIdentifier + "/" + provider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ppns[0].ppn").value("123456789"))
                .andExpect(jsonPath("$.ppns[0].providerPresent").value(true));
    }

    @Test
    @DisplayName("test WS online_identifier_2_ppn : erreur appel ws provider")
    void onlineIdentifer2PpnErreurAppelWs() throws Exception, IllegalPpnException {
        String type = "serial";
        String onlineIdentifier = "1234-1234";
        String provider = "CAIRN";

        Controlfield ctrlPpn = new Controlfield();
        ctrlPpn.setTag("001");
        ctrlPpn.setValue("123456789");

        Controlfield ctrlType = new Controlfield();
        ctrlType.setTag("008");
        ctrlType.setValue("Oax3");

        Datafield datafield = new Datafield();
        datafield.setTag("210");
        SubField subField = new SubField();
        subField.setCode("c");
        subField.setValue("CAIRN");
        datafield.setSubFields(Lists.newArrayList(subField));

        NoticeXml notice = new NoticeXml();
        notice.setLeader("     gam0 22        450 ");
        notice.setControlfields(Lists.newArrayList(ctrlPpn, ctrlType));
        notice.setDatafields(Lists.newArrayList(datafield));

        Mockito.when(providerService.getProviderDisplayName(Mockito.any())).thenThrow(new ConnectException());
        Mockito.when(issnService.checkFormat("1234-1234")).thenReturn(true);
        Mockito.when(issnService.getPpnFromIdentifiant("1234-1234")).thenReturn(Lists.newArrayList("123456789"));
        Mockito.when(noticeService.getNoticeByPpn(Mockito.any())).thenReturn(notice);

        this.mockMvc.perform(get("/v1/online_identifier_2_ppn/" + type + "/" + onlineIdentifier + "/" + provider))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ppns[0].ppn").value("123456789"))
                .andExpect(jsonPath("$.ppns[0].providerPresent").value(false))
                .andExpect(jsonPath("$.erreurs[0]").value("Impossible d'analyser le provider en raison d'un problème technique, poursuite du traitement"));
    }

    @Test
    @DisplayName("test WS print_identifier_2_ppn : serial + ISSN ok + 1 PPN non supprimé de doc imprimé")
    void printIdentifier2PpnCas1() throws Exception, IllegalPpnException {
        String type = "serial";
        String printIdentifier = "1234-1234";

        Controlfield ctrlPpn = new Controlfield();
        ctrlPpn.setTag("001");
        ctrlPpn.setValue("123456789");

        Controlfield ctrlType = new Controlfield();
        ctrlType.setTag("008");
        ctrlType.setValue("Aax3");

        NoticeXml notice = new NoticeXml();
        notice.setLeader("     gam0 22        450 ");
        notice.setControlfields(Lists.newArrayList(ctrlPpn, ctrlType));

        Mockito.when(issnService.checkFormat("1234-1234")).thenReturn(true);
        Mockito.when(issnService.getPpnFromIdentifiant("1234-1234")).thenReturn(Lists.newArrayList("123456789"));
        Mockito.when(noticeService.getNoticeByPpn(Mockito.any())).thenReturn(notice);

        this.mockMvc.perform(get("/v1/print_identifier_2_ppn/" + type + "/" + printIdentifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ppns[0].ppn").value("123456789"))
                .andExpect(jsonPath("$.ppns[0].providerPresent").value(false));
    }

    @Test
    @DisplayName("test WS print_identifier_2_ppn : serial + ISSN KO 0 ppn ne correspond")
    void printIdentifier2PpnCas0Ppn() throws Exception, IllegalPpnException {
        String type = "serial";
        String printIdentifier = "1234-1234";

        Mockito.when(issnService.checkFormat("1234-1234")).thenReturn(true);
        Mockito.when(issnService.getPpnFromIdentifiant("1234-1234")).thenReturn(Lists.newArrayList());

        this.mockMvc.perform(get("/v1/print_identifier_2_ppn/" + type + "/" + printIdentifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.erreurs[0]").value("Aucun PPN ne correspond au " + printIdentifier));
    }
    @Test
    @DisplayName("test WS print_identifier_2_ppn : serial + ISSN ok + 1 PPN supprimé de doc imprimé")
    void printIdentifier2PpnCas1Supprime() throws Exception, IllegalPpnException {
        String type = "serial";
        String printIdentifier = "1234-1234";

        Controlfield ctrlPpn = new Controlfield();
        ctrlPpn.setTag("001");
        ctrlPpn.setValue("123456789");

        Controlfield ctrlType = new Controlfield();
        ctrlType.setTag("008");
        ctrlType.setValue("Aax3");

        NoticeXml notice = new NoticeXml();
        notice.setLeader("     dam0 22        450 ");
        notice.setControlfields(Lists.newArrayList(ctrlPpn, ctrlType));

        Mockito.when(issnService.checkFormat("1234-1234")).thenReturn(true);
        Mockito.when(issnService.getPpnFromIdentifiant("1234-1234")).thenReturn(Lists.newArrayList("123456789"));
        Mockito.when(noticeService.getNoticeByPpn(Mockito.any())).thenReturn(notice);

        this.mockMvc.perform(get("/v1/print_identifier_2_ppn/" + type + "/" + printIdentifier))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("test WS print_identifier_2_ppn : serial + ISSN ok + 2 PPN non supprimés dont un ppn qui n'est pas une notice imprimée")
    void printIdentifier2PpnCas2() throws Exception, IllegalPpnException {
        String type = "serial";
        String onlineIdentifier = "1234-1234";

        Controlfield ctrlPpn = new Controlfield();
        ctrlPpn.setTag("001");
        ctrlPpn.setValue("123456789");

        Controlfield ctrlType = new Controlfield();
        ctrlType.setTag("008");
        ctrlType.setValue("Oax3");

        Controlfield ctrlPpn2 = new Controlfield();
        ctrlPpn2.setTag("001");
        ctrlPpn2.setValue("123456000");

        Controlfield ctrlType2 = new Controlfield();
        ctrlType2.setTag("008");
        ctrlType2.setValue("Aax3");

        NoticeXml notice = new NoticeXml();
        notice.setLeader("     gam0 22        450 ");
        notice.setControlfields(Lists.newArrayList(ctrlPpn, ctrlType));

        NoticeXml notice2 = new NoticeXml();
        notice2.setLeader("     gam0 22        450 ");
        notice2.setControlfields(Lists.newArrayList(ctrlPpn2, ctrlType2));

        Mockito.when(issnService.checkFormat("1234-1234")).thenReturn(true);
        Mockito.when(issnService.getPpnFromIdentifiant("1234-1234")).thenReturn(Lists.newArrayList("123456789", "123456000"));
        Mockito.when(noticeService.getNoticeByPpn("123456789")).thenReturn(notice);
        Mockito.when(noticeService.getNoticeByPpn("123456000")).thenReturn(notice2);

        this.mockMvc.perform(get("/v1/print_identifier_2_ppn/" + type + "/" + onlineIdentifier))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ppns[0].ppn").value("123456000"))
                .andExpect(jsonPath("$.ppns[0].providerPresent").value(false))
                .andExpect(jsonPath("$.erreurs[0]").value("Le PPN " + ctrlPpn.getValue() + " n'est pas une ressource imprimée"));

    }

    @Test
    @DisplayName("test WS print_identifier_2_ppn : serial + ISSN ok + exception erreur SQL")
    void printIdentifier2PpnCas3() throws Exception, IllegalPpnException {
        String type = "serial";
        String onlineIdentifier = "1234-1234";

        Controlfield ctrlPpn = new Controlfield();
        ctrlPpn.setTag("001");
        ctrlPpn.setValue("123456789");

        Controlfield ctrlType = new Controlfield();
        ctrlType.setTag("008");
        ctrlType.setValue("Oax3");

        NoticeXml notice = new NoticeXml();
        notice.setLeader("     gam0 22        450 ");
        notice.setControlfields(Lists.newArrayList(ctrlPpn, ctrlType));
        Mockito.when(issnService.checkFormat("1234-1234")).thenReturn(true);
        Mockito.when(issnService.getPpnFromIdentifiant("1234-1234")).thenReturn(Lists.newArrayList("123456789"));
        Mockito.doThrow(IOException.class).when(noticeService).getNoticeByPpn(Mockito.any());

        this.mockMvc.perform(get("/v1/print_identifier_2_ppn/" + type + "/" + onlineIdentifier))
                .andExpect(status().isNoContent())
                .andExpect(result -> Assertions.assertTrue((result.getResolvedException() instanceof IOException)));
    }

    @Test
    @DisplayName("test WS print_identifier_2_ppn : Serial + ISSN au mauvais format")
    void printIdentifier2PpnCas4() throws Exception {
        String type = "serial";
        String onlineIdentifier = "1234ZE234";

        Mockito.when(issnService.checkFormat("1234ZE234")).thenReturn(false);

        this.mockMvc.perform(get("/v1/print_identifier_2_ppn/" + type + "/" + onlineIdentifier))
                .andExpect(status().isBadRequest())
                .andExpect(result -> Assertions.assertTrue((result.getResolvedException() instanceof IllegalArgumentException)));
    }

    @Test
    @DisplayName("test WS print_identifier_to_ppn : Serial au mauvais format")
    void printIdentifier2PpnCas5() throws Exception {
        String type = "test";
        String printIdentifier = "12345678";

        this.mockMvc.perform(get("/v1/print_identifier_2_ppn/" + type + "/" + printIdentifier))
                .andExpect(status().isBadRequest())
                .andExpect(result -> Assertions.assertTrue((result.getResolvedException() instanceof IllegalArgumentException)));
    }
}