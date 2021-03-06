package com.variamos.moduino.binder.resolver;

import com.variamos.moduino.binder.resolver.json.DeviceJson;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class VariamosResolver {

    public static final String ENDPOINT = "https://cdn.itoxic.me/devices.json";

    public static DeviceJson[] resolveJSON() throws Exception {

        Gson gson = new Gson();
        DeviceJson[] json = gson.fromJson(getRemoteData(), DeviceJson[].class);

        return json;

    }

    public static String getRemoteData() throws Exception {

        try {

            URL url = new URL(ENDPOINT);
            URLConnection con = url.openConnection();

            con.addRequestProperty("User-Agent", "Mozilla");
            con.setReadTimeout(5000);
            con.setConnectTimeout(5000);

            StringBuilder textBuilder = new StringBuilder();

            try (Reader reader = new BufferedReader(new InputStreamReader
                    (con.getInputStream(), Charset.forName(StandardCharsets.UTF_8.name())))) {
                int c;
                while ((c = reader.read()) != -1)
                    textBuilder.append((char) c);
            }

            return textBuilder.toString();

        } catch (Exception e) {
            System.out.println("No se lograron recibir datos del ENDPOINT :: " + ENDPOINT + ", para la lectura de los dispositivos.");
            throw e;
        }

    }

}
