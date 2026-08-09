package fu.sap490.g23.backend.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AttachmentUploadRequestSizeFilterTest {

    private final AttachmentUploadRequestSizeFilter filter = new AttachmentUploadRequestSizeFilter();

    @Test
    void blocksOversizedStudentAttachmentBeforeMultipartParsing() throws Exception {
        MockHttpServletRequest request = spy(new MockHttpServletRequest("POST", "/api/student/classrooms/homework/attachments"));
        when(request.getContentLengthLong()).thenReturn(22L * 1024 * 1024);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(413, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void allowsAttachmentRequestWithinLimit() throws Exception {
        MockHttpServletRequest request = spy(new MockHttpServletRequest("POST", "/api/student/classrooms/15/tuition-proofs"));
        when(request.getContentLengthLong()).thenReturn(10L * 1024 * 1024);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
