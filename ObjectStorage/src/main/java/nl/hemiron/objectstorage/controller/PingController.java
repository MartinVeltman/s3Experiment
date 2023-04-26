package nl.hemiron.objectstorage.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("ping")
@Tag(name = "PingController", description = "Controller with single endpoint to check if the application is running")
public class PingController {

    @GetMapping()
    @Operation(summary = "Endpoint to check if the application is running", responses = {
            @ApiResponse(responseCode = "200", description = "Succesful operation")
    })
    public String ping() {
        return "Pong!";
    }
}