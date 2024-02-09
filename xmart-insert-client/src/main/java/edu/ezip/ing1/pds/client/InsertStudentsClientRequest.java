package edu.ezip.ing1.pds.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ezip.ing1.pds.business.dto.Student;
import edu.ezip.ing1.pds.client.commons.ClientRequest;
import edu.ezip.ing1.pds.client.commons.NetworkConfig;
import edu.ezip.ing1.pds.commons.Request;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.util.Map;

public class InsertStudentsClientRequest extends ClientRequest<Student, String> {

    // Constructor
    public InsertStudentsClientRequest(NetworkConfig networkConfig, int myBirthDate, Request request, Student info, byte[] bytes)
            throws IOException {
        super(networkConfig, myBirthDate, request, info, bytes);
    }

    @Override
    public String readResult(String body) throws IOException {
        // Use ObjectMapper to deserialize the response and extract the student ID or handle the error message
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> responseMap = mapper.readValue(body, new TypeReference<Map<String, String>>() {});
        
        if (responseMap.containsKey("student_id")) {
            return responseMap.get("student_id");
        } else if (responseMap.containsKey("error")) {
            return responseMap.get("error");
        } else {
            // Handle unknown response format
            return "Unknown response format";
        }
    }

    public byte[] buildRequestData() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        // "info" is a variable inherited from the parent class ClientRequest
        return mapper.writeValueAsBytes(getInfo());
    }
}