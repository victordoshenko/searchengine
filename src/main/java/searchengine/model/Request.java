package searchengine.model;

import searchengine.morphology.MorphologyAnalyzer;

import java.util.ArrayList;
import java.util.List;


public class Request {

    private final String req;
    private final List<String> reqLemmas;

    public List<String> getReqLemmas() {
        return reqLemmas;
    }

    public String getReq() {
        return req;
    }

    public Request(String req) {
        this.req = req;
        reqLemmas = new ArrayList<>();
        try {
            MorphologyAnalyzer analyzer = new MorphologyAnalyzer();
            reqLemmas.addAll(analyzer.getLemmas(req));
        } catch (Exception e) {
            System.out.println("ошибка морфологочиского анализа");
        }
    }
}
