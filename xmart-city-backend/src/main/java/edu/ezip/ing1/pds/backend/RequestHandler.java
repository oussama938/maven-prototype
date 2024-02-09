package edu.ezip.ing1.pds.backend;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import edu.ezip.commons.LoggingUtils;
import edu.ezip.ing1.pds.business.dto.Student;
import edu.ezip.ing1.pds.business.server.XMartCityService;
import edu.ezip.ing1.pds.commons.Request;
import edu.ezip.ing1.pds.commons.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.sql.Connection;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
public class RequestHandler implements Runnable {
    private final Socket socket;
    private final Connection connection;
    private final Thread self;
    private static final String threadNamePrfx = "core-request-handler";
    private final InputStream instream;
    private final OutputStream outstream;
    // private final Connection connection;
    private final static String LoggingLabel = "C o re - B a c k e n d - S e r v e r";
    private final Logger logger = LoggerFactory.getLogger(LoggingLabel);
    private final XMartCityService xmartCityService = XMartCityService.getInstance();
    private final CoreBackendServer father;

    private static final int maxTimeLapToGetAClientPayloadInMs = 5000;
    private static final int timeStepMs = 300;
    private final BlockingDeque<Integer> waitArtifact = new LinkedBlockingDeque<Integer>(1);

    protected RequestHandler(final Socket socket,
                             final Connection connection,
                             final int myBirthDate,
                             final CoreBackendServer father) throws IOException {
        this.socket = socket;
        this.connection = connection;
        this.father = father;
        final StringBuffer threadName = new StringBuffer();
        threadName.append(threadNamePrfx).append("★").append(String.format("%04d",myBirthDate));
        self = new Thread(this, threadName.toString());
        instream = socket.getInputStream();
        outstream = socket.getOutputStream();
        self.start();
    }



    @Override
    public void run() {
        try {

            int timeout = maxTimeLapToGetAClientPayloadInMs;
            while (0 == instream.available() && 0 < timeout) {
                waitArtifact.pollFirst(timeStepMs, TimeUnit.MILLISECONDS);
                timeout-=timeStepMs;
            }
            if (0>timeout) return;

            final byte [] inputData = new byte[instream.available()];
            instream.read(inputData);
            final Request request = getRequest(inputData);
            final Response response = xmartCityService.dispatch(request, connection);

            final byte [] outoutData = getResponse(response);
            LoggingUtils.logDataMultiLine(logger, Level.DEBUG, outoutData);
            outstream.write(outoutData);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            father.completeRequestHandler(this);
        }
    }

    private final Request getRequest(byte[] data) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper.readValue(data, Request.class);
    }

    private final byte[] getResponse(final Response response) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(response);
    }
    
    public final Connection getConnection() {
        return connection;
    }

    public final Socket getSocket() {
        return socket;
    }

    private void handleInsertStudentRequest(Request request, Connection connection) throws IOException, IllegalAccessException, InvocationTargetException, InterruptedException {
        // Convertissez le corps de la requ�te en objet Student
        ObjectMapper mapper = new ObjectMapper();
        Student student = mapper.readValue(request.getRequestBody(), Student.class);

        // Appelez la m�thode d'insertion du service backend
        Response response = xmartCityService.handleInsertStudent(request, connection);

        // Utilisez le message de la r�ponse g�n�r�e par la m�thode handleInsertStudent
        byte[] responseData = mapper.writeValueAsBytes(response);
        outstream.write(responseData);
    }

}
