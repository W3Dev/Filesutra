package cloudfilespicker

import grails.converters.JSON
import grails.util.Holders
import groovyx.net.http.RESTClient
import org.apache.commons.io.IOUtils

import static groovyx.net.http.ContentType.URLENC

/**
 * Created by vishesh on 06/05/15.
 */
class Google {

    def static grailsApplication = Holders.getGrailsApplication()

    private static final String CLIENT_ID     = grailsApplication.config.auth.google.CLIENT_ID
    private static final String CLIENT_SECRET = grailsApplication.config.auth.google.CLIENT_SECRET
    private static final String REDIRECT_URI = grailsApplication.config.auth.google.REDIRECT_URI

    private static final String AUTH_URL = "https://accounts.google.com/o/oauth2/auth"
    private static final String API_URL = "https://www.googleapis.com"

    static def getLoginUrl() {
        def params = [
                response_type: "code",
                client_id    : CLIENT_ID,
                redirect_uri : REDIRECT_URI,
                access_type  : "offline",
                scope        : "email https://www.googleapis.com/auth/drive"
        ]
        def url = "$AUTH_URL?" + params.collect { k, v -> "$k=$v" }.join('&')
        return url
    }

    static def exchangeCode(String code) {
        def restClient = new RESTClient(API_URL)
        def resp = restClient.post(
                path: '/oauth2/v3/token',
                body: [client_id   : CLIENT_ID, client_secret: CLIENT_SECRET,
                       redirect_uri: REDIRECT_URI, code: code, grant_type: 'authorization_code'],
                requestContentType: URLENC)
        return [
                accessToken: resp.data.access_token,
                refreshToken: resp.data.refresh_token
        ]
    }

    static def getEmailId(String accessToken) {
        def restClient = new RESTClient(API_URL)
        restClient.headers.Authorization = "Bearer $accessToken"
        def resp = restClient.get(path: '/plus/v1/people/me')
        return resp.data.emails[0].value
    }

    static def listItems(String folderId, String accessToken) {
        def restClient = new RESTClient(API_URL)
        restClient.headers.Authorization = "Bearer $accessToken"
        def resp = restClient.get(path: "/drive/v2/files", params : [q: "'$folderId' in parents and trashed=false"])
        return resp.data.items
    }

    static def getFile(String fileId, String accessToken) {
        def restClient = new RESTClient(API_URL)
        restClient.headers.Authorization = "Bearer $accessToken"
        def resp = restClient.get(path: "/drive/v2/files/$fileId")
        return resp.data
    }

    static URLConnection getDownloadUrlConnection(String fileId, String accessToken) {
        def file = getFile(fileId, accessToken)
        String contentUrl = file.downloadUrl ? file.downloadUrl : file.exportLinks?."application/pdf"
//        String contentUrl = "$API_URL/drive/v2/files/$fileId?alt=media"
        URL url = new URL(contentUrl)
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Authorization", 'Bearer ' + accessToken);
        return connection
    }

    static def refreshToken(String refreshToken) {
        def restClient = new RESTClient(API_URL)
        def resp = restClient.post(
                path: '/oauth2/v3/token',
                body: [client_id   : CLIENT_ID, client_secret: CLIENT_SECRET,
                       refresh_token: refreshToken, grant_type: 'authorization_code'],
                requestContentType: URLENC)
        return resp.data.access_token
    }
}