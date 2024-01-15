package org.fetch;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.fetch.exceptions.HttpMethodNotImplementedException;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.net.URL;

import org.apache.commons.lang3.tuple.Pair;


public class HealthChecker {

    public static void main(String[] args) {
        List<Endpoint> endpoints = parseYaml();
        Map<String, Pair<Integer, Integer>> availabilityPercentageMap = new HashMap<>();

        try {
            while (true) {
                for (Endpoint endpoint : endpoints) {
                    boolean isUp = isUp(endpoint);
                    String domain = getDomainFromUrl(endpoint.getUrl());
                    Pair<Integer, Integer> currentValue = availabilityPercentageMap.getOrDefault(domain, Pair.of(0, 0));
                    availabilityPercentageMap.put(domain, Pair.of(isUp ? currentValue.getLeft() + 1 : currentValue.getLeft(), currentValue.getRight() + 1));
                }
                logResults(availabilityPercentageMap);
                Thread.sleep(15000);
            }
        } catch (InterruptedException e) {
            // Handle interruption (e.g., when the user manually exits the program)
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static List<Endpoint> parseYaml() {
        InputStream inputStream = HealthChecker.class.getClassLoader().getResourceAsStream("endpoints.yml");
        Yaml yaml = new Yaml();
        List<LinkedHashMap> rawDateFromFile = yaml.load(inputStream);
        List<Endpoint> endpointList = new ArrayList<>();
        rawDateFromFile.forEach(x -> {
            endpointList.add(new ObjectMapper().convertValue(x, Endpoint.class));
        });
        return endpointList;
    }

    private static boolean isUp(Endpoint endpoint) throws IOException {
        HttpRequestBase request;
        switch (endpoint.getMethod()) {
            case GET:
                HttpGet httpGet = new HttpGet(endpoint.getUrl());
                endpoint.getHeaders().forEach((k, v) -> {
                    httpGet.addHeader(new BasicHeader(k, v));
                });
                request = httpGet;
                break;
            case POST:
                HttpPost httpPost = new HttpPost(endpoint.getUrl());
                httpPost.setEntity(new StringEntity(endpoint.getBody()));
                endpoint.getHeaders().forEach((k, v) -> {
                    httpPost.addHeader(new BasicHeader(k, v));
                });
                request = httpPost;
                break;
            case PATCH:
            case PUT:
            case DELETE:
            default:
                throw new HttpMethodNotImplementedException();
        }

        try {
            long currenTime = System.currentTimeMillis();
            HttpResponse response = HttpClients.createDefault().execute(request);
            long latency = System.currentTimeMillis() - currenTime;
            int statusCode = response.getStatusLine().getStatusCode();
            return statusCode >= 200 && statusCode < 300 && latency < 500;
        } catch (IOException e) {
            // Handle IOException (e.g., network error)
            return false;
        }
    }


    private static void logResults(Map<String, Pair<Integer, Integer>> availabilityPercentageMap) {
        availabilityPercentageMap.forEach((k, v) -> {
            double availabilityPercentage = ((double) v.getLeft() / (double) v.getRight()) * (double) 100;
            availabilityPercentage = Math.round(availabilityPercentage);
            System.out.println(k + " has " + (int) availabilityPercentage + "% availability percentage");
        });
    }

    private static String getDomainFromUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getHost();
        } catch (MalformedURLException e) {
            return null;
        }
    }
}
