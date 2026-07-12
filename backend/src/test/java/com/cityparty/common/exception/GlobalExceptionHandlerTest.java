package com.cityparty.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void businessErrorUsesMatchingHttpStatus() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        MockHttpServletResponse response = new MockHttpServletResponse();

        var result = handler.handleBusinessException(new BusinessException(403, "forbidden"), response);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(result.getCode()).isEqualTo(403);
    }

    @Test
    void maxUploadSizeExceededReturnsClientError() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var result = handler.handleMaxUploadSizeExceededException(new MaxUploadSizeExceededException(5));

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).contains("size limit");
    }
}
