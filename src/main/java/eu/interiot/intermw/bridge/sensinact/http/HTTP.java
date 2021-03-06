/**
 * /**
 * INTER-IoT. Interoperability of IoT Platforms.
 * INTER-IoT is a R&D project which has received funding from the European
 * Union's Horizon 2020 research and innovation programme under grant
 * agreement No 687283.
 * <p>
 * Copyright (C) 2017-2018, by : - Università degli Studi della Calabria
 * <p>
 * <p>
 * For more information, contact: - @author
 * <a href="mailto:g.caliciuri@dimes.unical.it">Giuseppe Caliciuri</a>
 * - Project coordinator:  <a href="mailto:coordinator@inter-iot.eu"></a>
 * <p>
 * <p>
 * This code is licensed under the EPL license, available at the root
 * application directory.
 */
package eu.interiot.intermw.bridge.sensinact.http;

import eu.interiot.intermw.bridge.sensinact.http.exception.UnsupportedAuthenticationMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.misc.BASE64Encoder;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by nj246216 on 15/09/16.
 */
public class HTTP {

    public static enum AuthenticationMethod {
        BASIC,
        DIGGEST,
        NONE
    }

    AuthenticationMethod authenticationMethod = AuthenticationMethod.NONE;

    private static final Logger LOG = LoggerFactory.getLogger(HTTP.class);
    private String contentType="application/json";
    private String method="GET";
    private Map<String,String> headers=new HashMap<String,String>();
    private String bodyRequest;
    private String bodyResponse;
    private String url;
    private String username;
    private String password;

    public HTTP(){
        getHeaders().put("Content-Type",contentType);
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String,String> getHeaders() {
        return headers;
    }

    public String submit(String url,String method,Map<String,String> headers) throws Exception {
        return submit(url,method,headers,null);
    }

    public String submit(String url,String method,Map<String,String> headers,String body) throws Exception{

        this.url=url;
        this.method=method;
        this.headers=headers;
        this.bodyRequest=body;

        return submit();

    }

    private void configAuthenticationHeaders() throws UnsupportedAuthenticationMethod {

        if(authenticationMethod==AuthenticationMethod.BASIC){
            final String usernamePasswordBase64=new BASE64Encoder().encode(String.format("%s:%s",getUsername(),getPassword()).getBytes());
            String encoded=" Basic "+usernamePasswordBase64;
            getHeaders().put("Authorization",encoded);
        }else if (authenticationMethod==AuthenticationMethod.NONE){
            //it is ok to not have authentication
        }
        else {
            throw new UnsupportedAuthenticationMethod("Only "+AuthenticationMethod.BASIC+" is currently implemented.");
        }

    }

    public String submit() throws IOException, UnsupportedAuthenticationMethod {
        configAuthenticationHeaders();
        StringBuffer sb=new StringBuffer();
        URL yahoo = new URL(url);
        HttpURLConnection con = (HttpURLConnection) yahoo.openConnection();
        con.setRequestMethod(method);

        if(getHeaders()!=null)
            for(Map.Entry<String,String> entry:getHeaders().entrySet()){
                con.setRequestProperty(entry.getKey(),
                        entry.getValue());
            }

        if(getBodyRequest()!=null){
            con.setDoOutput(true);
            PrintWriter pw=new PrintWriter(con.getOutputStream());
            pw.write(getBodyRequest());
            pw.close();
        }

        BufferedReader in = new BufferedReader(
                new InputStreamReader(
                        con.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            sb.append(inputLine);
        this.bodyResponse=sb.toString();

        return getBodyResponse();
    }

    public String submit(String url) throws Exception{
        return submit(url,getMethod(),getHeaders());
    }

    public String submit(String url,Map<String,String> headers) throws Exception{
        HashMap allHeaders=new HashMap<String,String>(getHeaders());
        allHeaders.putAll(headers);
        return submit(url,getMethod(),allHeaders);
    }

    public String submitStream() throws IOException, UnsupportedAuthenticationMethod {
        configAuthenticationHeaders();
        URL yahoo = new URL(url);
        HttpURLConnection con = (HttpURLConnection) yahoo.openConnection();
        con.setUseCaches(false);
        con.setDoInput(true);
        con.setRequestMethod(method);

        if(getHeaders()!=null)
            for(Map.Entry<String,String> entry:getHeaders().entrySet()){
                con.setRequestProperty(entry.getKey(),
                        entry.getValue());
            }

        if(getBodyRequest()!=null){
            con.setDoOutput(true);
            PrintWriter pw=new PrintWriter(con.getOutputStream());
            pw.write(getBodyRequest());
            pw.close();
        }

        if(con.getHeaderField("Content-Encoding")!=null && con.getHeaderField("Content-Encoding").contains("gzip")){
            ByteArrayOutputStream sb=new ByteArrayOutputStream();
            Integer token;
            if(con.getInputStream().available()>1)
                while((token=con.getInputStream().read())!=-1){
                    sb.write(token);
                }

            final byte[] datareturned=sb.toByteArray();
            if(datareturned.length>0 && isGZipCompressed(datareturned)){
                LOG.debug("Response is compressed, starting decompression");
                setBodyResponse(decompress(datareturned));
            }else {
                LOG.error("Response Header indicates compression, although the content seems to be corrupted");
                setBodyResponse(new String(datareturned));
            }

        }else {
            StringBuffer bodyString=new StringBuffer();
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            con.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                bodyString.append(inputLine);

            setBodyResponse(bodyString.toString());

        }

        return getBodyResponse();
    }

    public void setBodyRequest(String bodyRequest) {
        this.bodyRequest = bodyRequest;
    }

    public String getBodyRequest() {
        return bodyRequest;
    }

    public String getBodyResponse() {
        return bodyResponse;
    }

    private void setBodyResponse(String bodyResponse) {
        this.bodyResponse = bodyResponse;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private static String decompress(final byte[] compressed) throws IOException {
        String outStr = "";
        if ((compressed == null) || (compressed.length == 0)) {
            return "";
        }
        if (isGZipCompressed(compressed)) {
            GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compressed));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, "UTF-8"));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                outStr += line;
            }
        } else {
            outStr = new String(compressed);
        }
        return outStr;
    }

    private static boolean isGZipCompressed(final byte[] data) {
        return (data[0] == (byte) (GZIPInputStream.GZIP_MAGIC)) && (data[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));
    }

    public void setAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
