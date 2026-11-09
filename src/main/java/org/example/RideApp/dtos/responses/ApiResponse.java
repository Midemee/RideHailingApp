package org.example.RideApp.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class ApiResponse {
    private boolean successful;
    private Object data;
}
