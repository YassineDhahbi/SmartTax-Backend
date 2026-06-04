package tn.esprit.arabsoftback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    @JsonProperty("to")
    private String to;
    
    @JsonProperty("subject")
    private String subject;
    
    @JsonProperty("body")
    private String body;
    
    @JsonProperty("securityCode")
    private String securityCode;
    
    @JsonProperty("registrationLink")
    private String registrationLink;
}
