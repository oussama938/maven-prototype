package edu.ezip.ing1.pds.business.server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ezip.ing1.pds.business.dto.Student;
import edu.ezip.ing1.pds.business.dto.Students;
import edu.ezip.ing1.pds.commons.Request;
import edu.ezip.ing1.pds.commons.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class XMartCityService {

    private final static String LoggingLabel = "B u s i n e s s - S e r v e r";
    private final Logger logger = LoggerFactory.getLogger(LoggingLabel);
    private Response response; 
    private enum Queries {
        SELECT_ALL_STUDENTS("SELECT t.name, t.firstname, t.group FROM \"ezip-ing1\".students t"),
        INSERT_STUDENT("INSERT into \"ezip-ing1\".students (\"name\", \"firstname\", \"group\") values (?, ?, ?)");
        private final String query;

        private Queries(final String query) {
            this.query = query;
        }
    }

    public static XMartCityService inst = null;
    public static final XMartCityService getInstance()  {
        if(inst == null) {
            inst = new XMartCityService();
        }
        return inst;
    }

    private XMartCityService() {

    }

    public final Response dispatch(final Request request, final Connection connection)
            throws InvocationTargetException, IllegalAccessException, JsonProcessingException {
             
                if (request == null || request.getRequestOrder() == null) {
                    logger.error("Requête invalide reçue : {}", request);
                   // Recherchez le code qui génère une réponse d'erreur
                    return new Response("error", "Request or request order cannot be null");

                }
                
        // R�cup�rer le type d'op�ration demand� dans la requ�te
        String operation = request.getRequestOrder();

        // Dispatcher en fonction du type d'op�ration
        switch (operation) {
            case "INSERT_STUDENT":
                response = handleInsertStudent(request, connection);
                break;

            case "SELECT_ALL_STUDENTS":
                response = handleSelectAllStudents(request, connection);
                break;
            // Ajoutez d'autres cas pour d'autres op�rations si n�cessaire

            default:
                // Op�ration non prise en charge
                return new Response(request.getRequestId(), "\"Opération non supportée\"");
        }

        return response;
    }


    // Handle the insertion of a student
// Handle the insertion of a student
public Response handleInsertStudent(Request request, Connection connection) throws JsonProcessingException {
    try {
        ObjectMapper mapper = new ObjectMapper();
        Student student = mapper.readValue(request.getRequestBody(), Student.class);

        // Exécuter la requête d'insertion dans la base de données
        try (PreparedStatement preparedStatement = connection.prepareStatement(Queries.INSERT_STUDENT.query, Statement.RETURN_GENERATED_KEYS)) {
            Map<String, Integer> columnIndexes = new HashMap<>();
            columnIndexes.put("name", 1);
            columnIndexes.put("firstname", 2);
            columnIndexes.put("group", 3);

            preparedStatement.setString(columnIndexes.get("name"), student.getName());
            preparedStatement.setString(columnIndexes.get("firstname"), student.getFirstname());
            preparedStatement.setString(columnIndexes.get("group"), student.getGroup());

            
            
            preparedStatement.executeUpdate();

            // Récupérer l'ID généré pour le retourner dans la réponse
            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int studentId = generatedKeys.getInt(1);
                    // Construire une réponse appropriée avec l'ID généré
                    return new Response(request.getRequestId(), "Étudiant inséré avec succès. ID de l'étudiant : " + studentId);
                }
            }
        }
        
        return new Response(request.getRequestId(), "Etudiant insere avec succes");
    } catch (Exception e) {
        // En cas d'erreur, renvoyer une réponse avec le message d'erreur
        logger.error("", e.getMessage());
        Map<String, String> responseBody = new LinkedHashMap<>();
        responseBody.put("", "");
        String responseBodyJson = new ObjectMapper().writeValueAsString(responseBody);
        return new Response(request.getRequestId(), responseBodyJson);
    }
}


public Response handleSelectAllStudents(Request request, Connection connection) throws JsonProcessingException {
    System.out.println("hola"+request);
    try (Statement statement = connection.createStatement()) {
        // Execute  the SQL query to select all students
        ResultSet resultSet = statement.executeQuery(Queries.SELECT_ALL_STUDENTS.query);

        // Construct the response with the retrieved students
        Students students = new Students();
        while (resultSet.next()) {
            String name = resultSet.getString("name");
            String firstname = resultSet.getString("firstname");
            String group = resultSet.getString("group");
            students.add(new Student(name, firstname, group));
        }   
        ObjectMapper mapper = new ObjectMapper();
        String responseBody = mapper.writeValueAsString(students);
        return new Response(request.getRequestId(), responseBody);
    } catch (SQLException e) {
        logger.error("Error occurred during student selection: {}", e.getMessage());
        Map<String, String> responseBody = new LinkedHashMap<>();
        responseBody.put("error", "Error occurred during student selection");
        String responseBodyJson = new ObjectMapper().writeValueAsString(responseBody);
        return new Response(request.getRequestId(), responseBodyJson);
    }
}
}