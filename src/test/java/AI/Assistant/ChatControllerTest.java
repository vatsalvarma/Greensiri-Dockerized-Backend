package AI.Assistant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ChatService chatService;

    @Test
    void getWithoutMessageReturnsUsageInstructions() throws Exception {
        mockMvc.perform(get("/api/chat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.message").value(
                        "Use POST /api/chat with JSON {\"message\":\"Hello\"} or GET /api/chat?message=Hello"));
    }

    @Test
    void getWithMessageReturnsReply() throws Exception {
        given(chatService.chat("Hello")).willReturn("Hi there");

        mockMvc.perform(get("/api/chat").param("message", "Hello"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hi there"));
    }

    @Test
    void postWithValidBodyReturnsReply() throws Exception {
        given(chatService.chat("Hello")).willReturn("Hi there");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hi there"));
    }

    @Test
    void postWithEmptyMessageReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("message must not be empty"));
    }

    @Test
    void upstreamFailureReturnsBadGateway() throws Exception {
        doThrow(new IllegalStateException("invalid_api_key")).when(chatService).chat("Hello");

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Chat provider request failed"))
                .andExpect(jsonPath("$.details").value("invalid_api_key"));
    }
}
