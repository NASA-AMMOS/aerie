package gov.nasa.jpl.aerie.scheduler.aerie;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AerieClient {
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public final String url;
    private final OkHttpClient client;
    private final Request request;
    private String ssoCookieValue = null;

    public AerieClient(String url) {
        this.url = url;
        this.client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.MINUTES)
                .build();
        this.request = new Request.Builder()
                .url(url)
                .build();
    }

    public <D extends Operation.Data, T, V extends Operation.Variables> T request(Operation<D, T, V> operation, ScalarTypeAdapters typeAdapters) {
        Request.Builder httpRequestBuilder = request.newBuilder();
        if (ssoCookieValue != null) {
            httpRequestBuilder = httpRequestBuilder.addHeader("authorization", ssoCookieValue);
        }
        final Request httpRequest = httpRequestBuilder
                .post(RequestBody.create(operation.composeRequestBody(), JSON))
                .build();

        try {
            final okhttp3.Response httpResponse = client.newCall(httpRequest).execute();
            final Response<T> response = operation.parse(httpResponse.body().source(), typeAdapters);
            if (response.hasErrors() || response.getData() == null) {
                System.err.println(response.getErrors());
                System.exit(1);
            }
            return response.getData();
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
        return null;
    }

    public <D extends Operation.Data, T, V extends Operation.Variables> T request(Operation<D, T, V> operation) {
        return request(operation, ScalarTypeAdapters.DEFAULT);
    }

    public boolean authenticate(String username, String password) {
        LoginMutation mutation = LoginMutation.builder().username(username).password(password).build();
        LoginMutation.Data response = this.request(mutation);
        if (!response.login().success()) {
            return false;
        }
        this.ssoCookieValue = response.login().ssoCookieValue();
        return true;
    }

    /**
     * Prompts the user for credentials using an on-screen window and logs in. Loops until successful. Should probably
     * be replaced with something better in the long term.
     */
    public void getCredentialsAndAuthenticate() {
        final JTextField field1 = new JTextField();
        final JPasswordField field2 = new JPasswordField();
        final JPanel panel = new JPanel(new GridLayout(0, 1));
        panel.add(new JLabel("JPL username"));
        panel.add(field1);
        panel.add(new JLabel("Password"));
        panel.add(field2);
        while (true) {
            final int result = JOptionPane.showConfirmDialog(null, panel, "Login to AERIE",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                final String username = field1.getText();
                final String password = String.valueOf(field2.getPassword());
                if (this.authenticate(username, password)) {
                    break;
                }
            } else {
                System.exit(0);
            }
        }
    }
}
