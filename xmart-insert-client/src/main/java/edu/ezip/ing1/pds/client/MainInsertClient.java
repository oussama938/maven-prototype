package edu.ezip.ing1.pds.client;

import edu.ezip.ing1.pds.business.dto.Student;
import edu.ezip.ing1.pds.business.dto.Students;
import edu.ezip.ing1.pds.client.commons.ClientRequest;
import edu.ezip.ing1.pds.client.commons.ConfigLoader;
import edu.ezip.ing1.pds.client.commons.NetworkConfig;
import edu.ezip.ing1.pds.commons.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.UUID;
import java.util.Deque;
import java.util.ArrayDeque;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MainInsertClient {

    private final static String LoggingLabel = "I n s e r t e r - C l i e n t";
    private final static Logger logger = LoggerFactory.getLogger(LoggingLabel);
    private final static String studentsToBeInserted = "students-to-be-inserted.yaml";
    private final static String networkConfigFile = "network.yaml";
    private static final String requestOrder = "INSERT_STUDENT";
    @SuppressWarnings("rawtypes")
    private static final Deque<ClientRequest> clientRequests = new ArrayDeque<ClientRequest>();

    public static void main(String[] args) throws IOException, InterruptedException, SQLException {
        final Students students = ConfigLoader.loadConfig(Students.class, studentsToBeInserted);
        final NetworkConfig networkConfig = ConfigLoader.loadConfig(NetworkConfig.class, networkConfigFile);
        logger.debug("Load Network config file: {}", networkConfig.toString());

        ObjectMapper objectMapper = new ObjectMapper();
        int birthdate = 0;
        for (Student student : students.getStudents()) {
            final Request request = new Request();
            request.setRequestId(UUID.randomUUID().toString());
            request.setRequestOrder(requestOrder);
            String studentJson = objectMapper.writeValueAsString(student);
            request.setRequestContent(studentJson);
            final byte []  requestBytes = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(request);

            final InsertStudentsClientRequest clientRequest = new InsertStudentsClientRequest(
                networkConfig,
                birthdate++,
                request,
                student,
                requestBytes);
        clientRequests.push(clientRequest);
        }

       
        while (!clientRequests.isEmpty()) {
            final ClientRequest<?, ?> request = clientRequests.pop();
            if (request instanceof InsertStudentsClientRequest) {
                InsertStudentsClientRequest clientRequest = (InsertStudentsClientRequest) request;
                clientRequest.join();
                final Student student = clientRequest.getInfo();
                logger.debug("Thread {} complete: {} {} {} --> {}",
                        clientRequest.getThreadName(),
                        student.getFirstname(), student.getName(), student.getGroup(),
                        "Etudiant insere avec succes.");
            } else {
                // Handle unexpected type in the queue
                logger.error("Unexpected type in clientRequests queue");
            }
        }
}
}