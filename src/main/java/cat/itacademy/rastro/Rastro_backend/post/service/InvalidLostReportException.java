package cat.itacademy.rastro.Rastro_backend.post.service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class InvalidLostReportException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public InvalidLostReportException(Map<String, String> fieldErrors) {
        super("The lost report request is invalid");
        this.fieldErrors = Collections.unmodifiableMap(new LinkedHashMap<>(fieldErrors));
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
