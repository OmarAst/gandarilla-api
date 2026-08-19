package com.riogandarilla.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AppExceptionHandlerTest {

    @Test
    void shouldClassifyUnexpectedIllegalArgumentAsInternalError() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailingController())
                .setControllerAdvice(new AppExceptionHandler())
                .build();

        mvc.perform(get("/failure").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.meta.statusCode").value(500))
                .andExpect(jsonPath("$.data.code").value("INTERNAL_ERROR"));
    }

    @RestController
    static class FailingController {
        @GetMapping("/failure")
        String fail() {
            throw new IllegalArgumentException("detalle interno que no debe exponerse");
        }
    }
}
